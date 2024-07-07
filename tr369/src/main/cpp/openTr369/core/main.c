/*
 *
 * Copyright (C) 2019-2022, Broadband Forum
 * Copyright (C) 2016-2021  CommScope, Inc
 * Copyright (C) 2020, BT PLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

/**
 * \file main.c
 *
 * Main function for USP Agent
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <getopt.h>
#include <limits.h>
#include <curl/curl.h>
#include <pthread.h>
#include <signal.h>
#include <syslog.h>
#include <unistd.h>

#include "common_defs.h"
#include "mtp_exec.h"
#include "dm_exec.h"
#include "bdc_exec.h"
#include "data_model.h"
#include "dm_access.h"
#include "device.h"
#include "database.h"
#include "sync_timer.h"
#include "cli.h"
#include "os_utils.h"
#include "text_utils.h"
#include "usp_coap.h"
#include "stomp.h"
#include "retry_wait.h"
#include "nu_macaddr.h"

#include "vendor_api.h"
#include "sk_jni_callback.h"

#ifdef ENABLE_WEBSOCKETS
#include "wsclient.h"
#include "wsserver.h"
#endif

#ifndef OVERRIDE_MAIN
//--------------------------------------------------------------------------------------
// Array used by the getopt_long() function to parse a command line
// See http://www.gnu.org/s/hello/manual/libc/Getopt-Long-Options.html
// NOTE: When altering this array, make sure that you also alter the short options array as well
static struct option long_options[] =
{
//  long option,   option+argument?,  flag, short option
    {"help",       no_argument,       NULL, 'h'},    // Prints help for command line options
    {"log",        required_argument, NULL, 'l'},    // Sets the destination for the log file (either syslog, stdout or a filename)
    {"dbfile",     required_argument, NULL, 'f'},    // Sets the name of the path to use for the database file
    {"verbose",    required_argument, NULL, 'v'},    // Verbosity level for debug logging
    {"meminfo",    no_argument,       NULL, 'm'},    // Collects and prints information useful to debugging memory leaks
#ifdef HAVE_EXECINFO_H
    {"error",      no_argument,       NULL, 'e'},    // Prints the callstack whenever an error is detected
#endif
    {"prototrace", no_argument,       NULL, 'p'},    // Enables logging of the protocol trace
    {"command",    no_argument,       NULL, 'c'},    // The rest of the command line is a command to invoke on the active USP Agent.
                                                     // Using this option turns this executable into just a CLI for the active USP Agent.
    {"authcert",   required_argument, NULL, 'a'},    // Specifies the location of a file containing the client certificate to use authenticating this device
    {"truststore", required_argument, NULL, 't'},    // Specifies the location of a file containing the trust store certificates to use
    {"resetfile",  required_argument, NULL, 'r'},    // Specifies the location of a text file containing factory reset parameters
    {"interface",  required_argument, NULL, 'i'},    // Specifies the networking interface to use for communications

    {0, 0, 0, 0}
};

// In the string argument, the colons (after the option) mean that those options require arguments
static char short_options[] = "hl:f:v:a:t:r:i:mepc";
#endif

//--------------------------------------------------------------------------------------
// Variables set by command line arguments
bool enable_callstack_debug = false;    // Enables printing of the callstack when an error occurs


//--------------------------------------------------------------------------------------
// Forward declarations. Note these are not static, because we need them in the symbol table for USP_LOG_Callstack() to show them
void PrintUsage(char *prog_name);
int MAIN_Start(char *db_file, bool enable_mem_info);
void MAIN_Stop(void);

#ifndef OVERRIDE_MAIN
/*********************************************************************//**
**
** main
**
** Main function
**
** \param   argc - Number of command line arguments
** \param   argv - Array of pointers to command line argument strings
**
** \return  -1 (if this function ever returns, then it will be because of an error)
**
**************************************************************************/
int source_main(int argc, char *argv[])
{
    int err;
    int c;
    int option_index = 0;
    char *db_file = DEFAULT_DATABASE_FILE;
    bool enable_mem_info = false;

    // Determine a handle for the data model thread (this thread)
    OS_UTILS_SetDataModelThread();

    // Exit if unable to initialise basic subsystems
    USP_LOG_Init();
    USP_ERR_Init();
    err = USP_MEM_Init();
    if (err != USP_ERR_OK)
    {
        return -1;
    }

    // Iterate over all command line options
    while (FOREVER)
    {
        // Parse the next command line option
        c = getopt_long_only(argc, argv, short_options, long_options, &option_index);

        // Exit this loop, if no more options
        if (c == -1)
        {
            break;
        }

        // Determine which option was read this time
        switch (c)
        {
            case 'h':
                PrintUsage(argv[0]);
                exit(0);

            case 'l':
                // Exit if an error occurred whilst trying to open the log file
                err = USP_LOG_SetFile(optarg);
                if (err != USP_ERR_OK)
                {
                    goto exit;
                }
                break;

            case 'f':
                // Set the location of the database
                db_file = optarg;
                break;

            case 'm':
                // Enable memory info collection
                enable_mem_info = true;
                break;

#ifdef HAVE_EXECINFO_H
            case 'e':
                // Enable callstack printing when an error occurs
                enable_callstack_debug = true;
                break;
#endif

            case 'p':
                // Enable logging of protocol trace
                enable_protocol_trace = true;
                break;

            case 'a':
                // Set the location of the client certificate file to use
                auth_cert_file = optarg;
                break;

            case 't':
                // Set the location of the file containing trust store certificates
                usp_trust_store_file = optarg;
                break;

            case 'r':
                // Set the location of the text file containing the factory reset parameters
                sk_tr369_model_default = optarg;
                break;

            case 'i':
                // Set the networking interface to use for USP communication
                if (nu_ipaddr_is_valid_interface(optarg) != true)
                {
                    usp_log_level = kLogLevel_Error;
                    USP_LOG_Error("ERROR: Network interface '%s' does not exist or has no IP address", optarg);
                    goto exit;
                }
                usp_interface = optarg;
                break;

            case 'v':
                // Verbosity level
                err = TEXT_UTILS_StringToUnsigned(optarg, &usp_log_level);
                if ((err != USP_ERR_OK) || (usp_log_level >= kLogLevel_Max))
                {
                    usp_log_level = kLogLevel_Error;
                    USP_LOG_Error("ERROR: Verbosity level (%s) is invalid or out of range", optarg);
                    goto exit;
                }
                break;

            case 'c':
                // Rest of command line contains a command to send to the active USP Agent
                err = CLI_CLIENT_ExecCommand(argc-optind, &argv[optind], db_file);
                return err;

            default:
                USP_LOG_Error("ERROR: USP Agent was invoked with the '-%c' option but the code was not compiled in.", c);
                goto exit;

            case '?':
                usp_log_level = kLogLevel_Error;
                USP_LOG_Error("ERROR: Missing option value");
                goto exit;

        }
    }

    // Print a warning for any remaining command line arguments
    if (optind < argc)
    {
        USP_LOG_Error("WARNING: unknown command line arguments:-");
        while (optind < argc)
        {
            USP_LOG_Error("   %s", argv[optind++]);
        }
    }

    // Following debug is only logged when running as a daemon (not when running as CLI client).
    syslog(LOG_INFO, "USP Agent starting...");

    // Sleep until other services which USP Agent uses (eg DNS) are running
    // (ideally USP Agent should be started when the services are running, rather than sleeping here. But sometimes, there is no easy way to ensure this).
    if (DAEMON_START_DELAY_MS > 0)
    {
        usleep(DAEMON_START_DELAY_MS*1000);
    }


    // Exit if unable to start USP Agent
    err = MAIN_Start(db_file, enable_mem_info);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }

    // Exit if unable to spawn off a thread to service the MTPs
#ifndef DISABLE_STOMP
    err = OS_UTILS_CreateThread("MTP_STOMP", MTP_EXEC_StompMain, NULL);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }
#endif

#ifdef ENABLE_COAP
    err = OS_UTILS_CreateThread("MTP_CoAP", MTP_EXEC_CoapMain, NULL);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }
#endif

#ifdef ENABLE_MQTT
    err = OS_UTILS_CreateThread("MTP_MQTT", MTP_EXEC_MqttMain, NULL);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }
#endif

#ifdef ENABLE_WEBSOCKETS
    err = OS_UTILS_CreateThread("MTP_WSClient", WSCLIENT_Main, NULL);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }

    err = OS_UTILS_CreateThread("MTP_WSServer", WSSERVER_Main, NULL);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }
#endif

    // Exit if unable to spawn off a thread to perform bulk data collection posts
    err = OS_UTILS_CreateThread("BulkDataColl", BDC_EXEC_Main, NULL);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }

    // Run the data model main loop of USP Agent (this function does not return)
    DM_EXEC_Main(NULL);

exit:
    // If the code gets here, an error occurred
    USP_LOG_Error("USP Agent aborted unexpectedly");
    return -1;
}
#endif

/*********************************************************************//**
**
** SK_TR369_SetUspLogLevel
**
** Set the minimum level allowed for USP log printing.
**
** \param   level - Log level: kLogLevel_Error(1) ~ kLogLevel_Debug(4) means different printing levels.
**                  kLogLevel_Off(0) means turning off printing,
**                  kLogLevel_Max means turning on all printing.
**
** \return  None
**
**************************************************************************/
void SK_TR369_SetUspLogLevel(int level)
{
    if (level <= kLogLevel_Off)
    {
        usp_log_level = kLogLevel_Off;
    }
    else if (level >= kLogLevel_Max)
    {
        usp_log_level = kLogLevel_Max;
    }
    else
    {
        usp_log_level = level;
    }
}

/*********************************************************************//**
**
** SK_TR369_GetUspLogLevel
**
** Get the Verbosity level printed by the current USP log.
**
** \param   None
**
** \return  The log level.
**
**************************************************************************/
int SK_TR369_GetUspLogLevel()
{
    return usp_log_level;
}

/*********************************************************************//**
**
** SK_TR369_SetDefaultModelPath
**
** Set the storage path of the default value configuration file.
** The default path is: /data/user/0/com.sdt.android.tr369/sdt_tms_tr369_model.default
**
** \param   default_path - Pointer to the configuration file path.
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetDefaultModelPath(const char *const default_path)
{
    // 初始化Model默认值文件路径
    if (sk_tr369_model_default != NULL)
    {
        free(sk_tr369_model_default);
        sk_tr369_model_default = NULL;
    }

    unsigned int len = strlen(default_path);
    sk_tr369_model_default = (char *) malloc(len + 1);
    if (sk_tr369_model_default == NULL)
    {
        return USP_ERR_SK_MALLOC_FAILURE;
    }

    strncpy(sk_tr369_model_default, default_path, len);
    sk_tr369_model_default[len] = '\0';

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_GetDefaultModelPath
**
** Get the storage path of the default value configuration file.
** The default path is: /data/user/0/com.sdt.android.tr369/sdt_tms_tr369_model.default
**
** \param   None
**
** \return  Pointer to the configuration file path.
**
**************************************************************************/
char *SK_TR369_GetDefaultModelPath()
{
    return sk_tr369_model_default;
}

/*********************************************************************//**
**
** SK_TR369_SetMqttServerUrl
**
** Set the MQTT server URL.
**
** \param   mqtt_server - Pointer to the MQTT server URL
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetMqttServerUrl(const char *const mqtt_server)
{
    if (mqtt_server_url != NULL)
    {
        free(mqtt_server_url);
        mqtt_server_url = NULL;
    }

    unsigned int len = strlen(mqtt_server);
    mqtt_server_url = (char *) malloc(len + 1);
    if (mqtt_server_url == NULL)
    {
        return USP_ERR_SK_MALLOC_FAILURE;
    }

    strncpy(mqtt_server_url, mqtt_server, len);
    mqtt_server_url[len] = '\0';

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_GetMqttServerUrl
**
** Get the MQTT server URL.
**
** \param   None
**
** \return  Pointer to the MQTT server URL
**
**************************************************************************/
char *SK_TR369_GetMqttServerUrl()
{
    return mqtt_server_url;
}

/*********************************************************************//**
**
** SK_TR369_SetMqttClientId
**
** Set the client ID used to connect to the MQTT service
**
** \param   mqtt_server - Pointer to the client ID
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetMqttClientId(const char *const client_id)
{
    if (mqtt_client_id != NULL)
    {
        free(mqtt_client_id);
        mqtt_client_id = NULL;
    }

    unsigned int len = strlen(client_id);
    mqtt_client_id = (char *) malloc(len + 1);
    if (mqtt_client_id == NULL)
    {
        return USP_ERR_SK_MALLOC_FAILURE;
    }

    strncpy(mqtt_client_id, client_id, len);
    mqtt_client_id[len] = '\0';

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_GetMqttClientId
**
** Get the client ID used to connect to the MQTT service
**
** \param   None
**
** \return  Pointer to the MQTT server URL
**
**************************************************************************/
char *SK_TR369_GetMqttClientId()
{
    return mqtt_client_id;
}

/*********************************************************************//**
**
** SK_TR369_SetMqttUsername
**
** Set the client username used to connect to the MQTT service
**
** \param   mqtt_server - Pointer to the client username
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetMqttUsername(const char *const username)
{
    if (mqtt_client_username != NULL)
    {
        free(mqtt_client_username);
        mqtt_client_username = NULL;
    }

    unsigned int len = strlen(username);
    mqtt_client_username = (char *) malloc(len + 1);
    if (mqtt_client_username == NULL)
    {
        return USP_ERR_SK_MALLOC_FAILURE;
    }

    strncpy(mqtt_client_username, username, len);
    mqtt_client_username[len] = '\0';

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_GetMqttUsername
**
** Get the client username used to connect to the MQTT service
**
** \param   None
**
** \return  Pointer to the MQTT server URL
**
**************************************************************************/
char *SK_TR369_GetMqttUsername()
{
    return mqtt_client_username;
}

/*********************************************************************//**
**
** SK_TR369_SetMqttPassword
**
** Set the client password used to connect to the MQTT service
**
** \param   mqtt_server - Pointer to the client password
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetMqttPassword(const char *const password)
{
    if (mqtt_client_password != NULL)
    {
        free(mqtt_client_password);
        mqtt_client_password = NULL;
    }

    unsigned int len = strlen(password);
    mqtt_client_password = (char *) malloc(len + 1);
    if (mqtt_client_password == NULL)
    {
        return USP_ERR_SK_MALLOC_FAILURE;
    }

    strncpy(mqtt_client_password, password, len);
    mqtt_client_password[len] = '\0';

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_GetMqttPassword
**
** Get the client password used to connect to the MQTT service
**
** \param   None
**
** \return  Pointer to the MQTT server URL
**
**************************************************************************/
char *SK_TR369_GetMqttPassword()
{
    return mqtt_client_password;
}

/*********************************************************************//**
**
** SK_TR369_SetMqttCaCertContext
**
** Set the CA certificate.
**
** \param   ca_cert_context - Pointer to the CA certificate
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetMqttCaCertContext(const char *const ca_cert_context)
{
    if (usp_trust_store_str != NULL)
    {
        free(usp_trust_store_str);
        usp_trust_store_str = NULL;
    }

    unsigned int len = strlen(ca_cert_context);
    usp_trust_store_str = (char *) malloc(len + 1);
    if (usp_trust_store_str == NULL)
    {
        return USP_ERR_SK_MALLOC_FAILURE;
    }

    strncpy(usp_trust_store_str, ca_cert_context, len);
    usp_trust_store_str[len] = '\0';

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_SetMqttClientPrivateKey
**
** Set the MQTT client Private Key.
**
** \param   client_private_key - Pointer to the client's Private Key
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetMqttClientPrivateKey(const char *const client_private_key)
{
    if (auth_key_str != NULL)
    {
        free(auth_key_str);
        auth_key_str = NULL;
    }

    unsigned int len = strlen(client_private_key);
    auth_key_str = (char *) malloc(len + 1);
    if (auth_key_str == NULL)
    {
        return USP_ERR_SK_MALLOC_FAILURE;
    }

    strncpy(auth_key_str, client_private_key, len);
    auth_key_str[len] = '\0';

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_SetMqttClientCertContext
**
** Set the MQTT client certificate.
**
** \param   client_cert_context - Pointer to the client certificate context
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetMqttClientCertContext(const char *const client_cert_context)
{
    if (auth_cert_str != NULL)
    {
        free(auth_cert_str);
        auth_cert_str = NULL;
    }

    unsigned int len = strlen(client_cert_context);
    auth_cert_str = (char *) malloc(len + 1);
    if (auth_cert_str == NULL)
    {
        return USP_ERR_SK_MALLOC_FAILURE;
    }

    strncpy(auth_cert_str, client_cert_context, len);
    auth_cert_str[len] = '\0';

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_Start
**
** Customized initialization main function.
**
** \param   model_path - Node list storage path. The default file is: sdt_tms_tr369_model.xml
**              The default path is: /data/user/0/com.sdt.android.tr369/sdt_tms_tr369_model.xml
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_Start(const char *const model_path)
{
    int err;
    bool enable_mem_info = false;

    // Verbosity level
    usp_log_level = kLogLevel_Debug;

    // Determine a handle for the data model thread (this thread)
    OS_UTILS_SetDataModelThread();

    // Exit if unable to initialise basic subsystems
    err = USP_MEM_Init();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: USP_MEM_Init error(%d).", __FUNCTION__, err);
        goto exit;
    }

    // Enable logging of protocol trace
    enable_protocol_trace = true;

    // 初始化Model文件路径
    if (sk_tr369_model_xml != NULL)
    {
        free(sk_tr369_model_xml);
        sk_tr369_model_xml = NULL;
    }

    unsigned int len = strlen(model_path);
    sk_tr369_model_xml = (char *) malloc(len + 1);
    if (sk_tr369_model_xml == NULL)
    {
        err = USP_ERR_SK_MALLOC_FAILURE;
        USP_LOG_Error("%s: Pointer to 'sk_tr369_model_xml' is null.", __FUNCTION__);
        goto exit;
    }

    strncpy(sk_tr369_model_xml, model_path, len);
    sk_tr369_model_xml[len] = '\0';
    USP_LOG_Debug("%s: sk_tr369_model_default: %s, sk_tr369_model_xml: %s", __FUNCTION__, sk_tr369_model_default, sk_tr369_model_xml);

    // Following debug is only logged when running as a daemon (not when running as CLI client).
    USP_LOG_Info("USP Agent starting...");

    // Exit if unable to start USP Agent
    err = MAIN_Start(DEFAULT_DATABASE_FILE, enable_mem_info);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: MAIN_Start error(%d).", __FUNCTION__, err);
        goto exit;
    }

    // Exit if unable to spawn off a thread to service the MTPs
#ifndef DISABLE_STOMP
    err = OS_UTILS_CreateThread("MTP_STOMP", MTP_EXEC_StompMain, NULL);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }
#endif

#ifdef ENABLE_COAP
    err = OS_UTILS_CreateThread("MTP_CoAP", MTP_EXEC_CoapMain, NULL);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }
#endif

#ifdef ENABLE_MQTT
    err = OS_UTILS_CreateThread("MTP_MQTT", MTP_EXEC_MqttMain, NULL);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: CreateThread(MTP_MQTT) error(%d).", __FUNCTION__, err);
        goto exit;
    }
#endif

#ifdef ENABLE_WEBSOCKETS
    err = OS_UTILS_CreateThread("MTP_WSClient", WSCLIENT_Main, NULL);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }

    err = OS_UTILS_CreateThread("MTP_WSServer", WSSERVER_Main, NULL);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }
#endif

    // Exit if unable to spawn off a thread to perform bulk data collection posts
    err = OS_UTILS_CreateThread("BulkDataColl", BDC_EXEC_Main, NULL);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: CreateThread(BulkDataColl) error(%d).", __FUNCTION__, err);
        goto exit;
    }

    // Run the data model main loop of USP Agent (this function does not return)
    DM_EXEC_Main(NULL);
    return USP_ERR_OK;

exit:
    // If the code gets here, an error occurred
    USP_LOG_Error("USP Agent terminated abnormally.");
    return err;
}

/*********************************************************************//**
**
** MAIN_Start
**
** Initializes and starts USP Agent
**
** \param   db_file - pointer to name of USP Agent's database file to open
** \param   enable_mem_info - Set to true if memory debugging info should be collected
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int MAIN_Start(char *db_file, bool enable_mem_info)
{
    CURLcode curl_err;
    int err;

    // Exit if unable to initialise libraries which need to be initialised when running single threaded
    curl_err = curl_global_init(CURL_GLOBAL_ALL);
    if (curl_err != 0)
    {
        USP_LOG_Error("%s: curl_global_init() failed (curl_err=%d)", __FUNCTION__, curl_err);
        return USP_ERR_INTERNAL_ERROR;
    }

    SYNC_TIMER_Init();

    // Turn off SIGPIPE, since we use non-blocking connections and would prefer to get the EPIPE error
    // NOTE: If running USP Agent in GDB: GDB ignores this code and will still generate SIGPIPE
    signal(SIGPIPE, SIG_IGN);


    // Exit if an error occurred when initialising the database
    err = DATABASE_Init(db_file);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: DATABASE_Init error(%d).", __FUNCTION__, err);
        return err;
    }

    // Exit if an error occurred when initialising any of the the message queues used by the threads
    err = DM_EXEC_Init();
    err |= MTP_EXEC_Init();
    err |= BDC_EXEC_Init();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: *_EXEC_Init error(%d).", __FUNCTION__, err);
        return err;
    }

    // Initialise the random number generator seeds
    RETRY_WAIT_Init();

    // Exit if unable to add all schema paths to the data model
    err = DATA_MODEL_Init();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: DATA_MODEL_Init error(%d).", __FUNCTION__, err);
        return err;
    }

    // Start logging memory usage from now on (since the static data model schema allocations have completed)
    if (enable_mem_info)
    {
        USP_MEM_StartCollection();
    }

    // Exit if unable to start the datamodel objects
    err = DATA_MODEL_Start();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: DATA_MODEL_Start error(%d).", __FUNCTION__, err);
        return err;
    }

#ifndef DISABLE_STOMP
    // Start the STOMP connections. This must be done here, before other parts of the data model that require stomp connections
    // to queue messages (eg object creation/deletion notifications)
    err = DEVICE_STOMP_StartAllConnections();
    if (err != USP_ERR_OK)
    {
        return err;
    }
#endif

#ifdef ENABLE_MQTT
    // Start the MQTT connections. This must be done here, before other parts of the data model that require MQTT clients
    // to queue messages (eg object creation/deletion notifications)
    err = DEVICE_MQTT_StartAllClients();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: DEVICE_MQTT_StartAllClients error(%d).", __FUNCTION__, err);
        return err;
    }
#endif

    SK_TR369_API_StartServer();

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_Stop
**
** Frees all memory created by Skyworth.
**
** \param   None
**
** \return  None
**
**************************************************************************/
void SK_TR369_Stop()
{
    if (sk_tr369_model_default != NULL)
    {
        free(sk_tr369_model_default);
        sk_tr369_model_default = NULL;
    }

    if (sk_tr369_model_xml != NULL)
    {
        free(sk_tr369_model_xml);
        sk_tr369_model_xml = NULL;
    }

    if (mqtt_server_url != NULL)
    {
        free(mqtt_server_url);
        mqtt_server_url = NULL;
    }

    if (mqtt_client_id != NULL)
    {
        free(mqtt_client_id);
        mqtt_client_id = NULL;
    }

    if (mqtt_client_username != NULL)
    {
        free(mqtt_client_username);
        mqtt_client_username = NULL;
    }

    if (mqtt_client_password != NULL)
    {
        free(mqtt_client_password);
        mqtt_client_password = NULL;
    }

    if (usp_trust_store_str != NULL)
    {
        free(usp_trust_store_str);
        usp_trust_store_str = NULL;
    }

    if (auth_key_str != NULL)
    {
        free(auth_key_str);
        auth_key_str = NULL;
    }

    if (auth_cert_str != NULL)
    {
        free(auth_cert_str);
        auth_cert_str = NULL;
    }
}

/*********************************************************************//**
**
** MAIN_Stop
**
** Frees all memory and closes all sockets and file handles
** Called from the MTP thread
**
** \param   None
**
** \return  None
**
**************************************************************************/
void MAIN_Stop(void)
{
    // Free all memory used by Skyworth
    SK_TR369_Stop();
    // Free all memory used by USP Agent
    DM_EXEC_Destroy();
    curl_global_cleanup();
    USP_MEM_Destroy();
}

/*********************************************************************//**
**
** PrintUsage
**
** Prints the command line options for this program
**
** \param   prog_name - name of this executable from command line
**
** \return  None
**
**************************************************************************/
void PrintUsage(char *prog_name)
{
    char *p;
    char *name;

    // Strip off any leading directories from the executable path
    p = strrchr(prog_name, '/');
    name = (p == NULL) ? prog_name : &p[1];

    printf("USAGE: %s options\n", name);
    printf("--help (-h)       Displays this help\n");
    printf("--log (-l)        Sets the destination for debug logging. Default is 'stdout'. Can also use 'syslog' or a filename\n");
    printf("--dbfile (-f)     Sets the path of the file to store the database in (default=%s)\n", DEFAULT_DATABASE_FILE);
    printf("--verbose (-v)    Sets the debug verbosity log level: 0=Off, 1=Error(default), 2=Warning, 3=Info\n");
    printf("--prototrace (-p) Enables trace logging of the USP protocol messages\n");
    printf("--authcert (-a)   Sets the path of the PEM formatted file containing a client certificate and private key to authenticate this device with\n");
    printf("--truststore (-t) Sets the path of the PEM formatted file containing trust store certificates\n");
    printf("--resetfile (-r)  Sets the path of the text file containing factory reset parameters\n");
    printf("--interface (-i)  Sets the name of the networking interface to use for USP communication\n");
    printf("--meminfo (-m)    Collects and prints information useful to debugging memory leaks\n");
#ifdef HAVE_EXECINFO_H
    printf("--error (-e)      Enables printing of the callstack whenever an error is detected\n");
#endif
    printf("--command (-c)    Sends a CLI command to the running USP Agent and prints the response\n");
    printf("                  To get a list of all CLI commands use '-c help'\n");
    printf("\n");
}

