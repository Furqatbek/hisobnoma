package com.hisobnoma.platform.pos.controller;

import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.POSPaymentService;
import com.hisobnoma.platform.pos.service.POSTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/transactions")
@RequiredArgsConstructor
public class POSTransactionController {

    private final POSTransactionService transactionService;
    private final POSPaymentService paymentService;

    // ==================== Transaction CRUD ====================

    @GetMapping
    @RequiresPermission("POS_SALE_READ")
    public ResponseEntity<PageResponse<POSTransactionDto>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<POSTransactionDto> page = search != null && !search.isBlank()
                ? transactionService.search(search, pageable)
                : transactionService.findAll(pageable);

        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/{id}")
    @RequiresPermission("POS_SALE_READ")
    public ResponseEntity<ApiResponse<POSTransactionDto>> getById(@PathVariable Long id) {
        POSTransactionDto transaction = transactionService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @GetMapping("/number/{transactionNumber}")
    @RequiresPermission("POS_SALE_READ")
    public ResponseEntity<ApiResponse<POSTransactionDto>> getByNumber(@PathVariable String transactionNumber) {
        POSTransactionDto transaction = transactionService.findByNumber(transactionNumber);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @GetMapping("/shift/{shiftId}")
    @RequiresPermission("POS_SALE_READ")
    public ResponseEntity<ApiResponse<List<POSTransactionDto>>> getByShift(@PathVariable Long shiftId) {
        List<POSTransactionDto> transactions = transactionService.findByShift(shiftId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/held")
    @RequiresPermission("POS_SALE_READ")
    public ResponseEntity<ApiResponse<List<POSTransactionDto>>> getHeldTransactions(
            @RequestParam Long shiftId) {
        List<POSTransactionDto> transactions = transactionService.findHeldTransactions(shiftId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    // ==================== Transaction Operations ====================

    @PostMapping
    @RequiresPermission("POS_SALE_CREATE")
    public ResponseEntity<ApiResponse<POSTransactionDto>> create(
            @Valid @RequestBody CreateTransactionRequest request) {
        POSTransactionDto transaction = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(transaction));
    }

    @PostMapping("/{id}/lines")
    @RequiresPermission("POS_SALE_CREATE")
    public ResponseEntity<ApiResponse<POSTransactionDto>> addLine(
            @PathVariable Long id,
            @Valid @RequestBody AddLineRequest request) {
        POSTransactionDto transaction = transactionService.addLine(id, request);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Item added"));
    }

    @PutMapping("/{id}/lines/{lineId}")
    @RequiresPermission("POS_SALE_CREATE")
    public ResponseEntity<ApiResponse<POSTransactionDto>> updateLine(
            @PathVariable Long id,
            @PathVariable Long lineId,
            @Valid @RequestBody UpdateLineRequest request) {
        POSTransactionDto transaction = transactionService.updateLine(id, lineId, request);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Item updated"));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @RequiresPermission("POS_SALE_CREATE")
    public ResponseEntity<ApiResponse<POSTransactionDto>> removeLine(
            @PathVariable Long id,
            @PathVariable Long lineId) {
        POSTransactionDto transaction = transactionService.removeLine(id, lineId);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Item removed"));
    }

    @PostMapping("/{id}/discount")
    @RequiresPermission("POS_DISCOUNT_APPLY")
    public ResponseEntity<ApiResponse<POSTransactionDto>> applyDiscount(
            @PathVariable Long id,
            @Valid @RequestBody ApplyDiscountRequest request) {
        POSTransactionDto transaction = transactionService.applyDiscount(id, request);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Discount applied"));
    }

    @PostMapping("/{id}/hold")
    @RequiresPermission("POS_SALE_HOLD")
    public ResponseEntity<ApiResponse<POSTransactionDto>> holdTransaction(
            @PathVariable Long id,
            @Valid @RequestBody HoldTransactionRequest request) {
        POSTransactionDto transaction = transactionService.holdTransaction(id, request);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Transaction held"));
    }

    @PostMapping("/{id}/recall")
    @RequiresPermission("POS_SALE_HOLD")
    public ResponseEntity<ApiResponse<POSTransactionDto>> recallTransaction(@PathVariable Long id) {
        POSTransactionDto transaction = transactionService.recallTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Transaction recalled"));
    }

    @PostMapping("/{id}/void")
    @RequiresPermission("POS_SALE_VOID")
    public ResponseEntity<ApiResponse<POSTransactionDto>> voidTransaction(
            @PathVariable Long id,
            @Valid @RequestBody VoidTransactionRequest request) {
        POSTransactionDto transaction = transactionService.voidTransaction(id, request);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Transaction voided"));
    }

    @PostMapping("/{id}/complete")
    @RequiresPermission("POS_SALE_CREATE")
    public ResponseEntity<ApiResponse<POSTransactionDto>> completeTransaction(@PathVariable Long id) {
        POSTransactionDto transaction = transactionService.completeTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Transaction completed"));
    }

    // ==================== Payment Operations ====================

    @GetMapping("/{id}/payments")
    @RequiresPermission("POS_SALE_READ")
    public ResponseEntity<ApiResponse<List<POSPaymentDto>>> getPayments(@PathVariable Long id) {
        List<POSPaymentDto> payments = paymentService.findByTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/{id}/payments")
    @RequiresPermission("POS_PAYMENT_PROCESS")
    public ResponseEntity<ApiResponse<POSPaymentDto>> addPayment(
            @PathVariable Long id,
            @Valid @RequestBody AddPaymentRequest request) {
        POSPaymentDto payment = paymentService.addPayment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(payment, "Payment added"));
    }

    @PostMapping("/{id}/payments/{paymentId}/void")
    @RequiresPermission("POS_PAYMENT_VOID")
    public ResponseEntity<ApiResponse<POSPaymentDto>> voidPayment(
            @PathVariable Long id,
            @PathVariable Long paymentId,
            @RequestParam String reason) {
        POSPaymentDto payment = paymentService.voidPayment(id, paymentId, reason);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment voided"));
    }

    @PostMapping("/{id}/payments/{paymentId}/refund")
    @RequiresPermission("POS_SALE_REFUND")
    public ResponseEntity<ApiResponse<POSPaymentDto>> refundPayment(
            @PathVariable Long id,
            @PathVariable Long paymentId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason) {
        POSPaymentDto payment = paymentService.refundPayment(id, paymentId, amount, reason);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment refunded"));
    }

    // ==================== Return Operations ====================

    /**
     * Create a return transaction.
     * Can be linked to an original transaction or be a standalone return.
     */
    @PostMapping("/returns")
    @RequiresPermission("POS_RETURN_CREATE")
    public ResponseEntity<ApiResponse<POSTransactionDto>> createReturn(
            @Valid @RequestBody CreateReturnRequest request) {
        POSTransactionDto returnTransaction = transactionService.createReturn(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(returnTransaction, "Return transaction created"));
    }

    /**
     * Create a return from an existing transaction.
     * Convenience endpoint that validates the original transaction first.
     */
    @PostMapping("/{id}/return")
    @RequiresPermission("POS_RETURN_CREATE")
    public ResponseEntity<ApiResponse<POSTransactionDto>> createReturnFromTransaction(
            @PathVariable Long id,
            @Valid @RequestBody CreateReturnRequest request) {
        // Set the original transaction ID from the path
        request.setOriginalTransactionId(id);
        POSTransactionDto returnTransaction = transactionService.createReturn(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(returnTransaction, "Return transaction created from original"));
    }
}
