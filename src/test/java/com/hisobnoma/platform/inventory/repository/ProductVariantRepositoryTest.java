package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class ProductVariantRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .code("PCS").name("Pieces").active(true).isBaseUnit(true).build();
        uom = em.persistAndFlush(uom);

        product = Product.builder()
                .sku("PARENT-SKU")
                .name("Parent Product")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom)
                .build();
        product.setTenantId(1L);
        product = em.persistAndFlush(product);
    }

    private ProductVariant persistVariant(String sku, String name, int sortOrder, boolean active) {
        ProductVariant v = ProductVariant.builder()
                .sku(sku)
                .name(name)
                .product(product)
                .sortOrder(sortOrder)
                .active(active)
                .build();
        v.setTenantId(1L);
        return em.persistAndFlush(v);
    }

    @Test
    void findByProductIdOrderBySortOrder_returnsSorted() {
        persistVariant("V-B", "Variant B", 2, true);
        persistVariant("V-A", "Variant A", 1, true);
        persistVariant("V-C", "Variant C", 3, true);

        List<ProductVariant> result = productVariantRepository.findByProductIdOrderBySortOrder(product.getId());

        assertEquals(3, result.size());
        assertEquals("V-A", result.get(0).getSku());
        assertEquals("V-B", result.get(1).getSku());
        assertEquals("V-C", result.get(2).getSku());
    }

    @Test
    void findBySkuAndTenantId_existingSku_returnsVariant() {
        persistVariant("FIND-V", "Findable Variant", 1, true);

        Optional<ProductVariant> result = productVariantRepository.findBySkuAndTenantId("FIND-V", 1L);

        assertTrue(result.isPresent());
        assertEquals("FIND-V", result.get().getSku());
    }

    @Test
    void findByBarcodeAndTenantId_existingBarcode_returnsVariant() {
        ProductVariant v = ProductVariant.builder()
                .sku("BC-V")
                .name("Barcode Variant")
                .barcode("V-BARCODE")
                .product(product)
                .build();
        v.setTenantId(1L);
        em.persistAndFlush(v);

        Optional<ProductVariant> result = productVariantRepository.findByBarcodeAndTenantId("V-BARCODE", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findBySku_existingSku_returnsVariant() {
        persistVariant("GLOBAL-V", "Global Variant", 1, true);

        Optional<ProductVariant> result = productVariantRepository.findBySku("GLOBAL-V");

        assertTrue(result.isPresent());
    }

    @Test
    void findByBarcode_existingBarcode_returnsVariant() {
        ProductVariant v = ProductVariant.builder()
                .sku("BC-V2")
                .name("Barcode Variant 2")
                .barcode("V-BARCODE2")
                .product(product)
                .build();
        v.setTenantId(1L);
        em.persistAndFlush(v);

        Optional<ProductVariant> result = productVariantRepository.findByBarcode("V-BARCODE2");

        assertTrue(result.isPresent());
    }

    @Test
    void findByIdWithProduct_loadsProduct() {
        ProductVariant v = persistVariant("WP-V", "With Product", 1, true);
        em.clear();

        Optional<ProductVariant> result = productVariantRepository.findByIdWithProduct(v.getId());

        assertTrue(result.isPresent());
        assertNotNull(result.get().getProduct());
        assertEquals("PARENT-SKU", result.get().getProduct().getSku());
    }

    @Test
    void findByProductIdAndActiveTrue_returnsOnlyActive() {
        persistVariant("ACTIVE-V", "Active", 1, true);
        persistVariant("INACTIVE-V", "Inactive", 2, false);

        List<ProductVariant> result = productVariantRepository.findByProductIdAndActiveTrue(product.getId());

        assertEquals(1, result.size());
        assertEquals("ACTIVE-V", result.get(0).getSku());
    }

    @Test
    void existsBySkuAndTenantId_existingSku_returnsTrue() {
        persistVariant("EXISTS-V", "Exists", 1, true);

        assertTrue(productVariantRepository.existsBySkuAndTenantId("EXISTS-V", 1L));
        assertFalse(productVariantRepository.existsBySkuAndTenantId("EXISTS-V", 99L));
    }

    @Test
    void countByProductId_countsVariantsForProduct() {
        persistVariant("CNT-V1", "Count 1", 1, true);
        persistVariant("CNT-V2", "Count 2", 2, true);

        long count = productVariantRepository.countByProductId(product.getId());

        assertEquals(2, count);
    }
}
