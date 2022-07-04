package edu.tum.sse.dirts.test_code.finder;

import java.util.stream.Stream;

public interface SimpleInterface {



    double packagePrivatePrimitiveAttribute = 0;
    public char publicPrimitiveAttribute = 0;
    
    Boolean packagePrivateReferenceAttribute = null;
    public Stream<Integer> publicReferenceAttribute = null;

}
