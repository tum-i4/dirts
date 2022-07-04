package edu.tum.sse.dirts.test_code.delegation;

public class UselessDelegate {

    public int shouldNotBeUsed() {
        throw new IllegalStateException();
    }
}
