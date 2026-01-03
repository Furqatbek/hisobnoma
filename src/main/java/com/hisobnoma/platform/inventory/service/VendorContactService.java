package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.CreateVendorContactRequest;
import com.hisobnoma.platform.inventory.dto.VendorContactDto;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.entity.VendorContact;
import com.hisobnoma.platform.inventory.mapper.VendorContactMapper;
import com.hisobnoma.platform.inventory.repository.VendorContactRepository;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorContactService {

    private final VendorContactRepository vendorContactRepository;
    private final VendorRepository vendorRepository;
    private final VendorContactMapper vendorContactMapper;

    @Transactional(readOnly = true)
    public List<VendorContactDto> getContactsByVendor(Long vendorId) {
        List<VendorContact> contacts = vendorContactRepository.findByVendorIdAndActiveTrue(vendorId);
        return vendorContactMapper.toDtoList(contacts);
    }

    @Transactional(readOnly = true)
    public VendorContactDto getContact(Long id) {
        VendorContact contact = vendorContactRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vendor contact not found with id: " + id));
        return vendorContactMapper.toDto(contact);
    }

    @Transactional(readOnly = true)
    public VendorContactDto getPrimaryContact(Long vendorId) {
        VendorContact contact = vendorContactRepository.findByVendorIdAndPrimaryTrue(vendorId)
                .orElseThrow(() -> new NotFoundException("No primary contact found for vendor: " + vendorId));
        return vendorContactMapper.toDto(contact);
    }

    @Transactional(readOnly = true)
    public VendorContactDto getBillingContact(Long vendorId) {
        return vendorContactRepository.findByVendorIdAndBillingContactTrue(vendorId)
                .map(vendorContactMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public VendorContactDto getOrderingContact(Long vendorId) {
        return vendorContactRepository.findByVendorIdAndOrderingContactTrue(vendorId)
                .map(vendorContactMapper::toDto)
                .orElse(null);
    }

    @Transactional
    public VendorContactDto createContact(CreateVendorContactRequest request) {
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new NotFoundException("Vendor not found with id: " + request.getVendorId()));

        // Check for duplicate email within the same vendor
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            vendorContactRepository.findByVendorIdAndEmail(request.getVendorId(), request.getEmail())
                    .ifPresent(c -> {
                        throw new BusinessException("Contact with email '" + request.getEmail() + "' already exists for this vendor");
                    });
        }

        VendorContact contact = vendorContactMapper.toEntity(request);
        contact.setVendor(vendor);

        // If this is set as primary, unset existing primary contact
        if (request.isPrimary()) {
            vendorContactRepository.findByVendorIdAndPrimaryTrue(request.getVendorId())
                    .ifPresent(existingPrimary -> {
                        existingPrimary.setPrimary(false);
                        vendorContactRepository.save(existingPrimary);
                    });
        }

        contact = vendorContactRepository.save(contact);
        return vendorContactMapper.toDto(contact);
    }

    @Transactional
    public VendorContactDto updateContact(Long id, CreateVendorContactRequest request) {
        VendorContact contact = vendorContactRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vendor contact not found with id: " + id));

        // Check for duplicate email if changed
        if (request.getEmail() != null && !request.getEmail().isEmpty() &&
            !request.getEmail().equals(contact.getEmail())) {
            if (vendorContactRepository.existsByVendorIdAndEmailAndIdNot(
                    contact.getVendor().getId(), request.getEmail(), id)) {
                throw new BusinessException("Contact with email '" + request.getEmail() + "' already exists for this vendor");
            }
        }

        // If this is being set as primary, unset existing primary contact
        if (request.isPrimary() && !contact.isPrimary()) {
            vendorContactRepository.findByVendorIdAndPrimaryTrue(contact.getVendor().getId())
                    .ifPresent(existingPrimary -> {
                        if (!existingPrimary.getId().equals(id)) {
                            existingPrimary.setPrimary(false);
                            vendorContactRepository.save(existingPrimary);
                        }
                    });
        }

        vendorContactMapper.updateEntity(request, contact);
        contact = vendorContactRepository.save(contact);
        return vendorContactMapper.toDto(contact);
    }

    @Transactional
    public void deleteContact(Long id) {
        VendorContact contact = vendorContactRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vendor contact not found with id: " + id));

        // Soft delete by deactivating
        contact.setActive(false);
        vendorContactRepository.save(contact);
    }

    @Transactional
    public VendorContactDto setPrimaryContact(Long id) {
        VendorContact contact = vendorContactRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vendor contact not found with id: " + id));

        // Unset existing primary
        vendorContactRepository.findByVendorIdAndPrimaryTrue(contact.getVendor().getId())
                .ifPresent(existingPrimary -> {
                    existingPrimary.setPrimary(false);
                    vendorContactRepository.save(existingPrimary);
                });

        contact.setPrimary(true);
        contact = vendorContactRepository.save(contact);
        return vendorContactMapper.toDto(contact);
    }
}
