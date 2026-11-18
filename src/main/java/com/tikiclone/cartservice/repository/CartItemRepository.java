package com.tikiclone.cartservice.repository;

import com.tikiclone.cartservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByIdAsc(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    @Query("select coalesce(sum(c.unitPrice * c.quantity), 0) from CartItem c where c.userId = :userId")
    BigDecimal calculateTotal(@Param("userId") Long userId);

    long deleteByUserIdAndProductId(Long userId, Long productId);

    long deleteByUserId(Long userId);
}
