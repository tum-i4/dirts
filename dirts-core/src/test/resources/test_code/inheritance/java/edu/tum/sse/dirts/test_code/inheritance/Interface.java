package edu.tum.sse.dirts.test_code.inheritance;

public interface Interface {

    void interfaceMethod1(AbstractClass other);

    void interfaceMethod2(AbstractSuperClass other);

    default void defaultMethod() {

    }
}
