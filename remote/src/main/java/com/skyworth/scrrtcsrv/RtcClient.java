package com.skyworth.scrrtcsrv;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Loggable;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class RtcClient {
    private static final String TAG = "TR369 RtcClient";
    private static final int MAX_PEERS = 2;

    private PeerConnectionFactory mFactory;
    private MediaStream mLocalMediaStream;
    private VideoSource mVideoSource;
    private VideoCapturer mVideoCapturer;
    private boolean mIsCapturerStarted;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoFrameRate;
    private SurfaceTextureHelper mSurfaceHelper;
    private List<PeerConnection.IceServer> mIceServers;
    // TODO: protect this map, maybe not necessary
    private HashMap<String, Peer> mPeers = new HashMap<>();
    private PeerObserver mPeerObserver;
    private MediaConstraints mPeerConstraints;
    private EglBase mRootEglBase;
    private Handler mMainHandler;

    public void init(Context context, Intent intent, MediaOptions options,
                     List<PeerConnection.IceServer> iceServers, PeerObserver peerObserver) {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(true)
                        .setInjectableLogger(mRtcLogger, Logging.Severity.LS_INFO)
                        .createInitializationOptions()
        );
        mRootEglBase = EglBase.create();
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
                mRootEglBase.getEglBaseContext(), true, false);
        for (org.webrtc.VideoCodecInfo codecInfo : encoderFactory.getSupportedCodecs()) {
            if (codecInfo.name.equals("H264")) {
                Log.d(TAG, "Supported video encoder: " + codecInfo.name +
                        "(" + codecInfo.params.get(VideoCodecInfo.H264_FMTP_PROFILE_LEVEL_ID) + ")");
            } else {
                Log.d(TAG, "Supported video encoder: " + codecInfo.name);
            }
        }
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(
                mRootEglBase.getEglBaseContext());
        mFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        mIceServers = iceServers;
        mPeerObserver = peerObserver;

        mVideoWidth = options.videoWidth;
        mVideoHeight = options.videoHeight;
        mVideoFrameRate = options.videoFrameRate;

        mPeerConstraints = new MediaConstraints();
        mPeerConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        mPeerConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mPeerConstraints.mandatory.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        initScreenCaptureStream(context, intent);

        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public void destroy() {
        mPeers.forEach((key, value) -> {
            value.pc.close();
        });
        if (mLocalMediaStream != null) {
            mLocalMediaStream.dispose();
            mLocalMediaStream = null;
        }
        if (mSurfaceHelper != null) {
            mSurfaceHelper.dispose();
            mSurfaceHelper = null;
        }
        if (mVideoSource != null) {
            mVideoSource.dispose();
            mVideoSource = null;
        }
        if (mVideoCapturer != null) {
            mVideoCapturer.dispose();
            mVideoCapturer = null;
        }
        if (mFactory != null) {
            mFactory.dispose();
            mFactory = null;
        }
        if (mRootEglBase != null) {
            mRootEglBase.release();
            mRootEglBase = null;
        }
        mPeerConstraints = null;
        mMainHandler = null;
    }

    public boolean incoming(String peerId) {
        if (mPeers.containsKey(peerId)) {
            return false;
        }
        if (mPeers.size() == MAX_PEERS) {
            Log.e(TAG, "reached max peers " + mPeers.size());
            return false;
        }

        Peer peer = new Peer(peerId);
        mPeers.put(peerId, peer);

        return true;
    }

    public boolean answer(String peerId, String sdp) {
        Peer peer = mPeers.get(peerId);
        if (peer == null) {
            Log.e(TAG, "peer id " + peerId + " doesn't exist");
            return false;
        }

        peer.createAnswer(sdp);

        return true;
    }

    public boolean remoteAnswer(String peerId, String sdp) {
        Peer peer = mPeers.get(peerId);
        if (peer == null) {
            Log.e(TAG, "peer id " + peerId + " doesn't exist");
            return false;
        }

        peer.setRemoteAnswer(sdp);

        return true;
    }

    public boolean addCandidate(String peerId, String id, int label, String candidate) {
        Peer peer = mPeers.get(peerId);
        if (peer == null) {
            Log.e(TAG, "peer id " + peerId + " doesn't exist");
            return false;
        }

        return peer.addCandidate(id, label, candidate);
    }

    public interface PeerObserver {
        void onOfferCreated(String peerId, SessionDescription sdp);

        void onIceCandidate(String peerId, IceCandidate candidate);

        void onControl(String peerId, String message);

        void onStart();

        void onStop();
    }

    public void removePeer(String peerId) {
        Peer peer = mPeers.get(peerId);
        if (peer != null) {
            removePeer(peer);
        }
    }

    public void removeAllPeers() {
        mPeers.forEach((key, value) -> {
            value.pc.close();
        });
        mPeers.clear();
    }

    private void removePeer(Peer peer) {
        peer.pc.close();
        mPeers.remove(peer.id);
    }

    private class Peer implements SdpObserver, PeerConnection.Observer {
        PeerConnection pc;
        DataChannel dc;
        String id;

        public Peer(String id) {
            Log.d(TAG, "new Peer: " + id);
            this.pc = mFactory.createPeerConnection(mIceServers, this);
            DataChannel.Init init = new DataChannel.Init();
            init.id = 0;
            init.negotiated = false;
            this.dc = this.pc.createDataChannel("datachannel-ctrl", init);
            if (this.dc == null) {
                Log.e(TAG, "createDataChannel failed");
            } else {
                Log.d(TAG, "createDataChannel success");
                this.dc.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {
                        Log.d(TAG, "onBufferedAmountChange " + l);
                    }

                    @Override
                    public void onStateChange() {
                        DataChannel.State state = Peer.this.dc.state();
                        Log.d(TAG, "onStateChange " + state);
                        switch (state) {
                            case OPEN:
                                startCapture();
                                mPeerObserver.onStart();
                                break;
                            case CLOSED:
                                stopCapture();
                                mPeerObserver.onStop();
                                break;
                        }
                    }

                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        try {
                            String message = StandardCharsets.UTF_8.decode(buffer.data).toString();
                            Log.d(TAG, "onMessage " + message);
                            mPeerObserver.onControl(Peer.this.id, message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            this.id = id;
            this.pc.addStream(mLocalMediaStream);
            this.pc.createOffer(this, mPeerConstraints);
        }

        public void createAnswer(String sdp) {
            pc.setRemoteDescription(this, new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm("answer"), sdp));
            pc.createAnswer(this, mPeerConstraints);
        }

        public void setRemoteAnswer(String sdp) {
            pc.setRemoteDescription(this, new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm("answer"), sdp));
        }

        public boolean addCandidate(String id, int label, String candidate) {
            if (pc.getRemoteDescription() == null) {
                return false;
            }

            return pc.addIceCandidate(new IceCandidate(id, label, candidate));
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                Ln.w(id + " has been disconnected");
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        removePeer(Peer.this);
                    }
                });
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            mPeerObserver.onIceCandidate(id, iceCandidate);
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "onAddStream " + mediaStream.getId());
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "onRemoveStream " + mediaStream.getId());
            removePeer(this);
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG, "onDataChannel " + dataChannel.label());
        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

        }

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            // TODO: modify sdp to prefer
            mPeerObserver.onOfferCreated(this.id, sessionDescription);
            pc.setLocalDescription(this, sessionDescription);
        }

        @Override
        public void onSetSuccess() {

        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {

        }
    }

    public static class MediaOptions {
        final int videoWidth;
        final int videoHeight;
        final int videoFrameRate;

        private MediaOptions(int videoWidth, int videoHeight, int videoFrameRate) {
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.videoFrameRate = videoFrameRate;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int videoWidth;
            private int videoHeight;
            private int videoFrameRate;

            public Builder setVideoSize(int width, int height) {
                this.videoWidth = width;
                this.videoHeight = height;
                return this;
            }

            public Builder setVideoSize(Size size) {
                return setVideoSize(size.getWidth(), size.getHeight());
            }

            public Builder setFrameRate(int fps) {
                this.videoFrameRate = fps;
                return this;
            }

            public MediaOptions createMediaOptions() {
                return new MediaOptions(this.videoWidth, this.videoHeight, this.videoFrameRate);
            }
        }
    }

    private void initScreenCaptureStream(Context context, Intent intent) {
        // ScreenCapturerAndroid
        mVideoCapturer = new ScreenCapturer(intent, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.e(TAG, "User revoked permission to capture the screen");
                super.onStop();
            }
        });

        mVideoSource = mFactory.createVideoSource(mVideoCapturer.isScreencast());
        mSurfaceHelper = SurfaceTextureHelper.create(
                "sThread", mRootEglBase.getEglBaseContext());
        mVideoCapturer.initialize(mSurfaceHelper, context, mVideoSource.getCapturerObserver());
        // mVideoCapturer.startCapture(mVideoWidth, mVideoHeight, mVideoFrameRate);

        VideoTrack videoTrack = mFactory.createVideoTrack("SCRMSv0", mVideoSource);
        videoTrack.setEnabled(true);

        mLocalMediaStream = mFactory.createLocalMediaStream("SCRMS");
        mLocalMediaStream.addTrack(videoTrack);
    }

    private synchronized void startCapture() {
        if (mIsCapturerStarted) {
            return;
        }
        mIsCapturerStarted = true;
        mVideoCapturer.startCapture(mVideoWidth, mVideoHeight, mVideoFrameRate);
    }

    private synchronized void stopCapture() {
        if (!mIsCapturerStarted) {
            return;
        }
        mIsCapturerStarted = false;
        try {
            mVideoCapturer.stopCapture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Loggable mRtcLogger = new Loggable() {
        @Override
        public void onLogMessage(String s, Logging.Severity severity, String s1) {
            s = s1 + s.trim();
            switch (severity) {
                case LS_VERBOSE:
                    Log.v(TAG, s);
                    break;
                case LS_INFO:
                    Log.i(TAG, s);
                    break;
                case LS_WARNING:
                    Log.w(TAG, s);
                    break;
                case LS_ERROR:
                    Log.e(TAG, s);
                    break;
                case LS_NONE:
                default:
                    Log.d(TAG, s);
                    break;
            }
        }
    };
}
