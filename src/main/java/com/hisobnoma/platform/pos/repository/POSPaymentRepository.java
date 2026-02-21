package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.POSPayment;
import com.hisobnoma.platform.pos.entity.POSPaymentStatus;
import com.hisobnoma.platform.pos.entity.POSPaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface POSPaymentRepository extends JpaRepository<POSPayment, Long> {

    @Query("SELECT p FROM POSPayment p WHERE p.transaction.id = :transactionId ORDER BY p.paymentNumber")
    List<POSPayment> findByTransactionId(@Param("transactionId") Long transactionId);

    @Query("SELECT p FROM POSPayment p WHERE p.id = :paymentId AND p.transaction.id = :transactionId")
    Optional<POSPayment> findByIdAndTransactionId(@Param("paymentId") Long paymentId, @Param("transactionId") Long transactionId);

    @Query("SELECT SUM(p.amount) FROM POSPayment p WHERE p.transaction.id = :transactionId AND p.status = 'APPROVED'")
    BigDecimal sumApprovedByTransactionId(@Param("transactionId") Long transactionId);

    @Query("SELECT SUM(p.amount) FROM POSPayment p WHERE p.transaction.shift.id = :shiftId AND p.paymentType = :type AND p.status = 'APPROVED' AND p.transaction.status = 'COMPLETED'")
    BigDecimal sumByShiftIdAndPaymentType(@Param("shiftId") Long shiftId, @Param("type") POSPaymentType type);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM POSPayment p WHERE p.transaction.shift.id = :shiftId AND p.paymentType = :type " +
           "AND p.status = 'APPROVED' AND p.transaction.status = 'COMPLETED' AND p.transaction.transactionType = 'SALE'")
    BigDecimal sumSalesByShiftIdAndPaymentType(@Param("shiftId") Long shiftId, @Param("type") POSPaymentType type);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM POSPayment p WHERE p.transaction.shift.id = :shiftId AND p.paymentType = :type " +
           "AND p.status = 'APPROVED' AND p.transaction.status = 'COMPLETED' AND p.transaction.transactionType = 'RETURN'")
    BigDecimal sumRefundsByShiftIdAndPaymentType(@Param("shiftId") Long shiftId, @Param("type") POSPaymentType type);

    @Query("SELECT p FROM POSPayment p WHERE p.transaction.shift.id = :shiftId AND p.paymentType = :type ORDER BY p.createdAt DESC")
    List<POSPayment> findByShiftIdAndPaymentType(@Param("shiftId") Long shiftId, @Param("type") POSPaymentType type);

    @Query("SELECT MAX(p.paymentNumber) FROM POSPayment p WHERE p.transaction.id = :transactionId")
    Integer findMaxPaymentNumberByTransactionId(@Param("transactionId") Long transactionId);

    @Query("SELECT p FROM POSPayment p WHERE p.gatewayReference = :reference")
    Optional<POSPayment> findByGatewayReference(@Param("reference") String reference);

    @Query("SELECT p FROM POSPayment p WHERE p.giftCardNumber = :giftCardNumber AND p.status = 'APPROVED'")
    List<POSPayment> findByGiftCardNumber(@Param("giftCardNumber") String giftCardNumber);

    @Query("SELECT SUM(p.amount) FROM POSPayment p WHERE p.transaction.shift.id = :shiftId AND p.status = 'APPROVED' " +
           "AND p.transaction.status = 'COMPLETED'")
    BigDecimal sumAllByShiftId(@Param("shiftId") Long shiftId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM POSPayment p WHERE p.transaction.shift.id = :shiftId AND p.status = 'APPROVED' " +
           "AND p.transaction.status = 'COMPLETED' AND p.transaction.transactionType = 'SALE'")
    BigDecimal sumAllSalesByShiftId(@Param("shiftId") Long shiftId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM POSPayment p WHERE p.transaction.shift.id = :shiftId AND p.status = 'APPROVED' " +
           "AND p.transaction.status = 'COMPLETED' AND p.transaction.transactionType = 'RETURN'")
    BigDecimal sumAllRefundsByShiftId(@Param("shiftId") Long shiftId);

    @Query("SELECT p FROM POSPayment p WHERE p.status = :status AND p.createdAt >= :startDate AND p.createdAt < :endDate")
    List<POSPayment> findByStatusAndDateRange(
            @Param("status") POSPaymentStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
