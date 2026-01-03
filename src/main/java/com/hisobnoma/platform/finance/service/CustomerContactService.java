package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateCustomerContactRequest;
import com.hisobnoma.platform.finance.dto.CustomerContactDto;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.entity.CustomerContact;
import com.hisobnoma.platform.finance.mapper.CustomerContactMapper;
import com.hisobnoma.platform.finance.repository.CustomerContactRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerContactService {

    private final CustomerContactRepository customerContactRepository;
    private final CustomerRepository customerRepository;
    private final CustomerContactMapper customerContactMapper;

    @Transactional(readOnly = true)
    public List<CustomerContactDto> getContactsByCustomer(Long customerId) {
        List<CustomerContact> contacts = customerContactRepository.findByCustomerIdAndActiveTrue(customerId);
        return customerContactMapper.toDtoList(contacts);
    }

    @Transactional(readOnly = true)
    public CustomerContactDto getContact(Long id) {
        CustomerContact contact = customerContactRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer contact not found with id: " + id));
        return customerContactMapper.toDto(contact);
    }

    @Transactional(readOnly = true)
    public CustomerContactDto getPrimaryContact(Long customerId) {
        CustomerContact contact = customerContactRepository.findByCustomerIdAndPrimaryTrue(customerId)
                .orElseThrow(() -> new NotFoundException("No primary contact found for customer: " + customerId));
        return customerContactMapper.toDto(contact);
    }

    @Transactional(readOnly = true)
    public CustomerContactDto getBillingContact(Long customerId) {
        return customerContactRepository.findByCustomerIdAndBillingContactTrue(customerId)
                .map(customerContactMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CustomerContactDto getShippingContact(Long customerId) {
        return customerContactRepository.findByCustomerIdAndShippingContactTrue(customerId)
                .map(customerContactMapper::toDto)
                .orElse(null);
    }

    @Transactional
    public CustomerContactDto createContact(CreateCustomerContactRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + request.getCustomerId()));

        // Check for duplicate email within the same customer
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            customerContactRepository.findByCustomerIdAndEmail(request.getCustomerId(), request.getEmail())
                    .ifPresent(c -> {
                        throw new BusinessException("Contact with email '" + request.getEmail() + "' already exists for this customer");
                    });
        }

        CustomerContact contact = customerContactMapper.toEntity(request);
        contact.setCustomer(customer);

        // If this is set as primary, unset existing primary contact
        if (request.isPrimary()) {
            customerContactRepository.findByCustomerIdAndPrimaryTrue(request.getCustomerId())
                    .ifPresent(existingPrimary -> {
                        existingPrimary.setPrimary(false);
                        customerContactRepository.save(existingPrimary);
                    });
        }

        contact = customerContactRepository.save(contact);
        return customerContactMapper.toDto(contact);
    }

    @Transactional
    public CustomerContactDto updateContact(Long id, CreateCustomerContactRequest request) {
        CustomerContact contact = customerContactRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer contact not found with id: " + id));

        // Check for duplicate email if changed
        if (request.getEmail() != null && !request.getEmail().isEmpty() &&
            !request.getEmail().equals(contact.getEmail())) {
            if (customerContactRepository.existsByCustomerIdAndEmailAndIdNot(
                    contact.getCustomer().getId(), request.getEmail(), id)) {
                throw new BusinessException("Contact with email '" + request.getEmail() + "' already exists for this customer");
            }
        }

        // If this is being set as primary, unset existing primary contact
        if (request.isPrimary() && !contact.isPrimary()) {
            customerContactRepository.findByCustomerIdAndPrimaryTrue(contact.getCustomer().getId())
                    .ifPresent(existingPrimary -> {
                        if (!existingPrimary.getId().equals(id)) {
                            existingPrimary.setPrimary(false);
                            customerContactRepository.save(existingPrimary);
                        }
                    });
        }

        customerContactMapper.updateEntity(request, contact);
        contact = customerContactRepository.save(contact);
        return customerContactMapper.toDto(contact);
    }

    @Transactional
    public void deleteContact(Long id) {
        CustomerContact contact = customerContactRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer contact not found with id: " + id));

        // Soft delete by deactivating
        contact.setActive(false);
        customerContactRepository.save(contact);
    }

    @Transactional
    public CustomerContactDto setPrimaryContact(Long id) {
        CustomerContact contact = customerContactRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer contact not found with id: " + id));

        // Unset existing primary
        customerContactRepository.findByCustomerIdAndPrimaryTrue(contact.getCustomer().getId())
                .ifPresent(existingPrimary -> {
                    existingPrimary.setPrimary(false);
                    customerContactRepository.save(existingPrimary);
                });

        contact.setPrimary(true);
        contact = customerContactRepository.save(contact);
        return customerContactMapper.toDto(contact);
    }
}
