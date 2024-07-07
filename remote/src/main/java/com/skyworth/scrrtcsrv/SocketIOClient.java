package com.skyworth.scrrtcsrv;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketIOClient {
    private static final String TAG = "TR369 SocketIOClient";
    private final Socket mSocket;
    private final Callback mCallback;
    private final MessageHandler mMessageHandler = new MessageHandler();

    public SocketIOClient(String url, Callback cb) throws URISyntaxException {
        mSocket = IO.socket(url);
        mCallback = cb;
        mSocket.on("id", mMessageHandler.onId);
        mSocket.on("message", mMessageHandler.onMessage);
        mSocket.on(Socket.EVENT_CONNECT, mMessageHandler.onConnected);
        mSocket.on(Socket.EVENT_DISCONNECT, mMessageHandler.onDisconnected);
        mSocket.on(Socket.EVENT_ERROR, mMessageHandler.onError);
    }

    public void connect() {
        mSocket.connect();
    }

    public void send(String event, final Object... args) {
        mSocket.emit(event, args);
    }

    public void sendMessage(String to, String type, Object payload) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("to", to);
        message.put("type", type);
        message.put("payload", payload);
        mSocket.emit("message", message);
        Log.d(TAG, "socketio send " + type + " to " + to + ", payload: " + payload);
    }

    private interface Command {
        void execute(String peerId, JSONObject payload);
    }

    private class Call implements Command {
        @Override
        public void execute(String peerId, JSONObject payload) {
            mCallback.onCall(peerId);
        }
    }

    private class Offer implements Command {
        @Override
        public void execute(String peerId, JSONObject payload) {
            mCallback.onOffer(peerId, payload);
        }
    }

    private class Answer implements Command {
        @Override
        public void execute(String peerId, JSONObject payload) {
            mCallback.onAnswer(peerId, payload);
        }
    }

    private class Candidate implements Command {
        @Override
        public void execute(String peerId, JSONObject payload) {
            mCallback.onCandidate(peerId, payload);
        }
    }

    private class Control implements Command {
        @Override
        public void execute(String peerId, JSONObject payload) {
            mCallback.onControl(peerId, payload);
        }
    }

    private class MessageHandler {
        private Map<String, Command> mCommands;

        public MessageHandler() {
            mCommands = new HashMap<>();
            mCommands.put("init", new Call());
            mCommands.put("offer", new Offer());
            mCommands.put("answer", new Answer());
            mCommands.put("candidate", new Candidate());
            mCommands.put("control", new Control());
        }

        public Emitter.Listener onId = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String id = (String) args[0];
                mCallback.onRegistered(id);
            }
        };

        public Emitter.Listener onMessage = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                String from = data.optString("from");
                String type = data.optString("type");
                Command cmd = mCommands.get(type);
                if (cmd == null) {
                    Log.e(TAG, "received unknown message type " + type + " from " + from);
                    return;
                }

                JSONObject payload = data.optJSONObject("payload");
                Log.d(TAG, "received " + type + " from " + from + ", payload: " + payload);
                cmd.execute(from, payload);
            }
        };

        public Emitter.Listener onConnected = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "socketio connected");
            }
        };

        public Emitter.Listener onDisconnected = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "args.length: " + args.length + " args[0]: " + args[0].toString());
                mCallback.onDisconnected();
            }
        };

        public Emitter.Listener onError = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Exception reason = (Exception) args[0];
                mCallback.onError(reason.getMessage());
            }
        };
    }

    public interface Callback {
        void onRegistered(String id);

        void onCall(String peerId);

        void onOffer(String peerId, JSONObject payload);

        void onAnswer(String peerId, JSONObject payload);

        void onCandidate(String peerId, JSONObject payload);

        void onControl(String peerId, JSONObject payload);

        void onDisconnected();

        void onError(String reason);
    }
}
