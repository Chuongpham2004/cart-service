package com.tikiclone.cartservice.dto;

import com.tikiclone.cartservice.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long userId, List<Item> items, BigDecimal total) {

    public record Item(Long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {

        public static Item from(CartItem item) {
            return new Item(item.getProductId(), item.getProductName(), item.getQuantity(),
                    item.getUnitPrice(), item.getSubtotal());
        }
    }
}
