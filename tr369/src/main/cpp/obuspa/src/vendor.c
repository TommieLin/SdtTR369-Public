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
 * \file vendor.c
 *
 * Implements the interface to all vendor implemented data model nodes
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

#include <libxml/parser.h>
#include <libxml/tree.h>

#include "usp_err_codes.h"
#include "vendor_defs.h"
#include "vendor_api.h"
#include "usp_api.h"
#include "usp_log.h"
#include "sk_tr369_jni.h"
#include "sk_jni_callback.h"
#include "data_model.h"
#include "database.h"
#include "dm_trans.h"


//------------------------------------------------------------------------------------
// Skyworth Customized Content
typedef struct
{
    char name[MAX_PATH_SEGMENTS];   // 节点的名字
    char path[MAX_DM_PATH];         // 节点完整路径
    unsigned type;                  // 节点存储的数据类型
#ifdef ENABLE_SK_INFORM_CONFIG
    int inform;
    int app_inform;
#endif
//    char write;
    dm_get_value_cb_t getter;       // Get函数
    dm_set_value_cb_t setter;       // Set函数
//    dm_notify_set_cb_t notification;
    char value[MAX_DM_SHORT_VALUE_LEN];   // 默认的值 对应xml中的default
} sk_schema_node_t;

char *sk_tr369_model_xml = NULL;

/*********************************************************************//**
**
** SK_TR369_GetVendorParam
**
** Customized Get interface.
**
** Priority should be given to calling the API of the Java layer, and if the call fails,
** read the data from the database.
**
** \param   req - pointer to structure identifying the parameter
** \param   buf - pointer to buffer in which to return the value
** \param   len - length of return buffer
**
** \return  Node
**
**************************************************************************/
int SK_TR369_GetVendorParam(dm_req_t *req, char *buf, int len)
{
    int err = USP_ERR_OK;
    USP_LOG_Info("%s: The requested path: %s", __FUNCTION__, req->path);
    err = SK_TR369_API_GetParams(req->path, buf, len);
    USP_LOG_Debug("%s: SK_TR369_API_GetParams return: %d", __FUNCTION__, err);
    if (err != 0)
    {
        USP_ERR_SetMessage("%s: Execution failed: %s", __FUNCTION__, req->path);
        err = USP_ERR_SK_API_CALL_FAILURE;
    }
    return err;
}

/*********************************************************************//**
**
** SK_TR369_SetVendorParam
**
** Customized Set interface.
**
** Priority should be given to calling the API of the Java layer, and if the call fails,
** it should be written directly to the database.
**
** \param   req - pointer to structure identifying the parameter
** \param   buf - pointer to buffer in which to return the value
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetVendorParam(dm_req_t *req, char *buf)
{
    int err = USP_ERR_OK;
    USP_LOG_Info("%s: The requested path: %s", __FUNCTION__, req->path);
    err = SK_TR369_API_SetParams(req->path, buf);
    USP_LOG_Debug("%s: SK_TR369_API_SetParams return: %d", __FUNCTION__, err);
    if (err != 0)
    {
        USP_ERR_SetMessage("%s: Execution failed: %s", __FUNCTION__, req->path);
        err = USP_ERR_SK_API_CALL_FAILURE;
    }
    return err;
}

/*********************************************************************//**
**
** SK_TR369_GetNodeFullName
**
** Retrieve the complete path of the node in the XML and return it as its name.
**
** \param   node - The pointer to a node in XML.
** \param   name - The pointer to return the full name.
**
** \return  Node
**
**************************************************************************/
void SK_TR369_GetNodeFullName(xmlNodePtr node, char *name)
{
    xmlNodePtr current = node;
    if (node == NULL || name == NULL)
    {
        USP_LOG_Error("%s: Parameter error.", __FUNCTION__);
        return;
    }
    while (current != NULL)
    {
        xmlChar *node_name = xmlGetProp(current, (const xmlChar *)"name");
        if (node_name == NULL)
        {
            break;
        }

        xmlChar fullName[MAX_DM_PATH] = {0};
        if (name[0] != '\0')
        {
            sprintf((char *)fullName, "%s.%s", node_name, name);
        }
        else
        {
            sprintf((char *)fullName, "%s", node_name);
        }

        sprintf(name, "%s", fullName);
        USP_LOG_Debug("%s: The complete path of the node: %s", __FUNCTION__, name);

        xmlFree(node_name);
        current = current->parent;
    }
}

/*********************************************************************//**
**
** SK_TR369_ParseNode
**
** Parse the specific information of the node, including name, getter, setter, and default.
**
** \param   xml_node - The pointer to a node in XML.
** \param   schema_node - The specific content of the node to be parsed.
**
** \return  Node
**
**************************************************************************/
void SK_TR369_ParseNode(xmlNodePtr xml_node, sk_schema_node_t *schema_node)
{
    if (schema_node == NULL)
    {
        USP_LOG_Error("%s: Parameter error.", __FUNCTION__);
        return;
    }

    SK_TR369_GetNodeFullName(xml_node, schema_node->path);
    xmlChar *name = xmlGetProp(xml_node, (const xmlChar *)"name");
    xmlChar *getter = xmlGetProp(xml_node, (const xmlChar *)"getter");
    xmlChar *setter = xmlGetProp(xml_node, (const xmlChar *)"setter");

    if (name != NULL) sprintf(schema_node->name, "%s", name);
    schema_node->getter = (getter != NULL && (xmlStrcmp(getter, (const xmlChar *)"diagnose") == 0)) ? SK_TR369_GetVendorParam : NULL;
    schema_node->setter = (setter != NULL && (xmlStrcmp(setter, (const xmlChar *)"diagnose") == 0)) ? SK_TR369_SetVendorParam : NULL;

    xmlChar *default_value = xmlGetProp(xml_node, (const xmlChar *)"default");
    if (default_value != NULL) sprintf(schema_node->value, "%s", default_value);

    USP_LOG_Debug("%s: Path: %s, Name: %s, Getter: %s, Setter: %s, Default: %s",
            __FUNCTION__, schema_node->path, name, getter, setter, default_value);

#ifdef ENABLE_SK_INFORM_CONFIG
    // 判断该节点是否需要放到启动参数里上报(该判断交由服务端决定，即由服务端下发指令设置BootParameter)
    xmlChar *inform = xmlGetProp(xml_node, (const xmlChar *)"inform");
    if (inform != NULL && !xmlStrcmp(inform, (const xmlChar *)"true"))
    {
        USP_LOG_Debug("%s: schema_node->inform true", __FUNCTION__);
        schema_node->inform = 1;
    }
    xmlFree(inform);
#endif
    xmlFree(name);
    xmlFree(getter);
    xmlFree(setter);
}

/*********************************************************************//**
**
** SK_TR369_ParseType
**
** Parse the type of the node and convert it to the corresponding format supported by USP.
**
** \param   type - The type of node in the XML file.
** \param   schema_node - The specific content of the node to be parsed.
**
** \return  None
**
**************************************************************************/
void SK_TR369_ParseType(xmlChar *type, sk_schema_node_t *schema_node)
{
    if (schema_node == NULL)
    {
        USP_LOG_Error("%s: Parameter error.", __FUNCTION__);
        return;
    }

    if (type == NULL)
    {
        schema_node->type = DM_STRING;
    }
    else if (!xmlStrcmp(type, (const xmlChar *)"string"))
    {
        schema_node->type = DM_STRING;
    }
    else if (!xmlStrcmp(type, (const xmlChar *)"boolean"))
    {
        schema_node->type = DM_BOOL;
    }
    else if (!xmlStrcmp(type, (const xmlChar *)"dateTime"))
    {
        schema_node->type = DM_DATETIME;
    }
    else if (!xmlStrcmp(type, (const xmlChar *)"int"))
    {
        schema_node->type = DM_INT;
    }
    else if (!xmlStrcmp(type, (const xmlChar *)"unsignedInt"))
    {
        schema_node->type = DM_UINT;
    }
    else if (!xmlStrcmp(type, (const xmlChar *)"long"))
    {
        schema_node->type = DM_LONG;
    }
    else if (!xmlStrcmp(type, (const xmlChar *)"unsignedLong"))
    {
        schema_node->type = DM_ULONG;
    }
    else if (!xmlStrcmp(type, (const xmlChar *)"base64"))
    {
        schema_node->type = DM_BASE64;
    }
    else if (!xmlStrcmp(type, (const xmlChar *)"hex"))
    {
        schema_node->type = DM_HEXBIN;
    }
    else if (!xmlStrcmp(type, (const xmlChar *)"decimal"))
    {
        schema_node->type = DM_DECIMAL;
    }
    else
    {
        schema_node->type = DM_STRING;
    }
}

/*********************************************************************//**
**
** SK_TR369_AddNodeToUspDataModel
**
** Register the nodes parsed from the XML file onto the USP.
**
** \param   schema_node - The specific content of the node to be added.
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_AddNodeToUspDataModel(sk_schema_node_t *schema_node)
{
    int err = USP_ERR_OK;

    if (schema_node == NULL)
    {
        USP_LOG_Error("%s: Parameter error.", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }

    if (schema_node->setter != NULL)
    {
        USP_LOG_Debug("%s: USP_REGISTER_VendorParam_ReadWrite(%s...)", __FUNCTION__, schema_node->path);
        if (schema_node->getter != NULL)
        {
            err |= USP_REGISTER_VendorParam_ReadWrite(
                    schema_node->path,
                    schema_node->getter,
                    schema_node->setter,
                    NULL,
                    DM_STRING);
        }
        else
        {
            err |= USP_REGISTER_VendorParam_ReadWrite(
                    schema_node->path,
                    SK_TR369_GetVendorParam,
                    schema_node->setter,
                    NULL,
                    DM_STRING);
        }
    }
    else
    {
        if (schema_node->getter != NULL)
        {
            USP_LOG_Debug("%s: USP_REGISTER_VendorParam_ReadOnly(%s...)", __FUNCTION__, schema_node->path);
            err |= USP_REGISTER_VendorParam_ReadOnly(
                    schema_node->path,
                    schema_node->getter,
                    DM_STRING);
        }
        else
        {
            if (strlen(schema_node->value) != 0)
            {
                USP_LOG_Debug("%s: USP_REGISTER_DBParam_ReadOnly(%s...)", __FUNCTION__, schema_node->path);
                err |= USP_REGISTER_DBParam_ReadOnly(
                        schema_node->path,
                        schema_node->value,
                        schema_node->type);
            }
        }
    }

    return err;
}

#ifdef ENABLE_SK_INFORM_CONFIG
/*********************************************************************//**
**
** SK_TR369_AddNodeToBootParameter
**
** Add the node to the BootParameter startup parameter list.
**
** \param   schema_node - The specific content of the node to be added.
** \param   index - The index that needs to be added to the list.
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_AddNodeToBootParameter(sk_schema_node_t *schema_node, int index)
{
    int err = USP_ERR_OK;
    char enable_path[MAX_DM_PATH], param_path[MAX_DM_PATH];

    if (schema_node == NULL)
    {
        USP_LOG_Error("%s: Parameter error.", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }

    USP_SNPRINTF(enable_path, sizeof(enable_path), "Device.LocalAgent.Controller.1.BootParameter.%d.Enable", index);
    USP_SNPRINTF(param_path, sizeof(param_path), "Device.LocalAgent.Controller.1.BootParameter.%d.ParameterName", index);
    USP_LOG_Debug("%s: %s: %s", __FUNCTION__, param_path, schema_node->path);

    err |= DATA_MODEL_SetParameterInDatabase(enable_path, "true");
    err |= DATA_MODEL_SetParameterInDatabase(param_path, schema_node->path);
    USP_LOG_Debug("%s: The result of setting parameters is: %d", __FUNCTION__, err);
    return err;
}
#endif

/*********************************************************************//**
**
** SK_TR369_ParseSchema
**
** Parse the architecture from the root node, and then recursively parse the child nodes in sequence.
**
** \param   node - Root node
**
** \return  None
**
**************************************************************************/
void SK_TR369_ParseSchema(xmlNodePtr node)
{
    xmlNodePtr current = node;
    while (current != NULL)
    {
        if (xmlStrcmp(current->name, (const xmlChar *)"schema") == 0)
        {
            xmlChar *type = xmlGetProp(current, (const xmlChar *)"type");

            if (!xmlStrcmp(type, (const xmlChar *)"multipleNumber"))
            {
                xmlChar *table = xmlGetProp(current, (const xmlChar *)"table");
                if (table != NULL)
                {
                    char node_path[MAX_DM_PATH] = {0};
                    char table_path[MAX_DM_PATH] = {0};
                    sprintf(table_path, "%s", table);
                    SK_TR369_GetNodeFullName(current, node_path);
                    USP_LOG_Debug("%s: MultipleNumber: %s (%s)", __FUNCTION__, node_path, table_path);
                    USP_REGISTER_Param_NumEntries(node_path, table_path);
                    free(table);
                }
            }
            else if (!xmlStrcmp(type, (const xmlChar *)"multipleObject"))
            {
                char node_path[MAX_DM_PATH] = {0};
                SK_TR369_GetNodeFullName(current, node_path);
                USP_LOG_Debug("%s: MultipleObject: %s", __FUNCTION__, node_path);
                USP_REGISTER_Object(node_path, NULL, NULL, NULL, NULL, NULL, NULL);
            }
            else if (xmlStrcmp(type, (const xmlChar *)"object")
                    && xmlStrcmp(type, (const xmlChar *)"unknown"))
            {
                // 真正需要处理的部分：object类型的节点
                sk_schema_node_t schema_node = {0};
                // 解析出该节点的具体信息，包括name、getter、setter、default
                SK_TR369_ParseNode(current, &schema_node);
                // 解析出该节点的type，并将起转换为usp支持的对应格式
                SK_TR369_ParseType(type, &schema_node);
                // 将节点登记至usp上
                SK_TR369_AddNodeToUspDataModel(&schema_node);

#ifdef ENABLE_SK_INFORM_CONFIG
                // 判断该节点是否需要放到启动参数里上报(该判断交由服务端决定，即由服务端下发指令设置BootParameter)
                static int boot_param_number = 0;
                if (schema_node.inform == 1)
                {
                    boot_param_number++;
                    SK_TR369_AddNodeToBootParameter(&schema_node, boot_param_number);
                }
#endif
            }

            xmlFree(type);
        }
        // 继续递归调用所有子节点
        SK_TR369_ParseSchema(current->children);
        current = current->next;
    }
}

/*********************************************************************//**
**
** SK_TR369_ParseModelFile
**
** Parse the XML file and register each node for USP.
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_ParseModelFile(void)
{
    if (sk_tr369_model_xml == NULL)
    {
        USP_LOG_Error("%s: Model file path not initialized.", __FUNCTION__);
        return USP_ERR_SK_INIT_FAILURE;
    }
    USP_LOG_Debug("%s: Model file path: %s", __FUNCTION__, sk_tr369_model_xml);
    // 打开xml文件
    xmlDocPtr doc = xmlReadFile(sk_tr369_model_xml, "UTF-8", XML_PARSE_RECOVER);
    if (doc == NULL)
    {
        USP_LOG_Error("%s: Failed to read tr369 model file (%s)", __FUNCTION__, sk_tr369_model_xml);
        return USP_ERR_INTERNAL_ERROR;
    }
    // 获取根节点
    xmlNodePtr root = xmlDocGetRootElement(doc);
    // 从根节点解析架构
    SK_TR369_ParseSchema(root);
    // 释放空间
    xmlFreeDoc(doc);
    xmlCleanupParser();

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** VENDOR_Init
**
** Initialises this component, and registers all parameters and vendor hooks, which it implements
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int VENDOR_Init(void)
{
    // 解析Xml文件
    SK_TR369_ParseModelFile();
    // 初始化自定义事件
    SK_TR369_InitCustomEvent();

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** trim
**
** Remove whitespace at the beginning and end of the string.
**
** \param   str
**
** \return  None
**
**************************************************************************/
void trim(char *str)
{
    int len = strlen(str);
    if (len == 0) return;

    int start = 0;
    int end = len - 1;

    // Trim leading spaces
    while (start < len && (str[start] == ' ' || str[start] == '\t'))
    {
        start++;
    }

    // Trim trailing spaces
    while (end >= start
            && (str[end] == ' ' || str[end] == '\t' || str[end] == '\n' || str[end] == '\r'))
    {
        end--;
    }

    int trimmed_length = end - start + 1;

    if (start > 0)
    {
        memmove(str, str + start, trimmed_length);
    }
    str[trimmed_length] = '\0';
}

/*********************************************************************//**
**
** VENDOR_Start
**
** Called after data model has been registered and after instance numbers have been read from the USP database
** Typically this function is used to seed the data model with instance numbers or
** initialise internal data structures which require the data model to be running to access parameters
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int VENDOR_Start(void)
{
    // 设置默认MultiObject类型的节点
    SK_TR369_SetDefaultMultiObject();

    return USP_ERR_OK;
}

/*********************************************************************//**
**
** VENDOR_Stop
**
** Called when stopping USP agent gracefully, to free up memory and shutdown
** any vendor processes etc
**
** \param   None
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int VENDOR_Stop(void)
{
    // TODO
    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_GetDBParam
**
** The interface used to retrieve the value of a node from the database.
**
** \param   param - The name of the node to be get.
** \param   value - The value of the node to be get.
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_GetDBParam(const char *param, char *value)
{
    int err;
    dm_hash_t hash;
    char instances[MAX_DM_PATH];
    unsigned path_flags;

    // Exit if parameter path is incorrect
    err = DM_PRIV_FormDB_FromPath(param, &hash, instances, sizeof(instances));
    if (err != USP_ERR_OK)
    {
        return err;
    }

    // Exit, not printing any value, if this parameter is obfuscated (eg containing a password)
    value[0] = '\0';
    path_flags = DATA_MODEL_GetPathProperties(param, INTERNAL_ROLE, NULL, NULL, NULL);
    if (path_flags & PP_IS_SECURE_PARAM)
    {
        goto exit;
    }

    // Exit if unable to get value of parameter from DB
    USP_ERR_ClearMessage();
    err = DATABASE_GetParameterValue(param, hash, instances, value, MAX_DM_VALUE_LEN, 0);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("Parameter %s exists in the schema, but does not exist in the database", param);
        return err;
    }

exit:
    USP_LOG_Info("%s: The data obtained from the database: %s -> %s", __FUNCTION__, param, value);
    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_SetDBParam
**
** The interface used to set the values corresponding to nodes to the database.
**
** \param   param - The name of the node to be set.
** \param   value - The value of the node to be set.
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_SetDBParam(const char *param, const char *value)
{
    USP_LOG_Info("%s: The parameters to be executed are: %s -> %s", __FUNCTION__, param, value);
    // Exit if unable to directly set the parameter in the database
    int err = DATA_MODEL_SetParameterInDatabase(param, value);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Failed to set parameter: \"%s\"", __FUNCTION__, param);
    }
    return err;
}

/*********************************************************************//**
**
** SK_TR369_AddInstance
**
** An interface provided for internal use to add nodes of MultiObject type.
**
** \param   param - Name of the MultiObject type node to be added.
** \param   num - Number of MultiObject type nodes to be added.
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_AddInstance(const char *param, int num)
{
    int i, err;
    int instance = INVALID;
    char path[MAX_DM_PATH];
    kv_vector_t unique_key_params;

    KV_VECTOR_Init(&unique_key_params);

    for (i = 0; i < num; i++)
    {
        err = DATA_MODEL_AddInstance(param, &instance, 0);
        if (err != USP_ERR_OK)
        {
            goto exit;
        }
        USP_SNPRINTF(path, sizeof(path), "%s.%d", param, instance);
        USP_LOG_Debug("%s: The node to be added is: %s", __FUNCTION__, path);

        // Exit if unable to retrieve the parameters used as unique keys for this object
        err = DATA_MODEL_GetUniqueKeyParams(path, &unique_key_params, INTERNAL_ROLE);
        if (err != USP_ERR_OK)
        {
            goto exit;
        }

        // Exit if any unique keys have been left with a default value which is not unique
        err = DATA_MODEL_ValidateDefaultedUniqueKeys(path, &unique_key_params, NULL);
        if (err != USP_ERR_OK)
        {
            goto exit;
        }
    }

exit:
    KV_VECTOR_Destroy(&unique_key_params);
    return err;
}

/*********************************************************************//**
**
** SK_TR369_AddMultiObject
**
** An interface provided for external use to add nodes of MultiObject type.
**
** \param   param - Name of the MultiObject type node to be added.
** \param   num - Number of MultiObject type nodes to be added.
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_AddMultiObject(const char *param, int num)
{
    int err;
    dm_trans_vector_t trans;

    if (param == NULL)
    {
        USP_LOG_Error("%s: Parameters are empty, the command cannot be recognized.", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }
    USP_LOG_Info("%s: The parameters to be executed are: %s (%d)", __FUNCTION__, param, num);

    // Exit if unable to start a transaction
    err = DM_TRANS_Start(&trans);
    if (err != USP_ERR_OK)
    {
        DM_TRANS_Abort();
        return err;
    }

    err = SK_TR369_AddInstance(param, num);
    if (err != USP_ERR_OK)
    {
        DM_TRANS_Abort();
        return err;
    }

    err = DM_TRANS_Commit();
    if (err != USP_ERR_OK)
    {
        return err;
    }
    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_DeleteInstance
**
** An interface provided for internal use to delete nodes of MultiObject type.
**
** Note: This action will delete all data at once.
**
** \param   param - Name of MultiObject type node to be deleted.
** \param   sum - Total number of current MultiObject type nodes.
** \param   num - Number of MultiObject type nodes to be deleted.
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_DeleteInstance(const char *param, int sum, int num)
{
    int i, err;
    dm_trans_vector_t trans;

    // Exit if unable to start a transaction
    err = DM_TRANS_Start(&trans);
    if (err != USP_ERR_OK)
    {
        DM_TRANS_Abort();
        return err;
    }

    for (i = 0; i < num; i++)
    {
        int count = sum - i;
        if (count <= 0) break;

        char path[MAX_DM_PATH] = {0};
        USP_SNPRINTF(path, sizeof(path), "%s.%d", param, count);
        USP_LOG_Debug("%s: The node to be deleted is: %s", __FUNCTION__, path);

        err = DATA_MODEL_DeleteInstance(path, 0);
        if (err != USP_ERR_OK) break;
    }

    // Exit if unable to commit the transaction
    err = DM_TRANS_Commit();
    if (err != USP_ERR_OK)
    {
        return err;
    }
    return USP_ERR_OK;
}

/*********************************************************************//**
**
** SK_TR369_DelMultiObject
**
** An interface provided for external use to delete nodes of MultiObject type.
**
** Note: This action will delete all data at once.
**
** \param   param - Name of the MultiObject type node to be deleted.
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_DelMultiObject(const char *param)
{
    if (param == NULL)
    {
        USP_LOG_Error("%s: Parameters are empty, the command cannot be recognized.", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }
    USP_LOG_Info("%s: The parameters to be executed are: \"%s\"", __FUNCTION__, param);

    int_vector_t iv;
    INT_VECTOR_Init(&iv);
    int err = DATA_MODEL_GetInstances(param, &iv);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Failed to obtain instances.", __FUNCTION__);
        goto exit;
    }

    err = SK_TR369_DeleteInstance(param, iv.num_entries, iv.num_entries);
    if (err != USP_ERR_OK)
    {
        USP_LOG_Error("%s: Failed to delete instances.", __FUNCTION__);
        goto exit;
    }

exit:
    INT_VECTOR_Destroy(&iv);
    return err;
}

/*********************************************************************//**
**
** SK_TR369_UpdateMultiObject
**
** An interface provided for external use to update nodes of MultiObject type.
**
** \param   param - Name of the MultiObject type node to be updated.
** \param   num - Number of MultiObject type nodes to be updated.
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_UpdateMultiObject(const char *param, int num)
{
    if (num < 0)
    {
        USP_LOG_Error("%s: The quantity to be updated is abnormal.", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }
    int err;
    int_vector_t iv;

    INT_VECTOR_Init(&iv);
    err = DATA_MODEL_GetInstances(param, &iv);
    if (err != USP_ERR_OK)
    {
        goto exit;
    }

    if (num == iv.num_entries)
    {
        USP_LOG_Debug("%s: The quantity is consistent and does not need to be updated.", __FUNCTION__);
        err = USP_ERR_OK;
    }
    else if (num > iv.num_entries)
    {
        err = SK_TR369_AddMultiObject(param, num - iv.num_entries);
    }
    else
    {
        err = SK_TR369_DeleteInstance(param, iv.num_entries, iv.num_entries - num);
    }

exit:
    INT_VECTOR_Destroy(&iv);
    return err;
}

/*********************************************************************//**
**
** SK_TR369_ShowData
**
** Used for displaying all data content at once.
**
** \param   cmd - The executable parameters that can be processed are 'datamodel' and 'database'.
**                'datamodel' displays all content in the data model,
**                'database' displays all content in the database."
**
** \return  USP_ERR_OK if successful
**
**************************************************************************/
int SK_TR369_ShowData(const char *cmd)
{
    if (cmd == NULL)
    {
        USP_LOG_Error("%s: Parameters are empty, the command cannot be recognized.", __FUNCTION__);
        return USP_ERR_INTERNAL_ERROR;
    }

    USP_LOG_Debug("%s: The parameters to be executed are: \"%s\"", __FUNCTION__, cmd);
    // Show the data model schema if required
    if (strcmp(cmd, "datamodel") == 0)
    {
        USP_DUMP("WARNING: This is the data model of this CLI command, rather than the daemon instance of this executable");
        USP_DUMP("If the data model does not contain 'Device.Test', then you are not running this CLI command with the '-T' option");
        DATA_MODEL_DumpSchema();
        return USP_ERR_OK;
    }

    // Show the contents of the database if required
    if (strcmp(cmd, "database") == 0)
    {
        DATABASE_Dump();
        return USP_ERR_OK;
    }

    return USP_ERR_INTERNAL_ERROR;
}
