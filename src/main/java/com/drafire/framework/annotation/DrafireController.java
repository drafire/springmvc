package com.drafire.framework.annotation;

import java.lang.annotation.*;

@Documented
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DrafireController {
    String value() default "";
}
