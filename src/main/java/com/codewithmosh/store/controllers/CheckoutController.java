package com.codewithmosh.store.controllers;

import com.codewithmosh.store.dtos.CheckoutRequest;
import com.codewithmosh.store.dtos.CheckoutResponse;
import com.codewithmosh.store.dtos.ErrorDto;
import com.codewithmosh.store.entities.Order;
import com.codewithmosh.store.entities.OrderItem;
import com.codewithmosh.store.entities.OrderStatus;
import com.codewithmosh.store.repositories.CartRepository;
import com.codewithmosh.store.repositories.OrderRepository;
import com.codewithmosh.store.services.AuthService;
import com.codewithmosh.store.services.CartService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final AuthService authService;
    private final CartService cartService;

    @PostMapping
    public ResponseEntity<?> checkout(@Valid @RequestBody CheckoutRequest checkoutRequest) {
        //check if the cart exist
        var cart = cartRepository.getCartWithItems(checkoutRequest.getCartId()).orElse(null);
        if(cart == null) {
            return ResponseEntity.badRequest().body(
                   new ErrorDto("Cart not found")
            );
        }
        //check if the cart is empty with no items
        if(cart.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(
                   new ErrorDto("Cart is empty")
            );
        }

        //save the order
        Order order = new Order();
        order.setTotalPrice(cart.getTotalPrice());
        order.setStatus(OrderStatus.PENDING);
        order.setCustomer(authService.getCurrentUser());

        //iterate the cartItem to orderItem
        cart.getItems().forEach(item -> {
            var orderItem = new OrderItem();
            orderItem.setQuantity(item.getQuantity());
            orderItem.setOrder(order);
            orderItem.setUnitPrice(item.getProduct().getPrice());
            orderItem.setProduct(item.getProduct());
            orderItem.setTotalPrice(item.getTotalPrice());
            order.getItems().add(orderItem);
        });

        orderRepository.save(order);
        cartService.clearCart(cart.getId());

        return ResponseEntity.ok(new CheckoutResponse(order.getId())) ;
    }
}
