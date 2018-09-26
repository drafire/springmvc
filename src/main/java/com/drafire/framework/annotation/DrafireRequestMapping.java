package com.drafire.framework.annotation;

import java.lang.annotation.*;

@Documented
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DrafireRequestMapping {
    String value() default "";
}
