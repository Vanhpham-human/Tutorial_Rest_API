package com.fptaptech.catalog.order;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    OrderItemResponse toItem(OrderItem item);

    List<OrderItemResponse> toItems(List<OrderItem> items);
}
