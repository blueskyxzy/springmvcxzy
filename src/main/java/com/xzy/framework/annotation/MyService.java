package com.xzy.framework.annotation;

import java.lang.annotation.*;

/**
 * Created by xzy on 19/1/8  .
 */

@Target({ElementType.TYPE}) // 注释类上
@Retention(RetentionPolicy.RUNTIME)// 注解会在class字节码文件中存在，在运行时可以通过反射获取到
@Documented//说明该注解将被包含在javadoc中
public @interface MyService{
    String value() default "";
}
