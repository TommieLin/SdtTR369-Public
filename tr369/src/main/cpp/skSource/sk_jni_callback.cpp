//
// Created by Outis on 2023/10/10.
//
#include "sk_jni_callback.h"
#include <cstring>

#ifdef __cplusplus
extern "C" {
#endif

static bool isInit = false;
skJniCallback_t *paraJniCallbackFuncMap = nullptr;

#define pFunc paraJniCallbackFuncMap

void skSetJniCallback(skJniCallback_t *pFun) {
    if (isInit) return;
    isInit = true;
    paraJniCallbackFuncMap = pFun;
}

int SK_TR369_API_GetParams(const char *name, char *value, int size) {
    return (pFunc == nullptr) ? -1 : pFunc->SK_TR369_Callback_Get(OpenTR369CommandGet, value, size, name, nullptr);
}

int SK_TR369_API_SetParams(const char *name, const char *value) {
    return (pFunc == nullptr) ? -1 : pFunc->SK_TR369_Callback_Set(OpenTR369CommandSet, name, value, nullptr);
}

int SK_TR369_API_GetDatabaseStr(const char *name, const char *param, char *value, int size) {
    return (pFunc == nullptr) ? -1 : pFunc->SK_TR369_Callback_Get(OpenTR369CommandGetDatabaseStr, value, size, name, param);
}

int SK_TR369_API_SetProperty(const char *name, const char *value) {
    return (pFunc == nullptr) ? -1 : pFunc->SK_TR369_Callback_Set(OpenTR369CommandSetProperty, name, value, nullptr);
}

int SK_TR369_API_GetProperty(const char *name, char *value, int size, const char *defaultValue) {
    if (value == nullptr || size <= 0) return 0;
    value[0] = '\0';
    int ret = (pFunc == nullptr) ? 0 : pFunc->SK_TR369_Callback_Get(OpenTR369CommandGetProperty, value, size, name, nullptr);
    if (strlen(value) <= 0) {
        strcpy(value, defaultValue);
    }
    return ret;
}

void SK_TR369_API_StartServer() {
    if (pFunc != nullptr) pFunc->SK_TR369_Callback_Start();
}

#ifdef __cplusplus
}
#endif