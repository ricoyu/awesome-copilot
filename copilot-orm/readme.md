

## 一 配置

### 1.1 Maven依赖

接入的时候添加如下依赖

```xml
<dependency>
    <groupId>com.copilot</groupId>
    <artifactId>copilot-orm</artifactId>
    <version>17.0.0</version>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>6.5.2.Final</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <version>3.2.4</version>
</dependency>

<!-- 下面这三个xml的真的是要的, 别不信 -->
<dependency>
    <groupId>javax.xml.bind</groupId>
    <artifactId>jaxb-api</artifactId>
    <version>2.3.1</version>
</dependency>
<dependency>
    <groupId>jakarta.xml.bind</groupId>
    <artifactId>jakarta.xml.bind-api</artifactId>
    <version>4.0.0</version>
</dependency>
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>4.0.5</version> <!-- 请根据你的需求选择合适的版本 -->
</dependency>

<!-- web环境可以简化分页的使用 -->
<dependency>
    <groupId>com.copilot</groupId>
    <artifactId>copilot-spring-boot-web-starter</artifactId>
</dependency>
```

引入spring-boot-starter-data-jpa是为了让SpringBoot自动装配EntityManager

在 Hibernate 6.x 中, hibernate-entitymanager 模块已经被整合到 hibernate-core 中, 不再作为单独的依赖提供。

如果你使用的是 Hibernate 6.5.2.Final，你只需要:

1. **`hibernate-core`** (包含 JPA 实现)

现在hibernate的groupId改为 `org.hibernate.orm`

* 这是 Hibernate 6.0 及更高版本使用的规范 groupId
* 代表了 Hibernate 项目的官方组织方式
* 是 Hibernate 团队推荐的现代用法

### 1.2 application.yml

```yaml
management.endpoints.web.exposure.include: "*"
server:
  port: 8080

spring:
  application:
    name: avengers-jpa
  datasource:
    url: jdbc:mysql://localhost:3306/avengers?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useLegacyDatetimeCode=false&rewriteBatchedStatements=true&useCompression=true&useUnicode=true&autoReconnect=true&autoReconnectForPools=true&failOverReadOnly=false
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      auto-commit: true
      minimum-idle: 5
      idle-timeout: 60000
      connection-timeout: 30000
      max-lifetime: 1800000
      pool-name: AccountHikariCP
      maximum-pool-size: 30
      username: rico
      password: 123456
      driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        archive:
          scanner: org.hibernate.boot.archive.scan.internal.StandardScanner #解决新版Hibernate找不到hbm.xml文件问题
        show_sql: true
        format_sql: true
        ddl-auto: none
        jdbc.batch_size: 100
        order_inserts: true
        order_updates: true
        jdbc.time_zone: Asia/Shanghai
        generate_statistics: true
        entitymanager:
          class: org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
          mappingDirectoryLocations: classpath:named-sql
        packagesToScan: com.copilot.cloud.account.entity

        cache:
          use_second_level_cache: false
          use_query_cache: false

hibernate.query.mode: loose
hibernate.query.cache: true
hibernate.jdbc.batch_size: 100
```

### 1.3 named-sql目录

在 src/main/resources下新建named-sql目录, 里面放Hibernate映射文件xxx.hbm.xml, 比如Semester.hbm.xml, 在这里写原生SQL

```xml
<?xml version="1.0"?>
<!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
        "http://hibernate.org/dtd/hibernate-mapping-3.0.dtd">

<hibernate-mapping>
    <!-- 给你的原生SQL起一个名字, 调用的地方要用到 -->
    <sql-query name="semesters">
        <![CDATA[
            SELECT 
              s.semester,
              s.CENTRE_ID,
              s.desc,
              s.begin_date, s.end_date
            FROM
              semester s 
            WHERE s.deleted = 0
            #if($centreId)
                AND s.CENTRE_ID =:centreId
            #end
            ORDER BY s.begin_date desc 
		]]>
    </sql-query>
</hibernate-mapping>
```

### 1.4 Java Config

你的SpringBoot项目里只需要配置这个bean即可

```java
@Configuration
public class PersistentConfig {
	
	@Bean
	public JpaDao jpadao() {
		return new JpaDao();
	}
}
```

**解释一下:**

* 通过`@PersistenceContext`自动将系统中自动配置的EntityManager注入到了JpaDao里面

  ```java
  @PersistenceContext
  protected EntityManager entityManager;
  ```

### 1.5 BaseEntity 示例

```java
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类的父类, 自己选择要不要继承 
 * <p>
 * Copyright: Copyright (c) 2019-10-31 15:36
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@MappedSuperclass
public class BaseEntity implements Serializable {
	
	private static final long serialVersionUID = -7833247830642842225L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID", updatable = false, unique = true, nullable = false)
	private Long id;

	/**
	 * 默认映射的数据库字段类型为TIMESTAMP
	 */
	@Column(name = "CREATE_TIME", columnDefinition = "DATETIME", nullable = false, length = 19)
	private LocalDateTime createTime;

	@Column(name = "UPDATE_TIME", columnDefinition = "DATETIME", nullable = false, length = 19)
	private LocalDateTime updateTime;
	
	/**
	 * 在Entity被持久化之前做一些操作
	 */
	@PrePersist
	protected void onPrePersist() {
		LocalDateTime now = LocalDateTime.now();
		setCreateTime(now);
		setUpdateTime(now);
	}
	
	@PreUpdate
	protected void onPreUpdate() {
		setUpdateTime(LocalDateTime.now());
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	public LocalDateTime getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(LocalDateTime updateTime) {
		this.updateTime = updateTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
```



### 1.7 提供了雪花算法ID生成器

CopilotSnowflakeIdGenerator, 用法

```java
@Id
@GeneratedValue(generator = "snowflake-id")
@GenericGenerator(name = "snowflake-id", type = CopilotSnowflakeIdGenerator.class)
private Long id;
```



# 二 条件分支

## 2.1 基于存在性判断

1. 检查fullName不为null则输出and full_name=:fullName

   ```velocity
   #if($fullName)
   and full_name=:fullName
   #end
   ```

2. 判断变量为null(检查params里面没有塞这个参数)

   ```velocity
   #if (!$variable)
   
   #end
   ```

3. params(Map类型)包含key: roleId, role, 检查为null可以简化成这样(检查params里面有没有塞这个参数)

   ```velocity
   #if (!$variable)
   
   #end
   ```

   

4. if语句完整结构

   ```velocity
   #if(判断条件)
     .........
   #elseif(判断条件)
     .........
   #else
     .........
   #end
   ```

   ```velocity
   #if($centreId)
       AND s.CENTRE_ID =:centreId
   #end
   ```

## 2.2 基于值判断

1. 判断传了这个参数并且值是true

   ```velocity
   $usedInclude == true
   ```

2. 判断传了这个参数并且值是false

   ```velocity
   $usedInclude == false
   ```

3. 当sortField等于"salesCount"

   ```velocity
   #if($sortField == "salesCount")
       <!-- 当sortField等于"salesCount"时执行的代码 -->
   #end
   ```

4. 或者更严谨的写法（防止变量为null时出错）

   ```velocity
   #if("$!sortField" == "salesCount")
       <!-- 当sortField等于"salesCount"时执行的代码 -->
   #end
   ```

   

## 2.3 集合类型判断

1. 判断集合不为空(userIds是List)

   ```velocity
   #if(!$userIds.isEmpty())
   	WHERE u.id IN (:userIds)
   #end
   ```

## 2.4 自定义的条件判断命令

1. ifNull ifNotNull

   ```sql
   #ifNotNull($privilegeId)
   	and id != :privilegeId
   #end
   #ifNull($privilegeId)
   	and id != :privilegeId
   #end
   ```

2. ifPresent

   判断值不为null则输出指定的值, #if #else语句的简化版本

   ```sql
   SELECT
           #ifPresent($purchaser, "DISTINCT") 
           r.ID, r.RETURN_ID, 
           ......
           r.SETTLED,
           r.OWNER
   FROM
       purchase_return r
       #ifPresent($purchaser, "join return_item i on r.RETURN_ID = i.RETURN_ID and i.deleted = 0 and r.deleted = 0")
   WHERE r.deleted = 0
       #if($state)
           AND r.STATE = :state 
       #end
       #if($returnIds)
           AND r.RETURN_ID in (:returnIds) 
       #end
       #ifPresent($purchaser, "AND i.PURCHASER LIKE :purchaser")
       #between("date(r.BILLING_DATE)", $billingDateBegin, $billingDateEnd)
       #omitForCount()
       GROUP BY r.id
       #end
   ```

3. ifEqual

   ```velocity
   select * from product_spu
   where `status`=1
   #if($keyword)
       and name like :keyword
   #end
   #if($categoryIds)
       and category_id in :categoryIds
   #end
   #if($!sortField == "salesCount")
       ORDER BY (sales_count + virtual_sales_count) #ifEqual($sortAsc, true, "ASC", "DESC")
   #end
   ```

   

### SQL 中的分页

```sql
SELECT 
		p.id privilege_id, p.`create_time`, p.`creator`, p.`deleted`,
		p.`modifier`, p.`modify_time`, p.`privilege`, p.`remark`, p.`version`
FROM privilege p
WHERE p.`deleted` = '0'
group by ld.id
```

### SQL 中的BETWEEN

```sql
#between("CLOSE_MONTH", $beginDate, $endDate)       生成 AND CLOSE_MONTH BETWEEN :beginDate AND :endDate 
#between("CLOSE_MONTH", $beginDate, $endDate, "OR") 生成 OR CLOSE_MONTH BETWEEN :beginDate AND :endDate 
```

注意 #between 要小写, CLOSE_MONTH 指表的字段名, 要套上双引号, 不然会报错

### SQL 中的LIKE用法



# 三 核心接口

根据接口隔离原则, 结合使用场景划分为如下几个接口

## 3.1 EntityOperations

JPA的entity对象的一些简单操作API

* `public <T> void persist(T entity);`

  插入一条记录

* `public <T> void persist(List<T> entities);`

  插入一批记录

* `public <T> T save(T entity);`

  插入或者更新记录, 根据主键是否有值判断

* `public <T> List<T> save(List<T> entities);`

* ......

## 3.2 CriteriaOperations

一些简单的查询可以通过CriteriaOperations来完成, 毕竟写SQL码字数量也挺多的...

**这个接口操作的都是实体类, 不是任意的POJO**

* 根据属性查找

  ```java
  public <T> List<T> findByProperty(Class<T> entityClass, String propertyName, Object value, boolean includeDeleted);
  ```

* 支持排序的版本

  ```java
  public <T> List<T> findByProperty(Class<T> entityClass, String propertyName, Object value, OrderBean... orders)
  ```

* 分页

  ```java
  public <T> List<T> findByProperty(Class<T> entityClass, Predicate predicate, boolean includeDeleted, Page page);
  ```

* 根据属性查找，返回一个对象，如果找到多个，取第一个,可以指定是否包含软删除的记录,找不到则返回null

  ```java
  public <T> T findUniqueByProperty(Class<T> entityClass, String propertyName, Object value, OrderBean... orders);
  ```

* 根据日期区间查找

  ```java
  public <T> List<T> findBetween(Class<T> entityClass, String propertyName, LocalDateTime begin, LocalDateTime end);
  ```

* 根据属性在给定值列表中来获取，可以指定是否包含软删除的对象(IN 操作)

  ```java
  public <T> List<T> findIn(Class<T> entityClass, String propertyName, Collection<?> value);
  ```

* 找到某个属性是null的对象

  ```java
  public <T> List<T> findIsNull(Class<T> entityClass, String propertyName);
  ```

* 检查对应entity是否存在

  ```java
  public <T> boolean ifExists(Class<T> entityClass, String propertyName, Object value);
  ```

* 根据属性查找，返回唯一一个对象，如果找到多个，取第一个，如果找不到则抛出 EntityNotFoundException

  ```java
  public <T> T ensureEntityExists(Class<T> entityClass, String propertyName, Object value) throws EntityNotFoundException;
  ```

* 根据属性删除对象

  ```java
  public <T> int deleteByProperty(Class<T> entityClass, String propertyName, Object propertyValue);
  ```

* 骚操作太多, 不一一列举
  ......

## 3.3 SQLOperations

**重点总是留到最后**, 在src/main/resources/named-sql目录下添加**XXX.hbm.xml**, 这里面写SQL, 比如:MessageContent.hbm.xml

下面的SQL示例会根据传入参数不同而生成不同的SQL语句

```xml
<?xml version="1.0" encoding="UTF-8"?>  
<!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd" >
<hibernate-mapping>
    <sql-query name="queryNeedRetryMsg">
        <![CDATA[
            select * from message_content 
            where 
            #if($timeDiff)
            TIMESTAMPDIFF(SECOND, create_time, SYSDATE()) >:timeDiff and
            #end
            #if($msgStatus)
            msg_status!=:msgStatus and 
            #end
            current_retry<max_retry
    	]]>
    </sql-query>
</hibernate-mapping>
```

Service代码示例

```java
@Service
@Transactional
@Slf4j
public class RetryMsgTask {
  
  @Autowired
  private SQLOperations sqlOperations;
  
  /**
   * 延时5s启动
   * 周期10S一次
   */
  @Scheduled(initialDelay = 10000, fixedDelay = 20000)
  public void retrySend() {
    log.info("-----------------------------");
    //查询五分钟消息状态还没有完结的消息
    Map<String, Object> params = new HashMap<>();
    params.put("timeDiff", MQConstants.TIME_DIFF);
    QueryUtils.enumOrdinalCondition(params, "msgStatus", MsgStatus.CONSUMER_SUCCESS);
    List<MessageContent> messageContents = sqlOperations.namedSqlQuery("queryNeedRetryMsg", params, MessageContent.class);
    ......
  }
}
```

MessageContent可以是任意的POJO, 只要MessageContent的属性与数据库字段**对得上**, 看下面的DDL和Java Bean定义就明白**对得上**的意思了

```mysql
CREATE TABLE `message_content` (
  `msg_id` varchar(50) NOT NULL,
  `order_id` bigint(32) NOT NULL DEFAULT 0,
  `product_id` int(10) NOT NULL DEFAULT 0,
  `msg_status` int(10) NOT NULL DEFAULT 0 COMMENT '0-发送中, 1-mq的broker确认接受到消息, 2-没有对应交换机, 3-没有对应的路由, 4-消费端成功消费消息',
  `exchange` varchar(50) NOT NULL DEFAULT '',
  `routing_key` varchar(50) NOT NULL DEFAULT '',
  `err_cause` varchar(1000) NOT NULL DEFAULT '',
  `max_retry` int(10) NOT NULL DEFAULT 0,
  `current_retry` int(10) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT NOW(),
  `update_time` datetime NOT NULL DEFAULT NOW(),
  PRIMARY KEY (`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

```java
public class MessageContent {
  private String msgId;
  private Long orderId;
  private Long productId;
  private MsgStatus msgStatus;
  private String exchange;
  private String routingKey;
  private String errCause;
  private Integer maxRetry;
  private Integer currentRetry = 0;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
```

* order_id --> orderId

  你不需要做任何事情, 本框架会自动帮你把数据库字段名和Bean属性名对应上。所以我们在做表设计的时候可以保留数据库字段命名的风格(下划线_分隔), 不需要为了表字段名和Java bean属性名一致而做出妥协。

* msg_status(int) --> msgStatus(enum类型MsgStatus)

  你也不需要做任何事情, 本框会自动帮你把int型转成对应的enum类型。其他常用的数据类型都能识别并在需要时自动转换

**SQLOperations接口常用API示例**

* SQL查询

  ```java
  public <T> List<T> namedSqlQuery(String queryName, Map<String, Object> params, Class<T> clazz);
  ```

  * queryName 是写在XML里面的SQL语句的名字, 全局唯一
  * params 是SQL里面用到的参数
  * clazz 你想要返回的List里面是什么类型的对象, 这个clazz就传什么, clazz可以是任意的POJO

* 支持分页的SQL查询

  ```java
  public <T> List<T> namedSqlQuery(String queryName, Map<String, Object> params, Class<T> clazz, Page page);
  ```

* 跟namedSqlQuery的差别就是结果集不封装到Bean里面

  ```java
  public List<?> namedRawSqlQuery(String queryName);
  ```

* 返回单个值的查询. 比如type是BigDecimal.class，那么这个查询返回的是BigDecimal

  ```java
  public <T> T namedScalarQuery(String queryName, Map<String, Object> params, Class<T> type);
  ```

* 根据给定的查询条件判断是否有符合条件的记录存在

  ```java
  public boolean ifExists(String queryName, Map<String, Object> params);
  ```

* 执行更新

  ```java
  public int executeUpdate(String queryName, Map<String, Object> params);
  ```

* 骚操作太多, 不一一列举

  ......



# 四 类型转换

1. @Convert注解

   ```java
   @Convert(converter = StringListConverter.class)
   private List<String> sliderPicUrls;
   ```

   遇到自动类型无法完成的类型转换时, 可以实现自己的jakarta.persistence.AttributeConverter, 然后指定的对应字段的@Convert注解上
   
   * StringListConverter
   
     支持POJO属性是List<String>类型, 数据库字段是varchar类型, 值是'[item1,item2,item2]'这种形式互转



# 四 完整示例

## 4.1 分页查询

### 4.1.1 BrandController

```java
@PostMapping("/search")
public Result<List<BrandVO>> searchBrand(@RequestBody BrandDTO brandDTO) {
  List<BrandVO> brandVOS =brandService.searchBrand(brandDTO);
  return Results.<List<BrandVO>>success().data(brandVOS).build();
}
```

返回的Result对象里面有一个Page属性, 框架会通过AOP把分页结果(总记录数, 总页数等)回填进去, 我们业务代码不需要关心, 返回结果示例

```json
{
  "code": "0",
  "status": "success",
  "message": null,
  "page": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 1,
    "totalPages": 1
  },
  "data": [
    {
      "brandId": 1,
      "descript": "班尼路 1981 年诞生于香港，是家喻户晓的休闲服饰品牌。产品覆盖男、女、中性及童装等全品类，主打简约舒适、高性价比的设计风格，满足日常穿搭需求。品牌门店遍布全国，数量超 2500 家，凭借亲民定位收获了广泛消费群体的喜爱。",
      "firstLetter": "B",
      "logo": "http://localhost:8070/file/ceph/show/4303c60aa3f2_班尼路.jpeg",
      "name": "班尼路",
      "showStatus": 1,
      "sort": 1
    }
  ]
}
```



BrandDTO继承PageDTO是因为PageDTO里面有Page对象属性可以接收分页参数, 也可以用单独的请求参数来传递分页参数: 

* pageNum(第几页, 从1开始)
* pageSize(每页多少条记录)
* order(排序规则: 在字段名后加“:asc或:desc”指定升序 (降序), 多个字段使用逗号分隔, 省略排序默认使用升序)", example = "“字段1,字段2” 或者 “字段1:asc,字段2:desc)

```java
package com.copilot.bilibili.mall.product.dto;

import com.awesomecopilot.common.lang.dto.PageDTO;
import com.awesomecopilot.validation.validation.annotation.UniqueValue;
import com.copilot.bilibili.mall.common.validation.groups.AddGroups;
import com.copilot.bilibili.mall.common.validation.groups.UpdateGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 品牌数据传输对象
 */
@Data
@UniqueValue(table = "pms_brand", primaryKey = "brand_id", property = "name", message = "品牌名称已存在")
public class BrandDTO extends PageDTO {

    /**
     * 品牌id
     */
    @Null(groups = {AddGroups.class}, message = "品牌id必须为空")
    @NotNull(groups = {UpdateGroups.class}, message = "品牌id不能为空")
    private Long brandId;

    /**
     * 品牌名
     */
    @NotEmpty(message = "品牌名称不能为空", groups = {AddGroups.class, UpdateGroups.class})
    private String name;

    /**
     * 品牌logo地址
     */
    @NotEmpty(message = "品牌logo不能为空", groups = {AddGroups.class, UpdateGroups.class})
    @Size(max = 2000, message = "品牌logo长度不能超过2000", groups = {AddGroups.class, UpdateGroups.class})
    private String logo;

    /**
     * 介绍
     */
    @NotEmpty(message = "品牌介绍不能为空", groups = {AddGroups.class, UpdateGroups.class})
    private String descript;

    /**
     * 显示状态(0-不显示；1-显示)
     */
    @NotNull(message = "显示状态不能为空", groups = {AddGroups.class, UpdateGroups.class})
    private Boolean showStatus;

    /**
     * 检索首字母
     */
    @NotBlank(message = "检索首字母不能为空")
    private String firstLetter;

    /**
     * 排序
     */
    @NotNull(message = "排序不能为空")
    private Integer sort;
}
```



Page对象是commons-lang提供的, 可以作为接收查询参数的Bean的属性存在, 提交的POST请求查询数据格式示例:

```json
{
  "name": "Tiffany Schneider",
  "page": {
    "pageNum": 1,
    "pageSize": 10,
    "sorts": [
      "-full_name"
    ]
  }
}
```

如果有多个排序规则, 还提供了简化的语法, 字段前加"-"表示按这个字段DESC排, 否则ASC, 这个语法糖是通过PageDeserializer自定义了Jackson对Page对象的反序列化来实现的

```json
{
  "pageNum":3,
  "pageSize": 20,
  "sorts": ["age:desc", "salary" ]
}
```



### 4.1.2 Service 层

```java
public List<BrandVO> searchBrand(BrandDTO brandDTO) {
  List<BrandVO> brandVOs = sqlOperations.query("queryBrands")
      .addlikeParam("name", brandDTO.getName())
      .page(brandDTO.getPageNum(), brandDTO.getPageSize())
      .resultClass(BrandVO.class)
      .findPage();
  return brandVOs;
}
```

只需要注入SQLOperations对象, SQLOperations是一个接口, JpaDao实现了该接口, 主要提供原生SQL查询的功能, queryBrands是这个原生SQL的名字, 在named-sql目录下的xxx.hbm.xml中定义, BrandVO可以是JPA的实体类也可以是普通的POJO, 其中"name"是SQL语句中查询条件的占位符名字

* addlikeParam 是为like语句创建的便捷方法, 如果brandDTO.getName()不为空则会往params添加一个name变量, 值是%brandDTO.getName()%

* addLlikeParam 是往params添加一个name变量, 值是%brandDTO.getName()
* addRlikeParam 是往params添加一个name变量, 值是brandDTO.getName()%
* 不是like条件的话直接addParam即可

### 4.1.3 Brand.hbm.xml

分页语句以及排序子句都会自动生成, 你这边只需要写select以及查询条件即可

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN" "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd" >
<hibernate-mapping>
    <sql-query name="queryBrands">
        <![CDATA[
            select * from pms_brand where
            #if($name)
            name like :name
            order by `sort` asc
            #end
    	]]>
    </sql-query>
</hibernate-mapping>
```



## Service层注入JpaDao

在Service层根据需要注入JpaDao的实例到这几个核心接口类型上

```java
@Autowired
private EntityOperations entityOperations;

@Autowired
private CriteriaOperations criteriaOperations;

@Autowired
private SQLOperations sqlOperations;
```

业务方法里面执行查询

```java
public List<PurchaseOrderListsVO> searchPurchaseOrder(PurchaseOrderSearchVO purchaseOrderSearchVO) {
    Map<String, Object> params = new HashMap<>();
    params.put("beginDate", purchaseOrderSearchVO.getBeginDate());
    params.put("endDate", purchaseOrderSearchVO.getEndDate());
    params.put("supplierId", purchaseOrderSearchVO.getSupplierId());
    params.put("status", QueryUtils.innerMatch(purchaseOrderSearchVO.getStatus()));
    params.put("auditStatus", purchaseOrderSearchVO.getAuditStatus());
    params.put("skus", QueryUtils.innerMatch(purchaseOrderSearchVO.getSkus()));
    params.put("purchaseContractNo", QueryUtils.innerMatch(purchaseOrderSearchVO.getPurchaseContractNo()));
    List<PurchaseOrderListsVO> purchaseOrderListsVOS = sqlOperations.namedSqlQuery("searchPurchaseOrder", params, PurchaseOrderListsVO.class, purchaseOrderSearchVO.getPage());
	......
}
```

PurchaseOrderListsVO是普通的POJO或者Entity都可以, 查询结果的字段名(如PURCHASE_CONTRACT_NO)自动转成POJO的属性名purchaseContractNo

有数据类型不一致的也自动转换

```java
@Data
public class PurchaseOrderListsVO {
    private Long id;
    private String orderNo;
    private String purchaseContractNo;//采购单合同号
    private Long supplierId;//供应商ID
	......
}
```

其中`searchPurchaseOrder`是定义在XML中的SQL的名字, 看下面

## src/main/resources/named-sql

这个目录下放SQL语句, 如PurchaseOrder.hbm.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE hibernate-mapping PUBLIC "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
        "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd" >
<hibernate-mapping>
    <sql-query name="searchPurchaseOrder">
        <![CDATA[
            SELECT
			-- SQL分页支持, 会另外生成一条SQL查总记录数
             #count()
                po.*
             #end
             FROM purchase_order po
             WHERE po.SUPPLIER_ID = :supplierId
             -- 表示Java代码里面传了blocParentId这个参数的话, 生成的SQL里面就会包含 AND uo.BLOC_PARENT_ID= :blocParentId 这一段
             #if($blocParentId)
             AND po.BLOC_PARENT_ID= :blocParentId
             -- 支持elseif, else语句
             #elseif($count > 0)
             .........
             #else
              .........
             #end
			-- 如果传了beginDate和endDate, 则生成SQL: AND CLOSE_MONTH BETWEEN :beginDate AND :endDate
			-- 只传   beginDate, 那么生成SQL: AND CLOSE_MONTH >= :beginDate
			-- 只传   endDate,   那么生成SQL: AND CLOSE_MONTH <= :endDate
             -- 都不传的话什么都不生成
			#between("CLOSE_MONTH", $beginDate, $endDate)
		]]>
    </sql-query>
</hibernate-mapping>
```



# 五 多线程环境下没有Spring事务控制下使用

写数据需要手工开启事务/提交事务, 比如:

```java
entityOperations.begin();
entityOperations.save(outboundItems);
entityOperations.commit();
```



# 六 枚举类型用自定义属性值读写

1. 假设有这样一个有自定义属性的枚举类EquipmentType

   ```java
   public enum EquipmentType {
   	
   	ELEVATOR("LIFT","提升机"),
   	VEHICLE("TAMR","小车");
   	
   	private final String typeId;
   	private final String typeName;
   	
   	private EquipmentType(String typeId,String typeName){
   		this.typeId = typeId;
   		this.typeName = typeName;
   	}
   	
   	public String getTypeId() {
   		return typeId;
   	}
   	
   	public String getTypeName() {
   		return typeName;
   	}
   }
   ```

2. 实体类持久该enum类型, 但是数据库实际存的既不是这个enum的name, 也不是其ordinal, 而是其typeId属性值

   此时就可以加上 @Convert(converter = EquipmentTypeConverter.class)注解, 这是JPA原生 API

   **但是注意了:** 使用了 @Enumerated(EnumType.STRING)注解的话 @Convert注解就无效了

   ```java
   @Entity
   @Table(name = "tcp_task_status")
   public class TcpTaskStatus {
   
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Integer id;
   
       @Column(nullable = false)
       private Integer seq;
   
       @Column(nullable = false)
       @Enumerated(EnumType.STRING)
       @Convert(converter = EquipmentTypeConverter.class)
       private EquipmentType equipmentType;
   
       @Column(length = 64, nullable = false)
       private String equipmentId;
   
       @Column(nullable = false)
       private Integer qty;
   
       @Column(nullable = false)
       private Integer finishedQty;
   
       @Column(length = 20, nullable = false)
       private String taskType;
   
       @Column(length = 50, nullable = false)
       private String positionId;
   
       @Column(length = 1024, nullable = false)
       private String msg;
   
       @Column(length = 20, nullable = false)
       private String commandId;
   
       @Column(length = 64, nullable = false)
       private String creator;
   
       @Column(nullable = false)
       private LocalDateTime createTime;
   
       @Column(length = 64, nullable = false)
       private String modifier;
   
       @Column(nullable = false)
       private LocalDateTime modifyTime;
   
   }
   ```

3. EquipmentTypeConverter

   ```java
   @Converter
   public class EquipmentTypeConverter implements AttributeConverter<EquipmentType, String> {
   	
   	@Override
   	public String convertToDatabaseColumn(EquipmentType attribute) {
   		return attribute.getTypeId();
   	}
   	
   	@Override
   	public EquipmentType convertToEntityAttribute(String dbData) {
   		return EnumUtils.toEnum(EquipmentType.class, dbData, "typeId");
   	}
   }
   ```

# 七 ORM 错误

如果是多module项目, Entity实体类在一个module里面, 然后另一个module是一个SpringBoot应用, 在这里执行named-sql查询, xxx.hbm.xml也是放在这个module里面的, 那么正常的Idea debug没问题, 但是用Jrebel debug会找不到named-sql, 必须把xxx.hbm.xml和实体类放到同一个module下



# 八 自动添加 逻辑删除和租户ID 条件

默认就支持逻辑删除, 每条SQL后面都会自动加上deleted=0, 强制租户ID需要手工开启

请求头需要包含Tenant-Id, 然后引入TenantIdFilter(`copilot-spring-boot-web-starter`中已经自动配置), 该filter会把Tenant-Id请求头的值放到ThreadLocal里面, 解析SQL的时候自动检查ThreadLocal中包不包含tenantId这个变量, 包含的话就自动添加tenant_id=xxx的条件, 如果原始SQL已经写了tenant_id=xxx, 那么不会重复添加.

SQLOperations 和 CriteriaOperations两个接口都支持

配置方法:

1. 添加拦截器

   ```yaml
   spring:
     jpa:
       properties:
         hibernate:
           session_factory:
             statement_inspector: com.awesomecopilot.cloud.product.config.DeletedTenantIdConditionInterceptor
   ```

2. application.yaml配置

   ```yaml
   copilot:
     orm:
       logicalDelete:
         enabled: true
     filter:
       tenant:
         mandatory: true
         excludeUrls:
           - /**
   ```

3. 显式关闭逻辑删除功能

   ```yaml
   copilot:
     orm:
       logicalDelete:
         enabled: false
   ```


关闭以后只会检查租户ID的存在性来自动判断是否加入 tenant_id=xxx 条件



# 九  SQL自动修复

可以一定程度上对SQL做自动修复, 比如你写的SQL是: select * from sys_menu name='rico' age>18 is_deleted=0
显然少了一个where关键字, 那么开启SQL自动修复后会自动帮你加上where关键字, 修复后的SQL会变成: select * from sys_menu where name='rico' and age>18 and is_deleted=0

但是实测只对相对简单的SQL有效, 复杂SQL(多表join, 子查询等等)不一定靠谱

目前能做到的是已经不需要去写where 1=1这种SQL语句了, 示例如下:

```mysql
select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a
left join pms_category c on a.catelog_id = c.cat_id
LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id
LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id
where
#if($catelogId)
  a.catelog_id = :catelogId
#end
#if($attrName)
  and a.attr_name like :attrName
#end
order by agr.attr_group_id asc, agr.attr_sort
```

* 如果最后发现一个条件都没有, 那么多余的where关键字会被删掉
* 如果catelogId没传而attrName传了, 会发现where后面多了一个and关键字, and关键字也会被自动删掉

所以目前写动态SQL还是比较舒服的



# 十 排序

如果你写的原生SQL里面已经有排序字段了, 那么分页接口里面的动态排序字段就不生效了, 比如

```mysql
<sql-query name="searchAttrs">
    <![CDATA[
        select a.*, c.`name` catelog_name, ag.attr_group_name from pms_attr a
        left join pms_category c on a.catelog_id = c.cat_id
        LEFT JOIN pms_attr_attrgroup_relation agr on a.attr_id = agr.attr_id
        LEFT JOIN pms_attr_group ag on agr.attr_group_id = ag.attr_group_id
        where 1=1
        #if($catelogId)
          and a.catelog_id = :catelogId
        #end
        #if($attrName)
          and a.attr_name like :attrName
        #end
        order by attr_group_id, attr_sort
    ]]>
</sql-query>
```

那么如下分页查询接口中传入的排序规则就忽视了

```java
List<PmsAttrVO> attrVOs = sqlOperations.query("searchAttrs")
            .addParam("catelogId", catelogId)
            .addParam("attrName", attrSearchDTO.getAttrName())
            .page(attrSearchDTO.getPageNum(), attrSearchDTO.getPageSize())
            .order(attrSearchDTO.getOrder())
            .resultClass(PmsAttrVO.class)
            .findPage();
```

