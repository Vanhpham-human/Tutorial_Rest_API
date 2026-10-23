package com.fptaptech.catalog.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<Order> findByCode(String code);

    List<Order> findByStatusInAndDeliveredAtGreaterThanEqualAndDeliveredAtLessThan(
            Collection<OrderStatus> statuses, Instant from, Instant to);
}
