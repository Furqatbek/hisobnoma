package com.hisobnoma.platform.expense.repository;

import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecord, Long> {
}
