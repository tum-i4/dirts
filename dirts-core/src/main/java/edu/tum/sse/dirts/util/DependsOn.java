package edu.tum.sse.dirts.util;

public @interface DependsOn {

    Class<?> value();

    String method();
}
