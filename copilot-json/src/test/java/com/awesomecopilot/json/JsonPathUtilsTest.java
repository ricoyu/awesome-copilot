package com.awesomecopilot.json;

import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.json.jsonpath.JsonPathUtils;
import com.awesomecopilot.json.jsonpath.JsonPathUtils1;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class JsonPathUtilsTest {
    
    @Test
    public void test() {
        String json = """
                {
                  "error" : {
                    "root_cause" : [ {
                      "type" : "index_not_found_exception",
                      "reason" : "no such index [my_index]",
                      "resource.type" : "index_or_alias",
                      "resource.id" : "my_index",
                      "index_uuid" : "_na_",
                      "index" : "my_index"
                    } ],
                    "type" : "index_not_found_exception",
                    "reason" : "no such index [my_index]",
                    "resource.type" : "index_or_alias",
                    "resource.id" : "my_index",
                    "index_uuid" : "_na_",
                    "index" : "my_index"
                  },
                  "status" : 404
                }""";
        Object error = JsonPathUtils1.readNode(json, "$.error.root_cause[0].reason");
        Object error2 = JsonPathUtils1.readNode(json, "$.error.root_cause[0].reason");
        System.out.println( error);
    }
    @Test
    public void testIfExistsWithBlankJson() {
        assertFalse(JsonPathUtils.ifExists("", "$.name"));
        assertFalse(JsonPathUtils.ifExists("   ", "$.name"));
    }

    @Test
    public void testIfExistsWithNonExistingPath() {
        String json = "{\"name\": \"John Doe\"}";
        assertFalse(JsonPathUtils.ifExists(json, "$.address"));
    }

    @Test
    public void testIfExistsWithExistingPath() {
        String json = "{\"name\": \"John Doe\"}";
        assertTrue(JsonPathUtils.ifExists(json, "$.name"));
    }

    @Test
    public void testIfExistsWithExistingEmptyArray() {
        String json = "{\"emptyArray\": []}";
        assertFalse(JsonPathUtils.ifExists(json, "$.emptyArray[0]"));
    }

    @Test
    public void testIfExistsWithExistingNonEmptyArray() {
        String json = "{\"names\": [\"John\", \"Jane\"]}";
        assertTrue(JsonPathUtils.ifExists(json, "$.names[0]"));
        assertTrue(JsonPathUtils.ifExists(json, "$.names[1]"));
    }

    @Test
    public void testIfExistsWithNonExistentPropertyInArray() {
        String json = "[{\"name\": \"John\"}, {\"name\": \"Jane\"}]";
        assertFalse(JsonPathUtils.ifExists(json, "$[0].address"));
    }

    // Add more tests for edge cases and exceptional scenarios

    @Test
    public void testFBillNo() {
        String msgData = IOUtils.readFileAsString("d:/msg_data.json");
        System.out.println(msgData);
        Object billJson = JsonPathUtils.readNode(msgData, "$.billJson");
        System.out.println(billJson);
        Object billNo = JsonPathUtils.readNode(msgData, "$.billJson.FBillNo");
        System.out.println(billNo);
    }

    @Test
    public void testFBillNoInsideArr() {
        String msgData = IOUtils.readFileAsString("d:/msg_data3.json");
        //System.out.println(msgData);
        Object FBillNo = JsonPathUtils.readNode(msgData, "$.FBillNo");
        boolean exists = JsonPathUtils.ifExists(msgData, "$.FBillNo");
        //Object FBillNo = JsonPathUtils.readNode(msgData, "$.[0].FBillNo");
        System.out.println(FBillNo);
    }

}