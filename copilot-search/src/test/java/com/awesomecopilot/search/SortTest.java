package com.awesomecopilot.search;

import org.junit.Test;

import java.util.List;

import static com.awesomecopilot.search.enums.Direction.DESC;

public class SortTest {

    @Test
    public void testMatchAllSort() {
        List<Object> products = ElasticUtils.Query.matchAllQuery("product")
                .sort("name.keyword", DESC)
                .queryForList();
        products.forEach(System.out::println);
    }
}
