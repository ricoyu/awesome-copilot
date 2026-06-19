package com.awesomecopilot.search8x.support;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用的 Elasticsearch 文档操作结果包装类
 * 用于替代 Elasticsearch 7.x/8.x 的 Response 对象，避免版本兼容性问题
 */
public class DocumentOperationResult {
    private static final Logger log = LoggerFactory.getLogger(DocumentOperationResult.class);
    
    private final boolean success;
    private final String id;
    private final String index;
    private final long version;
    private final String result;
    private final String errorMessage;
    private final String rawJson;
    
    private DocumentOperationResult(boolean success, String id, String index, long version, 
                                     String result, String errorMessage, String rawJson) {
        this.success = success;
        this.id = id;
        this.index = index;
        this.version = version;
        this.result = result;
        this.errorMessage = errorMessage;
        this.rawJson = rawJson;
    }
    
    /**
     * 从 JSON 响应字符串解析创建结果对象
     */
    public static DocumentOperationResult fromJson(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            
            String id = root.optString("_id");
            String index = root.optString("_index");
            long version = root.optLong("_version", 1);
            String result = root.optString("result", "created");
            boolean found = root.optBoolean("found", true);
            
            // 判断是否成功
            boolean success = found && !"not_found".equals(result) && !"noop".equals(result);
            
            return new DocumentOperationResult(
                success,
                id,
                index,
                version,
                result,
                null,
                jsonResponse
            );
        } catch (Exception e) {
            log.error("Failed to parse document operation result from JSON: {}", jsonResponse, e);
            return new DocumentOperationResult(false, null, null, 0, null, e.getMessage(), jsonResponse);
        }
    }
    
    /**
     * 创建失败的结果
     */
    public static DocumentOperationResult failure(String errorMessage) {
        return new DocumentOperationResult(false, null, null, 0, null, errorMessage, null);
    }
    
    /**
     * 创建失败的结果（带原始JSON）
     */
    public static DocumentOperationResult failure(String errorMessage, String rawJson) {
        return new DocumentOperationResult(false, null, null, 0, null, errorMessage, rawJson);
    }
    
    // Getters
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getId() {
        return id;
    }
    
    public String getIndex() {
        return index;
    }
    
    public long getVersion() {
        return version;
    }
    
    public String getResult() {
        return result;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getRawJson() {
        return rawJson;
    }
    
    @Override
    public String toString() {
        return "DocumentOperationResult{" +
                "success=" + success +
                ", id='" + id + '\'' +
                ", index='" + index + '\'' +
                ", version=" + version +
                ", result='" + result + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
