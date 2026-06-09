package com.awesomecopilot.search8x;


import org.junit.jupiter.api.Test;

import java.util.List;

import static com.awesomecopilot.search8x.enums.Direction.DESC;

public class SortTest {

    @Test
    public void testMatchAllSort() {
        List<Object> products = ElasticUtils.Query.matchAllQuery("product")
                .sort("name.keyword", DESC)
                .queryForList();
        products.forEach(System.out::println);
    }
}