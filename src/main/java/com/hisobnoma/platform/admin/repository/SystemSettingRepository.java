package com.hisobnoma.platform.admin.repository;

import com.hisobnoma.platform.admin.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    List<SystemSetting> findByCategory(String category);

    List<SystemSetting> findByActiveTrue();

    List<SystemSetting> findByCategoryAndActiveTrue(String category);

    @Query("SELECT s FROM SystemSetting s WHERE s.active = true ORDER BY s.category, s.sortOrder, s.settingKey")
    List<SystemSetting> findAllActiveOrderedByCategoryAndSortOrder();

    @Query("SELECT DISTINCT s.category FROM SystemSetting s WHERE s.active = true ORDER BY s.category")
    List<String> findDistinctCategories();

    boolean existsBySettingKey(String settingKey);

    @Query("SELECT s FROM SystemSetting s WHERE s.settingKey IN :keys AND s.active = true")
    List<SystemSetting> findBySettingKeyIn(@Param("keys") List<String> keys);
}
