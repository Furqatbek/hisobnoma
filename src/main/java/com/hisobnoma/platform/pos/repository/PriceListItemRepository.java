package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.PriceListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceListItemRepository extends JpaRepository<PriceListItem, Long> {

    List<PriceListItem> findByPriceListId(Long priceListId);

    Page<PriceListItem> findByPriceListId(Long priceListId, Pageable pageable);

    Optional<PriceListItem> findByPriceListIdAndProductIdAndVariantIdIsNull(Long priceListId, Long productId);

    Optional<PriceListItem> findByPriceListIdAndProductIdAndVariantId(Long priceListId, Long productId, Long variantId);

    @Query("SELECT pli FROM PriceListItem pli WHERE pli.priceList.id = :priceListId " +
           "AND pli.product.id = :productId " +
           "AND (pli.variantId IS NULL OR pli.variantId = :variantId) " +
           "AND pli.active = true " +
           "AND (pli.startDate IS NULL OR pli.startDate <= :date) " +
           "AND (pli.endDate IS NULL OR pli.endDate >= :date) " +
           "AND (pli.minQuantity IS NULL OR pli.minQuantity <= :quantity) " +
           "AND (pli.maxQuantity IS NULL OR pli.maxQuantity >= :quantity) " +
           "ORDER BY pli.variantId DESC NULLS LAST, pli.minQuantity DESC NULLS LAST")
    List<PriceListItem> findEffectiveItems(
            @Param("priceListId") Long priceListId,
            @Param("productId") Long productId,
            @Param("variantId") Long variantId,
            @Param("quantity") BigDecimal quantity,
            @Param("date") LocalDate date);

    @Query("SELECT pli FROM PriceListItem pli " +
           "JOIN pli.priceList pl " +
           "WHERE pl.tenantId = :tenantId " +
           "AND pli.product.id = :productId " +
           "AND (pli.variantId IS NULL OR pli.variantId = :variantId) " +
           "AND pl.active = true AND pli.active = true " +
           "AND (pl.startDate IS NULL OR pl.startDate <= :date) " +
           "AND (pl.endDate IS NULL OR pl.endDate >= :date) " +
           "AND (pli.startDate IS NULL OR pli.startDate <= :date) " +
           "AND (pli.endDate IS NULL OR pli.endDate >= :date) " +
           "ORDER BY pl.priority DESC, pli.variantId DESC NULLS LAST")
    List<PriceListItem> findAllEffectiveItemsForProduct(
            @Param("tenantId") Long tenantId,
            @Param("productId") Long productId,
            @Param("variantId") Long variantId,
            @Param("date") LocalDate date);

    void deleteByPriceListId(Long priceListId);

    @Query("SELECT COUNT(pli) FROM PriceListItem pli WHERE pli.priceList.id = :priceListId")
    long countByPriceListId(@Param("priceListId") Long priceListId);
}
