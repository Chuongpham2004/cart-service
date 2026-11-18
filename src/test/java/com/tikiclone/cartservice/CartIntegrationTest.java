package com.tikiclone.cartservice;

import com.tikiclone.cartservice.dto.AddItemRequest;
import com.tikiclone.cartservice.dto.CartResponse;
import com.tikiclone.cartservice.repository.CartItemRepository;
import com.tikiclone.cartservice.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration Test chạy với PostgreSQL THẬT (service postgres:14-alpine trên GitLab CI).
 * Không dùng database giả lập (H2) để kiểm tra đúng logic truy vấn trên PostgreSQL.
 */
@SpringBootTest
class CartIntegrationTest {

    private static final Long USER_ID = 1001L;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldConnectToRealPostgresDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
    }

    @Test
    void shouldPersistItemToDatabase() {
        cartService.addItem(USER_ID, new AddItemRequest(1L, "Sách Clean Code", 2, new BigDecimal("150000")));

        assertThat(repository.findByUserIdAndProductId(USER_ID, 1L))
                .hasValueSatisfying(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getQuantity()).isEqualTo(2);
                });
    }

    @Test
    void shouldMergeQuantityWhenAddingSameProduct() {
        cartService.addItem(USER_ID, new AddItemRequest(1L, "Sách Clean Code", 2, new BigDecimal("150000")));
        CartResponse cart = cartService.addItem(USER_ID, new AddItemRequest(1L, "Sách Clean Code", 3, new BigDecimal("150000")));

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void shouldCalculateTotalWithDatabaseQuery() {
        cartService.addItem(USER_ID, new AddItemRequest(1L, "Sách Clean Code", 2, new BigDecimal("150000")));
        cartService.addItem(USER_ID, new AddItemRequest(2L, "Tai nghe Bluetooth", 1, new BigDecimal("499000.50")));

        assertThat(repository.calculateTotal(USER_ID)).isEqualByComparingTo("799000.50");
        assertThat(cartService.getCart(USER_ID).total()).isEqualByComparingTo("799000.50");
    }

    @Test
    void shouldReturnZeroTotalForEmptyCart() {
        assertThat(cartService.getCart(9999L).total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldIsolateCartsBetweenUsers() {
        cartService.addItem(USER_ID, new AddItemRequest(1L, "Sách Clean Code", 1, new BigDecimal("150000")));
        cartService.addItem(2002L, new AddItemRequest(2L, "Tai nghe Bluetooth", 1, new BigDecimal("499000")));

        assertThat(cartService.getCart(USER_ID).items())
                .extracting(CartResponse.Item::productId)
                .containsExactly(1L);
    }

    @Test
    void shouldRemoveItemAndClearCart() {
        cartService.addItem(USER_ID, new AddItemRequest(1L, "Sách Clean Code", 1, new BigDecimal("150000")));
        cartService.addItem(USER_ID, new AddItemRequest(2L, "Tai nghe Bluetooth", 1, new BigDecimal("499000")));

        assertThat(cartService.removeItem(USER_ID, 1L)).isTrue();
        assertThat(cartService.removeItem(USER_ID, 1L)).isFalse();
        assertThat(cartService.getCart(USER_ID).items()).hasSize(1);

        cartService.clearCart(USER_ID);
        assertThat(cartService.getCart(USER_ID).items()).isEmpty();
    }

    @Test
    void shouldEnforceUniqueConstraintOnUserAndProduct() {
        repository.saveAndFlush(new com.tikiclone.cartservice.entity.CartItem(
                USER_ID, 1L, "Sách Clean Code", 1, new BigDecimal("150000")));

        assertThatThrownBy(() -> repository.saveAndFlush(new com.tikiclone.cartservice.entity.CartItem(
                USER_ID, 1L, "Sách Clean Code", 1, new BigDecimal("150000"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
