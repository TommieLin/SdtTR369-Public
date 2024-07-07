//
// Created by Outis on 2023/9/11.
//

#ifndef __SK_TR369_H__
#define __SK_TR369_H__

#ifdef __cplusplus
extern "C" {
#endif

int SK_TR369_Start(const char *const);
int SK_TR369_SetDefaultModelPath(const char *const);
char *SK_TR369_GetDefaultModelPath();

int SK_TR369_SetMqttServerUrl(const char *const);
int SK_TR369_SetMqttClientId(const char *const);
int SK_TR369_SetMqttUsername(const char *const);
int SK_TR369_SetMqttPassword(const char *const);
int SK_TR369_SetMqttCaCertContext(const char *const);
int SK_TR369_SetMqttClientPrivateKey(const char *const);
int SK_TR369_SetMqttClientCertContext(const char *const);

void SK_TR369_SetUspLogLevel(int);
int SK_TR369_GetUspLogLevel();

int SK_TR369_GetDBParam(const char *, char *);
int SK_TR369_SetDBParam(const char *, const char *);
int SK_TR369_AddMultiObject(const char *, int);
int SK_TR369_DelMultiObject(const char *);
int SK_TR369_UpdateMultiObject(const char *, int);
int SK_TR369_ShowData(const char *);

char *SK_TR369_API_GetXAuthToken(const char *, const char *);
char *SK_TR369_API_GetCACertString();
char *SK_TR369_API_GetDevCertString();
char *SK_TR369_API_GetDevKeyString();

#ifdef __cplusplus
}
#endif

#endif //__SK_TR369_H__
