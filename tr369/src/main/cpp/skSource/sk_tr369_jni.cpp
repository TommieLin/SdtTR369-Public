
#include <cstring>
#include <cerrno>
#include <jni.h>
#include <unistd.h>
#include <net/if.h>
#include <sys/ioctl.h>
#include "sk_tr369_jni.h"
#include "sk_tr369_log.h"
#include "sk_jni_callback.h"
#include "vendor_defs.h"
#include "usp_err_codes.h"

#define JNI_REG_CLASS "com/sdt/opentr369/OpenTR369Native"

static JavaVM *mJavaVm = nullptr;
static jclass mClass = nullptr;

const struct {
    const char *name;
    const char *type;
} sFuncScript[] = {
        {.name="OpenTR369CallbackGet",
                .type="(ILjava/lang/String;Ljava/lang/String;)Ljava/lang/String;"},
        {.name="OpenTR369CallbackSet",
                .type="(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)I"},
        {.name="OpenTR369CallbackGetAttr",
                .type="(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"},
        {.name="OpenTR369CallbackSetAttr",
                .type="(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I"},
        {.name="OpenTR369CallbackStart", .type="()V"},
};
static jmethodID sJavaFunction[ARRAY_SIZE(sFuncScript)];

#define funcGet sJavaFunction[0]
#define funcSet sJavaFunction[1]
#define funcGetAttr sJavaFunction[2]
#define funcSetAttr sJavaFunction[3]
#define funcStart sJavaFunction[4]

#ifdef __cplusplus
extern "C" {
#endif

static inline JNIEnv *getJNIEnv(bool *needsDetach) {
    *needsDetach = false;
    JNIEnv *env = nullptr;
    int status = mJavaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4);
    if (status < 0) {
        JavaVMAttachArgs args = {JNI_VERSION_1_4, nullptr, nullptr};
        int result = mJavaVm->AttachCurrentThread(&env, (void *) &args);
        if (result != JNI_OK) {
            SK_ERR("Thread attach failed: %#x", result);
            return nullptr;
        }
        *needsDetach = true;
    }
    return env;
}

static inline void detachJNI() {
    int result = mJavaVm->DetachCurrentThread();
    if (result != JNI_OK) {
        SK_ERR("Thread detach failed: %#x", result);
    }
}

int SK_TR369_Callback_Get(const int what, char *dst, int size, const char *str1, const char *str2) {
    SK_DBG("==>: %d, %s, %s", what, str1, str2);
    do {
        bool needsDetach;
        const char *pStr = nullptr;
        JNIEnv *env = getJNIEnv(&needsDetach);
        CHECK_BREAK(env != nullptr);
        CHECK_BREAK(str1 != nullptr);
        CHECK_BREAK(dst != nullptr);
        CHECK_BREAK(size > 0);
        jstring req1, req2 = nullptr;
        if ((req1 = env->NewStringUTF(str1)) == nullptr)
            env->ExceptionClear();
        if (str2 && ((req2 = env->NewStringUTF(str2)) == nullptr))
            env->ExceptionClear();
        auto reply = (jstring) env->CallStaticObjectMethod(mClass, funcGet, what, req1, req2);
        if (reply) pStr = env->GetStringUTFChars(reply, nullptr);
        if (pStr) {
            memset(dst, 0, size);
            size_t len = strlen(pStr);
            len = (len > size) ? size : len;
            memcpy(dst, pStr, len);
            env->ReleaseStringUTFChars(reply, pStr);
        }
        if (req1) env->DeleteLocalRef(req1);
        if (req2) env->DeleteLocalRef(req2);

        if (env->ExceptionCheck()) env->ExceptionClear();
        if (needsDetach) detachJNI();
    } while (false);
    SK_DBG("<==: %s, %s", str1, dst);
    return 0;
}

int SK_TR369_Callback_Set(const int what, const char *str1, const char *str2, const char *str3) {
    SK_DBG("==>: %d, %s, %s, %s", what, str1, str2, str3);
    int ret = -1;
    do {
        bool needsDetach;
        JNIEnv *env = getJNIEnv(&needsDetach);
        CHECK_BREAK(env != nullptr);
        CHECK_BREAK(str1 != nullptr);
        jstring req1, req2 = nullptr, req3 = nullptr;
        if ((req1 = env->NewStringUTF(str1)) == nullptr) env->ExceptionClear();
        if (str2 && ((req2 = env->NewStringUTF(str2)) == nullptr)) env->ExceptionClear();
        if (str3 && ((req3 = env->NewStringUTF(str3)) == nullptr)) env->ExceptionClear();
        ret = env->CallStaticIntMethod(mClass, funcSet, what, req1, req2, req3);
        if (req1) env->DeleteLocalRef(req1);
        if (req2) env->DeleteLocalRef(req2);
        if (req3) env->DeleteLocalRef(req3);
        if (env->ExceptionCheck()) env->ExceptionClear();
        if (needsDetach) detachJNI();
    } while (false);
    SK_DBG("<==: %s, %s, %s", str1, str2, str3);
    return ret;
}

int SK_TR369_Callback_GetAttr(const char *path, const char *method, const char **value,
                              unsigned int *len) {
    SK_DBG("==>: %s", path);
    do {
        bool needsDetach;
        const char *pStr = nullptr;
        JNIEnv *env = getJNIEnv(&needsDetach);
        jstring req1 = nullptr, req2 = nullptr;
        if (path && ((req1 = env->NewStringUTF(path)) == nullptr)) env->ExceptionClear();
        if (method && ((req2 = env->NewStringUTF(method)) == nullptr)) env->ExceptionClear();
        auto reply = (jstring) env->CallStaticObjectMethod(mClass, funcGetAttr, req1, req2);
        if (reply) pStr = env->GetStringUTFChars(reply, nullptr);
        if (pStr) {
            *value = strdup(pStr);
            *len = strlen(pStr);
            env->ReleaseStringUTFChars(reply, pStr);
        }
        if (req1) env->DeleteLocalRef(req1);
        if (req2) env->DeleteLocalRef(req2);
        if (env->ExceptionCheck()) env->ExceptionClear();
        if (needsDetach) detachJNI();
    } while (false);
    SK_DBG("<==: %s, %s, %d", path, *value, *len);
    return 0;
}

int SK_TR369_Callback_SetAttr(const char *path, const char *method, const char *value,
                              unsigned int len) {
    SK_DBG("==>: %s->%s", path, method);
    int ret = -1;
    do {
        bool needsDetach;
        JNIEnv *env = getJNIEnv(&needsDetach);
        jstring req1 = nullptr, req2 = nullptr, req3 = nullptr;
        if (path && ((req1 = env->NewStringUTF(path)) == nullptr)) env->ExceptionClear();
        if (method && ((req2 = env->NewStringUTF(method)) == nullptr)) env->ExceptionClear();
        if (value && ((req3 = env->NewStringUTF(value)) == nullptr)) env->ExceptionClear();
        ret = env->CallStaticIntMethod(mClass, funcSetAttr, req1, req2, req3);
        if (req1) env->DeleteLocalRef(req1);
        if (req2) env->DeleteLocalRef(req2);
        if (req3) env->DeleteLocalRef(req3);
        if (env->ExceptionCheck()) env->ExceptionClear();
        if (needsDetach) detachJNI();
    } while (false);
    SK_DBG("<==: %d-%s-ret=%d", len, value, ret);
    return ret;
}

void SK_TR369_Callback_Start() {
    do {
        bool needsDetach;
        JNIEnv *env = getJNIEnv(&needsDetach);
        CHECK_BREAK(env != nullptr);
        env->CallStaticVoidMethod(mClass, funcStart);
        if (env->ExceptionCheck()) env->ExceptionClear();
        if (needsDetach) detachJNI();
    } while (false);
}

static void SK_TR369_JniConfig(JNIEnv *env) {
    jclass clazz;
    env->GetJavaVM(&mJavaVm);

    if ((clazz = env->FindClass(JNI_REG_CLASS)) == nullptr) {
        SK_ERR("Call FindClass(%s) failed", JNI_REG_CLASS);
        return;
    }
    mClass = reinterpret_cast<jclass> (env->NewGlobalRef(clazz));

    for (int i = 0; i < ARRAY_SIZE(sFuncScript); i++) {
        if ((sJavaFunction[i] =
                     env->GetStaticMethodID(mClass,
                                            sFuncScript[i].name,
                                            sFuncScript[i].type)) == nullptr) {
            SK_ERR("Call GetStaticMethodID %s(%s) failed", sFuncScript[i].name,
                          sFuncScript[i].type);
            return;
        }
    }
}

#ifdef __cplusplus
}
#endif

extern "C" JNIEXPORT jstring JNICALL
Java_com_sdt_opentr369_OpenTR369Native_stringFromJNI(JNIEnv *env, jclass thiz) {
    return env->NewStringUTF("USP Agent terminated abnormally.");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_OpenTR369Init(JNIEnv *env, jclass clazz,
                                                     const jstring path) {

    SK_TR369_JniConfig(env);
    static skJniCallback_t jniCallFuncion = {
            .SK_TR369_Callback_Get = SK_TR369_Callback_Get,
            .SK_TR369_Callback_Set = SK_TR369_Callback_Set,
            .SK_TR369_Callback_Start = SK_TR369_Callback_Start};
    skSetJniCallback(&jniCallFuncion);

    const char *const filePath = env->GetStringUTFChars(path, nullptr);
    int ret = SK_TR369_MAIN_Start(filePath);
    env->ReleaseStringUTFChars(path, filePath);
    return ret;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_SetDefaultModelPath(JNIEnv *env, jclass clazz,
                                                       const jstring default_path) {
    const char *const defaultFilePath = env->GetStringUTFChars(default_path, nullptr);
    int ret = SK_TR369_SetDefaultModelPath(defaultFilePath);
    env->ReleaseStringUTFChars(default_path, defaultFilePath);
    return ret;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetDefaultModelPath(JNIEnv *env, jclass clazz) {
    char *filePath = SK_TR369_GetDefaultModelPath();
    if (filePath != nullptr) {
        return env->NewStringUTF(filePath);
    }
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetMqttServerUrl(JNIEnv *env, jclass clazz) {
    // TODO: implement GetMqttServerUrl()
    char *url = SK_TR369_GetMqttServerUrl();
    if (url != nullptr) {
        return env->NewStringUTF(url);
    }
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_SetMqttServerUrl(JNIEnv *env, jclass clazz,
                                                        jstring mqtt_server) {
    // TODO: implement SetMqttServerUrl()
    const char *const mqttServer = env->GetStringUTFChars(mqtt_server, nullptr);
    int ret = SK_TR369_SetMqttServerUrl(mqttServer);
    env->ReleaseStringUTFChars(mqtt_server, mqttServer);
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_SetMqttClientId(JNIEnv *env, jclass clazz,
                                                       jstring client_id) {
    // TODO: implement SetMqttClientId()
    const char *const clientId = env->GetStringUTFChars(client_id, nullptr);
    int ret = SK_TR369_SetMqttClientId(clientId);
    env->ReleaseStringUTFChars(client_id, clientId);
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_SetMqttUsername(JNIEnv *env, jclass clazz,
                                                       jstring username_str) {
    // TODO: implement SetMqttUsername()
    const char *const username = env->GetStringUTFChars(username_str, nullptr);
    int ret = SK_TR369_SetMqttUsername(username);
    env->ReleaseStringUTFChars(username_str, username);
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_SetMqttPassword(JNIEnv *env, jclass clazz,
                                                       jstring password_str) {
    // TODO: implement SetMqttPassword()
    const char *const password = env->GetStringUTFChars(password_str, nullptr);
    int ret = SK_TR369_SetMqttPassword(password);
    env->ReleaseStringUTFChars(password_str, password);
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_SetMqttCaCertContext(JNIEnv *env, jclass clazz,
                                                            jstring ca_cert_context) {
    // TODO: implement SetMqttCaCertContext()
    const char *const caCertContext = env->GetStringUTFChars(ca_cert_context, nullptr);
    int ret = SK_TR369_SetMqttCaCertContext(caCertContext);
    env->ReleaseStringUTFChars(ca_cert_context, caCertContext);
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_SetMqttClientPrivateKey(JNIEnv *env, jclass clazz,
                                                               jstring client_private_key) {
    // TODO: implement SetMqttClientPrivateKey()
    const char *const clientPrivateKey = env->GetStringUTFChars(client_private_key, nullptr);
    int ret = SK_TR369_SetMqttClientPrivateKey(clientPrivateKey);
    env->ReleaseStringUTFChars(client_private_key, clientPrivateKey);
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_SetMqttClientCertContext(JNIEnv *env, jclass clazz,
                                                                jstring client_cert_context) {
    // TODO: implement SetMqttClientCertContext()
    const char *const clientCertContext = env->GetStringUTFChars(client_cert_context, nullptr);
    int ret = SK_TR369_SetMqttClientCertContext(clientCertContext);
    env->ReleaseStringUTFChars(client_cert_context, clientCertContext);
    return ret;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_sdt_opentr369_OpenTR369Native_SetUspLogLevel(JNIEnv *env, jclass clazz, jint level) {
    // TODO: implement SetUspLogLevel()
    SK_TR369_SetUspLogLevel(level);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetUspLogLevel(JNIEnv *env, jclass clazz) {
    // TODO: implement GetUspLogLevel()
    return SK_TR369_GetUspLogLevel();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetDBParam(JNIEnv *env, jclass clazz, const jstring path) {
    const char *param = env->GetStringUTFChars(path, nullptr);
    char value[MAX_DM_VALUE_LEN];
    int ret = SK_TR369_GetDBParam(param, value);
    env->ReleaseStringUTFChars(path, param);

    if (ret == USP_ERR_OK) {
        return env->NewStringUTF(value);
    }
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_SetDBParam(JNIEnv *env, jclass clazz, const jstring path,
                                                  const jstring value) {
    const char *param = env->GetStringUTFChars(path, nullptr);
    const char *paramValue = env->GetStringUTFChars(value, nullptr);

    int ret = SK_TR369_SetDBParam(param, paramValue);
    env->ReleaseStringUTFChars(path, param);
    env->ReleaseStringUTFChars(value, paramValue);
    return ret;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_ShowData(JNIEnv *env, jclass clazz, const jstring cmd) {
    const char *command = env->GetStringUTFChars(cmd, nullptr);
    int ret = SK_TR369_ShowData(command);
    env->ReleaseStringUTFChars(cmd, command);
    return ret;
}

/* Network */
extern "C"
JNIEXPORT jstring JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetXAuthToken(JNIEnv *env, jclass clazz, jstring mac,
                                                     jstring id) {
    // TODO: implement GetXAuthToken()
    jstring strRet = env->NewStringUTF("");
    const char *dev_mac = env->GetStringUTFChars(mac, nullptr);
    const char *dev_id = env->GetStringUTFChars(id, nullptr);
    char *token = SK_TR369_API_GetXAuthToken(dev_mac, dev_id);
    if (token) {
        strRet = env->NewStringUTF(token);
        free(token);
    }
    env->ReleaseStringUTFChars(mac, dev_mac);
    env->ReleaseStringUTFChars(id, dev_id);
    return strRet;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetCACertString(JNIEnv *env, jclass clazz) {
    // TODO: implement GetCACertString()
    jstring strRet = env->NewStringUTF("");
    char *ca = SK_TR369_API_GetCACertString();
    if (ca) {
        strRet = env->NewStringUTF(ca);
        free(ca);
    }
    return strRet;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetDevCertString(JNIEnv *env, jclass clazz) {
    // TODO: implement GetDevCertString()
    jstring strRet = env->NewStringUTF("");
    char *cert = SK_TR369_API_GetDevCertString();
    if (cert) {
        strRet = env->NewStringUTF(cert);
        free(cert);
    }
    return strRet;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetDevKeyString(JNIEnv *env, jclass clazz) {
    // TODO: implement GetDevKeyString()
    jstring strRet = env->NewStringUTF("");
    char *key = SK_TR369_API_GetDevKeyString();
    if (key) {
        strRet = env->NewStringUTF(key);
        free(key);
    }
    return strRet;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetNetDevInterfaceStatus(JNIEnv *env, jclass clazz,
                                                                jstring name) {
    // TODO: implement GetNetDevInterfaceStatus()
    struct ifreq ifr;
    int size = 50;
    int ifc_ctl_sock = -1;
    jstring strRet = env->NewStringUTF("");
    const char *paramName = env->GetStringUTFChars(name, nullptr);

    SK_DBG("GetNetDevInterfaceStatus paramName1: %s", paramName);
    ifc_ctl_sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (ifc_ctl_sock < 0) {
        SK_ERR("GetNetDevInterfaceStatus socket() failed: %s", strerror(errno));
        return strRet;
    }
    memset(&ifr, 0, sizeof(struct ifreq));
    snprintf(ifr.ifr_name, IFNAMSIZ, "%s", paramName);

    if (ioctl(ifc_ctl_sock, SIOCGIFFLAGS, &ifr) < 0) {
        SK_ERR("GetNetDevInterfaceStatus socket() failed: %s", strerror(errno));
        close(ifc_ctl_sock);
        return strRet;
    }
    SK_DBG("GetNetDevInterfaceStatus ifr.ifr_flags: %08x", ifr.ifr_flags);

    char *cRet = (char *) malloc(size);
    if (cRet == nullptr) {
        SK_ERR("GetNetDevInterfaceStatus malloc() failed: %s", strerror(errno));
        return strRet;
    }

#if 0
    if (IFF_UP == (ifr.ifr_flags & IFF_UP)) {
        snprintf(ret, size, "Up");
    }
    else if(IFF_RUNNING != (ifr.ifr_flags & IFF_RUNNING)){
        snprintf(ret, size, "Down");
    }
#else
    if (ifr.ifr_flags == 0x00001043) {
        snprintf(cRet, size, "Up");
    } else if (0x00001003 == ifr.ifr_flags || 0x00001002 == ifr.ifr_flags) {
        //ifconfig eth0 down --> 00001002
        //disable wif on setting --> 00001003
        //liaoqs 2020.9.20
        SK_DBG("GetNetDevInterfaceStatus paramName2: %s, size = %d", paramName, size);
        snprintf(cRet, size, "Down");
    }
#endif
    else if (IFF_LOWER_UP == (ifr.ifr_flags & IFF_LOWER_UP)) {
        SK_DBG("GetNetDevInterfaceStatus paramName3: %s", paramName);
        snprintf(cRet, size, "LowerLayerDown");
    } else if (IFF_DORMANT == (ifr.ifr_flags & IFF_DORMANT)) {
        SK_DBG("GetNetDevInterfaceStatus paramName4: %s", paramName);
        snprintf(cRet, size, "Dormant");
    } else {
        SK_DBG("GetNetDevInterfaceStatus paramName5: %s", paramName);
        snprintf(cRet, size, "Unknown");
    }

    SK_DBG("GetNetDevInterfaceStatus status: %s", cRet);
    close(ifc_ctl_sock);

    strRet = env->NewStringUTF(cRet);
    free(cRet);
    env->ReleaseStringUTFChars(name, paramName);
    return strRet;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetWirelessNoise(JNIEnv *env, jclass clazz, jstring name) {
    // TODO: implement GetWirelessNoise()
    int ret = 0;
    char strName[16] = {0};
    int nElem[4] = {0};
    char strLine[512] = {0};

    FILE *fp = fopen("/proc/net/wireless", "r");
    if (fp == nullptr) {
        SK_ERR("GetWirelessNoise fopen() failed: %s", strerror(errno));
        return ret;
    }

    if (!fgets(strLine, sizeof(strLine), fp)) {
        SK_ERR("GetWirelessNoise fgets() 1 failed: %s", strerror(errno));
        fclose(fp);
        return ret;
    }

    if (!fgets(strLine, sizeof(strLine), fp)) {
        SK_ERR("GetWirelessNoise fgets() 2 failed: %s", strerror(errno));
        fclose(fp);
        return ret;
    }

    const char *paramName = env->GetStringUTFChars(name, nullptr);
    while (nullptr != fgets(strLine, sizeof(strLine), fp)) {
        memset(strName, 0x00, sizeof(strName));
        memset(nElem, 0x00, sizeof(nElem));
        SK_ERR("getWirelessNoise strLine:%s", strLine);
        sscanf(strLine, "%s%d%d%d%d", strName, &nElem[0], &nElem[1], &nElem[2], &nElem[3]);
        strName[strlen(strName) - 1] = 0;
        SK_ERR("getWirelessNoise face:%s tus:%d link:%d level:%d noise:%d", strName, nElem[0],
              nElem[1], nElem[2], nElem[3]);

        if (!strcmp(paramName, strName)) {
            ret = nElem[3];
            break;
        }
    }
    fclose(fp);
    env->ReleaseStringUTFChars(name, paramName);
    return ret;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sdt_opentr369_OpenTR369Native_GetNetInterfaceStatus(JNIEnv *env, jclass clazz,
                                                             jstring name) {
    // TODO: implement GetNetInterfaceStatus()
    char strName[16] = {0};
    int nElem[10] = {0};
    char strLine[512] = {0};
    jstring strRet = env->NewStringUTF("");

    FILE *fp = fopen("/proc/net/dev", "r");
    if (fp == nullptr) {
        SK_ERR("GetNetInterfaceStatus fopen() failed: %s", strerror(errno));
        return strRet;
    }

    if (!fgets(strLine, sizeof(strLine), fp)) {
        SK_ERR("GetNetInterfaceStatus fgets() 1 failed: %s", strerror(errno));
        fclose(fp);
        return strRet;
    }

    if (!fgets(strLine, sizeof(strLine), fp)) {
        SK_ERR("GetNetInterfaceStatus fgets() 1 failed: %s", strerror(errno));
        fclose(fp);
        return strRet;
    }

    int size = 128;
    char *cRet = (char *) malloc(size);
    if (cRet == nullptr) {
        SK_ERR("GetNetInterfaceStatus malloc() failed: %s", strerror(errno));
        return strRet;
    }
    const char *paramName = env->GetStringUTFChars(name, nullptr);

    while (nullptr != fgets(strLine, sizeof(strLine), fp)) {
        memset(strName, 0x00, sizeof(strName));
        memset(nElem, 0x00, sizeof(nElem));
        SK_ERR("GetNetInterfaceStatus strLine: %s", strLine);
        sscanf(strLine, "%s%d%d%d%d%d%d%d%d%d%d", strName, &nElem[0], &nElem[1], &nElem[2],
               &nElem[3], &nElem[4], &nElem[5], &nElem[6], &nElem[7], &nElem[8], &nElem[9]);
        strName[strlen(strName) - 1] = 0;
        SK_ERR("GetNetInterfaceStatus link[%s]: face:%s Receive[bytes:%d packets:%d errs:%d drop:%d fifo:%d frame:%d compressed:%d multicast:%d] Transmit[bytes:%d packets:%d",
              paramName, strName, nElem[0], nElem[1],
              nElem[2], nElem[3], nElem[4], nElem[5],
              nElem[6], nElem[7], nElem[8], nElem[9]);

        if (!strcmp(paramName, strName)) {
            snprintf(cRet, size, "%d;%d;%d;%d;%d;%d", nElem[8], nElem[0], nElem[9], nElem[1],
                     nElem[3], nElem[2]);
            SK_ERR("GetNetInterfaceStatus ret = %s ", cRet);
            strRet = env->NewStringUTF(cRet);
            break;
        }
    }
    fclose(fp);
    free(cRet);
    env->ReleaseStringUTFChars(name, paramName);
    return strRet;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_AddMultiObject(JNIEnv *env, jclass clazz, jstring path,
                                                      jint num) {
    // TODO: implement AddInstance()
    const char *param = env->GetStringUTFChars(path, nullptr);
    int ret = SK_TR369_AddMultiObject(param, num);
    env->ReleaseStringUTFChars(path, param);
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_DelMultiObject(JNIEnv *env, jclass clazz, jstring path) {
    // TODO: implement DeleteInstance()
    const char *param = env->GetStringUTFChars(path, nullptr);
    int ret = SK_TR369_DelMultiObject(param);
    env->ReleaseStringUTFChars(path, param);
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sdt_opentr369_OpenTR369Native_UpdateMultiObject(JNIEnv *env, jclass clazz, jstring path,
                                                         jint num) {
    // TODO: implement UpdateMultiObject()
    const char *param = env->GetStringUTFChars(path, nullptr);
    int ret = SK_TR369_UpdateMultiObject(param, num);
    env->ReleaseStringUTFChars(path, param);
    return ret;
}