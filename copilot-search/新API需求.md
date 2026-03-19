#### 1) 嵌套子聚合

* 需求

  @d:\Learning\awesome-copilot\ 你学习一下com.awesomecopilot.search.AggTest这个测试类上多种聚合方式的用法, 现在我基于transportClient封装的ElasticUtils客户端, 对于聚合志指出一层嵌套, 比如这个聚合需求: 按照年龄段聚合, 并请求这些年龄段的人的平均工资

  用Elasticsearch的Query DSL来写是这样的:

  ```http
  POST bank/_search
  {
    "query": {
      "match_all": {}
    }, 
    "size": 0, 
    "aggs": {
      "age_term": {
        "terms": {
          "field": "age"
        },
        "aggs": {
          "salary_avg": {
            "avg": {
              "field": "balance"
            }
          }
        }
      }
    }
  }
  ```

  对应ElasticUtils API

  ```java
  @Test
  public void testAgeTermsSalarySubAgg() {
    //按照年龄段聚合, 并请求这些年龄段的人的平均工资
    List<Map<String, Object>> aggResult = ElasticUtils.Aggs.terms("bank")
        .of("age_term", "age")
        .subAggregation(SubAggregations.avg("salary_avg", "balance"))
        .get();
    
    System.out.println(toPrettyJson(aggResult));
  }
  ```

  这种嵌套一层的子聚合, 我的API已经支持了, 但是子聚合里再嵌套子聚合我当时实现的时候觉得太麻烦了就没有实现, 比如要用我的API实现下面这个聚合需求就还做不到

  

  查出所有年龄分布, 并且这些年龄段中M的平均薪资以及这个年龄段所有的薪资情况

  Elasticsearch的Query DSL写法

  ```http
  POST bank/_search
  {
    "query": {
      "match_all": {}
    },
    "size": 0,
    "aggs": {
      "age_agg": {
        "terms": {
          "field": "age",
          "size": 20
        },
        "aggs": {
          "gender_agg": {
            "terms": {
              "field": "gender.keyword",
              "size": 10
            },
            "aggs": {
              "salary_avg": {
                "stats": {
                  "field": "balance"
                }
              }
            }
          }
        }
      }
    }
  }
  ```

  对应ElasticUtils API目前没有对应实现, 请你阅读并学习copilot-search的源码, 帮我实现子聚合下嵌套子聚合的实现, 代码风格跟现在的实现保持一致