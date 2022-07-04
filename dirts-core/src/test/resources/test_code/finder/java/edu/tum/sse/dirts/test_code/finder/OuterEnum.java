package edu.tum.sse.dirts.test_code.finder;

public enum OuterEnum {
    ONE, TWO, THREE, FOUR;

    private class PrivateInnerClass {
    }

    public class PublicInnerClass {
    }

    private static class PrivateStaticInnerClass {
    }

    public static class PublicStaticInnerClass {
    }

    private interface PrivateInnerInterface {
    }

    public interface PublicInnerInterface {
    }

    private enum PrivateInnerEnum {

    }

    public enum PublicInnerEnum {

    }

    public @interface PublicInnerAnnotation {

    }

    private @interface PrivateInnerAnnotation {

    }
}
