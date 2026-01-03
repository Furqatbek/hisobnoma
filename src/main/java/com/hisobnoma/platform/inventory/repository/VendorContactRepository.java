package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.VendorContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorContactRepository extends JpaRepository<VendorContact, Long> {

    List<VendorContact> findByVendorId(Long vendorId);

    List<VendorContact> findByVendorIdAndActiveTrue(Long vendorId);

    Optional<VendorContact> findByVendorIdAndPrimaryTrue(Long vendorId);

    Optional<VendorContact> findByVendorIdAndBillingContactTrue(Long vendorId);

    Optional<VendorContact> findByVendorIdAndOrderingContactTrue(Long vendorId);

    @Query("SELECT vc FROM VendorContact vc WHERE vc.vendor.id = :vendorId AND vc.email = :email")
    Optional<VendorContact> findByVendorIdAndEmail(@Param("vendorId") Long vendorId, @Param("email") String email);

    boolean existsByVendorIdAndEmailAndIdNot(Long vendorId, String email, Long id);

    void deleteByVendorId(Long vendorId);

    int countByVendorId(Long vendorId);
}
