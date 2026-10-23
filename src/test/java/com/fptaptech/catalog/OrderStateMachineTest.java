package com.fptaptech.catalog;

import com.fptaptech.catalog.order.ActorRole;
import com.fptaptech.catalog.order.OrderAction;
import com.fptaptech.catalog.order.OrderStateMachine;
import com.fptaptech.catalog.order.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStateMachineTest {

    private final OrderStateMachine machine = new OrderStateMachine();

    @Test
    void allowsExactlyTheDiagramEdges() {
        var edges = OrderStateMachine.edges();
        assertThat(edges).hasSize(13);
        assertThat(machine.next(OrderStatus.PENDING, OrderAction.CONFIRM, ActorRole.ADMIN)).contains(OrderStatus.CONFIRMED);
        assertThat(machine.next(OrderStatus.PENDING, OrderAction.CANCEL, ActorRole.CUSTOMER)).contains(OrderStatus.CANCELLED);
        assertThat(machine.next(OrderStatus.PENDING, OrderAction.CANCEL, ActorRole.ADMIN)).contains(OrderStatus.CANCELLED);
        assertThat(machine.next(OrderStatus.CONFIRMED, OrderAction.SHIP, ActorRole.ADMIN)).contains(OrderStatus.SHIPPING);
        assertThat(machine.next(OrderStatus.CONFIRMED, OrderAction.SHIP, ActorRole.PARTNER)).contains(OrderStatus.SHIPPING);
        assertThat(machine.next(OrderStatus.CONFIRMED, OrderAction.CANCEL, ActorRole.ADMIN)).contains(OrderStatus.CANCELLED);
        assertThat(machine.next(OrderStatus.SHIPPING, OrderAction.DELIVER, ActorRole.PARTNER)).contains(OrderStatus.DELIVERED);
        assertThat(machine.next(OrderStatus.SHIPPING, OrderAction.DELIVER, ActorRole.ADMIN)).contains(OrderStatus.DELIVERED);
        assertThat(machine.next(OrderStatus.SHIPPING, OrderAction.FAIL_DELIVERY, ActorRole.PARTNER)).contains(OrderStatus.CONFIRMED);
        assertThat(machine.next(OrderStatus.SHIPPING, OrderAction.FAIL_DELIVERY, ActorRole.ADMIN)).contains(OrderStatus.CONFIRMED);
        assertThat(machine.next(OrderStatus.DELIVERED, OrderAction.REQUEST_RETURN, ActorRole.CUSTOMER)).contains(OrderStatus.RETURN_REQUESTED);
        assertThat(machine.next(OrderStatus.DELIVERED, OrderAction.REQUEST_RETURN, ActorRole.ADMIN)).contains(OrderStatus.RETURN_REQUESTED);
        assertThat(machine.next(OrderStatus.RETURN_REQUESTED, OrderAction.ACCEPT_RETURN, ActorRole.ADMIN)).contains(OrderStatus.RETURNED);
    }

    @Test
    void rejectsEveryOtherCombination() {
        Set<OrderStateMachine.Edge> allowed = OrderStateMachine.edges().keySet();
        int rejected = 0;
        for (OrderStatus status : OrderStatus.values()) {
            for (OrderAction action : OrderAction.values()) {
                for (ActorRole role : ActorRole.values()) {
                    var edge = new OrderStateMachine.Edge(status, action, role);
                    if (allowed.contains(edge)) {
                        assertThat(machine.next(status, action, role)).isPresent();
                    } else {
                        assertThat(machine.next(status, action, role)).isEmpty();
                        rejected++;
                    }
                }
            }
        }
        int total = OrderStatus.values().length * OrderAction.values().length * ActorRole.values().length;
        assertThat(rejected).isEqualTo(total - allowed.size());
        assertThat(OrderAction.values()).hasSize(7);
        assertThat(OrderStatus.values()).hasSize(7);
        assertThat(OrderStateMachine.edges()).hasSize(13);
    }

    @Test
    void customerMayOnlyCancelOrRequestReturn() {
        assertThat(machine.roleSupports(ActorRole.CUSTOMER, OrderAction.CANCEL)).isTrue();
        assertThat(machine.roleSupports(ActorRole.CUSTOMER, OrderAction.REQUEST_RETURN)).isTrue();
        assertThat(machine.roleSupports(ActorRole.CUSTOMER, OrderAction.CONFIRM)).isFalse();
        assertThat(machine.roleSupports(ActorRole.CUSTOMER, OrderAction.SHIP)).isFalse();
        assertThat(machine.roleSupports(ActorRole.ADMIN, OrderAction.DELIVER)).isTrue();
        assertThat(machine.roleSupports(ActorRole.PARTNER, OrderAction.FAIL_DELIVERY)).isTrue();
        assertThat(machine.roleSupports(ActorRole.PARTNER, OrderAction.CANCEL)).isFalse();
    }
}
