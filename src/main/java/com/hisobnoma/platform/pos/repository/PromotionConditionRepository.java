package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.PromotionCondition;
import com.hisobnoma.platform.pos.enums.PromotionConditionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionConditionRepository extends JpaRepository<PromotionCondition, Long> {

    List<PromotionCondition> findByPromotionId(Long promotionId);

    List<PromotionCondition> findByPromotionIdAndConditionType(Long promotionId, PromotionConditionType conditionType);

    void deleteByPromotionId(Long promotionId);
}
