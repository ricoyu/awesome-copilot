# 一 ES版本升级

awesome-copilot下有一个字module copilot-search, 这个module是一个Elasticsearch客户端的封装, 提供了fluent风格的更易于使用的API, 基于Elasticsearch的transport-client 7.9.3封装, 随着Elasticsearch的版本迭代, 在实际开发中也需要不断升级Elasticsearch, 现在Elasticsearch8.x已经彻底移除了transport-client, 转而使用如下官方推荐的client, 因此我的这个Elasticsearch客户端封装也不得不升级

```xml
    <dependency>
      <groupId>co.elastic.clients</groupId>
      <artifactId>elasticsearch-java</artifactId>
      <version>8.19.16</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.17.0</version>
    </dependency>
    <dependency>
      <groupId>jakarta.json</groupId>
      <artifactId>jakarta.json-api</artifactId>
      <version>2.0.1</version>
     </dependency>
```

请你在awesome-copilot下新建一个子module: copilot-search-8.x, 把Elasticsearch的客户端API替换成现在8.x推荐的这个elasticsearch-java 8.19.16, 但是要保留ElasticUtils的API实现, 只不过是把底层API从transport-client换成elasticsearch-java, 包名从com.awesomecopilot.search改为com.awesomecopilot.search8x, 我希望新的copilot-search-8.x中ElasticUtils的接口跟原来一致, 代码风格也一致, 并且做到原来copilot-search中的单元测试不需要任何改动就能在新的copilot-search-8.x中跑通; 但是如果完全无缝迁移比较难以实现, ElasticUtils 相关API可以略做微调