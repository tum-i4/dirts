package edu.tum.sse.dirts.test_code.finder;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class SimpleClass extends AbstractClass {

    public SimpleClass() {
    }

    //private int commentedOutAttribute;

    private int a, b, c;

    private int privatePrimitiveAttribute;
    protected float protectedPrimitiveAttribute;
    double packagePrivatePrimitiveAttribute;
    public char publicPrimitiveAttribute;

    private String privateReferenceAttribute;
    protected Float protectedReferenceAttribute;
    Boolean packagePrivateReferenceAttribute;
    public Stream<Integer> publicReferenceAttribute;

    private static int privateStaticPrimitiveAttribute;
    protected static float protectedStaticPrimitiveAttribute;
    static double packagePrivateStaticPrimitiveAttribute;
    public static char publicStaticPrimitiveAttribute;

    private static String privateStaticReferenceAttribute;
    protected static Float protectedStaticReferenceAttribute;
    static Boolean packagePrivateStaticReferenceAttribute;
    public static Stream<Integer> publicStaticReferenceAttribute;


    private void privateVoidMethod(int arg) {
    }

    protected void protectedVoidMethod() {
    }

    void packagePrivateVoidMethod(String arg) {
    }

    public void publicVoidMethod(String arg) {
    }


    private int privatePrimitiveMethod(int arg) {
        return 0;
    }

    protected long protectedPrimitiveMethod() {

        return 0;
    }

    float packagePrivatePrimitiveMethod(String arg) {
        return 0;
    }

    public char publicPrimitiveMethod(String arg) {
        return 0;
    }

    private String privateReferenceMethod(int arg) {
        return null;
    }

    protected Stream<Integer> protectedReferenceMethod() {
        return null;
    }

    Set<Boolean> packagePrivateReferenceMethod(String arg) {
        return null;
    }

    public List<Character> publicReferenceMethod(String arg) {
        return null;
    }
}
