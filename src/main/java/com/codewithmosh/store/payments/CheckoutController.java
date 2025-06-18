package com.codewithmosh.store.payments;

import com.codewithmosh.store.dtos.ErrorDto;
import com.codewithmosh.store.entities.Order;
import com.codewithmosh.store.entities.OrderItem;
import com.codewithmosh.store.entities.PaymentStatus;
import com.codewithmosh.store.exceptions.CartEmptyException;
import com.codewithmosh.store.exceptions.CartNotFoundException;
import com.codewithmosh.store.repositories.OrderRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final OrderRepository orderRepository;


    @PostMapping
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest checkoutRequest) {
        return checkoutService.checkout(checkoutRequest);
    }

    @PostMapping("/webhook")
    public void handleWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload) { //payload is what data that stripe send back
         checkoutService.handleWebhookEvent(new WebhookRequest(headers, payload));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorDto> handelPaymentException() {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDto("Error creating a checkout session."));
    }
    @ExceptionHandler({CartNotFoundException.class, CartEmptyException.class})
    public ResponseEntity<ErrorDto> handelException(Exception ex) {
        return ResponseEntity.badRequest().body(
                new ErrorDto(ex.getMessage())
        );
    }

    public static interface PaymentGateway {
        CheckoutSession createCheckoutSession(Order order);
        Optional<PaymentResult> parseWebhookRequest(WebhookRequest request);
    }

    @Service
    public static class StripePaymentGateway implements PaymentGateway {
        @Value("${websiteUrl}")
        private String websiteUrl;

        @Value("${stripe.webhookSecretKey}")
        private String webhookSecretKey;

        @Override
        public CheckoutSession createCheckoutSession(Order order) {
           try{
               //create a checkout session
               var builder = SessionCreateParams.builder()
                       .setMode(SessionCreateParams.Mode.PAYMENT)
                       .setSuccessUrl(websiteUrl +"/checkout-success?orderId=" + order.getId())
                       .setCancelUrl(websiteUrl + "/checkout-cancel")
                       .putMetadata("order_id", order.getId().toString());

               order.getItems().forEach(item -> {
                   var lineItem = createLineItem(item);
                   builder.addLineItem(lineItem);
               });

               var session = Session.create(builder.build());
               return new CheckoutSession(session.getUrl());
           }
           catch (StripeException ex){
               System.out.println(ex.getMessage());
               throw new PaymentException();
           }
        }

        @Override
        public Optional<PaymentResult> parseWebhookRequest(WebhookRequest request) {
            try {
                var payload = request.getPayload();
                var signature = request.getHeaders().get("stripe-signature");
                var event = Webhook.constructEvent(payload, signature,webhookSecretKey);


                return switch (event.getType()) {
                    case "payment_intent.succeeded" ->
                        //update order status(PAID)
                        Optional.of(new PaymentResult(extractOrderId(event), PaymentStatus.PAID));
                    case "payment_intent.payment_failed" ->
                        //update order status(FAILED)
                        Optional.of(new PaymentResult(extractOrderId(event),PaymentStatus.FAILED));
                    default -> Optional.empty();
                };
            } catch (SignatureVerificationException e) {
                throw new PaymentException("Invalid Signature");
            }
        }

        private Long extractOrderId(Event event) {
            var stripeObject = event.getDataObjectDeserializer().getObject().orElseThrow(
                    () -> new PaymentException("Could not deserialize Stripe event. Check the SDK and API version."));
            var paymentIntent = (PaymentIntent) stripeObject;
            return Long.valueOf(paymentIntent.getMetadata().get("order_id"));

        }

        private SessionCreateParams.LineItem createLineItem(OrderItem item) {
            return SessionCreateParams.LineItem.builder()
                    .setQuantity(Long.valueOf(item.getQuantity()))
                    .setPriceData(createPriceData(item)).build();
        }

        private SessionCreateParams.LineItem.PriceData createPriceData(OrderItem item) {
            return SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency("usd")
                    .setUnitAmountDecimal(item.getUnitPrice().multiply(BigDecimal.valueOf(100)))
                    .setProductData(createProductData(item))
                    .build();
        }

        private SessionCreateParams.LineItem.PriceData.ProductData createProductData(OrderItem item) {
            return SessionCreateParams.LineItem.PriceData.ProductData.builder()
                    .setName(item.getProduct().getName())
                    .build();
        }
    }
}
