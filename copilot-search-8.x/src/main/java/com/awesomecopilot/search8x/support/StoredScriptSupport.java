package com.awesomecopilot.search8x.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.awesomecopilot.json.jackson.JacksonUtils;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * ES 8.x Stored Script (Search Template) 支持类
 * 
 * 使用 ElasticsearchClient 8.x 管理 stored scripts
 * 
 * @author Rico Yu ricoyu520@gmail.com
 */
public class StoredScriptSupport {

    private StoredScriptSupport() {
    }

    /**
     * 创建/更新 stored script
     * 
     * @param client       ElasticsearchClient
     * @param templateName 模板名称
     * @param scriptJson   script JSON 内容（包含 lang 和 source）
     * @return boolean 是否成功
     */
    public static boolean putStoredScript(ElasticsearchClient client, String templateName, String scriptJson) {
        try {
            // 获取底层 RestClient
            RestClientTransport transport = (RestClientTransport) client._transport();
            RestClient restClient = transport.restClient();

            // 构建请求：PUT /_scripts/{templateName}
            Request request = new Request("PUT", "/_scripts/" + templateName);
            request.setJsonEntity(scriptJson);

            // 执行请求
            Response response = restClient.performRequest(request);
            String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            // 解析响应获取 acknowledged 状态
            Map<String, Object> responseMap = JacksonUtils.toObject(jsonResponse, Map.class);
            if (responseMap != null && responseMap.containsKey("acknowledged")) {
                Object acknowledged = responseMap.get("acknowledged");
                return Boolean.TRUE.equals(acknowledged);
            }

            return false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to put stored script: " + templateName, e);
        }
    }

    /**
     * 删除 stored script
     * 
     * @param client       ElasticsearchClient
     * @param templateName 模板名称
     * @return boolean 是否成功
     */
    public static boolean deleteStoredScript(ElasticsearchClient client, String templateName) {
        try {
            // 获取底层 RestClient
            RestClientTransport transport = (RestClientTransport) client._transport();
            RestClient restClient = transport.restClient();

            // 构建请求：DELETE /_scripts/{templateName}
            Request request = new Request("DELETE", "/_scripts/" + templateName);

            // 执行请求
            Response response = restClient.performRequest(request);
            String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            // 解析响应获取 acknowledged 状态
            Map<String, Object> responseMap = JacksonUtils.toObject(jsonResponse, Map.class);
            if (responseMap != null && responseMap.containsKey("acknowledged")) {
                Object acknowledged = responseMap.get("acknowledged");
                return Boolean.TRUE.equals(acknowledged);
            }

            return false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete stored script: " + templateName, e);
        }
    }

    /**
     * 构建 search template 的 script JSON
     * 
     * @param templateContent mustache 模板内容
     * @return script JSON 字符串
     */
    public static String buildScriptJson(String templateContent) {
        Map<String, Object> rootNode = new HashMap<>();
        Map<String, Object> scriptNode = new HashMap<>();
        rootNode.put("script", scriptNode);
        scriptNode.put("lang", "mustache");
        scriptNode.put("source", templateContent);
        return JacksonUtils.toJson(rootNode);
    }
}
