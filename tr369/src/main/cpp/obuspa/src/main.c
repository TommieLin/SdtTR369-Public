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
#include <stdbool.h>
#include <string.h>
#include <curl/curl.h>

#include "cli.h"
#include "bdc_exec.h"
#include "database.h"
#include "dm_exec.h"
#include "dm_trans.h"
#include "dm_inst_vector.h"
#include "os_utils.h"
#include "text_utils.h"
#include "retry_wait.h"
#include "vendor_api.h"
#include "sk_jni_callback.h"

//--------------------------------------------------------------------
// Pointer to the root data model nodes
extern dm_node_t *root_device_node;
extern dm_node_t *root_internal_node;

//--------------------------------------------------------------------
// Map containing all data model nodes, indexed by squashed hash value
dm_node_t *dm_node_map[MAX_NODE_MAP_BUCKETS] = { 0 };

//------------------------------------------------------------------------------
// Database paths associated with device parameters
static char *endpoint_id_path = "Device.LocalAgent.EndpointID";
static char *response_topic_path = "Device.LocalAgent.MTP.1.MQTT.ResponseTopicConfigured";
static char *short_msg_topic_path = "Device.MQTT.Client.1.Subscription.1.Topic";
static char *broker_address_path = "Device.MQTT.Client.1.BrokerAddress";
static char *broker_port_path = "Device.MQTT.Client.1.BrokerPort";
static char *transport_protocol_path = "Device.MQTT.Client.1.TransportProtocol";
static char *client_id_path = "Device.MQTT.Client.1.ClientID";
static char *client_username_path = "Device.MQTT.Client.1.Username";
static char *client_password_path = "Device.MQTT.Client.1.Password";

/*********************************************************************//**
**
** FindNodeFromHash
**
** Finds the node that matches the specified hash of its data model schema path
**
** \param   hash - hash value of the node path of the node to find
**
** \return  pointer to node matching hash in the data model, or NULL if no match
**
**************************************************************************/
dm_node_t *FindNodeFromHash(dm_hash_t hash)
{
    unsigned squashed_hash;
    dm_node_t *node;

    squashed_hash = ((unsigned)hash) % MAX_NODE_MAP_BUCKETS;

    // Find the node in the linked list of nodes which match the squashed hash
    node = dm_node_map[squashed_hash];
    while ((node != NULL) && (node->hash != hash))
    {
        node = node->next_node_map_link;
    }

    return node;
}

/*********************************************************************//**
**
** CreateNode
**
** Allocates and initialises a data model node, and adds it to dm_node_map[]
**
** \param   name - portion of the data model path that this node represents
** \param   type - type of node (eg object or parameter)
** \param   schema_path - path in the data model to this node. Used to calculate the hash for parameter nodes
**                      eg 'Device.LocalAgent.Controller.{i}.Enable'
**
** \return  pointer to created node
**
**************************************************************************/
dm_node_t *CreateNode(char *name, dm_node_type_t type, char *schema_path)
{
    dm_node_t *node;
    unsigned squashed_hash;
    dm_node_t *existing_node;
    dm_node_t *n;

    // Allocate memory for the node
    node = USP_MALLOC(sizeof(dm_node_t));
    memset(node, 0, sizeof(dm_node_t));     // NOTE: All roles start from zero permissions

    node->link.next = NULL;
    node->link.prev = NULL;
    node->next_node_map_link = NULL;
    node->type = type;
    node->name = USP_STRDUP(name);
    node->path = USP_STRDUP(schema_path);
    DLLIST_Init(&node->child_nodes);

    // Calculate hash of path
    node->hash = TEXT_UTILS_CalcHash(schema_path);

    // Exit if we have a hash collision
    n = FindNodeFromHash(node->hash);
    if (n != NULL)
    {
        USP_ERR_SetMessage("%s: Failed to add node %s because it's node hash conflicted with %s", __FUNCTION__, schema_path, n->name);
        USP_FREE(node);
        return NULL;
    }

    // Push this node at the front of the linked list of nodes matching the squashed hash
    squashed_hash = ((unsigned)node->hash) % MAX_NODE_MAP_BUCKETS;
    existing_node = dm_node_map[squashed_hash];
    if (existing_node != NULL)
    {
        node->next_node_map_link = existing_node;
    }
    dm_node_map[squashed_hash] = node;

    return node;
}

/*********************************************************************//**
**
** RegisterDefaultControllerTrust
**
** This function is called if no vendor hook overrides it
** Registers all controller trust roles and permissions
** This function is called inbetween VENDOR_Init() and VENDOR_Start()
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int RegisterDefaultControllerTrust(void)
{
    int err = USP_ERR_OK;

    // Currently, it is important that the first role registered is full access, as all controllers
    // inherit the first role in this table, and we currently want all controllers to have full access
    err |= USP_DM_RegisterRoleName(kCTrustRole_FullAccess, "Full Access");
    err |= USP_DM_AddControllerTrustPermission(kCTrustRole_FullAccess, "Device.", PERMIT_ALL);

    err |= USP_DM_RegisterRoleName(kCTrustRole_Untrusted,  "Untrusted");
    err |= USP_DM_AddControllerTrustPermission(kCTrustRole_Untrusted, "Device.", PERMIT_NONE);
    err |= USP_DM_AddControllerTrustPermission(kCTrustRole_Untrusted, "Device.DeviceInfo.", PERMIT_GET | PERMIT_OBJ_INFO);
    err |= USP_DM_AddControllerTrustPermission(kCTrustRole_Untrusted, "Device.LocalAgent.ControllerTrust.RequestChallenge()", PERMIT_OPER);
    err |= USP_DM_AddControllerTrustPermission(kCTrustRole_Untrusted, "Device.LocalAgent.ControllerTrust.ChallengeResponse()", PERMIT_OPER);

    if (err != USP_ERR_OK)
    {
        USP_ERR_SetMessage("%s() failed", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** DEVICE_MQTT_SetDefaultTopic
**
** Set the default value of the Topic node based on EndpointId. Related nodes:
**      1. Device.LocalAgent.MTP.1.MQTT.ResponseTopicConfigured
**      2. Device.MQTT.Client.1.Subscription.1.Topic (SMS topic)
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int DEVICE_MQTT_SetDefaultTopic(void)
{
    char endpoint_id[MAX_DM_SHORT_VALUE_LEN];
    char response_topic[MAX_DM_SHORT_VALUE_LEN];
    char short_msg_topic[MAX_DM_SHORT_VALUE_LEN];

    // Get the actual value of EndpointID
    // This may be the value in the USP DB or the default value (if not present in DB)
    int err = DATA_MODEL_GetParameterValue(endpoint_id_path, endpoint_id, sizeof(endpoint_id), 0);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Unable to get endpoint_id", __FUNCTION__);
        return err;
    }

    USP_SNPRINTF(response_topic, sizeof(response_topic), "sdtcpe/agent/resptopic/%s", endpoint_id);
    USP_SNPRINTF(short_msg_topic, sizeof(short_msg_topic), "sdtcpe/agent/shortmsg/%s", endpoint_id);

    // 1. Register the default value of ResponseTopicConfigured
    err = DATA_MODEL_SetParameterInDatabase(response_topic_path, response_topic);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Unable to set response_topic", __FUNCTION__);
        return err;
    }

    // 2. Register the default value of Subscription.1.Topic
    err = DATA_MODEL_SetParameterInDatabase(short_msg_topic_path, short_msg_topic);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Unable to set short_msg_topic", __FUNCTION__);
        return err;
    }

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** DEVICE_MQTT_SetDefaultServer
**
** Set the default value of the Mqtt Server node according to the data delivered by the server.
** Related nodes:
**      1. Device.MQTT.Client.1.BrokerAddress
**      2. Device.MQTT.Client.1.BrokerPort
**      3. Device.MQTT.Client.1.TransportProtocol
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int DEVICE_MQTT_SetDefaultServer(void)
{
    // Read the variable of the MQTT server URL
    char *mqtt_url = SK_TR369_GetMqttServerUrl();
    if ((mqtt_url == NULL) || (*mqtt_url == '\0') || (strcmp(mqtt_url, "null") == 0))
    {
        USP_LOG_Error("%s: Failed to read the MQTT server URL.", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }
    USP_LOG_Debug("%s: The server URL that needs to be subscribed is %s", __FUNCTION__, mqtt_url);

    // Separate the protocol, address, and port from the url
    char *protocol_ptr = strstr(mqtt_url, "://");
    if (protocol_ptr == NULL)
    {
        USP_LOG_Error("%s: The URL format is incorrect and the protocol cannot be extracted.", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }

    // Calculate the extracted protocol length.
    size_t protocol_length = protocol_ptr - mqtt_url;

    // Declare and initialize the string storing the protocol.
    char protocol[protocol_length + 1];
    strncpy(protocol, mqtt_url, protocol_length);
    protocol[protocol_length] = '\0';
    USP_LOG_Debug("%s: The parsed protocol is %s", __FUNCTION__, protocol);

    // Move the pointer to the beginning of the address.
    char *address_ptr = protocol_ptr + 3; // Skip "://"

    // Look for the ":" position, which marks the separation of address and port.
    char *port_separator_ptr = strchr(address_ptr, ':');
    if (port_separator_ptr == NULL)
    {
        USP_LOG_Error("%s: The URL format is incorrect and the address and port cannot be extracted.", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }

    // Calculate the extracted address length.
    size_t address_length = port_separator_ptr - address_ptr;

    // Declare and initialize the string storing the address.
    char address[address_length + 1];
    strncpy(address, address_ptr, address_length);
    address[address_length] = '\0';
    USP_LOG_Debug("%s: The parsed address is %s", __FUNCTION__, address);

    // Extract the port number and print it.
    char port[7]; // Assume the port number is at most 6 digits.
    strcpy(port, port_separator_ptr + 1);
    USP_LOG_Debug("%s: The parsed port is %s", __FUNCTION__, port);

    // 1. Register the default value of BrokerAddress
    int err = DATA_MODEL_SetParameterInDatabase(broker_address_path, address);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Unable to set broker_address", __FUNCTION__);
        return err;
    }

    // 2. Register the default value of BrokerPort
    err = DATA_MODEL_SetParameterInDatabase(broker_port_path, port);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Unable to set broker_port", __FUNCTION__);
        return err;
    }

    // 3. Register the default value of TransportProtocol
    if (strcasecmp(protocol, "tcp") == 0)
    {
        err = DATA_MODEL_SetParameterInDatabase(transport_protocol_path, "TCP/IP");
    }
    else if (strcasecmp(protocol, "ssl") == 0)
    {
        err = DATA_MODEL_SetParameterInDatabase(transport_protocol_path, "TLS");
    }
    else
    {
        USP_LOG_Error("%s: This protocol format (%s) is not recognized.", __FUNCTION__, protocol);
        err = USP_ERR_INTERNAL_ERROR;
    }
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Unable to set transport_protocol", __FUNCTION__);
        return err;
    }

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** DEVICE_MQTT_SetDefaultClient
**
** Set the default value of the Mqtt Client node according to the data delivered by the server.
** Related nodes:
**      1. Device.MQTT.Client.1.ClientID
**      2. Device.MQTT.Client.1.Username
**      3. Device.MQTT.Client.1.Password
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int DEVICE_MQTT_SetDefaultClient(void)
{
    // 1. Read the variable of the MQTT client ID
    char *client_id = SK_TR369_GetMqttClientId();
    if ((client_id == NULL) || (*client_id == '\0') || (strcmp(client_id, "null") == 0))
    {
        USP_LOG_Error("%s: Failed to read the MQTT client ID", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }
    USP_LOG_Debug("%s: The client ID that needs to be subscribed is %s", __FUNCTION__, client_id);

    int err = DATA_MODEL_SetParameterInDatabase(client_id_path, client_id);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Unable to set client_id", __FUNCTION__);
        return err;
    }

    // 2. Read the variable of the MQTT client username
    char *client_username = SK_TR369_GetMqttUsername();
    if ((client_username == NULL) || (*client_username == '\0') || (strcmp(client_username, "null") == 0))
    {
        USP_LOG_Error("%s: Failed to read the MQTT client username", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }
    USP_LOG_Debug("%s: The client username that needs to be subscribed is %s", __FUNCTION__, client_username);

    err = DATA_MODEL_SetParameterInDatabase(client_username_path, client_username);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Unable to set client_username", __FUNCTION__);
        return err;
    }

    // 3. Read the variable of the MQTT client password
    char *client_password = SK_TR369_GetMqttPassword();
    if ((client_password == NULL) || (*client_password == '\0') || (strcmp(client_password, "null") == 0))
    {
        USP_LOG_Error("%s: Failed to read the MQTT client password", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }
    USP_LOG_Debug("%s: The client password that needs to be subscribed is %s", __FUNCTION__, client_password);

    err = DATA_MODEL_SetParameterInDatabase(client_password_path, client_password);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Unable to set client_password", __FUNCTION__);
        return err;
    }

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_DATA_MODEL_Init
**
** Initialise the data model and register all nodes in the data model schema
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_DATA_MODEL_Init(void)
{
    int err;

    // Allocate the root nodes for the data model
#define DEVICE_NODE_NAME "Device"
    root_device_node = CreateNode(DEVICE_NODE_NAME, kDMNodeType_Object_SingleInstance, DEVICE_NODE_NAME);

#define INTERNAL_NODE_NAME "Internal"
    root_internal_node = CreateNode(INTERNAL_NODE_NAME, kDMNodeType_Object_SingleInstance, INTERNAL_NODE_NAME);

#ifdef ENABLE_COAP
    // Initialise CoAP protocol layer
    COAP_Init();
#endif

#ifdef ENABLE_WEBSOCKETS
    // Initialise WebSockets protocol layer
    WSCLIENT_Init();
    WSSERVER_Init();
#endif

    // Register core implemented nodes in the schema
    is_executing_within_dm_init = true;
    err = USP_ERR_OK;
    err |= DEVICE_LOCAL_AGENT_Init();
    err |= DEVICE_SECURITY_Init();
#ifndef REMOVE_DEVICE_TIME
    err |= DEVICE_TIME_Init();
#endif
    err |= DEVICE_CONTROLLER_Init();
    err |= DEVICE_MTP_Init();

#ifndef DISABLE_STOMP
    err |= DEVICE_STOMP_Init();
#endif

#ifdef ENABLE_MQTT
    err |= DEVICE_MQTT_Init();
#endif
    err |= DEVICE_SUBSCRIPTION_Init();
    err |= DEVICE_CTRUST_Init();
    err |= DEVICE_REQUEST_Init();
    err |= DEVICE_BULKDATA_Init();

#ifndef REMOVE_SELF_TEST_DIAG_EXAMPLE
    // Register data model parameters used by the Self Test Diagnostics example code
    err |= DEVICE_SELF_TEST_Init();
#endif

    // Exit if an error has occurred
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Unexpected error(%d).", __FUNCTION__, err);
        return err;
    }

    // Register vendor nodes in the schema
    err = VENDOR_Init();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: VENDOR_Init error(%d).", __FUNCTION__, err);
        return err;
    }

    // Exit if unable to potentially perform a programmatic factory reset of the parameters in the database
    // NOTE: This must be performed before DEVICE_LOCAL_AGENT_SetDefaults(), but after VENDOR_Init()
    err = DATABASE_Start();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: DATABASE_Start error(%d).", __FUNCTION__, err);
        return err;
    }

    // Set the default values of OUI, Serial Number and (LocalAgent) EndpointID, and cache EndpointID
    err = DEVICE_LOCAL_AGENT_SetDefaults();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: DEVICE_LOCAL_AGENT_SetDefaults error(%d).", __FUNCTION__, err);
        return err;
    }

#ifdef ENABLE_MQTT
    // Set the default value of the Topic node based on EndpointId
    err = DEVICE_MQTT_SetDefaultTopic();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: DEVICE_MQTT_SetDefaultTopic error(%d).", __FUNCTION__, err);
        return err;
    }

    // Set the default value of the Mqtt Server node according to the data delivered by the server
    err = DEVICE_MQTT_SetDefaultServer();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: DEVICE_MQTT_SetDefaultServer error(%d).", __FUNCTION__, err);
        return err;
    }

    // Set the default value of the Mqtt Client node according to the data delivered by the server
    err = DEVICE_MQTT_SetDefaultClient();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: DEVICE_MQTT_SetDefaultClient error(%d).", __FUNCTION__, err);
        return err;
    }
#endif

    is_executing_within_dm_init = false;

    // If the code gets here, then all of the data model components initialised successfully
    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_DATA_MODEL_Start
**
** Instantiate all instances in the data model
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_DATA_MODEL_Start(void)
{
    int err;
    dm_trans_vector_t trans;
    register_controller_trust_cb_t register_controller_trust_cb;

    // Seed data model with instance numbers from the database
    if (is_running_cli_local_command == false)
    {
        err = DATABASE_ReadDataModelInstanceNumbers(false);
        if (err != USP_ERR_OK)
        {
            USP_LOG_Error("%s: DATABASE_ReadDataModelInstanceNumbers error(%d).", __FUNCTION__, err);
            return err;
        }
    }

    // Determine function to call to register controller trust
    register_controller_trust_cb = vendor_hook_callbacks.register_controller_trust_cb;
    if (register_controller_trust_cb == NULL)
    {
        register_controller_trust_cb = RegisterDefaultControllerTrust;
    }

    // Set all roles and permissions
    // NOTE: This must be done before any transaction is started otherwise object deletion notifications are not sent
    // (because we are unable to generate the list of objects in a deletion subscription because of lack of permissions)
    err = register_controller_trust_cb();
    if (err != USP_ERR_OK)
    {
        USP_ERR_SetMessage("%s: register_controller_trust_cb() failed", __FUNCTION__);
        return err;
    }

    // As most start routines also clean the database, start a transaction
    err = DM_TRANS_Start(&trans);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: DM_TRANS_Start error(%d).", __FUNCTION__, err);
        goto exit;
    }

    // Exit if unable to start all nodes in the schema (that require a separate start)
    // Typically these functions seed the data model with instance numbers or require the
    // data model to be running to access database parameters (seeded from the database - above)
    err = USP_ERR_OK;
    err |= DEVICE_LOCAL_AGENT_Start();
#ifndef REMOVE_DEVICE_TIME
    err |= DEVICE_TIME_Start();
#endif
    err |= DEVICE_CONTROLLER_Start();

    // Load trust store and client certs into USP Agent's cache
    // NOTE: This call does not leave any dynamic allocations owned by SSL (which is necessary, since libwebsockets is going to re-initialise SSL)
    err |= DEVICE_SECURITY_Start();

#ifdef ENABLE_WEBSOCKETS
    // IMPORTANT: libwebsockets re-initialises libssl here, then loads the trust store and client cert from USP Agent's cache
    err |= WSCLIENT_Start();
    err |= WSSERVER_Start();
#endif

#ifndef DISABLE_STOMP
    err |= DEVICE_STOMP_Start();          // NOTE: This must come after DEVICE_SECURITY_Start(), as it assumes the trust store and client certs have been locally cached
#endif

#ifdef ENABLE_COAP
    err |= COAP_Start();                  // NOTE: This must come after DEVICE_SECURITY_Start(), as it assumes the trust store and client certs have been locally cached
#endif

#ifdef ENABLE_MQTT
    err |= DEVICE_MQTT_Start();
#endif
    err |= DEVICE_MTP_Start();            // NOTE: This must come after COAP_Start, as it assumes that the CoAP SSL contexts have been created
    err |= DEVICE_SUBSCRIPTION_Start();   // NOTE: This must come after DEVICE_LOCAL_AGENT_Start(), as it calls DEVICE_LOCAL_AGENT_GetRebootInfo()
    err |= DEVICE_CTRUST_Start();
    err |= DEVICE_BULKDATA_Start();

    // Always start the vendor last
    err |= VENDOR_Start();

    // Refresh all objects which use the refresh instances vendor hook
    // This provides the baseline after which object/additions deletions are notified (if relevant subscriptions exist)
    DM_INST_VECTOR_RefreshBaselineInstances(root_device_node);

    // Ensure that if the Boot! event is generated a long time after startup, that it refreshes the instance numbers if they have expired
    DM_INST_VECTOR_NextLockPeriod();

exit:
    // Commit all database changes
    if (err == USP_ERR_OK)
    {
        err = DM_TRANS_Commit();
    }
    else
    {
        USP_LOG_Error("%s: Unexpected error(%d).", __FUNCTION__, err);
        DM_TRANS_Abort(); // Ignore error from this - we want to return the error from the body of this function instead
    }

    return err;
}

/*********************************************************************//**
**
** SK_TR369_USP_Agent_Start
**
** Initializes and starts USP Agent
**
** \param   db_file - pointer to name of USP Agent's database file to open
** \param   enable_mem_info - Set to true if memory debugging info should be collected
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_USP_Agent_Start(char *db_file, bool enable_mem_info)
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
    err = SK_TR369_DATA_MODEL_Init();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: SK_TR369_DATA_MODEL_Init error(%d).", __FUNCTION__, err);
        return err;
    }

    // Start logging memory usage from now on (since the static data model schema allocations have completed)
    if (enable_mem_info)
    {
        USP_MEM_StartCollection();
    }

    // Exit if unable to start the datamodel objects
    err = SK_TR369_DATA_MODEL_Start();
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: SK_TR369_DATA_MODEL_Start error(%d).", __FUNCTION__, err);
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
** SK_TR369_MAIN_Start
**
** Customized initialization main function.
**
** \param   model_path - Node list storage path. The default file is: sdt_tms_tr369_model.xml
**              The default path is: /data/user/0/com.sdt.android.tr369/sdt_tms_tr369_model.xml
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_MAIN_Start(const char *const model_path)
{
    int err;
    bool enable_mem_info = false;

    // Verbosity level
    SK_TR369_SetUspLogLevel(kLogLevel_Debug);

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
    err = SK_TR369_USP_Agent_Start(DEFAULT_DATABASE_FILE, enable_mem_info);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: SK_TR369_USP_Agent_Start error(%d).", __FUNCTION__, err);
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


