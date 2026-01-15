package com.hisobnoma.platform.admin.repository;

import com.hisobnoma.platform.admin.entity.TenantSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantSettingRepository extends JpaRepository<TenantSetting, Long> {

    Optional<TenantSetting> findByTenantIdAndSettingKey(Long tenantId, String settingKey);

    List<TenantSetting> findByTenantId(Long tenantId);

    List<TenantSetting> findByTenantIdAndActiveTrue(Long tenantId);

    List<TenantSetting> findByTenantIdAndCategory(Long tenantId, String category);

    List<TenantSetting> findByTenantIdAndCategoryAndActiveTrue(Long tenantId, String category);

    @Query("SELECT t FROM TenantSetting t WHERE t.tenantId = :tenantId AND t.active = true " +
           "ORDER BY t.category, t.settingKey")
    List<TenantSetting> findAllActiveByTenantIdOrdered(@Param("tenantId") Long tenantId);

    @Query("SELECT DISTINCT t.category FROM TenantSetting t WHERE t.tenantId = :tenantId AND t.active = true " +
           "ORDER BY t.category")
    List<String> findDistinctCategoriesByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByTenantIdAndSettingKey(Long tenantId, String settingKey);

    @Query("SELECT t FROM TenantSetting t WHERE t.tenantId = :tenantId AND t.settingKey IN :keys AND t.active = true")
    List<TenantSetting> findByTenantIdAndSettingKeyIn(@Param("tenantId") Long tenantId, @Param("keys") List<String> keys);

    void deleteByTenantId(Long tenantId);
}
