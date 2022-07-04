package edu.tum.sse.dirts.test_code.member_access;

public class Observer {

    private ObjectMemberAccess objectMemberAccess;

    public void observe() {
        if (objectMemberAccess.member == 0) {
            System.out.println("Aha!");
        }
        if (StaticMemberAccess.staticMember == null) {
            System.out.println("I see");
        }
        if (edu.tum.sse.dirts.test_code.member_access.StaticMemberAccess.staticMember2 == 0) {
            System.out.println("Oh!");
        }
    }
}
