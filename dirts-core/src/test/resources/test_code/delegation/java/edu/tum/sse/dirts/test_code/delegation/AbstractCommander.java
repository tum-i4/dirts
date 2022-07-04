package edu.tum.sse.dirts.test_code.delegation;

public abstract class AbstractCommander {

    protected final String name;

    public AbstractCommander(String name) {
        this.name = name;
    }

    public void abstractOrders() {

    }
}
