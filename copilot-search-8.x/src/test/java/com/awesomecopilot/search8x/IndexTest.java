package com.awesomecopilot.search8x;

import com.awesomecopilot.search8x.ElasticUtils.Admin;
import com.awesomecopilot.search8x.enums.FieldType;
import com.awesomecopilot.search8x.support.UpdateResult;
import com.awesomecopilot.search8x.vo.Index;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.awesomecopilot.json.jackson.JacksonUtils.toJson;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IndexTest {

    /**
     * 创建Index的同时插入文档, 对应的DSL语句
     * PUT product/_doc/1
     * {
     *   "name": "小米手机",
     *   "desc": "手机中的战斗机",
     *   "price": 3999,
     *   "lv": "旗舰机",
     *   "type": "手机",
     *   "createtime": "2020-10-01T08:00:00Z",
     *   "tags": [
     *     "性价比",
     *     "发烧",
     *     "不卡顿"
     *   ]
     * }
     */
    @Test
    public void testCreateProduct() {
        boolean deleted = ElasticUtils.Admin.deleteIndex("product");
        System.out.println(deleted);
        String result = ElasticUtils.index("product")
                .id("1")
                .doc("{\n" +
                        "  \"name\": \"小米手机\",\n" +
                        "  \"desc\": \"手机中的战斗机\",\n" +
                        "  \"price\": 3999,\n" +
                        "  \"lv\": \"旗舰机\",\n" +
                        "  \"type\": \"手机\",\n" +
                        "  \"createtime\": \"2020-10-01T08:00:00Z\",\n" +
                        "  \"tags\": [\n" +
                        "    \"性价比\",\n" +
                        "    \"发烧\", \n" +
                        "    \"不卡顿\"\n" +
                        "  ]\n" +
                        "}")
                .execute();
        System.out.println(result);
    }
    
    @Test
    public void testIndexDoc() {
        String indexed = ElasticUtils.index("users", """
                {
                  "firstName": "Jack",\s
                  "lastName": "Johnson",
                  "tags":["guitar", "skateboard"]
                }""", 1);
        System.out.println(indexed);
    }

    /**
     * 仅创建Index, 同时设置分片和副本数
     */
    @Test
    public void testCreateIndexWithSettings() {
        boolean created = ElasticUtils.Admin.createIndex("blogs")
                .settings()
                .numberOfShards(3) //#主分片数3
                .numberOfReplicas(1) //每个主分片1个副本
                .thenCreate();
        assertTrue(created);
    }

    /**
     * GET _cat/indices?v
     */
    @Test
    public void testListIndices() {
        List<String> indices = ElasticUtils.Admin.listIndexNames();
        indices.forEach(System.out::println);

        List<Index> indices2 = ElasticUtils.Admin.listIndices();
        indices2.forEach(System.out::println);
    }

    @Test
    public void testUpdateIndex() {
        UpdateResult updateResult = ElasticUtils.update("product", 2, "{\"name\": \"xiaomi nfc phone\", \"doc\": \"zhichi quangongneng nfc,shou ji zhong de jianjiji\", \"price\": 8999, \"tags\": [\"xingjiabi\", \"fashao\", \"gongjiaoka\"] }");
        System.out.println(toJson(updateResult));
    }
    
    /**
     * 对应的DSL语句
     * <pre>
     * PUT /test
     * {
     *   "settings": {
     *     "index.number_of_shards": 1,
     *     "number_of_replicas": 0
     *   },
     *   "mappings": {
     *     "properties": {
     *       "name": {"type": "text"},
     *       "age": {"type": "integer"}
     *     }
     *   }
     * }
     * </pre>
     */
    @Test
    public void testCreateIndexWithSettingsMappings() {
        boolean exists = Admin.existsIndex("test");
        if (exists) {
            Admin.deleteIndex("test");
        }
        boolean created = Admin.createIndex("test")
                .settings()
                .numberOfShards(1)
                .numberOfReplicas(0)
                .and()
                .mappings()
                .field("name", FieldType.TEXT)
                .field("age", FieldType.INTEGER)
                .thenCreate();
        assertTrue(created);
    }
}