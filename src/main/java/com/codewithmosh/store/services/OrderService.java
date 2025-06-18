package com.codewithmosh.store.services;

import com.codewithmosh.store.auth.AuthService;
import com.codewithmosh.store.dtos.OrderDto;
import com.codewithmosh.store.exceptions.OrderNotFoundException;
import com.codewithmosh.store.mappers.OrderMapper;
import com.codewithmosh.store.repositories.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class OrderService {

    private final AuthService authService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public List<OrderDto> getAllOrders(){
        //get authenticated user
        var user = authService.getCurrentUser();

        //get the orders that created by this user
        var orders = orderRepository.getOrdersByCustomer(user);

        return orders.stream().map(orderMapper::toDto).toList();
    }

    public OrderDto getOrder(Long orderId) {
        //check if the order exist
        var order = orderRepository.getOrderWithItems(orderId).orElseThrow(OrderNotFoundException::new);

        //check if the order belongs to this user
        var user = authService.getCurrentUser();
        if(!order.isPlacedBy(user)) {
            throw new AccessDeniedException("You do not have permission to access this order");
        }

        //return order and 200
        return orderMapper.toDto(order);
    }
}
