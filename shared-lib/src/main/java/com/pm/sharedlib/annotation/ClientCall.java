package com.pm.sharedlib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClientCall {
    String name();
    String destinationUrl() default "";
    String topic() default "";
    String method() default "";
    String protocol() default "HTTP";
}
