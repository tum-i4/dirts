package edu.tum.sse.dirts.util.tuples;

public class Triple<F,S,T> {

    private F first;

    private S second;

    private T third;

    public Triple() {
        first = null;
        second = null;
        third = null;
    }

    public Triple(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    public T getThird() {
        return third;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public void setSecond(S second) {
        this.second = second;
    }

    public void setThird(T third) {
        this.third = third;
    }
}
