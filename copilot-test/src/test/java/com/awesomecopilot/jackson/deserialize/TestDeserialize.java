package com.awesomecopilot.jackson.deserialize;

import com.awesomecopilot.common.lang.utils.IOUtils;
import com.awesomecopilot.json.jackson.JacksonUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestDeserialize {

    @Test
    public void testDeserialize() {
        String body = IOUtils.readClassPathFileAsString("requestBody.json");
        RequestDTO object = JacksonUtils.toObject(body, RequestDTO.class);
        assertEquals(object.getBizId(), "W00002");
    }

    @Test
    public void testFormatJson() {
        String body = IOUtils.readClassPathFileAsString("requestBody.json");
        String formated = JacksonUtils.formatJsonString(body);
        System.out.println(formated);
    }
}
