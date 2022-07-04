package edu.tum.sse.dirts.test_code.finder;

import java.util.stream.Stream;

public @interface SimpleAnnotation {
    
    double packagePrivatePrimitiveAttribute = 0;
    public char publicPrimitiveAttribute = 0;
    
    Boolean packagePrivateReferenceAttribute = null;
    public Stream<Integer> publicReferenceAttribute = null;
    
    static double packagePrivateStaticPrimitiveAttribute = 0;
    public static char publicStaticPrimitiveAttribute = 0;
    
    static Boolean packagePrivateStaticReferenceAttribute = null;
    public static Stream<Integer> publicStaticReferenceAttribute = null;
}
