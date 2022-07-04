package edu.tum.sse.dirts.test_code.static_;

public class QualifiedCommander {

    public static void main(String[] args) {
        StaticDelegate.orderQualified();

        NonStaticDelegate nonStaticDelegate = new NonStaticDelegate();
        nonStaticDelegate.delegate();
        nonStaticDelegate.member = 0;
    }
}
