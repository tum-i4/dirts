package edu.tum.sse.dirts.test_code.finder;

import java.util.stream.Stream;

public class OuterClass {

    private class PrivateInnerClass {
        private int privatePrimitiveAttribute;
        protected float protectedPrimitiveAttribute;
        double packagePrivatePrimitiveAttribute;
        public char publicPrimitiveAttribute;

        private String privateReferenceAttribute;
        protected Float protectedReferenceAttribute;
        Boolean packagePrivateReferenceAttribute;
        public Stream<Integer> publicReferenceAttribute;
    }

    public class PublicInnerClass {
    }

    private static class PrivateStaticInnerClass {
    }

    public static class PublicStaticInnerClass {
    }

    private interface PrivateInnerInterface {
        double packagePrivatePrimitiveAttribute = 0;
        public char publicPrimitiveAttribute = 0;

        Boolean packagePrivateReferenceAttribute = null;
        public Stream<Integer> publicReferenceAttribute = null;
    }

    public interface PublicInnerInterface {
    }

    private static enum PrivateInnerEnum {
        ;

        private int privatePrimitiveAttribute;
        protected float protectedPrimitiveAttribute;
        double packagePrivatePrimitiveAttribute;
        public char publicPrimitiveAttribute;

        private String privateReferenceAttribute;
        protected Float protectedReferenceAttribute;
        Boolean packagePrivateReferenceAttribute;
        public Stream<Integer> publicReferenceAttribute;
    }

    public enum PublicInnerEnum {
    }

    public @interface PublicInnerAnnotation {

    }

    private static @interface PrivateInnerAnnotation {
        double packagePrivatePrimitiveAttribute = 0;
        public char publicPrimitiveAttribute = 0;

        Boolean packagePrivateReferenceAttribute = null;
        public Stream<Integer> publicReferenceAttribute = null;

        static double packagePrivateStaticPrimitiveAttribute = 0;
        public static char publicStaticPrimitiveAttribute = 0;

        static Boolean packagePrivateStaticReferenceAttribute = null;
        public static Stream<Integer> publicStaticReferenceAttribute = null;
    }
}
