package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, Long> {

    List<CustomerContact> findByCustomerId(Long customerId);

    List<CustomerContact> findByCustomerIdAndActiveTrue(Long customerId);

    Optional<CustomerContact> findByCustomerIdAndPrimaryTrue(Long customerId);

    Optional<CustomerContact> findByCustomerIdAndBillingContactTrue(Long customerId);

    Optional<CustomerContact> findByCustomerIdAndShippingContactTrue(Long customerId);

    @Query("SELECT cc FROM CustomerContact cc WHERE cc.customer.id = :customerId AND cc.email = :email")
    Optional<CustomerContact> findByCustomerIdAndEmail(@Param("customerId") Long customerId, @Param("email") String email);

    boolean existsByCustomerIdAndEmailAndIdNot(Long customerId, String email, Long id);

    void deleteByCustomerId(Long customerId);

    int countByCustomerId(Long customerId);
}
