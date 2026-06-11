package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.entity.CustomerPriceList;
import com.hisobnoma.platform.pos.entity.PriceList;
import com.hisobnoma.platform.pos.entity.PriceListItem;
import com.hisobnoma.platform.pos.mapper.PriceListItemMapper;
import com.hisobnoma.platform.pos.mapper.PriceListMapper;
import com.hisobnoma.platform.pos.repository.CustomerPriceListRepository;
import com.hisobnoma.platform.pos.repository.PriceListItemRepository;
import com.hisobnoma.platform.pos.repository.PriceListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing Price Lists and Price List Items.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceListService {

    private final PriceListRepository priceListRepository;
    private final PriceListItemRepository priceListItemRepository;
    private final CustomerPriceListRepository customerPriceListRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final PriceListMapper priceListMapper;
    private final PriceListItemMapper priceListItemMapper;
    private final SecurityContextHelper securityContextHelper;

    // ==================== Price List CRUD ====================

    @Transactional(readOnly = true)
    public Page<PriceListDto> findAllPriceLists(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return priceListRepository.findByTenantId(tenantId, pageable)
                .map(priceListMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<PriceListDto> findActivePriceLists() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        LocalDate today = LocalDate.now();
        return priceListMapper.toDtoList(
                priceListRepository.findEffectivePriceLists(tenantId, today));
    }

    @Transactional(readOnly = true)
    public PriceListDto findPriceListById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        PriceList priceList = priceListRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", id));
        return priceListMapper.toDtoWithItems(priceList);
    }

    @Transactional(readOnly = true)
    public PriceListDto findPriceListByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        PriceList priceList = priceListRepository.findByCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "code", code));
        return priceListMapper.toDtoWithItems(priceList);
    }

    @Transactional
    public PriceListDto createPriceList(CreatePriceListRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check for duplicate code
        if (priceListRepository.findByCodeAndTenantId(request.getCode(), tenantId).isPresent()) {
            throw new IllegalArgumentException("Price list with code '" + request.getCode() + "' already exists");
        }

        PriceList priceList = priceListMapper.toEntity(request);
        priceList.setTenantId(tenantId);

        priceList = priceListRepository.save(priceList);
        log.info("Created price list: {} (ID: {})", priceList.getCode(), priceList.getId());

        return priceListMapper.toDto(priceList);
    }

    @Transactional
    public PriceListDto updatePriceList(Long id, CreatePriceListRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        PriceList priceList = priceListRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", id));

        // Check for duplicate code if changed
        if (!priceList.getCode().equals(request.getCode())) {
            if (priceListRepository.findByCodeAndTenantId(request.getCode(), tenantId).isPresent()) {
                throw new IllegalArgumentException("Price list with code '" + request.getCode() + "' already exists");
            }
        }

        priceListMapper.updateEntity(request, priceList);
        priceList = priceListRepository.save(priceList);
        log.info("Updated price list: {} (ID: {})", priceList.getCode(), priceList.getId());

        return priceListMapper.toDto(priceList);
    }

    @Transactional
    public void deletePriceList(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        PriceList priceList = priceListRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", id));

        // Delete associated items first
        priceListItemRepository.deleteAll(priceList.getItems());

        // Delete customer associations
        customerPriceListRepository.deleteByPriceListId(id);

        priceListRepository.delete(priceList);
        log.info("Deleted price list: {} (ID: {})", priceList.getCode(), id);
    }

    @Transactional
    public PriceListDto activatePriceList(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        PriceList priceList = priceListRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", id));

        priceList.setActive(true);
        priceList = priceListRepository.save(priceList);
        log.info("Activated price list: {} (ID: {})", priceList.getCode(), id);

        return priceListMapper.toDto(priceList);
    }

    @Transactional
    public PriceListDto deactivatePriceList(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        PriceList priceList = priceListRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", id));

        priceList.setActive(false);
        priceList = priceListRepository.save(priceList);
        log.info("Deactivated price list: {} (ID: {})", priceList.getCode(), id);

        return priceListMapper.toDto(priceList);
    }

    // ==================== Price List Items ====================

    @Transactional(readOnly = true)
    public Page<PriceListItemDto> findPriceListItems(Long priceListId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        PriceList priceList = priceListRepository.findByIdAndTenantId(priceListId, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", priceListId));

        return priceListItemRepository.findByPriceListId(priceListId, pageable)
                .map(item -> enrichPriceListItemDto(priceListItemMapper.toDto(item)));
    }

    @Transactional(readOnly = true)
    public PriceListItemDto findPriceListItem(Long priceListId, Long itemId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        priceListRepository.findByIdAndTenantId(priceListId, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", priceListId));

        PriceListItem item = priceListItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("PriceListItem", "id", itemId));

        if (!item.getPriceList().getId().equals(priceListId)) {
            throw new NotFoundException("PriceListItem", "id", itemId);
        }

        return enrichPriceListItemDto(priceListItemMapper.toDto(item));
    }

    @Transactional
    public PriceListItemDto addPriceListItem(Long priceListId, CreatePriceListItemRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        PriceList priceList = priceListRepository.findByIdAndTenantId(priceListId, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", priceListId));

        Product product = productRepository.findByIdAndTenantId(request.getProductId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Product", "id", request.getProductId()));

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .filter(v -> v.getProduct().getId().equals(product.getId()))
                    .orElseThrow(() -> new NotFoundException("ProductVariant", "id", request.getVariantId()));
        }

        PriceListItem item = priceListItemMapper.toEntity(request);
        item.setPriceList(priceList);
        item.setProduct(product);
        item.setVariant(variant);

        item = priceListItemRepository.save(item);
        log.info("Added item to price list {} for product {} (Item ID: {})",
                priceList.getCode(), product.getSku(), item.getId());

        return enrichPriceListItemDto(priceListItemMapper.toDto(item));
    }

    @Transactional
    public PriceListItemDto updatePriceListItem(Long priceListId, Long itemId, CreatePriceListItemRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        priceListRepository.findByIdAndTenantId(priceListId, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", priceListId));

        PriceListItem item = priceListItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("PriceListItem", "id", itemId));

        if (!item.getPriceList().getId().equals(priceListId)) {
            throw new NotFoundException("PriceListItem", "id", itemId);
        }

        // Update product if changed
        if (!item.getProduct().getId().equals(request.getProductId())) {
            Product product = productRepository.findByIdAndTenantId(request.getProductId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Product", "id", request.getProductId()));
            item.setProduct(product);
        }

        // Update variant if changed — must belong to the item's product
        if (request.getVariantId() != null) {
            if (item.getVariant() == null || !item.getVariant().getId().equals(request.getVariantId())) {
                Long itemProductId = item.getProduct().getId();
                ProductVariant variant = variantRepository.findById(request.getVariantId())
                        .filter(v -> v.getProduct().getId().equals(itemProductId))
                        .orElseThrow(() -> new NotFoundException("ProductVariant", "id", request.getVariantId()));
                item.setVariant(variant);
            }
        } else {
            item.setVariant(null);
        }

        priceListItemMapper.updateEntity(request, item);
        item = priceListItemRepository.save(item);
        log.info("Updated price list item: {}", itemId);

        return enrichPriceListItemDto(priceListItemMapper.toDto(item));
    }

    @Transactional
    public void removePriceListItem(Long priceListId, Long itemId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        priceListRepository.findByIdAndTenantId(priceListId, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", priceListId));

        PriceListItem item = priceListItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("PriceListItem", "id", itemId));

        if (!item.getPriceList().getId().equals(priceListId)) {
            throw new NotFoundException("PriceListItem", "id", itemId);
        }

        priceListItemRepository.delete(item);
        log.info("Removed price list item: {}", itemId);
    }

    // ==================== Customer Price List Assignments ====================

    @Transactional
    public void assignPriceListToCustomer(Long priceListId, Long customerId, Integer priority) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        PriceList priceList = priceListRepository.findByIdAndTenantId(priceListId, tenantId)
                .orElseThrow(() -> new NotFoundException("PriceList", "id", priceListId));

        CustomerPriceList assignment = CustomerPriceList.builder()
                .customerId(customerId)
                .priceList(priceList)
                .priority(priority != null ? priority : 0)
                .active(true)
                .build();

        customerPriceListRepository.save(assignment);
        log.info("Assigned price list {} to customer {}", priceList.getCode(), customerId);
    }

    @Transactional
    public void removePriceListFromCustomer(Long priceListId, Long customerId) {
        customerPriceListRepository.deleteByCustomerIdAndPriceListId(customerId, priceListId);
        log.info("Removed price list {} from customer {}", priceListId, customerId);
    }

    @Transactional(readOnly = true)
    public List<PriceListDto> findCustomerPriceLists(Long customerId) {
        LocalDate today = LocalDate.now();
        List<CustomerPriceList> customerPriceLists =
                customerPriceListRepository.findEffectiveForCustomer(customerId, today);

        return customerPriceLists.stream()
                .map(cpl -> priceListMapper.toDto(cpl.getPriceList()))
                .toList();
    }

    // ==================== Helper Methods ====================

    private PriceListItemDto enrichPriceListItemDto(PriceListItemDto dto) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantId(dto.getProductId(), tenantId).orElse(null);
        if (product != null) {
            BigDecimal basePrice = product.getSellingPrice();
            if (dto.getVariantId() != null) {
                ProductVariant variant = variantRepository.findById(dto.getVariantId())
                        .filter(v -> v.getProduct().getId().equals(product.getId()))
                        .orElse(null);
                if (variant != null && variant.getSellingPrice() != null) {
                    basePrice = variant.getSellingPrice();
                }
            }
            dto.setBasePrice(basePrice);

            // Calculate effective price
            if (dto.getPrice() != null) {
                dto.setEffectivePrice(dto.getPrice());
            } else if (dto.getMarkupPercent() != null && basePrice != null) {
                BigDecimal markup = basePrice.multiply(dto.getMarkupPercent())
                        .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
                dto.setEffectivePrice(basePrice.add(markup));
            } else {
                dto.setEffectivePrice(basePrice);
            }
        }

        return dto;
    }
}
