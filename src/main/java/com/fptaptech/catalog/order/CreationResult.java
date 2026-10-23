package com.fptaptech.catalog.order;

public record CreationResult(OrderCreatedResponse body, String location, boolean replayed) {
}
