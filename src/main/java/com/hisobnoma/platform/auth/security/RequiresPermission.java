package com.hisobnoma.platform.auth.security;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * The permission codes required to access the method/type.
     * If multiple permissions are specified, ALL are required (AND logic).
     */
    String[] value();

    /**
     * If true, only ONE of the specified permissions is required (OR logic).
     * Default is false (AND logic).
     */
    boolean anyOf() default false;
}
