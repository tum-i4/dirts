package edu.tum.sse.dirts.test_code.finder;

import java.util.stream.Stream;

public enum SimpleEnum {
    ONE(1, "One"), TWO(2, "Two"), THREE(3, "Three"), FOUR(4, "Four");

    SimpleEnum(int number, String name) {
    }

    private int privatePrimitiveAttribute;
    protected float protectedPrimitiveAttribute;
    double packagePrivatePrimitiveAttribute;
    public char publicPrimitiveAttribute;

    private String privateReferenceAttribute;
    protected Float protectedReferenceAttribute;
    Boolean packagePrivateReferenceAttribute;
    public Stream<Integer> publicReferenceAttribute;
}
