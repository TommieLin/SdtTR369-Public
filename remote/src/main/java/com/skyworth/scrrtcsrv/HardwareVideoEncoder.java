package com.skyworth.scrrtcsrv;

import android.graphics.Matrix;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.opengl.GLES20;
import android.os.Bundle;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.webrtc.EglBase;
import org.webrtc.EglBase14;
import org.webrtc.EglBase14.Context;
import org.webrtc.EncodedImage;
import org.webrtc.EncodedImage.Builder;
import org.webrtc.EncodedImage.FrameType;
import org.webrtc.GlRectDrawer;
import org.webrtc.Logging;
import org.webrtc.ThreadUtils;
import org.webrtc.ThreadUtils.ThreadChecker;
import org.webrtc.VideoCodecStatus;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoFrame;
import org.webrtc.VideoFrame.Buffer;
import org.webrtc.VideoFrame.I420Buffer;
import org.webrtc.VideoFrame.TextureBuffer;
import org.webrtc.VideoFrameDrawer;
import org.webrtc.YuvHelper;

import androidx.annotation.Nullable;

class HardwareVideoEncoder implements VideoEncoder {
    private static final String TAG = "HardwareVideoEncoder";
    private static final int VIDEO_ControlRateConstant = 2;
    private static final String KEY_BITRATE_MODE = "bitrate-mode";
    private static final int VIDEO_AVC_PROFILE_HIGH = 8;
    private static final int VIDEO_AVC_LEVEL_3 = 256;
    private static final int MAX_VIDEO_FRAMERATE = 30;
    private static final int MAX_ENCODER_Q_SIZE = 2;
    private static final int MEDIA_CODEC_RELEASE_TIMEOUT_MS = 5000;
    private static final int DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US = 100000;
    private final MediaCodecWrapperFactory mediaCodecWrapperFactory;
    private final String codecName;
    private final VideoCodecMimeType codecType;
    private final Integer surfaceColorFormat;
    private final Integer yuvColorFormat;
    private final HardwareVideoEncoder.YuvFormat yuvFormat;
    private final Map<String, String> params;
    private final int keyFrameIntervalSec;
    private final long forcedKeyFrameNs;
    private final BitrateAdjuster bitrateAdjuster;
    private final Context sharedContext;
    private final GlRectDrawer textureDrawer = new GlRectDrawer();
    private final VideoFrameDrawer videoFrameDrawer = new VideoFrameDrawer();
    private final BlockingDeque<Builder> outputBuilders = new LinkedBlockingDeque();
    private final ThreadChecker encodeThreadChecker = new ThreadChecker();
    private final ThreadChecker outputThreadChecker = new ThreadChecker();
    private final HardwareVideoEncoder.BusyCount outputBuffersBusyCount = new HardwareVideoEncoder.BusyCount();
    private Callback callback;
    private boolean automaticResizeOn;
    @Nullable
    private MediaCodecWrapper codec;
    @Nullable
    private ByteBuffer[] outputBuffers;
    @Nullable
    private Thread outputThread;
    @Nullable
    private EglBase14 textureEglBase;
    @Nullable
    private Surface textureInputSurface;
    private int width;
    private int height;
    private boolean useSurfaceMode;
    private long lastKeyFrameNs;
    @Nullable
    private ByteBuffer configBuffer;
    private int adjustedBitrate;
    private volatile boolean running;
    @Nullable
    private volatile Exception shutdownException;

    public HardwareVideoEncoder(MediaCodecWrapperFactory mediaCodecWrapperFactory,
                                String codecName, VideoCodecMimeType codecType,
                                Integer surfaceColorFormat, Integer yuvColorFormat,
                                Map<String, String> params, int keyFrameIntervalSec,
                                int forceKeyFrameIntervalMs, BitrateAdjuster bitrateAdjuster,
                                Context sharedContext) {
        this.mediaCodecWrapperFactory = mediaCodecWrapperFactory;
        this.codecName = codecName;
        this.codecType = codecType;
        this.surfaceColorFormat = surfaceColorFormat;
        this.yuvColorFormat = yuvColorFormat;
        this.yuvFormat = YuvFormat.valueOf(yuvColorFormat);
        this.params = params;
        this.keyFrameIntervalSec = keyFrameIntervalSec;
        this.forcedKeyFrameNs = TimeUnit.MILLISECONDS.toNanos(forceKeyFrameIntervalMs);
        this.bitrateAdjuster = bitrateAdjuster;
        this.sharedContext = sharedContext;

        // Allow construction on a different thread.
        this.encodeThreadChecker.detachThread();
    }

    public VideoCodecStatus initEncode(Settings settings, Callback callback) {
        this.encodeThreadChecker.checkIsOnValidThread();
        this.callback = callback;
        this.automaticResizeOn = settings.automaticResizeOn;
        this.width = settings.width;
        this.height = settings.height;
        this.useSurfaceMode = this.canUseSurface();
        if (settings.startBitrate != 0 && settings.maxFramerate != 0) {
            this.bitrateAdjuster.setTargets(settings.startBitrate * 1000, settings.maxFramerate);
        }

        this.adjustedBitrate = this.bitrateAdjuster.getAdjustedBitrateBps();
        Logging.d(TAG, "initEncode: " + this.width + " x " + this.height + ". @ " +
                settings.startBitrate + "kbps. Fps: " + settings.maxFramerate + " Use surface mode: " + this.useSurfaceMode);
        return this.initEncodeInternal();
    }

    private VideoCodecStatus initEncodeInternal() {
        this.encodeThreadChecker.checkIsOnValidThread();
        this.lastKeyFrameNs = -1L;

        try {
            this.codec = this.mediaCodecWrapperFactory.createByCodecName(this.codecName);
        } catch (IllegalArgumentException | IOException var6) {
            Logging.e(TAG, "Cannot create media encoder " + this.codecName);
            return VideoCodecStatus.FALLBACK_SOFTWARE;
        }

        if (this.codec == null) {
            Logging.e(TAG, "createByCodecName returns a null pointer");
            return VideoCodecStatus.FALLBACK_SOFTWARE;
        }

        int colorFormat = this.useSurfaceMode ? this.surfaceColorFormat : this.yuvColorFormat;

        try {
            MediaFormat format = MediaFormat.createVideoFormat(this.codecType.mimeType(), this.width, this.height);
            format.setInteger(MediaFormat.KEY_BIT_RATE, this.adjustedBitrate);
            format.setInteger(KEY_BITRATE_MODE, VIDEO_ControlRateConstant);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, this.bitrateAdjuster.getCodecConfigFramerate());
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, this.keyFrameIntervalSec);
            if (this.codecType == VideoCodecMimeType.H264) {
                String profileLevelId = this.params.get(H264Utils.H264_FMTP_PROFILE_LEVEL_ID);
                if (profileLevelId == null) {
                    profileLevelId = H264Utils.H264_CONSTRAINED_BASELINE_3_1;
                }

                byte var5 = -1;
                switch (profileLevelId.hashCode()) {
                    case 1537948542:
                        if (profileLevelId.equals(H264Utils.H264_CONSTRAINED_BASELINE_3_1)) {
                            var5 = 1;
                        }
                        break;
                    case 1595523974:
                        if (profileLevelId.equals(H264Utils.H264_CONSTRAINED_HIGH_3_1)) {
                            var5 = 0;
                        }
                }

                switch (var5) {
                    case 0:
                        format.setInteger(MediaFormat.KEY_PROFILE, VIDEO_AVC_PROFILE_HIGH);
                        format.setInteger(MediaFormat.KEY_LEVEL, VIDEO_AVC_LEVEL_3);
                    case 1:
                        break;
                    default:
                        Logging.w(TAG, "Unknown profile level id: " + profileLevelId);
                }
            }

            Logging.d(TAG, "Format: " + format);
            this.codec.configure(format, (Surface) null, (MediaCrypto) null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            if (this.useSurfaceMode) {
                this.textureEglBase = EglBase.createEgl14(this.sharedContext, EglBase.CONFIG_RECORDABLE);
                this.textureInputSurface = this.codec.createInputSurface();
                this.textureEglBase.createSurface(this.textureInputSurface);
                this.textureEglBase.makeCurrent();
            }

            this.codec.start();
            this.outputBuffers = this.codec.getOutputBuffers();
        } catch (IllegalStateException var7) {
            Logging.e(TAG, "initEncodeInternal failed", var7);
            this.release();
            return VideoCodecStatus.FALLBACK_SOFTWARE;
        }

        this.running = true;
        this.outputThreadChecker.detachThread();
        this.outputThread = this.createOutputThread();
        this.outputThread.start();
        return VideoCodecStatus.OK;
    }

    public VideoCodecStatus release() {
        this.encodeThreadChecker.checkIsOnValidThread();
        VideoCodecStatus returnValue;
        if (this.outputThread == null) {
            returnValue = VideoCodecStatus.OK;
        } else {
            this.running = false;
            if (!ThreadUtils.joinUninterruptibly(this.outputThread, MEDIA_CODEC_RELEASE_TIMEOUT_MS)) {
                Logging.e(TAG, "Media encoder release timeout");
                returnValue = VideoCodecStatus.TIMEOUT;
            } else if (this.shutdownException != null) {
                Logging.e(TAG, "Media encoder release exception", this.shutdownException);
                returnValue = VideoCodecStatus.ERROR;
            } else {
                returnValue = VideoCodecStatus.OK;
            }
        }

        this.textureDrawer.release();
        this.videoFrameDrawer.release();
        if (this.textureEglBase != null) {
            this.textureEglBase.release();
            this.textureEglBase = null;
        }

        if (this.textureInputSurface != null) {
            this.textureInputSurface.release();
            this.textureInputSurface = null;
        }

        this.outputBuilders.clear();
        this.codec = null;
        this.outputBuffers = null;
        this.outputThread = null;
        this.encodeThreadChecker.detachThread();
        return returnValue;
    }

    public VideoCodecStatus encode(VideoFrame videoFrame, EncodeInfo encodeInfo) {
        this.encodeThreadChecker.checkIsOnValidThread();
        if (this.codec == null) {
            return VideoCodecStatus.UNINITIALIZED;
        } else {
            Buffer videoFrameBuffer = videoFrame.getBuffer();
            boolean isTextureBuffer = videoFrameBuffer instanceof TextureBuffer;
            int frameWidth = videoFrame.getBuffer().getWidth();
            int frameHeight = videoFrame.getBuffer().getHeight();
            boolean shouldUseSurfaceMode = this.canUseSurface() && isTextureBuffer;
            if (frameWidth != this.width || frameHeight != this.height || shouldUseSurfaceMode != this.useSurfaceMode) {
                VideoCodecStatus status = this.resetCodec(frameWidth, frameHeight, shouldUseSurfaceMode);
                if (status != VideoCodecStatus.OK) {
                    return status;
                }
            }

            if (this.outputBuilders.size() > MAX_ENCODER_Q_SIZE) {
                Logging.e(TAG, "Dropped frame, encoder queue full");
                return VideoCodecStatus.NO_OUTPUT;
            } else {
                boolean requestedKeyFrame = false;
                FrameType[] var9 = encodeInfo.frameTypes;
                int var10 = var9.length;

                for (int var11 = 0; var11 < var10; ++var11) {
                    FrameType frameType = var9[var11];
                    if (frameType == FrameType.VideoFrameKey) {
                        requestedKeyFrame = true;
                    }
                }

                if (requestedKeyFrame || this.shouldForceKeyFrame(videoFrame.getTimestampNs())) {
                    this.requestKeyFrame(videoFrame.getTimestampNs());
                }

                int bufferSize = videoFrameBuffer.getHeight() * videoFrameBuffer.getWidth() * 3 / 2;
                Builder builder = EncodedImage.builder().setCaptureTimeNs(videoFrame.getTimestampNs()).setCompleteFrame(true).setEncodedWidth(videoFrame.getBuffer().getWidth()).setEncodedHeight(videoFrame.getBuffer().getHeight()).setRotation(videoFrame.getRotation());
                this.outputBuilders.offer(builder);
                VideoCodecStatus returnValue;
                if (this.useSurfaceMode) {
                    returnValue = this.encodeTextureBuffer(videoFrame);
                } else {
                    returnValue = this.encodeByteBuffer(videoFrame, videoFrameBuffer, bufferSize);
                }

                if (returnValue != VideoCodecStatus.OK) {
                    this.outputBuilders.pollLast();
                }

                return returnValue;
            }
        }
    }

    private VideoCodecStatus encodeTextureBuffer(VideoFrame videoFrame) {
        this.encodeThreadChecker.checkIsOnValidThread();

        try {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            VideoFrame derotatedFrame = new VideoFrame(videoFrame.getBuffer(), 0, videoFrame.getTimestampNs());
            this.videoFrameDrawer.drawFrame(derotatedFrame, this.textureDrawer, (Matrix) null);
            if (this.textureEglBase == null) {
                Logging.e(TAG, "encodeTextureBuffer textureEglBase null pointer");
                return VideoCodecStatus.ERROR;
            }
            this.textureEglBase.swapBuffers(videoFrame.getTimestampNs());
        } catch (RuntimeException var3) {
            Logging.e(TAG, "encodeTexture failed", var3);
            return VideoCodecStatus.ERROR;
        }

        return VideoCodecStatus.OK;
    }

    private VideoCodecStatus encodeByteBuffer(VideoFrame videoFrame, Buffer videoFrameBuffer, int bufferSize) {
        if (this.codec == null) {
            Logging.e(TAG, "encodeByteBuffer codec null pointer");
            return VideoCodecStatus.UNINITIALIZED;
        }
        this.encodeThreadChecker.checkIsOnValidThread();
        long presentationTimestampUs = (videoFrame.getTimestampNs() + 500L) / 1000L;

        int index;
        try {
            index = this.codec.dequeueInputBuffer(0L);
        } catch (IllegalStateException var11) {
            Logging.e(TAG, "dequeueInputBuffer failed", var11);
            return VideoCodecStatus.ERROR;
        }

        if (index == -1) {
            Logging.d(TAG, "Dropped frame, no input buffers available");
            return VideoCodecStatus.NO_OUTPUT;
        } else {
            ByteBuffer buffer;
            try {
                buffer = this.codec.getInputBuffers()[index];
            } catch (IllegalStateException var10) {
                Logging.e(TAG, "getInputBuffers failed", var10);
                return VideoCodecStatus.ERROR;
            }

            this.fillInputBuffer(buffer, videoFrameBuffer);

            try {
                this.codec.queueInputBuffer(index, 0, bufferSize, presentationTimestampUs, 0);
            } catch (IllegalStateException var9) {
                Logging.e(TAG, "queueInputBuffer failed", var9);
                return VideoCodecStatus.ERROR;
            }

            return VideoCodecStatus.OK;
        }
    }

    public VideoCodecStatus setRateAllocation(BitrateAllocation bitrateAllocation, int framerate) {
        this.encodeThreadChecker.checkIsOnValidThread();
        if (framerate > MAX_VIDEO_FRAMERATE) {
            framerate = MAX_VIDEO_FRAMERATE;
        }

        this.bitrateAdjuster.setTargets(bitrateAllocation.getSum(), framerate);
        return VideoCodecStatus.OK;
    }

    public ScalingSettings getScalingSettings() {
        this.encodeThreadChecker.checkIsOnValidThread();
        if (this.automaticResizeOn) {
            boolean kLowH264QpThreshold;
            boolean kHighH264QpThreshold;
            if (this.codecType == VideoCodecMimeType.VP8) {
                kLowH264QpThreshold = true;
                kHighH264QpThreshold = true;
                return new ScalingSettings(29, 95);
            }

            if (this.codecType == VideoCodecMimeType.H264) {
                kLowH264QpThreshold = true;
                kHighH264QpThreshold = true;
                return new ScalingSettings(24, 37);
            }
        }

        return ScalingSettings.OFF;
    }

    public String getImplementationName() {
        return "HWEncoder";
    }

    private VideoCodecStatus resetCodec(int newWidth, int newHeight, boolean newUseSurfaceMode) {
        this.encodeThreadChecker.checkIsOnValidThread();
        VideoCodecStatus status = this.release();
        if (status != VideoCodecStatus.OK) {
            return status;
        } else {
            this.width = newWidth;
            this.height = newHeight;
            this.useSurfaceMode = newUseSurfaceMode;
            return this.initEncodeInternal();
        }
    }

    private boolean shouldForceKeyFrame(long presentationTimestampNs) {
        this.encodeThreadChecker.checkIsOnValidThread();
        return this.forcedKeyFrameNs > 0L && presentationTimestampNs > this.lastKeyFrameNs + this.forcedKeyFrameNs;
    }

    private void requestKeyFrame(long presentationTimestampNs) {
        if (this.codec == null) {
            Logging.e(TAG, "requestKeyFrame codec null pointer");
            return;
        }
        this.encodeThreadChecker.checkIsOnValidThread();

        try {
            Bundle b = new Bundle();
            b.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
            this.codec.setParameters(b);
        } catch (IllegalStateException var4) {
            Logging.e(TAG, "requestKeyFrame failed", var4);
            return;
        }

        this.lastKeyFrameNs = presentationTimestampNs;
    }

    private Thread createOutputThread() {
        return new Thread() {
            public void run() {
                while (HardwareVideoEncoder.this.running) {
                    HardwareVideoEncoder.this.deliverEncodedImage();
                }

                HardwareVideoEncoder.this.releaseCodecOnOutputThread();
            }
        };
    }

    protected void deliverEncodedImage() {
        if (this.codec == null) {
            Logging.e(TAG, "deliverEncodedImage codec null pointer");
            return;
        }
        this.outputThreadChecker.checkIsOnValidThread();

        try {
            BufferInfo info = new BufferInfo();
            int index = this.codec.dequeueOutputBuffer(info, DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US);
            if (index < 0) {
                if (index == -3) {
                    this.outputBuffersBusyCount.waitForZero();
                    this.outputBuffers = this.codec.getOutputBuffers();
                }

                return;
            }

            if (this.outputBuffers == null) {
                Logging.e(TAG, "deliverEncodedImage outputBuffers null pointer");
                return;
            }
            ByteBuffer codecOutputBuffer = this.outputBuffers[index];
            codecOutputBuffer.position(info.offset);
            codecOutputBuffer.limit(info.offset + info.size);
            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                Logging.d(TAG, "Config frame generated. Offset: " + info.offset + ". Size: " + info.size);
                this.configBuffer = ByteBuffer.allocateDirect(info.size);
                this.configBuffer.put(codecOutputBuffer);
            } else {
                this.bitrateAdjuster.reportEncodedFrame(info.size);
                if (this.adjustedBitrate != this.bitrateAdjuster.getAdjustedBitrateBps()) {
                    this.updateBitrate();
                }

                boolean isKeyFrame = (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                if (isKeyFrame) {
                    Logging.d(TAG, "Sync frame generated");
                }

                ByteBuffer frameBuffer;
                if (isKeyFrame && this.codecType == VideoCodecMimeType.H264) {
                    if (this.configBuffer == null) {
                        Logging.e(TAG, "deliverEncodedImage configBuffer null pointer");
                        return;
                    }
                    Logging.d(TAG, "Prepending config frame of size " + this.configBuffer.capacity() +
                            " to output buffer with offset " + info.offset + ", size " + info.size);
                    frameBuffer = ByteBuffer.allocateDirect(info.size + this.configBuffer.capacity());
                    this.configBuffer.rewind();
                    frameBuffer.put(this.configBuffer);
                    frameBuffer.put(codecOutputBuffer);
                    frameBuffer.rewind();
                } else {
                    frameBuffer = codecOutputBuffer.slice();
                }

                FrameType frameType = isKeyFrame ? FrameType.VideoFrameKey : FrameType.VideoFrameDelta;
                this.outputBuffersBusyCount.increment();
                Builder builder = (Builder) this.outputBuilders.poll();
                if (builder == null) {
                    Logging.e(TAG, "deliverEncodedImage builder null pointer");
                    return;
                }
                EncodedImage encodedImage = builder.setBuffer(frameBuffer, () -> {
                    try {
                        this.codec.releaseOutputBuffer(index, false);
                    } catch (Exception var3) {
                        Logging.e(TAG, "releaseOutputBuffer failed", var3);
                    }

                    this.outputBuffersBusyCount.decrement();
                }).setFrameType(frameType).createEncodedImage();
                this.callback.onEncodedFrame(encodedImage, new CodecSpecificInfo());
                encodedImage.release();
            }
        } catch (IllegalStateException var9) {
            Logging.e(TAG, "deliverOutput failed", var9);
        }

    }

    private void releaseCodecOnOutputThread() {
        this.outputThreadChecker.checkIsOnValidThread();
        Logging.d(TAG, "Releasing MediaCodec on output thread");
        this.outputBuffersBusyCount.waitForZero();

        if (this.codec != null) {
            try {
                this.codec.stop();
            } catch (Exception var3) {
                Logging.e(TAG, "Media encoder stop failed", var3);
            }

            try {
                this.codec.release();
            } catch (Exception var2) {
                Logging.e(TAG, "Media encoder release failed", var2);
                this.shutdownException = var2;
            }
        } else {
            Logging.e(TAG, "Media encoder stop and release failed, codec null pointer");
        }

        this.configBuffer = null;
        Logging.d(TAG, "Release on output thread done");
    }

    private VideoCodecStatus updateBitrate() {
        if (this.codec == null) {
            Logging.e(TAG, "updateBitrate codec null pointer");
            return VideoCodecStatus.ERROR;
        }
        this.outputThreadChecker.checkIsOnValidThread();
        this.adjustedBitrate = this.bitrateAdjuster.getAdjustedBitrateBps();

        try {
            Bundle params = new Bundle();
            params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, this.adjustedBitrate);
            this.codec.setParameters(params);
            return VideoCodecStatus.OK;
        } catch (IllegalStateException var2) {
            Logging.e(TAG, "updateBitrate failed", var2);
            return VideoCodecStatus.ERROR;
        }
    }

    private boolean canUseSurface() {
        return this.sharedContext != null && this.surfaceColorFormat != null;
    }

    protected void fillInputBuffer(ByteBuffer buffer, Buffer videoFrameBuffer) {
        this.yuvFormat.fillBuffer(buffer, videoFrameBuffer);
    }

    private static enum YuvFormat {
        I420 {
            void fillBuffer(ByteBuffer dstBuffer, Buffer srcBuffer) {
                I420Buffer i420 = srcBuffer.toI420();
                YuvHelper.I420Copy(i420.getDataY(), i420.getStrideY(), i420.getDataU(),
                        i420.getStrideU(), i420.getDataV(), i420.getStrideV(), dstBuffer, i420.getWidth(), i420.getHeight());
                i420.release();
            }
        },
        NV12 {
            void fillBuffer(ByteBuffer dstBuffer, Buffer srcBuffer) {
                I420Buffer i420 = srcBuffer.toI420();
                YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataU(),
                        i420.getStrideU(), i420.getDataV(), i420.getStrideV(), dstBuffer, i420.getWidth(), i420.getHeight());
                i420.release();
            }
        };

        private YuvFormat() {
        }

        abstract void fillBuffer(ByteBuffer var1, Buffer var2);

        static HardwareVideoEncoder.YuvFormat valueOf(int colorFormat) {
            switch (colorFormat) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    return I420;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar:
                case MediaCodecUtils.COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m:
                    return NV12;
                default:
                    throw new IllegalArgumentException("Unsupported colorFormat: " + colorFormat);
            }
        }
    }

    private static class BusyCount {
        private final Object countLock;
        private int count;

        private BusyCount() {
            this.countLock = new Object();
        }

        public void increment() {
            synchronized (this.countLock) {
                ++this.count;
            }
        }

        public void decrement() {
            synchronized (this.countLock) {
                --this.count;
                if (this.count == 0) {
                    this.countLock.notifyAll();
                }

            }
        }

        public void waitForZero() {
            boolean wasInterrupted = false;
            synchronized (this.countLock) {
                while (this.count > 0) {
                    try {
                        this.countLock.wait();
                    } catch (InterruptedException var5) {
                        Logging.e(TAG, "Interrupted while waiting on busy count", var5);
                        wasInterrupted = true;
                    }
                }
            }

            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }

        }
    }
}

