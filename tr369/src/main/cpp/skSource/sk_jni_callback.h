//
// Created by Outis on 2023/10/10.
//

#ifndef __SK_JNI_CALLBACK_H__
#define __SK_JNI_CALLBACK_H__

#ifdef __cplusplus
extern "C" {
#endif

#define OpenTR369CommandGet 1
#define OpenTR369CommandSet 2
#define OpenTR369CommandGetDatabaseStr 3
#define OpenTR369CommandSetProperty 4
#define OpenTR369CommandGetProperty 5

typedef struct {
    int (*SK_TR369_Callback_Get) (const int what, char *dst, int size, const char *str1, const char *str2);
    int (*SK_TR369_Callback_Set) (const int what, const char *str1, const char *str2, const char *str3);
    void (*SK_TR369_Callback_Start) ();
} skJniCallback_t;

void skSetJniCallback(skJniCallback_t *);

extern int SK_TR369_API_SetProperty(const char *name, const char *value);
extern int SK_TR369_API_GetProperty(const char *name, char *value, int size, const char *defaultValue);
extern int SK_TR369_API_GetParams(const char *name, char *value, int size);
extern int SK_TR369_API_SetParams(const char *name, const char *value);
extern int SK_TR369_API_GetDatabaseStr(const char *name, const char *param, char *value, int size);
extern void SK_TR369_API_StartServer();

#ifdef __cplusplus
}
#endif

#endif //__SK_JNI_CALLBACK_H__
