package com.fptaptech.catalog.order;

import java.util.Map;
import java.util.Optional;

import static com.fptaptech.catalog.order.ActorRole.ADMIN;
import static com.fptaptech.catalog.order.ActorRole.CUSTOMER;
import static com.fptaptech.catalog.order.ActorRole.PARTNER;
import static com.fptaptech.catalog.order.OrderAction.ACCEPT_RETURN;
import static com.fptaptech.catalog.order.OrderAction.CANCEL;
import static com.fptaptech.catalog.order.OrderAction.CONFIRM;
import static com.fptaptech.catalog.order.OrderAction.DELIVER;
import static com.fptaptech.catalog.order.OrderAction.FAIL_DELIVERY;
import static com.fptaptech.catalog.order.OrderAction.REQUEST_RETURN;
import static com.fptaptech.catalog.order.OrderAction.SHIP;
import static com.fptaptech.catalog.order.OrderStatus.CANCELLED;
import static com.fptaptech.catalog.order.OrderStatus.CONFIRMED;
import static com.fptaptech.catalog.order.OrderStatus.DELIVERED;
import static com.fptaptech.catalog.order.OrderStatus.PENDING;
import static com.fptaptech.catalog.order.OrderStatus.RETURNED;
import static com.fptaptech.catalog.order.OrderStatus.RETURN_REQUESTED;
import static com.fptaptech.catalog.order.OrderStatus.SHIPPING;

public final class OrderStateMachine {

    public record Edge(OrderStatus from, OrderAction action, ActorRole role) {
    }

    private static final Map<Edge, OrderStatus> EDGES = Map.ofEntries(
            Map.entry(new Edge(PENDING, CONFIRM, ADMIN), CONFIRMED),
            Map.entry(new Edge(PENDING, CANCEL, CUSTOMER), CANCELLED),
            Map.entry(new Edge(PENDING, CANCEL, ADMIN), CANCELLED),
            Map.entry(new Edge(CONFIRMED, SHIP, ADMIN), SHIPPING),
            Map.entry(new Edge(CONFIRMED, SHIP, PARTNER), SHIPPING),
            Map.entry(new Edge(CONFIRMED, CANCEL, ADMIN), CANCELLED),
            Map.entry(new Edge(SHIPPING, DELIVER, ADMIN), DELIVERED),
            Map.entry(new Edge(SHIPPING, DELIVER, PARTNER), DELIVERED),
            Map.entry(new Edge(SHIPPING, FAIL_DELIVERY, ADMIN), CONFIRMED),
            Map.entry(new Edge(SHIPPING, FAIL_DELIVERY, PARTNER), CONFIRMED),
            Map.entry(new Edge(DELIVERED, REQUEST_RETURN, CUSTOMER), RETURN_REQUESTED),
            Map.entry(new Edge(DELIVERED, REQUEST_RETURN, ADMIN), RETURN_REQUESTED),
            Map.entry(new Edge(RETURN_REQUESTED, ACCEPT_RETURN, ADMIN), RETURNED)
    );

    public Optional<OrderStatus> next(OrderStatus from, OrderAction action, ActorRole role) {
        return Optional.ofNullable(EDGES.get(new Edge(from, action, role)));
    }

    public boolean roleSupports(ActorRole role, OrderAction action) {
        return EDGES.keySet().stream().anyMatch(edge -> edge.role() == role && edge.action() == action);
    }

    public static Map<Edge, OrderStatus> edges() {
        return EDGES;
    }
}
