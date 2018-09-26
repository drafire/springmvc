package com.drafire.framework.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)        //自动注入只能用于成员变量
public @interface DrafireAutowire {
    String value() default "";
}
