package com.skyworth.scrrtcsrv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;

public class RtcService extends Service {
    private static final String TAG = "TR369 RtcService";
    private static final Pattern PATTERN_TURN = Pattern.compile(
            "^turn:(.+):(.+)@(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SIZE = Pattern.compile(
            "^([0-9]+)x([0-9]+)$");

    private SocketIOClient mSocket;
    private RtcClient mRtcClient;
    private Device mDevice;
    private Controller mController;
    private String mDeviceName;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //支持多次启动，后面启动的Service将停止前面启动的Service
        if (mDevice != null || mRtcClient != null || mController != null) {
            destroy();
        }

        Bundle bundle = intent.getExtras();
        String url = bundle.getString(getString(R.string.socketio_url));
        ArrayList<String> ice_servers = bundle.getStringArrayList(getString(R.string.ice_servers));
        String video_size = bundle.getString(getString(R.string.video_size));
        int video_fps = bundle.getInt(getString(R.string.video_fps));
        int video_width = 640;
        int video_height = 360;

        mDeviceName = bundle.getString(getString(R.string.dev_name));
        Log.d(TAG, "device name: " + mDeviceName);
        Log.d(TAG, "video fps: " + video_fps);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final String channelId = "ScreenCapture1";
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Foreground notification", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(
                    getApplicationContext(), channelId).build();

            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }

        if (video_size != null && !video_size.isEmpty()) {
            Log.d(TAG, "set video size:" + video_size);
            Matcher matcher = PATTERN_SIZE.matcher(video_size);
            if (matcher.matches()) {
                try {
                    video_width = Integer.parseInt(matcher.group(1));
                    video_height = Integer.parseInt(matcher.group(2));
                    Log.d(TAG, "got video size: " + video_width + "x" + video_height);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "invalid video size: " + video_size);
            }
        }

        // TODO: configure options
        Options options = new Options();
        mDevice = new Device(options);
        mDevice.setMaxSize(Math.max(video_width, video_height));
        Size videoSize = mDevice.getScreenInfo().getVideoSize();
        Log.d(TAG, "real video size: " + videoSize.getWidth() + "x" + videoSize.getHeight());

        try {

            LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
            if (ice_servers != null) {
                ice_servers.forEach((item) -> {
                    if (item.startsWith("turn:")) {
                        // turn:user:pass@host:port
                        Matcher matcher = PATTERN_TURN.matcher(item);
                        if (matcher.matches()) {
                            iceServers.add(PeerConnection.IceServer
                                    .builder(matcher.group(3))
                                    .setUsername(matcher.group(1))
                                    .setPassword(matcher.group(2))
                                    .createIceServer());
                        } else {
                            Log.e(TAG, "invalid turn uri: " + item);
                        }
                    } else {
                        iceServers.add(PeerConnection.IceServer
                                .builder(item)
                                .createIceServer());
                    }
                });
            }
            iceServers.add(PeerConnection.IceServer
                    .builder("stun:stun.l.google.com:19302")
                    .createIceServer());

            mRtcClient = new RtcClient();
            mRtcClient.init(this, intent,
                    RtcClient.MediaOptions.builder()
                            .setVideoSize(videoSize)
                            .setFrameRate(video_fps)
                            .createMediaOptions(),
                    iceServers, mPeerObserver);

            mSocket = new SocketIOClient(url, mSocketCallback);
            Log.d(TAG, "socketio connecting to " + url);
            mSocket.connect();

            mController = new Controller(mDevice,
                    options.getClipboardAutosync(), options.getPowerOn());
            mController.start();
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        destroy();
        super.onDestroy();
    }

    private void destroy() {
        if (mRtcClient != null) {
            mRtcClient.destroy();
            mRtcClient = null;
        }
        if (mController != null) {
            mController.stop();
            mController = null;
        }
        mDevice = null;
    }

    private final RtcClient.PeerObserver mPeerObserver = new RtcClient.PeerObserver() {
        @Override
        public void onOfferCreated(String peerId, SessionDescription sdp) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("type", sdp.type.canonicalForm());
                payload.put("sdp", sdp.description);
                mSocket.sendMessage(peerId, sdp.type.canonicalForm(), payload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onIceCandidate(String peerId, IceCandidate candidate) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", candidate.sdpMid);
                payload.put("label", candidate.sdpMLineIndex);
                payload.put("candidate", candidate.sdp);
                mSocket.sendMessage(peerId, "candidate", payload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onControl(String peerId, String message) {
            ControlMessage msg = ControlMessageReader.parse(message);
            if (msg != null && mController != null) {
                mController.handleEvent(msg);
            }
        }

        @Override
        public void onStart() {
            Log.d(TAG, "peer connected...");
        }

        @Override
        public void onStop() {
            Log.d(TAG, "peer disconnected, stop RtcService");
            stopSelf();
        }
    };

    private final SocketIOClient.Callback mSocketCallback = new SocketIOClient.Callback() {
        @Override
        public void onRegistered(String id) {
            Log.d(TAG, id + " registered");
            try {
                JSONObject message = new JSONObject();
                if (mDeviceName != null && !mDeviceName.isEmpty()) {
                    message.put("name", mDeviceName);
                } else {
                    message.put("name", "Android-" + id);
                }
                mSocket.send("readyToStream", message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCall(String peerId) {
            mRtcClient.incoming(peerId);
        }

        @Override
        public void onOffer(String peerId, JSONObject payload) {
            mRtcClient.answer(peerId, payload.optString("sdp"));
        }

        @Override
        public void onAnswer(String peerId, JSONObject payload) {
            mRtcClient.remoteAnswer(peerId, payload.optString("sdp"));
        }

        @Override
        public void onCandidate(String peerId, JSONObject payload) {
            mRtcClient.addCandidate(peerId,
                    payload.optString("id"),
                    payload.optInt("lable"),
                    payload.optString("candidate"));
        }

        @Override
        public void onControl(String peerId, JSONObject payload) {
            ControlMessage msg = ControlMessageReader.parse(payload);
            if (msg != null && mController != null) {
                mController.handleEvent(msg);
            }
        }

        @Override
        public void onDisconnected() {
            Log.e(TAG, "socketio disconnected");
            mRtcClient.removeAllPeers();
            stopSelf();
        }

        @Override
        public void onError(String reason) {
            Log.e(TAG, "socketio error: " + reason);
            stopSelf();
        }
    };
}
