package edu.tum.sse.dirts.test_code.delegation;

import java.util.ArrayList;
import java.util.List;

public class Commander extends AbstractCommander {

    private final List<ObjectDelegate> objectDelegates = new ArrayList<>();

    // Delegation to constructor in member initialization
    private final MemberInitDelegate memberInitDelegate = new MemberInitDelegate(Order.head_right);

    // Delegation to constructor in static member initialization
    private static final StaticInitDelegate staticInitDelegate = new StaticInitDelegate(Order.attention);

    // no actual delegation, no methods called
    private UselessDelegate uselessDelegate1;

    public Commander(String name) {
        // Delegation to parent class
        super(name);
    }

    public Commander() {
        // Delegation to own class
        this("DefaultName");
        super.abstractOrders();
    }

    private void giveOrders(UselessDelegate uselessDelegate2) {

        // not resolvable
        General general = getGeneral();
        general.receiveOrders();

        // no actual delegation, no methods called
        UselessDelegate uselessDelegate3;

        for (ObjectDelegate objectDelegate : objectDelegates) {
            // Delegation to object method
            objectDelegate.acceptOrders(Order.attention);
        }

        // Delegation to static method
        StaticDelegate.acceptOrders(Order.head_left);

        // Delegation to static method with static import
        StaticImportDelegate.acceptOrders(Order.head_left);
    }

    private General getGeneral() {
        return null;
    }
}
