package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.*;
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
class StockRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StockRepository stockRepository;

    private Product product;
    private Product product2;
    private Location location;
    private Location location2;

    @BeforeEach
    void setUp() {
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .code("PCS").name("Pieces").active(true).isBaseUnit(true).build();
        uom = em.persistAndFlush(uom);

        product = Product.builder()
                .sku("STK-P1").name("Stock Product 1")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom)
                .reorderPoint(new BigDecimal("10"))
                .minStockLevel(new BigDecimal("5"))
                .build();
        product.setTenantId(1L);
        product = em.persistAndFlush(product);

        product2 = Product.builder()
                .sku("STK-P2").name("Stock Product 2")
                .sellingPrice(new BigDecimal("200.00"))
                .baseUom(uom)
                .reorderPoint(new BigDecimal("20"))
                .minStockLevel(new BigDecimal("10"))
                .build();
        product2.setTenantId(1L);
        product2 = em.persistAndFlush(product2);

        location = Location.builder()
                .code("WH-1").name("Warehouse 1")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location.setTenantId(1L);
        location = em.persistAndFlush(location);

        location2 = Location.builder()
                .code("WH-2").name("Warehouse 2")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location2.setTenantId(1L);
        location2 = em.persistAndFlush(location2);
    }

    private Stock persistStock(Product p, Location l, BigDecimal qty, BigDecimal reserved,
                               BigDecimal avgCost) {
        Stock s = Stock.builder()
                .product(p)
                .location(l)
                .quantityOnHand(qty)
                .quantityReserved(reserved)
                .averageCost(avgCost)
                .build();
        s.setTenantId(1L);
        return em.persistAndFlush(s);
    }

    @Test
    void findByProductIdAndLocationIdAndTenantId_existingStock_returnsStock() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));

        Optional<Stock> result = stockRepository.findByProductIdAndLocationIdAndTenantId(
                product.getId(), location.getId(), 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByProductIdAndTenantId_returnsAllLocationsForProduct() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product, location2, new BigDecimal("50"), BigDecimal.ZERO, new BigDecimal("50"));

        List<Stock> result = stockRepository.findByProductIdAndTenantId(product.getId(), 1L);

        assertEquals(2, result.size());
    }

    @Test
    void findByLocationIdAndTenantId_list_returnsAllProductsAtLocation() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product2, location, new BigDecimal("200"), BigDecimal.ZERO, new BigDecimal("75"));

        List<Stock> result = stockRepository.findByLocationIdAndTenantId(location.getId(), 1L);

        assertEquals(2, result.size());
    }

    @Test
    void findByTenantId_paged_returnsAllForTenant() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product2, location, new BigDecimal("200"), BigDecimal.ZERO, new BigDecimal("75"));

        Page<Stock> result = stockRepository.findByTenantId(1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findOutOfStock_returnsZeroQuantityStock() {
        persistStock(product, location, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product2, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));

        Page<Stock> result = stockRepository.findOutOfStock(1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getTotalQuantityByProduct_sumsAcrossLocations() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product, location2, new BigDecimal("50"), BigDecimal.ZERO, new BigDecimal("50"));

        BigDecimal total = stockRepository.getTotalQuantityByProduct(product.getId(), 1L);

        assertEquals(0, new BigDecimal("150").compareTo(total));
    }

    @Test
    void getAvailableQuantityByProduct_subtractsReserved() {
        persistStock(product, location, new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("50"));
        persistStock(product, location2, new BigDecimal("50"), new BigDecimal("10"), new BigDecimal("50"));

        BigDecimal available = stockRepository.getAvailableQuantityByProduct(product.getId(), 1L);

        assertEquals(0, new BigDecimal("110").compareTo(available));
    }

    @Test
    void getTotalStockValue_calculatesValueForTenant() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product2, location, new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("75"));

        BigDecimal totalValue = stockRepository.getTotalStockValue(1L);

        // 100*50 + 10*75 = 5000 + 750 = 5750
        assertEquals(0, new BigDecimal("5750.0000").compareTo(totalValue));
    }

    @Test
    void getStockValueByLocation_calculatesValueForLocation() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product, location2, new BigDecimal("200"), BigDecimal.ZERO, new BigDecimal("50"));

        BigDecimal value = stockRepository.getStockValueByLocation(location.getId(), 1L);

        assertEquals(0, new BigDecimal("5000.0000").compareTo(value));
    }

    @Test
    void countProductsWithStock_countsDistinctProducts() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product, location2, new BigDecimal("50"), BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product2, location, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50"));

        Long count = stockRepository.countProductsWithStock(1L);

        assertEquals(1, count);
    }

    @Test
    void findAllWithProductAndLocation_loadsFetchJoins() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));
        em.clear();

        List<Stock> result = stockRepository.findAllWithProductAndLocation(1L);

        assertFalse(result.isEmpty());
        assertNotNull(result.get(0).getProduct());
        assertNotNull(result.get(0).getLocation());
    }

    @Test
    void findByTenantId_list_returnsAllStockForTenant() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));

        List<Stock> result = stockRepository.findByTenantId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void calculateTotalInventoryValue_returnsCorrectValue() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));

        BigDecimal value = stockRepository.calculateTotalInventoryValue(1L);

        assertEquals(0, new BigDecimal("5000.0000").compareTo(value));
    }

    @Test
    void countOutOfStockProducts_countsCorrectly() {
        persistStock(product, location, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product2, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));

        Integer count = stockRepository.countOutOfStockProducts(1L);

        assertEquals(1, count);
    }

    @Test
    void searchStock_matchesProductNameAndSku() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));

        Page<Stock> byName = stockRepository.searchStock(1L, "Stock Product 1", PageRequest.of(0, 10));
        assertEquals(1, byName.getTotalElements());

        Page<Stock> bySku = stockRepository.searchStock(1L, "STK-P1", PageRequest.of(0, 10));
        assertEquals(1, bySku.getTotalElements());
    }

    @Test
    void getTotalQuantitiesByTenant_groupsByProduct() {
        persistStock(product, location, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product, location2, new BigDecimal("50"), BigDecimal.ZERO, new BigDecimal("50"));
        persistStock(product2, location, new BigDecimal("200"), BigDecimal.ZERO, new BigDecimal("75"));

        List<Object[]> result = stockRepository.getTotalQuantitiesByTenant(1L);

        assertEquals(2, result.size());
    }
}
