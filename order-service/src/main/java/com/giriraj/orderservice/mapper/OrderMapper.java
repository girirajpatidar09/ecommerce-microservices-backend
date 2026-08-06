package com.giriraj.orderservice.mapper;

import com.giriraj.orderservice.dto.OrderItemResponseDTO;
import com.giriraj.orderservice.dto.OrderResponseDTO;
import com.giriraj.orderservice.entity.Order;
import com.giriraj.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(
            target = "totalPrice",
            expression = """
                    java(
                        orderItem.getUnitPrice().multiply(
                            java.math.BigDecimal.valueOf(
                                orderItem.getQuantity()
                            )
                        )
                    )
                    """
    )
    OrderItemResponseDTO toItemResponse(
            OrderItem orderItem
    );

    OrderResponseDTO toResponse(Order order);
}