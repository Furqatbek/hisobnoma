package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.CustomerPriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPriceListRepository extends JpaRepository<CustomerPriceList, Long> {

    List<CustomerPriceList> findByCustomerId(Long customerId);

    List<CustomerPriceList> findByPriceListId(Long priceListId);

    Optional<CustomerPriceList> findByCustomerIdAndPriceListId(Long customerId, Long priceListId);

    @Query("SELECT cpl FROM CustomerPriceList cpl " +
           "JOIN FETCH cpl.priceList pl " +
           "WHERE cpl.customerId = :customerId " +
           "AND cpl.active = true " +
           "AND pl.active = true " +
           "AND (cpl.startDate IS NULL OR cpl.startDate <= :date) " +
           "AND (cpl.endDate IS NULL OR cpl.endDate >= :date) " +
           "AND (pl.startDate IS NULL OR pl.startDate <= :date) " +
           "AND (pl.endDate IS NULL OR pl.endDate >= :date) " +
           "ORDER BY cpl.priority DESC, pl.priority DESC")
    List<CustomerPriceList> findEffectiveForCustomer(
            @Param("customerId") Long customerId,
            @Param("date") LocalDate date);

    void deleteByCustomerId(Long customerId);

    void deleteByPriceListId(Long priceListId);

    void deleteByCustomerIdAndPriceListId(Long customerId, Long priceListId);

    @Query("SELECT COUNT(cpl) FROM CustomerPriceList cpl WHERE cpl.priceList.id = :priceListId")
    long countByPriceListId(@Param("priceListId") Long priceListId);
}
