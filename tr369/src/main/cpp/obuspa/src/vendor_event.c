/*
 *
 * Copyright (C) 2019, Broadband Forum
 * Copyright (C) 2016-2019  CommScope, Inc
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
 * \file vendor_event.c
 *
 * Implements the interface to all vendor implemented data model nodes
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

#include "data_model.h"
#include "usp_err_codes.h"
#include "usp_api.h"
#include "usp_log.h"
#include "sk_jni_callback.h"
#include "sk_tr369_jni.h"

//------------------------------------------------------------------------------------
// Array of valid input arguments
static char *upload_file_input_args[] =
{
    "FileType",
    "DelaySeconds",
    "Url",
};

static char *upgrade_file_input_args[] =
{
    "FileType",
    "Url",
};

static char *download_file_input_args[] =
{
    "FileType",
    "Url",
};

//------------------------------------------------------------------------------------
// Array of valid output arguments
static char *x_event_output_args[] =
{
    "Status",
    "Message",
};

/*********************************************************************//**
**
** SK_TR369_Start_UploadFile
**
** Called when sync command Device.X_Skyworth.UploadFile() is executed
**
** \param   req - pointer to structure identifying the command
** \param   command_key - not used, OnBoardRequest notification doesn't have a command key field
** \param   input_args - not used, the command doesn't receive parameters
** \param   output_args - not used, the command doesn't return values
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_Start_UploadFile(dm_req_t *req, char *command_key, kv_vector_t *input_args, kv_vector_t *output_args)
{
    int err = USP_ERR_OK;
    char param[1024] = {0};
    // Input variables
    char *input_file_type, *input_delay_seconds, *input_url;

    // Extract the input arguments using KV_VECTOR_ functions
    input_file_type = USP_ARG_Get(input_args, "FileType", "");
    input_delay_seconds = USP_ARG_Get(input_args, "DelaySeconds", "");
    input_url = USP_ARG_Get(input_args, "Url", "");

    USP_LOG_Debug("%s: FileType: %s, DelaySeconds: %s, Url: %s",
                  __FUNCTION__, input_file_type, input_delay_seconds, input_url);

    if (strcmp(input_file_type, "") == 0
        || strcmp(input_delay_seconds, "") == 0
        || strcmp(input_url, "") == 0)
    {
        // if it doesn't, return invalid value
        USP_ERR_SetMessage("%s: Invalid value - The parameters for UploadFile() are empty.", __FUNCTION__);
        err = USP_ERR_INVALID_VALUE;
        goto exit;
    }

    strcpy(param, "UploadFile###");
    strcat(param, input_file_type);
    strcat(param, "###");
    strcat(param, input_delay_seconds);
    strcat(param, "###");
    strcat(param, input_url);

    SK_TR369_API_SetParams("skyworth.tr369.event", param);

    // Save all results into the output arguments using KV_VECTOR_ functions
    char status[16], message[256];
    SK_TR369_GetDBParam("Device.X_Skyworth.UploadResponse.Status", status);
    SK_TR369_GetDBParam("Device.X_Skyworth.UploadResponse.Message", message);

    USP_ARG_Add(output_args, "Status", status);
    USP_ARG_Add(output_args, "Message", message);

    USP_LOG_Info("%s: Status: %s, Message: %s", __FUNCTION__, status, message);

exit:
    return err;
}

/*********************************************************************//**
**
** SK_TR369_Start_UpgradeFile
**
** Called when sync command Device.X_Skyworth.UpgradeFile() is executed
**
** \param   req - pointer to structure identifying the command
** \param   command_key - not used, OnBoardRequest notification doesn't have a command key field
** \param   input_args - not used, the command doesn't receive parameters
** \param   output_args - not used, the command doesn't return values
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_Start_UpgradeFile(dm_req_t *req, char *command_key, kv_vector_t *input_args, kv_vector_t *output_args)
{
    int err = USP_ERR_OK;
    char param[1024] = {0};
    // Input variables
    char *input_file_type, *input_url;

    // Extract the input arguments using KV_VECTOR_ functions
    input_file_type = USP_ARG_Get(input_args, "FileType", "");
    input_url = USP_ARG_Get(input_args, "Url", "");

    USP_LOG_Debug("%s: FileType: %s, Url: %s", __FUNCTION__, input_file_type, input_url);

    if (strcmp(input_file_type, "") == 0
        || strcmp(input_url, "") == 0)
    {
        // if it doesn't, return invalid value
        USP_ERR_SetMessage("%s: Invalid value - The parameters for UpgradeFile() are empty.", __FUNCTION__);
        err = USP_ERR_INVALID_VALUE;
        goto exit;
    }

//    if (strcmp(input_file_type, "4 App Install Package") != 0)
//    {
//        USP_ERR_SetMessage("%s: The file type (%s) does not match.", __FUNCTION__, input_file_type);
//        err = USP_ERR_INVALID_VALUE;
//        goto exit;
//    }

    strcpy(param, "UpgradeFile###");
    strcat(param, input_url);
    strcat(param, "###");
    strcat(param, input_file_type);

    SK_TR369_API_SetParams("skyworth.tr369.event", param);

    // Save all results into the output arguments using KV_VECTOR_ functions
    USP_ARG_Add(output_args, "Status", "Complete");
    USP_LOG_Info("%s function execution completed.", __FUNCTION__);

exit:
    return err;
}

/*********************************************************************//**
**
** SK_TR369_Start_DownloadFile
**
** Called when sync command Device.X_Skyworth.DownloadFile() is executed
**
** \param   req - pointer to structure identifying the command
** \param   command_key - not used, OnBoardRequest notification doesn't have a command key field
** \param   input_args - not used, the command doesn't receive parameters
** \param   output_args - not used, the command doesn't return values
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_Start_DownloadFile(dm_req_t *req, char *command_key, kv_vector_t *input_args, kv_vector_t *output_args)
{
    int err = USP_ERR_OK;
    char param[1024] = {0};
    // Input variables
    char *input_file_type, *input_url;

    // Extract the input arguments using KV_VECTOR_ functions
    input_file_type = USP_ARG_Get(input_args, "FileType", "");
    input_url = USP_ARG_Get(input_args, "Url", "");

    USP_LOG_Debug("%s: FileType: %s, Url: %s", __FUNCTION__, input_file_type, input_url);

    if (strcmp(input_file_type, "") == 0
        || strcmp(input_url, "") == 0)
    {
        // if it doesn't, return invalid value
        USP_ERR_SetMessage("%s: Invalid value - The parameters for DownloadFile() are empty.", __FUNCTION__);
        err = USP_ERR_INVALID_VALUE;
        goto exit;
    }

//    if (strcmp(input_file_type, "3 Vendor Configuration File") != 0)
//    {
//        USP_ERR_SetMessage("%s: The file type (%s) does not match.", __FUNCTION__, input_file_type);
//        err = USP_ERR_INVALID_VALUE;
//        goto exit;
//    }

    strcpy(param, "DownloadFile###");
    strcat(param, input_url);
    strcat(param, "###");
    strcat(param, input_file_type);

    SK_TR369_API_SetParams("skyworth.tr369.event", param);

    // Save all results into the output arguments using KV_VECTOR_ functions
    USP_ARG_Add(output_args, "Status", "Complete");
    USP_LOG_Info("%s function execution completed.", __FUNCTION__);

exit:
    return err;
}

//------------------------------------------------------------------------------------
// Array of valid input arguments
static char *ip_ping_input_args[] =
{
    "Host",
    "DataBlockSize",
    "NumberOfRepetitions",
    "Timeout",      // 单位毫秒
    // Not used.
    "DSCP",
    "Interface",
    "ProtocolVersion",
};

//------------------------------------------------------------------------------------
// Array of valid output arguments
static char *ip_ping_output_args[] =
{
    "Status",
    "SuccessCount",
    "FailureCount",
    "AverageResponseTime",
    "MinimumResponseTime",
    "MaximumResponseTime",
    "AverageResponseTimeDetailed",
    "MinimumResponseTimeDetailed",
    "MaximumResponseTimeDetailed",
    "IPAddressUsed",
};

/*********************************************************************//**
**
** SK_TR369_Start_IPPing
**
** Called when sync command Device.IP.Diagnostics.IPPing() is executed
**
** \param   req - pointer to structure identifying the command
** \param   command_key - not used, OnBoardRequest notification doesn't have a command key field
** \param   input_args - not used, the command doesn't receive parameters
** \param   output_args - not used, the command doesn't return values
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_Start_IPPing(dm_req_t *req, char *command_key, kv_vector_t *input_args, kv_vector_t *output_args)
{
    int err = USP_ERR_OK;
    char param[1024] = {0};
    // Input variables
    char *input_host, *input_size, *input_count, *input_timeout_ms;

    // Extract the input arguments using KV_VECTOR_ functions
    input_host = USP_ARG_Get(input_args, "Host", "");
    input_size = USP_ARG_Get(input_args, "DataBlockSize", "");
    input_count = USP_ARG_Get(input_args, "NumberOfRepetitions", "");
    input_timeout_ms = USP_ARG_Get(input_args, "Timeout", "");

    USP_LOG_Debug("%s: Host: %s, DataBlockSize: %s, NumberOfRepetitions: %s, Timeout: %s",
                  __FUNCTION__, input_host, input_size, input_count, input_timeout_ms);

    if (strcmp(input_host, "") == 0
        || strcmp(input_size, "") == 0
        || strcmp(input_count, "") == 0
        || strcmp(input_timeout_ms, "") == 0)
    {
        // if it doesn't, return invalid value
        USP_ERR_SetMessage("%s: Invalid value - The parameters for IPPing() are empty.", __FUNCTION__);
        err = USP_ERR_INVALID_VALUE;
        goto exit;
    }

    strcpy(param, "IPPing###");
    strcat(param, input_host);
    strcat(param, "###");
    strcat(param, input_size);
    strcat(param, "###");
    strcat(param, input_count);
    strcat(param, "###");
    strcat(param, input_timeout_ms);

    SK_TR369_API_SetParams("skyworth.tr369.event", param);

    // Save all results into the output arguments using KV_VECTOR_ functions
    char status[16], ipAddressUsed[16], successCount[8], failureCount[8], avg[8], min[8], max[8], avg_ns[16], min_ns[16], max_ns[16];
    SK_TR369_GetDBParam("Device.IP.Diagnostics.IPPing.Status", status);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.IPPing.IPAddressUsed", ipAddressUsed);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.IPPing.SuccessCount", successCount);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.IPPing.FailureCount", failureCount);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.IPPing.AverageResponseTime", avg);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.IPPing.MinimumResponseTime", min);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.IPPing.MaximumResponseTime", max);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.IPPing.AverageResponseTimeDetailed", avg_ns);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.IPPing.MinimumResponseTimeDetailed", min_ns);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.IPPing.MaximumResponseTimeDetailed", max_ns);

    USP_ARG_Add(output_args, "Status", status);
    USP_ARG_Add(output_args, "SuccessCount", successCount);
    USP_ARG_Add(output_args, "FailureCount", failureCount);
    USP_ARG_Add(output_args, "AverageResponseTime", avg);
    USP_ARG_Add(output_args, "MinimumResponseTime", min);
    USP_ARG_Add(output_args, "MaximumResponseTime", max);
    USP_ARG_Add(output_args, "AverageResponseTimeDetailed", avg_ns);
    USP_ARG_Add(output_args, "MinimumResponseTimeDetailed", min_ns);
    USP_ARG_Add(output_args, "MaximumResponseTimeDetailed", max_ns);
    USP_ARG_Add(output_args, "IPAddressUsed", ipAddressUsed);

    USP_LOG_Info("%s: Status: %s, SuccessCount: %s, FailureCount: %s, "
                 "AverageResponseTime: %s, MinimumResponseTime: %s, MaximumResponseTime: %s, "
                 "AverageResponseTimeDetailed: %s, MinimumResponseTimeDetailed: %s, MaximumResponseTimeDetailed: %s, IPAddressUsed: %s",
                 __FUNCTION__, status, successCount, failureCount, avg, min, max, avg_ns, min_ns, max_ns, ipAddressUsed);

exit:
    return err;
}

//------------------------------------------------------------------------------------
// Array of valid input arguments
static char *trace_route_input_args[] =
{
    "Host",
    "Timeout",
    "MaxHopCount",
    // Not used.
    "DSCP",
    "DataBlockSize",
    "Interface",
    "ProtocolVersion",
};

//------------------------------------------------------------------------------------
// Array of valid output arguments
static char *trace_route_output_args[] =
{
    "Status",
    "ResponseTime",
    "RouteHops.",
};

/*********************************************************************//**
**
** SK_TR369_Start_TraceRoute
**
** Called when sync command Device.IP.Diagnostics.TraceRoute() is executed
**
** \param   req - pointer to structure identifying the command
** \param   command_key - not used, OnBoardRequest notification doesn't have a command key field
** \param   input_args - not used, the command doesn't receive parameters
** \param   output_args - not used, the command doesn't return values
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_Start_TraceRoute(dm_req_t *req, char *command_key, kv_vector_t *input_args, kv_vector_t *output_args)
{
    int i, err = USP_ERR_OK;
    // Input variables
    char *input_host, *input_timeout, *input_max_hop_count;

    // Extract the input arguments using KV_VECTOR_ functions
    input_host = USP_ARG_Get(input_args, "Host", "");
    input_timeout = USP_ARG_Get(input_args, "Timeout", "");
    input_max_hop_count = USP_ARG_Get(input_args, "MaxHopCount", "");

    USP_LOG_Debug("%s: Host: %s, Timeout: %s, MaxHopCount: %s",
                  __FUNCTION__, input_host, input_timeout, input_max_hop_count);

    if (strcmp(input_host, "") == 0
        || strcmp(input_timeout, "") == 0
        || strcmp(input_max_hop_count, "") == 0)
    {
        // if it doesn't, return invalid value
        USP_ERR_SetMessage("%s: Invalid value - The parameters for TraceRoute() are empty.", __FUNCTION__);
        err = USP_ERR_INVALID_VALUE;
        goto exit;
    }

    SK_TR369_SetDBParam("Device.IP.Diagnostics.TraceRoute.Host", input_host);
    SK_TR369_SetDBParam("Device.IP.Diagnostics.TraceRoute.Timeout", input_timeout);
    SK_TR369_SetDBParam("Device.IP.Diagnostics.TraceRoute.MaxHopCount", input_max_hop_count);

    SK_TR369_API_SetParams("skyworth.tr369.event", "TraceRoute");

    // Save all results into the output arguments using KV_VECTOR_ functions
    char status[32], responseTime[16];
    SK_TR369_GetDBParam("Device.IP.Diagnostics.TraceRoute.Status", status);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.TraceRoute.ResponseTime", responseTime);

    USP_ARG_Add(output_args, "Status", status);
    USP_ARG_Add(output_args, "ResponseTime", responseTime);

    USP_LOG_Info("%s: Status: %s, ResponseTime: %s", __FUNCTION__, status, responseTime);

    int_vector_t iv;
    INT_VECTOR_Init(&iv);
    err = DATA_MODEL_GetInstances("Device.IP.Diagnostics.TraceRoute.RouteHops", &iv);
    if (err != USP_ERR_OK)
    {
        INT_VECTOR_Destroy(&iv);
        goto exit;
    }

    for (i = 0; i < iv.num_entries; i++)
    {
        char output_host[32], output_host_path[32], host_path[MAX_DM_PATH] = {0};
        USP_SNPRINTF(output_host_path, sizeof(output_host_path), "RouteHops.%d.Host", iv.vector[i]);
        USP_SNPRINTF(host_path, sizeof(host_path), "Device.IP.Diagnostics.TraceRoute.%s", output_host_path);
        SK_TR369_API_GetParams(host_path, output_host, sizeof(output_host));

        char output_address[32], output_address_path[32], address_path[MAX_DM_PATH] = {0};
        USP_SNPRINTF(output_address_path, sizeof(output_address_path), "RouteHops.%d.HostAddress", iv.vector[i]);
        USP_SNPRINTF(address_path, sizeof(address_path), "Device.IP.Diagnostics.TraceRoute.%s", output_address_path);
        SK_TR369_API_GetParams(address_path, output_address, sizeof(output_address));

        char output_time[32], output_time_path[32], time_path[MAX_DM_PATH] = {0};
        USP_SNPRINTF(output_time_path, sizeof(output_time_path), "RouteHops.%d.RTTimes", iv.vector[i]);
        USP_SNPRINTF(time_path, sizeof(time_path), "Device.IP.Diagnostics.TraceRoute.%s", output_time_path);
        SK_TR369_API_GetParams(time_path, output_time, sizeof(output_time));

        USP_ARG_Add(output_args, output_host_path, output_host);
        USP_ARG_Add(output_args, output_address_path, output_address);
        USP_ARG_Add(output_args, output_time_path, output_time);

        USP_LOG_Info("%s: [%d] %s: %s, %s: %s, %s: %s",
                     __FUNCTION__,i, output_host_path, output_host, output_address_path, output_address, output_time_path, output_time);
    }
    INT_VECTOR_Destroy(&iv);

exit:
    return err;
}


//------------------------------------------------------------------------------------
// Array of valid input arguments
static char *download_diagnostics_input_args[] =
{
    "DownloadURL",
    "TimeBasedTestDuration",
    // Not used.
    "Interface",
    "DSCP",
    "EthernetPriority",
    "TimeBasedTestMeasurementInterval",
    "TimeBasedTestMeasurementOffset",
    "ProtocolVersion",
    "NumberOfConnections",
    "EnablePerConnectionResults",
};

//------------------------------------------------------------------------------------
// Array of valid output arguments
static char *download_diagnostics_output_args[] =
{
    "Status",
    "BOMTime",
    "EOMTime",
    "TestBytesReceived",
};

/*********************************************************************//**
**
** SK_TR369_Start_DownloadDiagnostics
**
** Called when sync command Device.IP.Diagnostics.DownloadDiagnostics() is executed
**
** \param   req - pointer to structure identifying the command
** \param   command_key - not used, OnBoardRequest notification doesn't have a command key field
** \param   input_args - not used, the command doesn't receive parameters
** \param   output_args - not used, the command doesn't return values
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_Start_DownloadDiagnostics(dm_req_t *req, char *command_key, kv_vector_t *input_args, kv_vector_t *output_args)
{
    int err = USP_ERR_OK;
    char param[1024] = {0};
    // Input variables
    char *input_download_url, *input_duration;

    // Extract the input arguments using KV_VECTOR_ functions
    input_download_url = USP_ARG_Get(input_args, "DownloadURL", "");
    input_duration = USP_ARG_Get(input_args, "TimeBasedTestDuration", "");

    USP_LOG_Debug("%s: DownloadURL: %s, TimeBasedTestDuration: %s",
                 __FUNCTION__, input_download_url, input_duration);

    if (strcmp(input_download_url, "") == 0
        || strcmp(input_duration, "") == 0)
    {
        // if it doesn't, return invalid value
        USP_ERR_SetMessage("%s: Invalid value - The parameters for DownloadDiagnostics() are empty.", __FUNCTION__);
        err = USP_ERR_INVALID_VALUE;
        goto exit;
    }

    strcpy(param, "DownloadDiagnostics###");
    strcat(param, input_download_url);
    strcat(param, "###");
    strcat(param, input_duration);

    SK_TR369_API_SetParams("skyworth.tr369.event", param);

    // Save all results into the output arguments using KV_VECTOR_ functions
    char status[32], BOMTime[32], EOMTime[32], testBytesReceived[16];
    SK_TR369_GetDBParam("Device.IP.Diagnostics.DownloadDiagnostics.Status", status);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.DownloadDiagnostics.BOMTime", BOMTime);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.DownloadDiagnostics.EOMTime", EOMTime);
    SK_TR369_GetDBParam("Device.IP.Diagnostics.DownloadDiagnostics.TestBytesReceived", testBytesReceived);

    USP_ARG_Add(output_args, "Status", status);
    USP_ARG_Add(output_args, "BOMTime", BOMTime);
    USP_ARG_Add(output_args, "EOMTime", EOMTime);
    USP_ARG_Add(output_args, "TestBytesReceived", testBytesReceived);

    USP_LOG_Info("%s: Status: %s, BOMTime: %s, EOMTime: %s, testBytesReceived: %s",
                 __FUNCTION__, status, BOMTime, EOMTime, testBytesReceived);

exit:
    return err;
}

/*********************************************************************//**
**
** SK_TR369_InitCustomEvent
**
** Customize Command events.
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_InitCustomEvent(void)
{
    int err = USP_ERR_OK;
    // X_Skyworth.Upload()
    err |= USP_REGISTER_SyncOperation("Device.X_Skyworth.UploadFile()", SK_TR369_Start_UploadFile);
    err |= USP_REGISTER_OperationArguments("Device.X_Skyworth.UploadFile()",
            upload_file_input_args, NUM_ELEM(upload_file_input_args),
            x_event_output_args, NUM_ELEM(x_event_output_args));

    // X_Skyworth.UpgradeFile()
    err |= USP_REGISTER_SyncOperation("Device.X_Skyworth.UpgradeFile()", SK_TR369_Start_UpgradeFile);
    err |= USP_REGISTER_OperationArguments("Device.X_Skyworth.UpgradeFile()",
            upgrade_file_input_args, NUM_ELEM(upgrade_file_input_args),
            x_event_output_args, NUM_ELEM(x_event_output_args));

    // X_Skyworth.DownloadFile()
    err |= USP_REGISTER_SyncOperation("Device.X_Skyworth.DownloadFile()", SK_TR369_Start_DownloadFile);
    err |= USP_REGISTER_OperationArguments("Device.X_Skyworth.DownloadFile()",
            download_file_input_args, NUM_ELEM(download_file_input_args),
            x_event_output_args, NUM_ELEM(x_event_output_args));

    // Device.IP.Diagnostics.IPPing()
    err |= USP_REGISTER_SyncOperation("Device.IP.Diagnostics.IPPing()", SK_TR369_Start_IPPing);
    err |= USP_REGISTER_OperationArguments("Device.IP.Diagnostics.IPPing()",
            ip_ping_input_args, NUM_ELEM(ip_ping_input_args),
            ip_ping_output_args, NUM_ELEM(ip_ping_output_args));

    // Device.IP.Diagnostics.TraceRoute()
    err |= USP_REGISTER_SyncOperation("Device.IP.Diagnostics.TraceRoute()", SK_TR369_Start_TraceRoute);
    err |= USP_REGISTER_OperationArguments("Device.IP.Diagnostics.TraceRoute()",
            trace_route_input_args, NUM_ELEM(trace_route_input_args),
            trace_route_output_args, NUM_ELEM(trace_route_output_args));

    // Device.IP.Diagnostics.DownloadDiagnostics()
    err |= USP_REGISTER_SyncOperation("Device.IP.Diagnostics.DownloadDiagnostics()", SK_TR369_Start_DownloadDiagnostics);
    err |= USP_REGISTER_OperationArguments("Device.IP.Diagnostics.DownloadDiagnostics()",
            download_diagnostics_input_args, NUM_ELEM(download_diagnostics_input_args),
            download_diagnostics_output_args, NUM_ELEM(download_diagnostics_output_args));


    return err;
}

//------------------------------------------------------------------------------------
// Array of multiple object arguments
char *sk_multi_object_map[] =
{
    "Device.DeviceInfo.TemperatureStatus.TemperatureSensor",
    "Device.DeviceInfo.FirmwareImage",
    "Device.Ethernet.Link",
    "Device.IP.Interface",
    "Device.IP.Interface.1.IPv4Address",
    "Device.WiFi.Radio",
    "Device.WiFi.SSID",
    "Device.WiFi.EndPoint",
    "Device.WiFi.EndPoint.1.Profile",
    "Device.Services.STBService",
    "Device.Services.STBService.1.AVPlayer",
    "Device.Services.STBService.1.Components.HDMI",
    "Device.Services.STBService.1.Components.AudioOutput",
    "Device.Services.STBService.1.Components.AudioDecoder",
    "Device.Services.STBService.1.Components.VideoOutput",
    "Device.Services.STBService.1.Components.VideoDecoder",
    "Device.Services.STBService.1.Capabilities.VideoDecoder.MPEG2Part2.ProfileLevel",
    "Device.Services.STBService.1.Capabilities.VideoDecoder.MPEG4Part2.ProfileLevel",
    "Device.Services.STBService.1.Capabilities.VideoDecoder.MPEG4Part10.ProfileLevel",
    "Device.USB.USBHosts.Host",
    "Device.USB.USBHosts.Host.1.Device"
//    "Device.IP.Diagnostics.TraceRoute.RouteHops",       // 由RouteHops()事件触发更新
//    "Device.DeviceInfo.ProcessStatus.Process",          // 该节点需要动态添加
//    "Device.WiFi.NeighboringWiFiDiagnostic.Result",     // 该节点需要动态添加
//    "Device.X_Skyworth.App",                    // 该节点需要动态添加
//    "Device.X_Skyworth.App.1.Permissions",      // 该节点需要动态添加
//    "Device.X_Skyworth.BluetoothDevice"         // 该节点需要动态添加
};

/*********************************************************************//**
**
** SK_TR369_SetDefaultProcessStatus
**
** Initialize the MultiObject node for "Device.DeviceInfo.ProcessStatus.Process.{i}."
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetDefaultProcessStatus(void)
{
    int err = USP_ERR_OK;
    char num_buf[MAX_DM_INSTANCE_ORDER] = {0};

    SK_TR369_API_GetParams("Device.DeviceInfo.ProcessStatus.ProcessNumberOfEntries", num_buf, sizeof(num_buf));
    int num = atoi(num_buf);
    USP_LOG_Info("%s: Get ProcessNumberOfEntries: %s (%d)", __FUNCTION__, num_buf, num);
    if (num > 0)
    {
        err = SK_TR369_UpdateMultiObject("Device.DeviceInfo.ProcessStatus.Process", num);
        if (err != USP_ERR_OK)
        {
            return err;
        }
    }
    return err;
}

/*********************************************************************//**
**
** SK_TR369_SetDefaultNeighboringWiFi
**
** Initialize the MultiObject node for "Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}."
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetDefaultNeighboringWiFi(void)
{
    int err = USP_ERR_OK;
    char num_buf[MAX_DM_INSTANCE_ORDER] = {0};

    SK_TR369_API_GetParams("Device.WiFi.NeighboringWiFiDiagnostic.ResultNumberOfEntries", num_buf, sizeof(num_buf));
    int num = atoi(num_buf);
    USP_LOG_Info("%s: Get ResultNumberOfEntries: %s (%d)", __FUNCTION__, num_buf, num);
    if (num > 0)
    {
        err = SK_TR369_UpdateMultiObject("Device.WiFi.NeighboringWiFiDiagnostic.Result", num);
        if (err != USP_ERR_OK)
        {
            return err;
        }
    }
    return err;
}

/*********************************************************************//**
**
** SK_TR369_SetDefaultMultiObject
**
** Initialize all MultiObject nodes. The node is obtained from the array of "sk_multi_object_map".
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetDefaultMultiObject(void)
{
    int i = 0;
    int err = USP_ERR_OK;
    int instance = INVALID;
    int_vector_t iv;

    int map_size = sizeof(sk_multi_object_map) / sizeof(sk_multi_object_map[0]);
    for (i = 0; i < map_size; i++)
    {
        INT_VECTOR_Init(&iv);
        err = DATA_MODEL_GetInstances(sk_multi_object_map[i], &iv);
        if (err != USP_ERR_OK)
        {
            INT_VECTOR_Destroy(&iv);
            continue;
        }
        if (iv.num_entries == 0)
        {
            err = DATA_MODEL_AddInstance(sk_multi_object_map[i], &instance, 0);
            USP_LOG_Info("%s: Nodes added: %s.%d", __FUNCTION__, sk_multi_object_map[i], instance);
            if (err != USP_ERR_OK)
            {
                INT_VECTOR_Destroy(&iv);
                continue;
            }
        }
        INT_VECTOR_Destroy(&iv);
    }

    // 初始化 ProcessStatus.Process.{i} MultiObject节点
    SK_TR369_SetDefaultProcessStatus();

    // 初始化 NeighboringWiFiDiagnostic.Result.{i} MultiObject节点
    SK_TR369_SetDefaultNeighboringWiFi();

    return err;
}