package com.fptaptech.catalog.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByCustomerIdAndIdempotencyKey(Long customerId, String idempotencyKey);
}
