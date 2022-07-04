package edu.tum.sse.dirts.test_code.static_;

public class Commander {
    public static void main(String[] args) {
        StaticDelegate.staticMember = 1;

        NonStaticDelegate nonStaticDelegate = new NonStaticDelegate();
        nonStaticDelegate.delegate();
        nonStaticDelegate.member = 0;
    }
}
