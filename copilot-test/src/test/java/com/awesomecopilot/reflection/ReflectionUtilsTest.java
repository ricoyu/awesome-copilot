package com.awesomecopilot.reflection;

import com.awesomecopilot.common.lang.utils.ReflectionUtils;
import com.awesomecopilot.common.lang.vo.Result;
import com.awesomecopilot.common.lang.vo.Results;
import org.junit.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static com.awesomecopilot.common.lang.utils.ReflectionUtils.isCamelCase;
import static com.awesomecopilot.common.lang.utils.ReflectionUtils.toUnderScore;

/**
 * <p>
 * Copyright: (C), 2020-11-18 9:51
 * <p>
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ReflectionUtilsTest {

    @Test
    public void testGetSpringMvcControllerAnnotation() {
        boolean exists = ReflectionUtils.existsAnnotation(RestController.class, MyController.class);
        if (exists) {
            Set<String> uris = new HashSet<>();
            Set<Method> methods = ReflectionUtils.filterMethodByAnnotation(MyController.class, SpeedUpDev.class);
            for (Method method : methods) {
                String[] values = null;
                GetMapping getMapping = method.getAnnotation(GetMapping.class);
                if (getMapping != null) {
                    values = getMapping.value();
                    for (int i = 0; i < values.length; i++) {
                        String uri = values[i];
                        if (uri != null) {
                            uris.add(uri);
                        }
                    }

                } else {
                    PostMapping postMapping = method.getAnnotation(PostMapping.class);
                    if (postMapping != null) {
                        values = postMapping.value();
                        for (int i = 0; i < values.length; i++) {
                            String uri = values[i];
                            if (uri != null) {
                                uris.add(uri);
                            }
                        }
                    }
                }
            }

            uris.forEach(System.out::println);
        }
    }

    @Test
    public void test() {
        String[] underscoreNames = new String[]{"src_field", "basicTask_equipmentChangeNotify"};
        for (int i = 0; i < underscoreNames.length; i++) {
            String underscoreName = underscoreNames[i];
            String camelCaseName = ReflectionUtils.toCamelCase(underscoreName);
            System.out.println(camelCaseName); // 输出: srcField
        }
    }

    @org.junit.jupiter.api.Test
    public void testIsCamelCase() {
        // 符合驼峰的场景（包含单单词纯小写）
        System.out.println(isCamelCase("id"));            // true
        System.out.println(isCamelCase("name"));          // true
        System.out.println(isCamelCase("userId"));        // true
        System.out.println(isCamelCase("user123Name"));   // true
        // 不符合的场景
        System.out.println(isCamelCase("ID"));            // false（全大写，首字符非小写）
        System.out.println(isCamelCase("UserId"));        // false（首字符大写）
        System.out.println(isCamelCase("user_name"));     // false（包含下划线）
        System.out.println(isCamelCase("123userId"));     // false（首字符数字）
        System.out.println(isCamelCase("user name"));     // false（包含空格）
        System.out.println(isCamelCase(""));              // false（空字符串）
        System.out.println(isCamelCase(null));            // false（空指针）
    }

    @org.junit.jupiter.api.Test
    public void testCamelCase2UnderScore() {
        // 优化后测试：userID → user_id（而非 user_i_d）
        System.out.println(toUnderScore("id")); // id
        System.out.println(toUnderScore("userID")); // user_id
        System.out.println(toUnderScore("userIDCard")); // user_id_card
    }

    @Inherited
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface SpeedUpDev {

    }

    @RestController
    static class MyController {

        @SpeedUpDev
        @GetMapping("/hi")
        public Result hi() {
            return Results.success().build();
        }

        @SpeedUpDev
        @PostMapping("/create")
        public Result create() {
            return Results.success().build();
        }
    }
}
