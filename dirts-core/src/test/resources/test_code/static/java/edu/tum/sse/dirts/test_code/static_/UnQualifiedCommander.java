package edu.tum.sse.dirts.test_code.static_;

import static edu.tum.sse.dirts.test_code.static_.StaticDelegate.orderUnQualified;

public class UnQualifiedCommander {

    public static void main(String[] args) {
        orderUnQualified();

        NonStaticDelegate nonStaticDelegate = new NonStaticDelegate();
        nonStaticDelegate.delegate();
        nonStaticDelegate.member = 0;
    }
}
