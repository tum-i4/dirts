package edu.tum.sse.dirts.test_code.member_access;

public class Controller {

    private ObjectMemberAccess objectMemberAccess;

    private int multipleMemberA, multipleMemberB;

    public void control() {
        objectMemberAccess.member = 1;
        StaticMemberAccess.staticMember = "DifferentString";

        multipleMemberA = 0;

        System.out.println("Hello World");
    }
}
