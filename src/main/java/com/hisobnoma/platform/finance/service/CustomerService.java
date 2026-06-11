package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.dto.CustomerDto;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.mapper.CustomerMapper;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> getCustomers(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Customer> page = customerRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(customerMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> getActiveCustomers() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Customer> customers = customerRepository.findAllActiveByTenantId(tenantId);
        return customerMapper.toDtoList(customers);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomer(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + id));
        return customerMapper.toDto(customer);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomerByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Customer customer = customerRepository.findByCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with code: " + code));
        return customerMapper.toDto(customer);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> searchCustomers(String query, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Customer> page = customerRepository.searchByTenantId(tenantId, query, pageable);
        return PageResponse.of(page.map(customerMapper::toDto));
    }

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Auto-generate code if not provided
        if (request.getCode() == null || request.getCode().isBlank()) {
            request.setCode(generateCustomerCode(tenantId));
        }

        // Check for duplicate code
        if (customerRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new BusinessException("Customer with code '" + request.getCode() + "' already exists");
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setTenantId(tenantId);
        customer.setCurrentBalance(BigDecimal.ZERO);
        customer.setTotalInvoiced(BigDecimal.ZERO);
        customer.setTotalReceived(BigDecimal.ZERO);

        customer = customerRepository.save(customer);
        log.info("Created customer: {} - {}", customer.getCode(), customer.getName());

        return customerMapper.toDto(customer);
    }

    @Transactional
    public CustomerDto updateCustomer(Long id, CreateCustomerRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + id));

        // Check for duplicate code if changed
        if (!customer.getCode().equals(request.getCode()) &&
            customerRepository.existsByCodeAndTenantIdAndIdNot(request.getCode(), tenantId, id)) {
            throw new BusinessException("Customer with code '" + request.getCode() + "' already exists");
        }

        customerMapper.updateEntity(request, customer);
        customer = customerRepository.save(customer);
        log.info("Updated customer: {} - {}", customer.getCode(), customer.getName());

        return customerMapper.toDto(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + id));

        // Check if customer has outstanding balance
        if (customer.getCurrentBalance() != null && customer.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("Cannot delete customer with outstanding balance");
        }

        // Soft delete
        customer.setActive(false);
        customerRepository.save(customer);
        log.info("Deactivated customer: {} - {}", customer.getCode(), customer.getName());
    }

    @Transactional
    public CustomerDto activateCustomer(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + id));

        customer.setActive(true);
        customer = customerRepository.save(customer);
        log.info("Activated customer: {} - {}", customer.getCode(), customer.getName());

        return customerMapper.toDto(customer);
    }

    @Transactional
    public CustomerDto setCreditHold(Long id, boolean hold) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + id));

        customer.setCreditHold(hold);
        customer = customerRepository.save(customer);
        log.info("{} credit hold for customer: {} - {}", hold ? "Enabled" : "Disabled",
                customer.getCode(), customer.getName());

        return customerMapper.toDto(customer);
    }

    @Transactional
    public CustomerDto updateCreditLimit(Long id, BigDecimal creditLimit) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + id));

        customer.setCreditLimit(creditLimit);
        customer = customerRepository.save(customer);
        log.info("Updated credit limit for customer {} to {}", customer.getCode(), creditLimit);

        return customerMapper.toDto(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> getCustomersOnCreditHold() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Customer> customers = customerRepository.findCreditHoldByTenantId(tenantId);
        return customerMapper.toDtoList(customers);
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> getCustomersOverCreditLimit() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Customer> customers = customerRepository.findOverCreditLimitByTenantId(tenantId);
        return customerMapper.toDtoList(customers);
    }

    /**
     * Update customer balance after invoice or payment. Takes the
     * already-loaded (tenant-validated) customer so no unscoped re-fetch
     * by id is needed.
     */
    @Transactional
    public void updateCustomerBalance(Customer customer, BigDecimal amount) {
        BigDecimal currentBalance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : BigDecimal.ZERO;
        customer.setCurrentBalance(currentBalance.add(amount));
        customerRepository.save(customer);
    }

    /**
     * Get the next auto-generated customer code for preview purposes.
     */
    @Transactional(readOnly = true)
    public String getNextCustomerCode() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return generateCustomerCode(tenantId);
    }

    private String generateCustomerCode(Long tenantId) {
        Integer maxNumber = customerRepository.findMaxCustomerCodeNumber(tenantId);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("CUST-%06d", nextNumber);
    }

    /**
     * Check if customer can be invoiced (not on credit hold, within credit limit).
     */
    @Transactional(readOnly = true)
    public boolean canBeInvoiced(Long customerId, BigDecimal invoiceAmount) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Customer customer = customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));

        if (customer.isCreditHold()) {
            return false;
        }

        if (customer.getCreditLimit() == null || customer.getCreditLimit().compareTo(BigDecimal.ZERO) == 0) {
            return true; // No limit set (null or 0 means unlimited)
        }

        BigDecimal currentBalance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(invoiceAmount);

        return newBalance.compareTo(customer.getCreditLimit()) <= 0;
    }
}
