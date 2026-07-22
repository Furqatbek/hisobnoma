package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Brand;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProductRepository productRepository;

    private UnitOfMeasure uom;

    @BeforeEach
    void setUp() {
        uom = UnitOfMeasure.builder()
                .code("PCS")
                .name("Pieces")
                .active(true)
                .isBaseUnit(true)
                .build();
        uom = em.persistAndFlush(uom);
    }

    private Product persistProduct(String sku, String name, Long tenantId, boolean active,
                                   boolean sellable, boolean purchasable) {
        Product p = Product.builder()
                .sku(sku)
                .name(name)
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom)
                .active(active)
                .sellable(sellable)
                .purchasable(purchasable)
                .build();
        p.setTenantId(tenantId);
        return em.persistAndFlush(p);
    }

    private Product persistProduct(String sku, String name, Long tenantId) {
        return persistProduct(sku, name, tenantId, true, true, true);
    }

    @Test
    void findAllByTenantId_returnsMatchingAndNullTenant() {
        persistProduct("SKU-1", "Product 1", 1L);
        persistProduct("SKU-2", "Product 2", null);
        persistProduct("SKU-3", "Product 3", 2L);

        Page<Product> result = productRepository.findAllByTenantId(1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findAllActiveByTenantId_returnsOnlyActiveProducts() {
        persistProduct("SKU-A", "Active", 1L, true, true, true);
        persistProduct("SKU-I", "Inactive", 1L, false, true, true);

        Page<Product> result = productRepository.findAllActiveByTenantId(1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("SKU-A", result.getContent().get(0).getSku());
    }

    @Test
    void findByIdAndTenantId_existingProduct_returnsProduct() {
        Product p = persistProduct("SKU-1", "Product 1", 1L);

        Optional<Product> result = productRepository.findByIdAndTenantId(p.getId(), 1L);

        assertTrue(result.isPresent());
        assertEquals(p.getId(), result.get().getId());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        Product p = persistProduct("SKU-1", "Product 1", 1L);

        Optional<Product> result = productRepository.findByIdAndTenantId(p.getId(), 99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findBySkuAndTenantId_existingSku_returnsProduct() {
        persistProduct("FIND-SKU", "Findable", 1L);

        Optional<Product> result = productRepository.findBySkuAndTenantId("FIND-SKU", 1L);

        assertTrue(result.isPresent());
        assertEquals("FIND-SKU", result.get().getSku());
    }

    @Test
    void findByBarcodeAndTenantId_existingBarcode_returnsProduct() {
        Product p = Product.builder()
                .sku("BC-SKU")
                .name("Barcode Product")
                .barcode("1234567890")
                .sellingPrice(new BigDecimal("50.00"))
                .baseUom(uom)
                .build();
        p.setTenantId(1L);
        em.persistAndFlush(p);

        Optional<Product> result = productRepository.findByBarcodeAndTenantId("1234567890", 1L);

        assertTrue(result.isPresent());
        assertEquals("1234567890", result.get().getBarcode());
    }

    @Test
    void findBySku_existingSku_returnsProduct() {
        persistProduct("GLOBAL-SKU", "Global", null);

        Optional<Product> result = productRepository.findBySku("GLOBAL-SKU");

        assertTrue(result.isPresent());
    }

    @Test
    void findByBarcode_existingBarcode_returnsProduct() {
        Product p = Product.builder()
                .sku("BC-SKU2")
                .name("Barcode Product 2")
                .barcode("9876543210")
                .sellingPrice(new BigDecimal("50.00"))
                .baseUom(uom)
                .build();
        em.persistAndFlush(p);

        Optional<Product> result = productRepository.findByBarcode("9876543210");

        assertTrue(result.isPresent());
    }

    @Test
    void findByIdWithDetails_loadsAssociations() {
        Category cat = Category.builder().code("CAT1").name("Category 1").build();
        cat = em.persistAndFlush(cat);

        Brand brand = Brand.builder().code("BR1").name("Brand 1").build();
        brand = em.persistAndFlush(brand);

        Product p = Product.builder()
                .sku("DET-SKU")
                .name("Detailed Product")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom)
                .category(cat)
                .brand(brand)
                .build();
        p.setTenantId(1L);
        p = em.persistAndFlush(p);

        em.clear();

        Optional<Product> result = productRepository.findByIdWithDetails(p.getId(), 1L);

        assertTrue(result.isPresent());
        assertNotNull(result.get().getCategory());
        assertNotNull(result.get().getBrand());
        assertTrue(productRepository.findByIdWithDetails(p.getId(), 2L).isEmpty(),
                "Another tenant must not load this product by id");
        assertNotNull(result.get().getBaseUom());
    }

    @Test
    void findByCategoryIdAndTenantId_returnsProductsInCategory() {
        Category cat = Category.builder().code("CATX").name("Category X").build();
        cat.setTenantId(1L);
        cat = em.persistAndFlush(cat);

        Product p = Product.builder()
                .sku("CAT-SKU")
                .name("Categorized")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom)
                .category(cat)
                .build();
        p.setTenantId(1L);
        em.persistAndFlush(p);

        persistProduct("NO-CAT", "No Category", 1L);

        Page<Product> result = productRepository.findByCategoryIdAndTenantId(
                cat.getId(), 1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByBrandIdAndTenantId_returnsProductsOfBrand() {
        Brand brand = Brand.builder().code("BRX").name("Brand X").build();
        brand.setTenantId(1L);
        brand = em.persistAndFlush(brand);

        Product p = Product.builder()
                .sku("BR-SKU")
                .name("Branded")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom)
                .brand(brand)
                .build();
        p.setTenantId(1L);
        em.persistAndFlush(p);

        Page<Product> result = productRepository.findByBrandIdAndTenantId(
                brand.getId(), 1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchByTenantId_matchesNameSkuBarcodeDescription() {
        Product p = Product.builder()
                .sku("SRCH-SKU")
                .name("Searchable Widget")
                .description("A fantastic widget")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom)
                .build();
        p.setTenantId(1L);
        em.persistAndFlush(p);

        persistProduct("OTHER-SKU", "Other Product", 1L);

        Page<Product> byName = productRepository.searchByTenantId(1L, "Widget", PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());

        Page<Product> bySku = productRepository.searchByTenantId(1L, "SRCH", PageRequest.of(0, 10));
        assertEquals(1, bySku.getTotalElements());

        Page<Product> byDesc = productRepository.searchByTenantId(1L, "fantastic", PageRequest.of(0, 10));
        assertEquals(1, byDesc.getTotalElements());
    }

    @Test
    void existsBySkuAndTenantId_existingSku_returnsTrue() {
        persistProduct("EXISTS-SKU", "Exists", 1L);

        assertTrue(productRepository.existsBySkuAndTenantId("EXISTS-SKU", 1L));
        assertFalse(productRepository.existsBySkuAndTenantId("EXISTS-SKU", 99L));
    }

    @Test
    void existsBySkuAndTenantIdIsNull_globalProduct_returnsTrue() {
        persistProduct("GLOBAL-SKU2", "Global Product", null);

        assertTrue(productRepository.existsBySkuAndTenantIdIsNull("GLOBAL-SKU2"));
        assertFalse(productRepository.existsBySkuAndTenantIdIsNull("NONEXISTENT"));
    }

    @Test
    void findAllSellableByTenantId_returnsOnlySellable() {
        persistProduct("SELL-1", "Sellable", 1L, true, true, true);
        persistProduct("NOSELL-1", "Not Sellable", 1L, true, false, true);

        List<Product> result = productRepository.findAllSellableByTenantId(1L);

        assertEquals(1, result.size());
        assertEquals("SELL-1", result.get(0).getSku());
    }

    @Test
    void findAllPurchasableByTenantId_returnsOnlyPurchasable() {
        persistProduct("PURCH-1", "Purchasable", 1L, true, true, true);
        persistProduct("NOPURCH-1", "Not Purchasable", 1L, true, true, false);

        List<Product> result = productRepository.findAllPurchasableByTenantId(1L);

        assertEquals(1, result.size());
        assertEquals("PURCH-1", result.get(0).getSku());
    }

    @Test
    void countByTenantId_countsMatchingAndNullTenant() {
        persistProduct("C1", "Count 1", 1L);
        persistProduct("C2", "Count 2", null);
        persistProduct("C3", "Count 3", 2L);

        long count = productRepository.countByTenantId(1L);

        assertEquals(2, count);
    }

    @Test
    void countActiveByTenantId_countsOnlyActive() {
        persistProduct("A1", "Active", 1L, true, true, true);
        persistProduct("I1", "Inactive", 1L, false, true, true);

        long count = productRepository.countActiveByTenantId(1L);

        assertEquals(1, count);
    }

    @Test
    void searchActiveByTenantId_returnsOnlyActiveMatches() {
        Product active = Product.builder()
                .sku("ACT-SRCH")
                .name("Active Searchable")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom)
                .active(true)
                .build();
        active.setTenantId(1L);
        em.persistAndFlush(active);

        Product inactive = Product.builder()
                .sku("INACT-SRCH")
                .name("Inactive Searchable")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom)
                .active(false)
                .build();
        inactive.setTenantId(1L);
        em.persistAndFlush(inactive);

        Page<Product> result = productRepository.searchActiveByTenantId(1L, "Searchable", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("ACT-SRCH", result.getContent().get(0).getSku());
    }

    @Test
    void countByTenantIdAndActiveTrue_countsActiveForTenant() {
        persistProduct("T1", "Tenant Active", 1L, true, true, true);
        persistProduct("T2", "Tenant Inactive", 1L, false, true, true);

        Integer count = productRepository.countByTenantIdAndActiveTrue(1L);

        assertEquals(1, count);
    }

    @Test
    void findByTenantIdAndActiveTrue_returnsActiveForTenant() {
        persistProduct("TA1", "Active 1", 1L, true, true, true);
        persistProduct("TI1", "Inactive 1", 1L, false, true, true);

        List<Product> result = productRepository.findByTenantIdAndActiveTrue(1L);

        assertEquals(1, result.size());
    }

    @Test
    void searchByNameOrSkuOrBarcode_matchesMultipleFields() {
        Product p = Product.builder()
                .sku("MOB-SKU")
                .name("Mobile Widget")
                .barcode("MOB123")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom)
                .active(true)
                .build();
        p.setTenantId(1L);
        em.persistAndFlush(p);

        Page<Product> byName = productRepository.searchByNameOrSkuOrBarcode(1L, "Widget", PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());

        Page<Product> bySku = productRepository.searchByNameOrSkuOrBarcode(1L, "MOB-SKU", PageRequest.of(0, 10));
        assertEquals(1, bySku.getTotalElements());

        Page<Product> byBarcode = productRepository.searchByNameOrSkuOrBarcode(1L, "MOB123", PageRequest.of(0, 10));
        assertEquals(1, byBarcode.getTotalElements());
    }
}
