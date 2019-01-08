package com.xzy.framework.annotation;
import java.lang.annotation.*;


@Target({ElementType.TYPE,ElementType.METHOD}) // 注释在类上也可以在方法上
@Retention(RetentionPolicy.RUNTIME)// 注解会在class字节码文件中存在，在运行时可以通过反射获取到
@Documented//说明该注解将被包含在javadoc中
public @interface MyRequestMapping {
    String value() default "";
}

