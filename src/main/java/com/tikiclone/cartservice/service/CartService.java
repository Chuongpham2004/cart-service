package com.tikiclone.cartservice.service;

import com.tikiclone.cartservice.dto.AddItemRequest;
import com.tikiclone.cartservice.dto.CartResponse;
import com.tikiclone.cartservice.entity.CartItem;
import com.tikiclone.cartservice.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartItemRepository repository;

    public CartService(CartItemRepository repository) {
        this.repository = repository;
    }

    /**
     * Thêm sản phẩm vào giỏ. Nếu sản phẩm đã có trong giỏ thì cộng dồn số lượng.
     */
    @Transactional
    public CartResponse addItem(Long userId, AddItemRequest request) {
        CartItem item = repository.findByUserIdAndProductId(userId, request.productId())
                .map(existing -> {
                    existing.increaseQuantity(request.quantity());
                    return existing;
                })
                .orElseGet(() -> new CartItem(userId, request.productId(), request.productName(),
                        request.quantity(), request.unitPrice()));
        repository.save(item);
        return getCart(userId);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<CartResponse.Item> items = repository.findByUserIdOrderByIdAsc(userId).stream()
                .map(CartResponse.Item::from)
                .toList();
        return new CartResponse(userId, items, repository.calculateTotal(userId));
    }

    @Transactional
    public boolean removeItem(Long userId, Long productId) {
        return repository.deleteByUserIdAndProductId(userId, productId) > 0;
    }

    @Transactional
    public void clearCart(Long userId) {
        repository.deleteByUserId(userId);
    }
}
