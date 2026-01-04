package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.PromotionAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionActionRepository extends JpaRepository<PromotionAction, Long> {

    List<PromotionAction> findByPromotionIdOrderBySortOrder(Long promotionId);

    void deleteByPromotionId(Long promotionId);
}
