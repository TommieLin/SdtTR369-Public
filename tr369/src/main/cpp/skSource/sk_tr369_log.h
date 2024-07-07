//
// Created by Outis on 2023/10/10.
//

#ifndef __SK_TR369_LOG_H__
#define __SK_TR369_LOG_H__

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif

#define LOG_TAG "tr369"

#define ALOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define ALOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGW(...)  __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define ALOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define ALOGF(...)  __android_log_print(ANDROID_LOG_FATAL, LOG_TAG, __VA_ARGS__)

#define SK_DBG(fmt, args...)    ALOGD("%s: " fmt, __FUNCTION__, ##args)
#define SK_ERR(fmt, args...)    ALOGE("%s: " fmt, __FUNCTION__, ##args)

#define CHECK_BREAK(e) if(!(e)) {ALOGE("CHECK_BREAK(%s)", #e); break;}
#define ARRAY_SIZE(a) (sizeof(a) / sizeof((a)[0]))

#ifdef __cplusplus
}
#endif

#endif //__SK_TR369_LOG_H__
