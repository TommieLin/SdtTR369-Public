package com.sdt.android.tr369;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.sdt.android.tr369.Bean.CaCertBean;
import com.sdt.android.tr369.Bean.ClientCertBean;
import com.sdt.android.tr369.Bean.MqttConfigsResponseBean;
import com.sdt.android.tr369.Utils.FileUtils;
import com.sdt.android.tr369.Utils.GzipUtils;
import com.sdt.diagnose.SpeedTestServiceManager;
import com.sdt.diagnose.Tr369PathInvoke;
import com.sdt.diagnose.common.DeviceInfoUtils;
import com.sdt.diagnose.common.NetworkUtils;
import com.sdt.diagnose.common.bean.SpeedTestBean;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpsUtils;
import com.sdt.opentr369.OpenTR369Native;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SdtTr369Receiver extends BroadcastReceiver {
    private final static String TAG = "SdtTr369Receiver";
    private final Context mContext;
    private final HandlerThread mHandlerThread;
    private Handler mHandler = null;
    public static final int MSG_START_TR369_PROTOCOL = 3302;
    public static final int MSG_REQUEST_MQTT_CONFIGS = 3303;
    private static final int DEFAULT_PERIOD_MILLIS_TIME = 120000;   // 默认两分钟请求一次

    public void handleProtocolMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_START_TR369_PROTOCOL:
                mHandler.removeMessages(MSG_START_TR369_PROTOCOL);
                LogUtils.d(TAG, "MSG_START_TR369_PROTOCOL");
                startTr369Protocol();
                break;
            case MSG_REQUEST_MQTT_CONFIGS:
                mHandler.removeMessages(MSG_REQUEST_MQTT_CONFIGS);
                LogUtils.d(TAG, "MSG_REQUEST_MQTT_CONFIGS");
                handleMqttServerConfigs();
                break;
            default:
                break;
        }
    }

    public SdtTr369Receiver(Context context) {
        LogUtils.d(TAG, "create");
        mContext = context;
        mHandlerThread = new HandlerThread("tr369_protocol");
        // 先启动，再初始化handler
        mHandlerThread.start();
        if (mHandler == null) {
            mHandler = new Handler(mHandlerThread.getLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    handleProtocolMessage(msg);
                }
            };
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtils.d(TAG, "action: " + action);
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            // 获取承有网络连接信恿
            if (isConnected(context.getApplicationContext())) {
                LogUtils.d(TAG, "Connected to the network.");
                mHandler.sendEmptyMessageDelayed(MSG_REQUEST_MQTT_CONFIGS, 5000);
            } else {
                LogUtils.e(TAG, "Not connected to the network.");
                mHandler.removeMessages(MSG_REQUEST_MQTT_CONFIGS);
                // 检测网络测速任务是否正在执行，如果在执行则停止测速
                if ("1".equals(SpeedTestBean.getInstance().getEnable())
                        && !TextUtils.isEmpty(SpeedTestBean.getInstance().getUrl())
                        && !TextUtils.isEmpty(SpeedTestBean.getInstance().getTransactionId())) {
                    SpeedTestServiceManager.getInstance().unbindSpeedTestService();
                }
            }
        }
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void startTr369Protocol() {
        FileUtils.copyTr369AssetsToFile(mContext);
        String defaultFilePath = mContext.getFilesDir().getPath() + "/" + FileUtils.PLATFORM_TMS_TR369_MODEL_DEFAULT;
        LogUtils.d(TAG, "startTr369Protocol defaultFilePath: " + defaultFilePath);

        int ret = OpenTR369Native.SetDefaultModelPath(defaultFilePath);
        LogUtils.d(TAG, "startTr369Protocol SetDefaultModelPath ret: " + ret);

        String modelFile = mContext.getFilesDir().getPath() + "/" + FileUtils.PLATFORM_TMS_TR369_MODEL_XML;
        ret = OpenTR369Native.OpenTR369Init(modelFile);
        LogUtils.d(TAG, "startTr369Protocol ret: " + ret);

        // 执行到此处说明TR369协议异常退出, 请检查日志以分析问题所在!!!
        String over_str = OpenTR369Native.stringFromJNI();
        LogUtils.e(TAG, "startTr369Protocol ***** bad results: " + over_str + " *****");
    }

    private void handleMqttServerConfigs() {
        String id = DeviceInfoUtils.getUspAgentID();
        String mac = NetworkUtils.getEthernetMacAddress();
        if (TextUtils.isEmpty(mac)) {
            mac = NetworkUtils.getWifiMacAddress();
        }
        LogUtils.d(TAG, "handleMqttServerConfigs id: " + id + ", mac: " + mac);

        String token = OpenTR369Native.GetXAuthToken(mac, id);
//        LogUtils.d(TAG, "handleMqttServerConfigs token: " + token);     // 此打印信息释放版本时禁止打开

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("id", id);
        hashMap.put("mac", mac);

        String url = Tr369PathInvoke.getInstance().getString("Device.X_Skyworth.ManagementServer.Url");
        if (TextUtils.isEmpty(url)) {
            LogUtils.e(TAG, "TR369 related configuration is missing in the config.properties file.");
            return;
        }
        String requestUrl = url + "/tr369/mqttaddr/enquire";

        HttpsUtils.requestMqttServerConfigs(requestUrl, token, hashMap, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LogUtils.e(TAG, "requestMqttServerConfigs onFailure: " + e.getMessage());
                mHandler.sendEmptyMessageDelayed(MSG_REQUEST_MQTT_CONFIGS, DEFAULT_PERIOD_MILLIS_TIME);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                LogUtils.d(TAG, "requestMqttServerConfigs Protocol: " + response.protocol()
                        + ", Code: " + response.code());
                if (response.code() == 200 && (response.body() != null)) {
                    String responseBody = response.body().string();
                    if (handleMqttResponseBody(responseBody)) {
                        mHandler.sendEmptyMessage(MSG_START_TR369_PROTOCOL);
                        return;
                    }
                } else if (response.code() == 403 && (response.body() != null)) {
                    LogUtils.e(TAG, "Response: " + response.protocol() + " " + response.code()
                            + " Body: " + response.body().string());
                } else if (response.code() == 400 && (response.body() != null)) {
                    // 参数异常，无需再发送
                    LogUtils.e(TAG, "Response: " + response.protocol() + " " + response.code()
                            + " Body: " + response.body().string());
                    return;
                }
                mHandler.sendEmptyMessageDelayed(MSG_REQUEST_MQTT_CONFIGS, DEFAULT_PERIOD_MILLIS_TIME);
            }
        });
    }

    private boolean handleMqttResponseBody(String responseBody) {
        if (TextUtils.isEmpty(responseBody)) {
            LogUtils.e(TAG, "handleMqttResponseBody: Parameters are empty");
            return false;
        }

        Gson gson = new Gson();
        MqttConfigsResponseBean bean = gson.fromJson(responseBody, MqttConfigsResponseBean.class);
        if (bean == null) {
            LogUtils.e(TAG, "handleMqttResponseBody: Response body parsing failed");
            return false;
        }

        boolean enable = bean.isEnable();
        if (!enable) {
            LogUtils.e(TAG, "handleMqttResponseBody: The server does not allow this request");
            mHandler.sendEmptyMessageDelayed(MSG_REQUEST_MQTT_CONFIGS, DEFAULT_PERIOD_MILLIS_TIME);
            return false;
        }

        CaCertBean caCert = bean.getCaCert();
        if (caCert == null) {
            LogUtils.e(TAG, "handleMqttResponseBody: The CA certificate data is abnormal");
            return false;
        }
        boolean isCaCertGzip = caCert.isGzip();
        String caCertContext = (isCaCertGzip)
                ? GzipUtils.dataHandleByUnGzip(caCert.getCertContent())
                : caCert.getCertContent();

        ClientCertBean clientCert = bean.getClientCert();
        if (clientCert == null) {
            LogUtils.e(TAG, "handleMqttResponseBody: The client certificate data is abnormal");
            return false;
        }
        boolean isClientCertGzip = clientCert.isGzip();
        String clientPrivateKey = (isClientCertGzip)
                ? GzipUtils.dataHandleByUnGzip(clientCert.getPrivateKey())
                : clientCert.getPrivateKey();
        String clientCertContext = (isClientCertGzip)
                ? GzipUtils.dataHandleByUnGzip(clientCert.getCertContent())
                : clientCert.getCertContent();

        return initMqttServerConfigs(bean, caCertContext, clientPrivateKey, clientCertContext);
    }

    private boolean initMqttServerConfigs(MqttConfigsResponseBean bean, String caCertContext, String clientPrivateKey, String clientCertContext) {
        if (bean == null
                || TextUtils.isEmpty(caCertContext)
                || TextUtils.isEmpty(clientPrivateKey)
                || TextUtils.isEmpty(clientCertContext)) {
            LogUtils.e(TAG, "initMqttServerConfigs: Certificate parameters are empty");
            return false;
        }

        String mqttServer = bean.getMqttServer();
        String clientId = bean.getClientId();
        String username = bean.getUsername();
        String password = bean.getPassword();

        if (TextUtils.isEmpty(mqttServer)
                || TextUtils.isEmpty(clientId)
                || TextUtils.isEmpty(username)
                || TextUtils.isEmpty(password)) {
            LogUtils.e(TAG, "initMqttServerConfigs: Configuration parameters are empty");
            return false;
        }

        // 此打印信息释放版本时禁止打开
//        LogUtils.d(TAG, "initMqttServerConfigs mqttServer: " + mqttServer
//                + ", clientId: " + clientId
//                + ", username: " + username
//                + ", password: " + password
//                + ", caCertCertContent: " + caCertContext
//                + ", clientCertPrivateKey: " + clientPrivateKey
//                + ", clientCertCertContent: " + clientCertContext);

        if (OpenTR369Native.SetMqttServerUrl(mqttServer) != 0) {
            LogUtils.e(TAG, "initMqttServerConfigs: Failed to set the mqtt server url");
            return false;
        }
        if (OpenTR369Native.SetMqttClientId(clientId) != 0) {
            LogUtils.e(TAG, "initMqttServerConfigs: Failed to set the client Id");
            return false;
        }
        if (OpenTR369Native.SetMqttUsername(username) != 0) {
            LogUtils.e(TAG, "initMqttServerConfigs: Failed to set the client username");
            return false;
        }
        if (OpenTR369Native.SetMqttPassword(password) != 0) {
            LogUtils.e(TAG, "initMqttServerConfigs: Failed to set the client password");
            return false;
        }
        if (OpenTR369Native.SetMqttCaCertContext(caCertContext) != 0) {
            LogUtils.e(TAG, "initMqttServerConfigs: Failed to set the CA certificate");
            return false;
        }
        if (OpenTR369Native.SetMqttClientPrivateKey(clientPrivateKey) != 0) {
            LogUtils.e(TAG, "initMqttServerConfigs: Failed to set the client private key");
            return false;
        }
        if (OpenTR369Native.SetMqttClientCertContext(clientCertContext) != 0) {
            LogUtils.e(TAG, "initMqttServerConfigs: Failed to set the client certificate");
            return false;
        }
        LogUtils.d(TAG, "initMqttServerConfigs: The MQTT parameters have been set");
        return true;
    }

}
