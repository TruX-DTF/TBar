package edu.lu.uni.serval.jdt.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Register {
    String id();
    String[] accept() default { };
    int priority() default Registry.Priority.MEDIUM;
}
