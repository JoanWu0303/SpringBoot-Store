package com.codewithmosh.store.services;

import com.codewithmosh.store.dtos.CheckoutRequest;
import com.codewithmosh.store.dtos.CheckoutResponse;
import com.codewithmosh.store.dtos.ErrorDto;
import com.codewithmosh.store.entities.Order;
import com.codewithmosh.store.exceptions.CartEmptyException;
import com.codewithmosh.store.exceptions.CartNotFoundException;
import com.codewithmosh.store.repositories.CartRepository;
import com.codewithmosh.store.repositories.OrderRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final AuthService authService;
    private final OrderRepository orderRepository;
    private final CartService cartService;

    @Value("${websiteUrl}")
    private String websiteUrl;

    public CheckoutResponse checkout(CheckoutRequest checkoutRequest) throws StripeException {
        //check if the cart exist
        var cart = cartRepository.getCartWithItems(checkoutRequest.getCartId()).orElse(null);
        if(cart == null) {
            throw new CartNotFoundException();
        }

        //check if the cart is empty with no items
        if(cart.isEmpty()) {
           throw new CartEmptyException();
        }

        //create order and orderItem object inside the Order class
        var order = Order.fromCart(cart, authService.getCurrentUser());
        orderRepository.save(order);

        //create a checkout session
        var builder = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(websiteUrl +"/checkout-success?orderId=" + order.getId())
                        .setCancelUrl(websiteUrl + "/checkout-cancel");

        order.getItems().forEach(item -> {
           var lineItem = SessionCreateParams.LineItem.builder()
                    .setQuantity(Long.valueOf(item.getQuantity()))
                    .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmountDecimal(item.getUnitPrice())
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName(item.getProduct().getName())
                                                .build()
                                        ).build()
                    ).build();
           builder.addLineItem(lineItem);
        });

        var session = Session.create(builder.build());

        cartService.clearCart(cart.getId());

        return new CheckoutResponse(order.getId(), session.getUrl()) ;
    }
}
