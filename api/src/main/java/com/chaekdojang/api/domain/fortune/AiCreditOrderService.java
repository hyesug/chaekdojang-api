package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiCreditOrderService {
    private final JdbcTemplate jdbc;
    private final AiCreditService credits;
    private final WebClient webClient;
    private static final java.time.Duration PORTONE_TIMEOUT = java.time.Duration.ofSeconds(10);

    @Value("${portone.api-secret:}")
    private String apiSecret;

    // 테스트에서 가짜 PortOne 서버를 붙일 수 있게만 열어 둔다. 운영은 기본값을 쓴다
    @Value("${portone.api-base-url:https://api.portone.io}")
    private String apiBaseUrl;

    @Value("${app.ai-credit.sales-enabled:false}")
    private boolean salesEnabled;

    public List<Product> products() {
        return java.util.Arrays.stream(AiCreditProduct.values()).map(p -> new Product(p.name(), p.displayName(), p.credits(), p.price(), p.validDays())).toList();
    }

    @Transactional
    public Order create(Long userId, AiCreditProduct product) {
        if (!salesEnabled) throw new CustomException(ErrorCode.PAYMENT_NOT_CONFIGURED);
        if (product == null) throw new CustomException(ErrorCode.INVALID_REQUEST);
        UUID id = UUID.randomUUID();
        String paymentId = "ai-credit-" + id;
        jdbc.update("INSERT INTO ai_credit_orders(id,user_id,product,amount,credits,payment_id,status) VALUES (?,?,?,?,?,?, 'PENDING')", id, userId, product.name(), product.price(), product.credits(), paymentId);
        return new Order(id, paymentId, product.displayName(), product.price(), product.credits(), product.validDays(), "PENDING", null);
    }

    @Transactional
    public Order complete(Long userId, UUID orderId) {
        OrderRow order = jdbc.query("SELECT id,user_id,product,amount,credits,payment_id,status,paid_at FROM ai_credit_orders WHERE id=? FOR UPDATE", (rs, n) -> new OrderRow(UUID.fromString(rs.getString(1)), rs.getLong(2), AiCreditProduct.valueOf(rs.getString(3)), rs.getInt(4), rs.getInt(5), rs.getString(6), rs.getString(7), rs.getTimestamp(8) == null ? null : rs.getTimestamp(8).toLocalDateTime()), orderId).stream().findFirst().orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (order.userId() != userId) throw new CustomException(ErrorCode.FORBIDDEN);
        if ("PAID".equals(order.status())) return order.view();
        verifyPaid(order);
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("UPDATE ai_credit_orders SET status='PAID', paid_at=? WHERE id=?", Timestamp.valueOf(now), orderId);
        credits.grantPurchase(userId, orderId, order.product());
        return new Order(order.id(), order.paymentId(), order.product().displayName(), order.amount(), order.credits(), order.product().validDays(), "PAID", now);
    }

    private void verifyPaid(OrderRow order) {
        if (apiSecret.isBlank()) throw new CustomException(ErrorCode.PAYMENT_NOT_CONFIGURED);
        try {
            // 주문 행을 잠근 채 부르므로 응답을 무한정 기다리면 DB 연결이 묶인다
            Map<?, ?> login = webClient.post().uri(apiBaseUrl + "/login/api-secret").bodyValue(Map.of("apiSecret", apiSecret)).retrieve().bodyToMono(Map.class).block(PORTONE_TIMEOUT);
            Object accessToken = login == null ? null : login.get("accessToken");
            if (!(accessToken instanceof String token) || token.isBlank()) throw new IllegalStateException("PortOne access token missing");
            Map<?, ?> payment = webClient.get().uri(apiBaseUrl + "/payments/{paymentId}", order.paymentId()).headers(h -> h.setBearerAuth(token)).retrieve().bodyToMono(Map.class).block(PORTONE_TIMEOUT);
            Object amount = payment == null ? null : payment.get("amount");
            Object status = payment == null ? null : payment.get("status");
            Object total = amount instanceof Map<?, ?> values ? values.get("total") : null;
            Object currency = payment == null ? null : payment.get("currency");
            if (!"PAID".equals(status) || !"KRW".equals(currency) || !(total instanceof Number paid) || paid.longValue() != order.amount()) throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (CustomException e) { throw e; }
        catch (Exception e) { throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED); }
    }

    private record OrderRow(UUID id, long userId, AiCreditProduct product, int amount, int credits, String paymentId, String status, LocalDateTime paidAt) {
        Order view() { return new Order(id, paymentId, product.displayName(), amount, credits, product.validDays(), status, paidAt); }
    }
    public record Product(String code, String name, int credits, int price, int validDays) {}
    public record Order(UUID orderId, String paymentId, String productName, int amount, int credits, int validDays, String status, LocalDateTime paidAt) {}
}
