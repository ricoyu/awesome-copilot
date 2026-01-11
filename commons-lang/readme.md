# 一 全局ID生成

雪花算法ID生成器实现 SnowflakeId, 可以在application.yaml或者application.properties里面配置

```properties
copilot.snowflake.workerId=1
copilot.snowflake.datacenter-id=1
```

不配置的话workerId默认基于服务器IP生成, datacenter-id默认取1