package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.entity.PriceList;
import com.hisobnoma.platform.pos.entity.PriceListItem;
import com.hisobnoma.platform.pos.enums.PriceListType;
import com.hisobnoma.platform.pos.mapper.PriceListItemMapper;
import com.hisobnoma.platform.pos.mapper.PriceListMapper;
import com.hisobnoma.platform.pos.repository.CustomerPriceListRepository;
import com.hisobnoma.platform.pos.repository.PriceListItemRepository;
import com.hisobnoma.platform.pos.repository.PriceListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceListServiceTest {

    @Mock
    private PriceListRepository priceListRepository;
    @Mock
    private PriceListItemRepository priceListItemRepository;
    @Mock
    private CustomerPriceListRepository customerPriceListRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private PriceListMapper priceListMapper;
    @Mock
    private PriceListItemMapper priceListItemMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private PriceListService priceListService;

    private PriceList priceList;
    private Product product;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        priceList = PriceList.builder()
                .id(1L)
                .code("STANDARD")
                .name("Standard Price List")
                .type(PriceListType.STANDARD)
                .priority(0)
                .active(true)
                .tenantId(TENANT_ID)
                .items(new ArrayList<>())
                .customerAssignments(new ArrayList<>())
                .build();

        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .sku("PROD-001")
                .sellingPrice(new BigDecimal("50000"))
                .build();
    }

    // ==================== findAllPriceLists ====================

    @Test
    void getPriceLists_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PriceList> page = new PageImpl<>(List.of(priceList), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(priceListMapper.toDto(any(PriceList.class))).thenReturn(PriceListDto.builder().id(1L).code("STANDARD").build());

        // When
        Page<PriceListDto> result = priceListService.findAllPriceLists(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== findActivePriceLists ====================

    @Test
    void getActivePriceLists_returnsOnlyActive() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findEffectivePriceLists(eq(TENANT_ID), any())).thenReturn(List.of(priceList));
        when(priceListMapper.toDtoList(any())).thenReturn(
                List.of(PriceListDto.builder().id(1L).active(true).build()));

        // When
        List<PriceListDto> result = priceListService.findActivePriceLists();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== findPriceListById ====================

    @Test
    void getPriceList_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(priceList));
        when(priceListMapper.toDtoWithItems(priceList)).thenReturn(
                PriceListDto.builder().id(1L).code("STANDARD").build());

        // When
        PriceListDto result = priceListService.findPriceListById(1L);

        // Then
        assertNotNull(result);
        assertEquals("STANDARD", result.getCode());
    }

    @Test
    void getPriceList_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> priceListService.findPriceListById(999L));
    }

    // ==================== createPriceList ====================

    @Test
    void createPriceList_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByCodeAndTenantId("NEW-PL", TENANT_ID)).thenReturn(Optional.empty());
        when(priceListMapper.toEntity(any())).thenReturn(PriceList.builder().code("NEW-PL").name("New").build());
        when(priceListRepository.save(any())).thenAnswer(inv -> {
            PriceList saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(priceListMapper.toDto(any(PriceList.class))).thenReturn(
                PriceListDto.builder().id(2L).code("NEW-PL").build());

        CreatePriceListRequest request = CreatePriceListRequest.builder()
                .code("NEW-PL")
                .name("New Price List")
                .build();

        // When
        PriceListDto result = priceListService.createPriceList(request);

        // Then
        assertNotNull(result);
        assertEquals("NEW-PL", result.getCode());
        verify(priceListRepository).save(any());
    }

    @Test
    void createPriceList_duplicateName_throwsException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByCodeAndTenantId("STANDARD", TENANT_ID)).thenReturn(Optional.of(priceList));

        CreatePriceListRequest request = CreatePriceListRequest.builder()
                .code("STANDARD")
                .name("Standard")
                .build();

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> priceListService.createPriceList(request));
    }

    // ==================== updatePriceList ====================

    @Test
    void updatePriceList_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(priceList));
        when(priceListRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceListMapper.toDto(any(PriceList.class))).thenReturn(
                PriceListDto.builder().id(1L).code("STANDARD").name("Updated").build());

        CreatePriceListRequest request = CreatePriceListRequest.builder()
                .code("STANDARD")
                .name("Updated Standard")
                .build();

        // When
        PriceListDto result = priceListService.updatePriceList(1L, request);

        // Then
        assertNotNull(result);
        verify(priceListRepository).save(any());
    }

    // ==================== addPriceListItem ====================

    @Test
    void addPriceListItem_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(priceList));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(priceListItemMapper.toEntity(any())).thenReturn(PriceListItem.builder()
                .price(new BigDecimal("45000")).build());
        when(priceListItemRepository.save(any())).thenAnswer(inv -> {
            PriceListItem saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(priceListItemMapper.toDto(any())).thenReturn(
                PriceListItemDto.builder().id(1L).productId(1L).price(new BigDecimal("45000")).build());

        CreatePriceListItemRequest request = CreatePriceListItemRequest.builder()
                .productId(1L)
                .price(new BigDecimal("45000"))
                .build();

        // When
        PriceListItemDto result = priceListService.addPriceListItem(1L, request);

        // Then
        assertNotNull(result);
        verify(priceListItemRepository).save(any());
    }

    @Test
    void addPriceListItem_productNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(priceList));
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        CreatePriceListItemRequest request = CreatePriceListItemRequest.builder()
                .productId(999L)
                .price(new BigDecimal("45000"))
                .build();

        // When/Then
        assertThrows(NotFoundException.class, () -> priceListService.addPriceListItem(1L, request));
    }

    // ==================== removePriceListItem ====================

    @Test
    void removePriceListItem_success() {
        // Given
        PriceListItem item = PriceListItem.builder()
                .id(10L)
                .priceList(priceList)
                .product(product)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(priceList));
        when(priceListItemRepository.findById(10L)).thenReturn(Optional.of(item));

        // When
        priceListService.removePriceListItem(1L, 10L);

        // Then
        verify(priceListItemRepository).delete(item);
    }

    @Test
    void removePriceListItem_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(priceList));
        when(priceListItemRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> priceListService.removePriceListItem(1L, 999L));
    }

    // ==================== activate / deactivate ====================

    @Test
    void activatePriceList_success() {
        // Given
        priceList.setActive(false);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(priceList));
        when(priceListRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceListMapper.toDto(any(PriceList.class))).thenReturn(
                PriceListDto.builder().id(1L).active(true).build());

        // When
        PriceListDto result = priceListService.activatePriceList(1L);

        // Then
        assertNotNull(result);
        assertTrue(priceList.isActive());
    }

    @Test
    void deactivatePriceList_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(priceListRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(priceList));
        when(priceListRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceListMapper.toDto(any(PriceList.class))).thenReturn(
                PriceListDto.builder().id(1L).active(false).build());

        // When
        PriceListDto result = priceListService.deactivatePriceList(1L);

        // Then
        assertNotNull(result);
        assertFalse(priceList.isActive());
    }
}
