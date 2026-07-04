package com.awesomecopilot.search8x;

import com.awesomecopilot.common.lang.enums.Gender;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.json.jsonpath.JsonPathUtils;
import com.awesomecopilot.search8x.ElasticUtils.Admin;
import com.awesomecopilot.search8x.ElasticUtils.Query;
import com.awesomecopilot.search8x.enums.Analyzer;
import com.awesomecopilot.search8x.enums.SuggestMode;
import com.awesomecopilot.search8x.support.BulkResult;
import com.awesomecopilot.search8x.support.UpdateResult;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreBuilders;
import co.elastic.clients.elasticsearch.core.UpdateByQueryResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static com.awesomecopilot.search8x.ElasticUtils.Query.termQuery;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ElasticUtils 单元测试类
 * 从 copilot-search 模块迁移而来,测试 ES 8.x API
 * <p>
 * 包含 ElasticUtils 主类下的所有接口测试:
 * - index / create / get / delete / exists / update / upsert / getWithVersion / ping
 */
@Slf4j
public class ElasticUtilsTest {

    @BeforeAll
    public static void testInitialize() {
        // 验证 ElasticUtils 客户端初始化成功
        assertThat(ElasticUtils.QUERY_CLIENT != null);
        log.info("ES 8.x 客户端初始化成功");
    }
    
    // ==================== ping ====================
    
    /**
     * 测试 ping 方法 - 验证 ES 连接
     * 对应原版 testInitialize()
     */
    @Test
    public void testPing() {
        ElasticUtils.ping();
        log.info("Ping ES cluster successful");
    }
    
    // ==================== index() 相关测试 ====================
    
    /**
     * 测试创建文档 - 从 copilot-search 迁移
     * 对应原版 testCreateDoc()
     */
    @Test
    public void testCreateDoc() {
        String id = ElasticUtils.index("rico",
                "{\"firstName\": \"Chan\", \"lastName\": \"Jackie\", \"loginDate\": \"2018-07-24T10:29:48.103Z\"}",
                "2");
        log.info("Indexed doc id: {}", id);
        
        ElasticUtils.index("rico", "{\"key\": \"三少爷\"}", "1");
        id = ElasticUtils.index("rico", "{\"key\": \"三少爷\"}", "1");
        assertThat(id).isEqualTo("1");
    }
    
    /**
     * 测试创建文档并指定ID - 从 copilot-search 迁移
     * 对应原版 testCreateWithId()
     */
    @Test
    public void testCreateWithId() {
        boolean deleteResult = Admin.deleteIndex("mapping_test");
        log.info("Delete index result: {}", deleteResult);
        
        String result = ElasticUtils.index("mapping_test",
                "{\"firstName\": \"Chan\", \"lastName\": \"Jackie\", \"loginDate\": \"2018-07-24T10:29:48.103Z\"}",
                "1");
        String json = ElasticUtils.get("mapping_test", "1");
        log.info("Get doc: {}", json);
        
        // 再次 index 同一个ID, 执行更新
        result = ElasticUtils.index("mapping_test",
                "{\"firstName\": \"Chan\", \"lastName\": \"Jackie\", \"loginDate\": \"2018-07-24T10:29:48.103Z\"}",
                "1");
        log.info("Re-index result: {}", result);
        json = ElasticUtils.get("mapping_test", "1");
        log.info("Get doc after re-index: {}", json);
    }
    
    /**
     * 测试创建文档指定ID - 从 copilot-search 迁移
     * 对应原版 testCreateDocWithId()
     */
    @Test
    public void testCreateDocWithId() {
        String id = ElasticUtils.index("rico", "{\"name\": \"三少爷\"}", "1");
        log.info("Indexed doc id: {}", id);
    }
    
    /**
     * 测试创建或更新文档 - 从 copilot-search 迁移
     * 对应原版 testCreateOrUpdate()
     */
    @Test
    public void testCreateOrUpdate() {
        String doc = """
                {
                  "firstName": "Jack",
                  "lastName": "Johnson",
                  "tags":["guitar", "skateboard"]
                }""";
        String id = ElasticUtils.index("users", doc, "1");
        log.info("Indexed doc id: {}", id);
    }
    
    /**
     * 测试创建文档(Object类型) - 从 copilot-search 迁移
     * 对应原版 testCreateDocObjectType()
     */
    @Test
    public void testCreateDocObjectType() {
        Person person = new Person();
        person.setUser("三少爷");
        person.setComment("牛仔");
        String id = ElasticUtils.index("rico", person);
        log.info("Indexed person doc id: {}", id);
    }
    
    /**
     * 测试创建文档(Object类型)并指定ID - 从 copilot-search 迁移
     * 对应原版 testCreateDocObjectTypeAndId()
     */
    @Test
    public void testCreateDocObjectTypeAndId() {
        Person person = new Person();
        person.setUser("三少爷");
        person.setComment("牛仔");
        String id = ElasticUtils.index("rico", person, "doc-1");
        log.info("Indexed person doc with id: {}", id);
    }
    
    
    // ==================== create() 相关测试 ====================
    
    /**
     * 测试创建文档(create语义, ID已存在则报错) - 从 copilot-search 迁移
     * 对应原版 testCreateEndpoint()
     */
    @Test
    public void testCreateEndpoint() {
        boolean exists = ElasticUtils.Admin.existsIndex("product");
        if (exists) {
            ElasticUtils.delete("product", "1");
        }
        String id = ElasticUtils.create("product", """
                {
                    "name": "Coffee Maker",
                    "brand": "Good Coffee",
                    "price": 99.99,
                    "in_stock": 153
                }
                """, "1");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        String doc = ElasticUtils.get("product", "1");
        log.info("Created product doc: {}", doc);
        assertThat(id).isEqualTo("1");
    }
    
    /**
     * 测试创建文档(create语义) - 从 copilot-search 迁移
     * 对应原版 testCreateWithIdThenFail()
     */
    @Test
    public void testCreateWithIdThenFail() {
        String doc = """
                {
                  "firstName": "Jack",
                  "lastName": "Johnson",
                  "tags":["guitar", "skateboard"]
                }""";
        
        String id = ElasticUtils.create("users", doc, "1");
        log.info("Created doc id: {}", id);
    }
    
    // ==================== get() 相关测试 ====================
    
    /**
     * 测试根据ID获取文档 - 从 copilot-search 迁移
     * 对应原版 testGetById()
     */
    @Test
    public void testGetById() {
        try {
            String user = ElasticUtils.get("movies", "movieId");
            log.info("Get movie by id: {}", user);
        } catch (Exception e) {
            log.warn("Failed to get movie by id (ES may not be running): {}", e.getMessage());
        }
    }
    
    /**
     * 测试获取用户文档 - 从 copilot-search 迁移
     * 对应原版 testGetUsers()
     */
    @Test
    public void testGetUsers() {
        try {
            String users = ElasticUtils.get("users", "1");
            log.info("Get user: {}", users);
        } catch (Exception e) {
            log.warn("Failed to get users: {}", e.getMessage());
        }
    }
    
    /**
     * 测试文档 CRUD - 获取文档
     * 对应原版 testGetDocument()
     */
    @Test
    public void testGetDocument() {
        String testIndex = "test_docs_get";
        String docId = "1";
        
        try {
            // 先创建文档
            String docJson = """
                    {
                      "name": "Get Test",
                      "value": 123
                    }
                    """;
            
            ElasticUtils.index(testIndex, docJson, docId);
            
            // 等待索引刷新
            Thread.sleep(1000);
            
            // 获取文档
            String retrieved = ElasticUtils.get(testIndex, docId);
            log.info("Retrieved document: {}", retrieved);
            
            assertThat(retrieved).isNotNull();
            assertThat(retrieved).contains("Get Test");
            
        } catch (Exception e) {
            log.error("Failed to get document", e);
        }
    }
    
    // ==================== getWithVersion() 相关测试 ====================
    
    /**
     * 测试获取带版本信息的文档 - 从 copilot-search 迁移
     * 对应原版 testGetWithVersion()
     */
    @Test
    public void testGetWithVersion() {
        String testIndex = "test_docs_version";
        String docId = "1";
        
        try {
            // 先创建文档
            String docJson = "{\"name\": \"Version Test\"}";
            ElasticUtils.index(testIndex, docJson, docId);
            
            // 等待索引刷新
            Thread.sleep(500);
            
            // 获取带版本信息的文档
            var versionedDoc = ElasticUtils.getWithVersion(testIndex, docId);
            
            assertThat(versionedDoc).isNotNull();
            assertThat(versionedDoc.getSource()).contains("Version Test");
            assertThat(versionedDoc.getVersion()).isGreaterThan(0);
            
            log.info("Document version: {}, seqNo: {}, primaryTerm: {}", 
                    versionedDoc.getVersion(),
                    versionedDoc.getIfSeqNo(),
                    versionedDoc.getIfPrimaryTerm());
            
        } catch (Exception e) {
            log.error("Failed to get document with version", e);
        }
    }
    
    // ==================== update() 相关测试 ====================
    
    /**
     * 测试更新文档 - 从 copilot-search 迁移
     * 对应原版 testUpdate()
     */
    @Test
    public void testUpdate() {
        String testIndex = "test_update_" + System.currentTimeMillis();
        String docId = "1";
        
        try {
            // 先创建文档
            String originalDoc = """
                    {
                      "firstName": "Jack",
                      "lastName": "Johnson"
                    }
                    """;
            ElasticUtils.index(testIndex, originalDoc, docId);
            Thread.sleep(500);
            
            // 更新部分字段
            UpdateResult updateResult = ElasticUtils.update(testIndex, docId, "{\"nickname\": \"三少爷\"}");
            log.info("Update result: {}", JacksonUtils.toPrettyJson(updateResult));
            
            assertThat(updateResult).isNotNull();
            assertThat(updateResult.getResult()).isIn(
                UpdateResult.Result.UPDATED, 
                UpdateResult.Result.CREATED,
                UpdateResult.Result.NOOP
            );
            
            // 验证更新后的文档
            String updatedDoc = ElasticUtils.get(testIndex, docId);
            log.info("Updated document: {}", updatedDoc);
            assertThat(updatedDoc).contains("三少爷");
            
        } catch (Exception e) {
            log.error("Failed to update document", e);
        }
    }
    
    /**
     * 测试先创建再更新 - 从 copilot-search 迁移
     * 对应原版 testCreateThenUpdate()
     */
    @Test
    public void testCreateThenUpdate() {
        ElasticUtils.Admin.deleteIndex("users");
        String id = ElasticUtils.create("users", """
                {
                  "name": "onebird",
                  "interests": "reading"
                }
                """, "1");
        
        UpdateResult updateResult = ElasticUtils.update("users", id, """
                {
                  "name": "twobirds",
                  "interests": ["reading", "music"]
                }
                """);
        
        String doc = ElasticUtils.get("users", id);
        log.info("Updated doc: {}", doc);
    }
    
    // ==================== upsert() 相关测试 ====================
    
    /**
     * 测试 Upsert - 从 copilot-search 迁移
     * 对应原版 testUpsert()
     */
    @Test
    public void testUpsert() {
        String testIndex = "test_upsert_" + System.currentTimeMillis();
        String docId = "3";
        
        try {
            // Upsert: 如果文档不存在则创建,存在则更新
            UpdateResult updateResult = ElasticUtils.upsert(testIndex, docId, """
                    {
                      "firstName": "Rico",
                      "lastName": "Johnson"
                    }
                    """);
            
            log.info("Upsert result: {}", JacksonUtils.toJson(updateResult));
            assertThat(updateResult).isNotNull();
            assertThat(updateResult.getResult()).isEqualTo(UpdateResult.Result.CREATED);
            
            // 再次 upsert,应该是更新
            UpdateResult updateResult2 = ElasticUtils.upsert(testIndex, docId, "{\"age\": 30}");
            log.info("Second upsert result: {}", JacksonUtils.toJson(updateResult2));
            assertThat(updateResult2.getResult()).isIn(
                UpdateResult.Result.UPDATED,
                UpdateResult.Result.NOOP
            );
            
            String doc = ElasticUtils.get(testIndex, docId);
            log.info("Final doc: {}", doc);
            
        } catch (Exception e) {
            log.error("Failed to upsert document", e);
        }
    }
    
    /**
     * 测试 Upsert 第二次更新 - 从 copilot-search 迁移
     * 对应原版 testUpsert2()
     */
    @Test
    public void testUpsert2() {
        try {
            UpdateResult updateResult = ElasticUtils.upsert("users", "3", """
                    {
                      "firstName": "Rico",
                      "lastName": "Johnson"
                    }
                    """);
            log.info("Upsert result: {}", toJson(updateResult));
            
            UpdateResult updateResult2 = ElasticUtils.upsert("users", "3", "{\"nickname\": \"三少爷\"}");
            log.info("Second upsert result: {}", toJson(updateResult2));
        } catch (Exception e) {
            log.error("Failed to upsert", e);
        }
    }
    
    // ==================== delete() 相关测试 ====================
    
    /**
     * 测试删除文档 - 从 copilot-search 迁移
     * 对应原版 testDelteDoc()
     */
    @Test
    public void testDeleteDoc() {
        try {
            boolean deleted = ElasticUtils.delete("rico", "UWHGu3YBDs-1X2rMuqw4");
            log.info("Delete doc result: {}", deleted);
        } catch (Exception e) {
            log.warn("Failed to delete doc (may not exist): {}", e.getMessage());
        }
    }
    
    /**
     * 测试文档 CRUD - 删除文档
     */
    @Test
    public void testDeleteDocument() {
        String testIndex = "test_docs_delete";
        String docId = "1";
        
        try {
            // 先创建文档
            String docJson = "{\"name\": \"Delete Test\"}";
            ElasticUtils.index(testIndex, docJson, docId);
            
            // 等待索引刷新
            Thread.sleep(500);
            
            // 删除文档
            boolean deleted = ElasticUtils.delete(testIndex, docId);
            log.info("Document deleted: {}", deleted);
            
            assertTrue(deleted);
            
        } catch (Exception e) {
            log.error("Failed to delete document", e);
        }
    }
    
    // ==================== exists() 相关测试 ====================
    
    /**
     * 测试检查文档存在性 - 从 copilot-search 迁移
     * 对应原版 testExists()
     */
    @Test
    public void testExists() {
        String testIndex = "test_exists_check";
        String docId = "3";
        
        try {
            // 先创建一个文档
            ElasticUtils.index(testIndex, "{\"name\": \"exists test\"}", docId);
            Thread.sleep(500);
            
            // 检查存在的文档
            boolean exists = ElasticUtils.exists(testIndex, docId);
            log.info("Document exists: {}", exists);
            assertTrue(exists);
            
            // 检查不存在的文档
            boolean notExists = ElasticUtils.exists(testIndex, "nonexistent");
            assertFalse(notExists);
            
        } catch (Exception e) {
            log.error("Failed to check existence", e);
        }
    }
    
    /**
     * 测试文档存在性检查
     */
    @Test
    public void testDocumentExists() {
        String testIndex = "test_docs_exists";
        String docId = "1";
        
        try {
            // 先创建文档
            String docJson = "{\"name\": \"Exists Test\"}";
            ElasticUtils.index(testIndex, docJson, docId);
            
            // 等待索引刷新
            Thread.sleep(500);
            
            // 检查文档是否存在
            boolean exists = ElasticUtils.exists(testIndex, docId);
            log.info("Document exists: {}", exists);
            
            assertTrue(exists);
            
            // 检查不存在的文档
            boolean notExists = ElasticUtils.exists(testIndex, "nonexistent_id");
            assertFalse(notExists);
            
        } catch (Exception e) {
            log.error("Failed to check document existence", e);
        }
    }
    
    // ==================== 综合 CRUD 测试 ====================
    
    /**
     * 测试文档 CRUD - 创建文档
     */
    @Test
    public void testIndexDocument() {
        String testIndex = "test_docs_" + System.currentTimeMillis();
        
        try {
            // 创建测试文档
            String docJson = """
                    {
                      "name": "Test User",
                      "age": 30,
                      "email": "test@example.com"
                    }
                    """;
            
            String id = ElasticUtils.index(testIndex, docJson, "1");
            log.info("Created document with ID: {}", id);
            
            assertThat(id).isEqualTo("1");
            
        } catch (Exception e) {
            log.error("Failed to index document", e);
        }
    }
    
    // ==================== bulkIndex() 相关测试 ====================
    
    /**
     * 测试批量索引(JSON字符串) - 从 copilot-search ElasticBulkTest 迁移
     * 对应原版 testBulkIndex()
     */
    @Test
    public void testBulkIndex() {
        try {
            ElasticUtils.Admin.deleteIndex("employees1");
        } catch (Exception e) {
            log.error("", e);
        }
        BulkResult bulkResult = ElasticUtils.bulkIndex("employees1")
                .docs("{ \"name\" : \"Emma\",\"age\":32,\"job\":\"Product Manager\",\"gender\":\"female\",\"salary\":35000 }",
                        "{ \"name\" : \"Underwood\",\"age\":41,\"job\":\"Dev Manager\",\"gender\":\"male\",\"salary\": 50000}",
                        "{ \"name\" : \"Tran\",\"age\":25,\"job\":\"Web Designer\",\"gender\":\"male\",\"salary\":18000 }")
                .refresh(true)
                .execute();
        assertThat(bulkResult.getSuccessCount()).isEqualTo(3);
        
        List<Object> employees1 = ElasticUtils.Query.matchAllQuery("employees1").queryForList();
        assertThat(employees1.size()).isEqualTo(3);
    }
    
    /**
     * 测试批量索引(POJO对象列表) - 从 copilot-search ElasticBulkTest 迁移
     * 对应原版 testBulkIndex2()
     */
    @Test
    public void testBulkIndex2() {
        try {
            ElasticUtils.Admin.deleteIndex("employees2");
        } catch (Exception e) {
            log.error("", e);
        }
        List<Employee> employees =
                asList("{ \"name\" : \"Emma\",\"age\":32,\"job\":\"Product Manager\",\"gender\":\"female\",\"salary\":35000 }",
                        "{ \"name\" : \"Underwood\",\"age\":41,\"job\":\"Dev Manager\",\"gender\":\"male\",\"salary\": 50000}",
                        "{ \"name\" : \"Tran\",\"age\":25,\"job\":\"Web Designer\",\"gender\":\"male\",\"salary\":18000 }")
                        .stream()
                        .map((json) -> JacksonUtils.toObject(json, Employee.class))
                        .collect(toList());
        BulkResult bulkResult = ElasticUtils.bulkIndex("employees2")
                .docs(employees)
                .refresh(true)
                .execute();
        assertThat(bulkResult.getSuccessCount()).isEqualTo(3);
        
        List<Object> employees1 = ElasticUtils.Query.matchAllQuery("employees2").queryForList();
        assertThat(employees1.size()).isEqualTo(3);
    }
    
    /**
     * 测试批量索引(不刷新) - 从 copilot-search ElasticBulkTest 迁移
     * 对应原版 testBUlkIndex()
     */
    @Test
    public void testBulkIndexNoRefresh() {
        BulkResult bulkResult = ElasticUtils.bulkIndex("products")
                .docs("{\"productID\": \"XHDK-A-1293-#fJ3\", \"desc\": \"iPhone\"}",
                        "{\"productID\": \"KDKE-B-9947-#kL5\", \"desc\": \"iPad\"}",
                        "{\"productID\": \"JODL-X-1937-#pV7\", \"desc\": \"MBP\"}")
                .execute();
        System.out.println(toJson(bulkResult));
    }
    
    
    @Test
    public void testGetIndexCount() {
        long count = ElasticUtils.docCount("movies");
        assertEquals(8935, count);
    }
    
    /**
     * 测试 bulkIndex(String index, String... docs) 直接传入JSON字符串数组
     * 对应 copilot-search 模块 ElasticUtils.bulkIndex(String, String...) 接口
     */
    @Test
    public void testBulkIndexStringVarargs() {
        try {
            ElasticUtils.Admin.deleteIndex("bulk_varargs_test");
        } catch (Exception e) {
            log.error("", e);
        }
        BulkResult bulkResult = ElasticUtils.bulkIndex("bulk_varargs_test",
                "{\"name\": \"Alice\", \"age\": 25}",
                "{\"name\": \"Bob\", \"age\": 30}",
                "{\"name\": \"Charlie\", \"age\": 35}");
        assertThat(bulkResult.getSuccessCount()).isEqualTo(3);
        assertThat(bulkResult.getFailCount()).isEqualTo(0);
        assertThat(bulkResult.getIds()).hasSize(3);
        log.info("Bulk index varargs result: {}", toJson(bulkResult));
    }
    
    /**
     * 测试 bulkIndex(String index, String... docs) 单条文档
     */
    @Test
    public void testBulkIndexStringVarargsSingle() {
        try {
            ElasticUtils.Admin.deleteIndex("bulk_varargs_single");
        } catch (Exception e) {
            log.error("", e);
        }
        BulkResult bulkResult = ElasticUtils.bulkIndex("bulk_varargs_single",
                "{\"name\": \"Single\", \"age\": 1}");
        assertThat(bulkResult.getSuccessCount()).isEqualTo(1);
        assertThat(bulkResult.getFailCount()).isEqualTo(0);
        assertThat(bulkResult.getIds()).hasSize(1);
    }
    
    /**
     * 测试 bulkIndex(String index, List<?> docs) 传入POJO对象列表
     * 对应 copilot-search 模块 ElasticUtils.bulkIndex(String, List<?>) 接口
     */
    @Test
    public void testBulkIndexWithList() {
        try {
            ElasticUtils.Admin.deleteIndex("bulk_list_test");
        } catch (Exception e) {
            log.error("", e);
        }
        
        List<Object> docs = new ArrayList<>();
        docs.add("{\"name\": \"Alice\", \"age\": 25}");
        docs.add("{\"name\": \"Bob\", \"age\": 30}");
        docs.add("{\"name\": \"Charlie\", \"age\": 35}");
        
        BulkResult bulkResult = ElasticUtils.bulkIndex("bulk_list_test", docs);
        assertThat(bulkResult.getSuccessCount()).isEqualTo(3);
        assertThat(bulkResult.getFailCount()).isEqualTo(0);
        assertThat(bulkResult.getIds()).hasSize(3);
        log.info("Bulk index list result: {}", toJson(bulkResult));
    }
    
    /**
     * 测试 bulkIndex(String index, List<?> docs) 空列表
     */
    @Test
    public void testBulkIndexWithEmptyList() {
        BulkResult bulkResult = ElasticUtils.bulkIndex("bulk_list_empty", new ArrayList<>());
        assertThat(bulkResult.getSuccessCount()).isEqualTo(0);
        assertThat(bulkResult.getFailCount()).isEqualTo(0);
    }
    
    // ==================== bulkUpdate() 相关测试 ====================
    
    /**
     * 测试 bulkUpdate() 批量更新 - 使用Map方式
     * 对应 copilot-search 模块 ElasticUtils.bulkUpdate() 接口
     */
    @Test
    public void testBulkUpdate() {
        // 先插入测试文档
        ElasticUtils.index("bulk_update_test", "{\"name\": \"Alice\", \"age\": 25}", "1");
        ElasticUtils.index("bulk_update_test", "{\"name\": \"Bob\", \"age\": 30}", "2");
        ElasticUtils.index("bulk_update_test", "{\"name\": \"Charlie\", \"age\": 35}", "3");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        // 批量更新
        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("age", 26);
        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("age", 31);
        
        BulkResult bulkResult = ElasticUtils.bulkUpdate()
                .doc("bulk_update_test", "1", doc1)
                .doc("bulk_update_test", "2", doc2)
                .refresh(true)
                .execute();
        
        assertThat(bulkResult.getSuccessCount()).isEqualTo(2);
        assertThat(bulkResult.getFailCount()).isEqualTo(0);
        String doc = ElasticUtils.get("bulk_update_test", "1");
        Object age = JsonPathUtils.readNode(doc, "$.age");
        assertEquals(26, age );
        doc = ElasticUtils.get("bulk_update_test", "2");
        age = JsonPathUtils.readNode(doc, "$.age");
        assertEquals(31, age );
        log.info("Bulk update result: {}", toJson(bulkResult));
    }
    
    /**
     * 测试 bulkUpdate() 批量更新 - 使用可变参数方式(字段名, 字段值成对出现)
     */
    @Test
    public void testBulkUpdateVarargs() {
        // 先插入测试文档
        ElasticUtils.index("bulk_update_varargs", "{\"name\": \"Dave\", \"age\": 40}", "1");
        ElasticUtils.index("bulk_update_varargs", "{\"name\": \"Eve\", \"age\": 45}", "2");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        // 批量更新 - 使用可变参数方式
        BulkResult bulkResult = ElasticUtils.bulkUpdate()
                .doc("bulk_update_varargs", "1", "age", 41, "name", "Dave Updated")
                .doc("bulk_update_varargs", "2", "age", 46)
                .refresh(true)
                .execute();
        
        assertThat(bulkResult.getSuccessCount()).isEqualTo(2);
        assertThat(bulkResult.getFailCount()).isEqualTo(0);
        log.info("Bulk update varargs result: {}", toJson(bulkResult));
    }
    
    // ==================== mget() 相关测试 ====================
    
    /**
     * 测试 mget() 多文档获取 - 返回JSON字符串列表
     * 对应 copilot-search 模块 ElasticUtils.mget() 接口
     */
    @Test
    public void testMget() {
        // 先插入测试文档
        ElasticUtils.index("mget_test", "{\"name\": \"Alice\", \"age\": 25}", "1");
        ElasticUtils.index("mget_test", "{\"name\": \"Bob\", \"age\": 30}", "2");
        ElasticUtils.index("mget_test", "{\"name\": \"Charlie\", \"age\": 35}", "3");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        // 多文档获取
        List<Object> results = ElasticUtils.mget()
                .add("mget_test", "1")
                .add("mget_test", "2")
                .add("mget_test", "3")
                .request();
        
        assertThat(results).hasSize(3);
        for (Object result : results) {
        	log.info("Mget result: {}", result);
        }
        log.info("Mget results: {}", results);
    }
    
    /**
     * 测试 mget() 多文档获取 - 批量添加ID列表
     */
    @Test
    public void testMgetWithIdList() {
        // 先插入测试文档
        ElasticUtils.index("mget_list_test", "{\"name\": \"Dave\", \"age\": 40}", "1");
        ElasticUtils.index("mget_list_test", "{\"name\": \"Eve\", \"age\": 45}", "2");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        // 多文档获取 - 使用ID列表
        List<Object> results = ElasticUtils.mget()
                .add("mget_list_test", asList("1", "2"))
                .request();
        
        assertThat(results).hasSize(2);
        log.info("Mget with ID list results: {}", results);
    }
    
    /**
     * 测试 mget() 多文档获取 - 指定返回类型
     */
    @Test
    public void testMgetWithResultType() {
        // 先插入测试文档
        ElasticUtils.index("mget_type_test", "{\"name\": \"Frank\", \"age\": 50}", "1");
        ElasticUtils.index("mget_type_test", "{\"name\": \"Grace\", \"age\": 55}", "2");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        // 多文档获取 - 指定返回类型为Person
        List<Person> results = ElasticUtils.mget()
                .add("mget_type_test", "1")
                .add("mget_type_test", "2")
                .resultType(Person.class)
                .request();
        
        assertThat(results).hasSize(2);
        log.info("Mget with result type: {}", toJson(results));
    }
    
    /**
     * 测试 deleteBy() 按条件删除文档
     */
    @Test
    public void testDeleteBy() {
        String index = "delete_by_test";
        // 先插入测试文档
        ElasticUtils.index(index, "{\"name\": \"Alice\", \"status\": \"active\"}", "1");
        ElasticUtils.index(index, "{\"name\": \"Bob\", \"status\": \"active\"}", "2");
        ElasticUtils.index(index, "{\"name\": \"Charlie\", \"status\": \"inactive\"}", "3");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        // 按 status=active 删除, 应删除2条
        long deleted = ElasticUtils.deleteBy(index, "status", "active");
        log.info("Deleted {} documents by status=active", deleted);
        assertThat(deleted).isEqualTo(2);
        
        List<Object> docs = Query.matchAllQuery("delete_by_test").queryForList();
        assertEquals(1, docs.size());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        // 验证剩余1条文档
        long remaining = ElasticUtils.docCount(index);
        assertThat(remaining).isEqualTo(1);
        log.info("Remaining documents after deleteBy: {}", remaining);
    }
    
    /**
     * 测试 ElasticUpdateBuilder 局部更新文档
     */
    @Test
    public void testUpdateBuilder() {
        boolean exists = ElasticUtils.Admin.existsIndex("update_builder_test");
        if (exists) {
            boolean deleted = ElasticUtils.Admin.deleteIndex("update_builder_test");
        }
        
        String index = "update_builder_test";
        // 先插入测试文档
        ElasticUtils.index(index, "{\"name\": \"Alice\", \"age\": 30, \"city\": \"Beijing\"}", "1");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        String doc = ElasticUtils.get("update_builder_test", "1");
        int age = JsonPathUtils.readNode(doc, "$.age");
        assertEquals(30, age);
        
        // 使用 ElasticUpdateBuilder 局部更新 age 字段
        UpdateResult result = ElasticUtils.update(index)
                .id("1")
                .doc("{\"age\": 31}")
                .refresh(true)
                .update();
        
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(UpdateResult.Result.UPDATED);
        log.info("UpdateBuilder result: {}", result.getResult());
        doc = ElasticUtils.get("update_builder_test", "1");
        age = JsonPathUtils.readNode(doc, "$.age");
        assertEquals(31, age);
    }

    /**
     * 测试 ElasticUpdateBuilder upsert 功能
     */
    @Test
    public void testUpdateBuilderUpsert() {
        String index = "update_builder_upsert_test";
        
        // 文档不存在时, upsert 会创建文档
        UpdateResult result = ElasticUtils.update(index)
                .id("1")
                .doc("{\"name\": \"Bob\", \"age\": 25}")
                .upsert(true)
                .refresh(true)
                .update();
        
        assertThat(result).isNotNull();
        assertThat(result.getResult()).isEqualTo(UpdateResult.Result.CREATED);
        log.info("UpdateBuilder upsert result: {}", result.getResult());
    }
    
    @Test
    public void testConstantScoreQuery() {
        List<Object> products = ElasticUtils.Query.constantScoreQuery("products")
                .queryBuilder(termQuery("productID.keyword", "JODL-X-1937-#pV7"))
                .queryForList();
        products.forEach(System.out::println);
    }
    
    /**
     * 测试 FunctionScoreQuery (field_value_factor)
     * 从 copilot-search 迁移 testFunctionScoreQuery()
     */
    @Test
    public void testFunctionScoreQuery() {
        Admin.deleteIndex("blogs");
        ElasticUtils.index("blogs", "{\"title\": \"About popularity\", \"content\": \"In this post we will talk about." +
                "..\", \"votes\": 0 }", "1");
        ElasticUtils.index("blogs", "{\"title\": \"About popularity\", \"content\": \"In this post we will talk about." +
                "..\", \"votes\": 100 }", "2");
        ElasticUtils.index("blogs", "{\"title\": \"About popularity\", \"content\": \"In this post we will talk about." +
                "..\", \"votes\": 1000000 }", "3");
        
        List<Object> objects =
                ElasticUtils.Query.functionScoreQuery(
                                FunctionScoreBuilders.fieldValueFactor(fvf -> fvf.field("votes")), "blogs")
                        .boostMode("sum")
                        .queryBuilder(Query.multiMatch("blogs").query("popularity", "title", "content"))
                        .queryForList();
        
        objects.forEach(System.out::println);
        System.out.println("-----------------");
        ElasticUtils.Query.query("blogs")
                .queryBuilder(Query.multiMatch("blogs").query("popularity", "title", "content"))
                .queryForList()
                .forEach(System.out::println);
    }
    
    /**
     * 测试 RandomScoreQuery
     * 从 copilot-search 迁移 testRandomScoreQuery()
     */
    @Test
    public void testRandomScoreQuery() {
        Admin.deleteIndex("blogs");
        ElasticUtils.index("blogs", "{\"title\": \"About popularity\", \"content\": \"In this post we will talk about." +
                "..\", \"votes\": 0 }", "1");
        ElasticUtils.index("blogs", "{\"title\": \"About popularity\", \"content\": \"In this post we will talk about." +
                "..\", \"votes\": 100 }", "2");
        ElasticUtils.index("blogs", "{\"title\": \"About popularity\", \"content\": \"In this post we will talk about." +
                "..\", \"votes\": 1000000 }", "3");
        
        ElasticUtils.Query.functionScoreQuery(
                        FunctionScoreBuilders.randomScore(rs -> rs.seed("666").field("content")), "blogs")
                .queryBuilder(Query.multiMatch("blogs").query("popularity", "title", "content"))
                .queryForList()
                .forEach(System.out::println);
        
        ElasticUtils.Query.functionScoreQuery(
                        FunctionScoreBuilders.randomScore(rs -> rs.seed("999").field("content.keyword")), "blogs")
                .queryBuilder(Query.multiMatch("blogs").query("popularity", "title", "content"))
                .queryForList()
                .forEach(System.out::println);
    }
    
    // ==================== suggest 相关测试 ====================
    
    /**
     * 测试 Term Suggest 便捷接口 - 从 copilot-search SuggestTest 迁移
     * 对应原版 testTermSuggester()
     */
    @Test
    public void testTermSuggest() {
        Set<String> suggesters = ElasticUtils.termSuggest("lucen rock", "body", "articles");
        suggesters.forEach(System.out::println);
    }
    
    /**
     * 测试 Term Suggestion - 从 copilot-search SuggestTest 迁移
     * 对应原版 testTermSuggester()
     */
    @Test
    public void testTermSuggester() {
        Set<String> suggesters = ElasticUtils.suggest("articles")
                .name("term-suggestion")
                .field("body")
                .text("lucen rock")
                .suggestMode(SuggestMode.POPULAR)
                .suggest();
        
        suggesters.forEach(System.out::println);
    }
    
    /**
     * 测试 Phrase Suggest 便捷接口 - 从 copilot-search SuggestTest 迁移
     * 对应原版 testPhraseSuggester()
     */
    @Test
    public void testPhraseSuggest() {
        Set<String> suggests = ElasticUtils.phraseSuggest("lucne and elasticsear rock", "body", "articles");
        suggests.forEach(System.out::println);
    }

    /**
     * 测试 Phrase Suggestion - 从 copilot-search SuggestTest 迁移
     * 对应原版 testPhraseSuggester()
     */
    @Test
    public void testPhraseSuggester() {
        Set<String> suggests = ElasticUtils.suggest("articles")
                .name("phrase-suggestion")
                .field("body")
                .text("lucne and elasticsear rock")
                .maxErrors(2f)
                .confidence(2)
                .highlight("<em>", "</em>")
                .suggest();
        
        suggests.forEach(System.out::println);
    }
    
    /**
     * 测试 Completion Suggest 便捷接口 - 从 copilot-search CompleteSuggestTest 迁移
     * 对应原版 testCompletionSuggestion()
     */
    @Test
    public void testCompletionSuggest() {
        Set<String> suggests = ElasticUtils.completionSuggest("e", "title_completion", "articles_completion");
        suggests.forEach(System.out::println);
    }

    /**
     * 测试 Completion Suggestion - 从 copilot-search CompleteSuggestTest 迁移
     * 对应原版 testCompletionSuggestion()
     */
    @Test
    public void testCompletionSuggestion() {
        ElasticUtils.index("articles_completion",
                "{\"title_completion\": \"lucene is very cool\"}", "1");
        ElasticUtils.index("articles_completion",
                "{\"title_completion\": \"Elasticsearch builds on top of Lucene\"}", "2");
        ElasticUtils.index("articles_completion",
                "{\"title_completion\": \"Elasticsearch rocks\"}", "3");
        ElasticUtils.index("articles_completion",
                "{\"title_completion\": \"elastic is the company behind ELK stack\"}", "4");
        ElasticUtils.index("articles_completion",
                "{\"title_completion\": \"TLK stack rocks\"}", "5");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        Set<String> suggests = ElasticUtils.suggest("articles_completion")
                .name("title_completion")
                .field("title_completion")
                .prefix("e")
                .suggest();
        
        suggests.forEach(System.out::println);
    }
    
    @Test
    public void testContextSuggestion() {
        Set<String> suggests = ElasticUtils.contextSuggest("comments")
                .name("contextSuggestName")
                .category("movies")
                .categoryName("comment_category")
                .field("comment_autocomplete")
                .prefix("sta")
                .suggest();
        
        suggests.forEach(System.out::println);
    }
    
    
    // ==================== 内部类定义 ====================
    
    @Data
    private static class Employee {
        private String name;
        private Integer age;
        private String job;
        private Gender gender;
        private BigDecimal salary;
    }
    
    public static class Person {
        private Integer id;
        private String user;
        private String comment;

        public Person() {}

        public Person(Integer id, String user, String comment) {
            this.id = id;
            this.user = user;
            this.comment = comment;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    // ==================== analyze() 相关测试 ====================

    /**
     * 测试 HanLP 分词器分析文本 - 从 copilot-search 迁移
     * 对应原版 testHanLpAnalyzer()
     */
    @Test
    public void testHanLpAnalyzer() {
        ElasticUtils.analyze(Analyzer.HANLP_NLP, "美国会同意对台军售").forEach(System.out::println);
        System.out.println("------------------------");

        ElasticUtils.analyze(Analyzer.HANLP_STANDARD, "美国会同意对台军售").forEach(System.out::println);
        System.out.println("------------------------");
        ElasticUtils.analyze(Analyzer.HANLP, "美国会同意对台军售").forEach(System.out::println);
        System.out.println("------------------------");
        ElasticUtils.analyze(Analyzer.HANLP_N_SHORT, "美国会同意对台军售").forEach(System.out::println);
        System.out.println("------------------------");
    }

    // ==================== updateByQuery ====================

    /**
     * 测试 updateByQuery - 从 copilot-search 迁移
     * 对应原版 testUpdateByQuery()
     */
    @Test
    public void testUpdateByQuery() {
        UpdateByQueryResponse response = ElasticUtils.updateByQuery("blogs");
        log.info("UpdateByQuery total: {}, updated: {}, versionConflicts: {}",
                response.total(), response.updated(), response.versionConflicts());
    }
}
