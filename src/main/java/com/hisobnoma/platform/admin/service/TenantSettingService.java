package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.TenantSettingDTO;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.entity.TenantSetting;
import com.hisobnoma.platform.admin.mapper.TenantSettingMapper;
import com.hisobnoma.platform.admin.repository.SystemSettingRepository;
import com.hisobnoma.platform.admin.repository.TenantSettingRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSettingService {

    private final TenantSettingRepository tenantSettingRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final TenantSettingMapper tenantSettingMapper;
    private final SecurityContextHelper securityContextHelper;
    private final SystemSettingService systemSettingService;

    @Transactional(readOnly = true)
    public List<TenantSettingDTO> getAllSettings() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<TenantSetting> settings = tenantSettingRepository.findAllActiveByTenantIdOrdered(tenantId);
        return tenantSettingMapper.toDtoList(settings);
    }

    @Transactional(readOnly = true)
    public List<TenantSettingDTO> getSettingsByCategory(String category) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<TenantSetting> settings = tenantSettingRepository.findByTenantIdAndCategoryAndActiveTrue(tenantId, category);
        return tenantSettingMapper.toDtoList(settings);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tenantSettings", key = "#tenantId + ':' + #settingKey")
    public TenantSettingDTO getSetting(Long tenantId, String settingKey) {
        TenantSetting setting = tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, settingKey)
                .orElseThrow(() -> new NotFoundException("TenantSetting", "key", settingKey));
        return tenantSettingMapper.toDto(setting);
    }

    @Transactional(readOnly = true)
    public String getSettingValue(String settingKey) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return getSettingValue(tenantId, settingKey);
    }

    @Transactional(readOnly = true)
    public String getSettingValue(Long tenantId, String settingKey) {
        // First check tenant-specific setting
        return tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, settingKey)
                .map(TenantSetting::getSettingValue)
                // Fall back to system default
                .orElseGet(() -> systemSettingService.getSettingValue(settingKey));
    }

    @Transactional(readOnly = true)
    public String getSettingValue(String settingKey, String defaultValue) {
        String value = getSettingValue(settingKey);
        return value != null ? value : defaultValue;
    }

    @Transactional(readOnly = true)
    public boolean getBooleanSetting(String settingKey, boolean defaultValue) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, settingKey)
                .map(TenantSetting::getBooleanValue)
                .orElseGet(() -> systemSettingService.getBooleanSetting(settingKey, defaultValue));
    }

    @Transactional(readOnly = true)
    public Integer getIntegerSetting(String settingKey, Integer defaultValue) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, settingKey)
                .map(TenantSetting::getIntegerValue)
                .orElseGet(() -> systemSettingService.getIntegerSetting(settingKey, defaultValue));
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return tenantSettingRepository.findDistinctCategoriesByTenantId(tenantId);
    }

    @Transactional
    @CacheEvict(value = "tenantSettings", key = "#result.tenantId + ':' + #result.settingKey")
    public TenantSettingDTO createSetting(TenantSettingDTO dto) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        if (tenantSettingRepository.existsByTenantIdAndSettingKey(tenantId, dto.getSettingKey())) {
            throw new DuplicateResourceException("TenantSetting", "key", dto.getSettingKey());
        }

        TenantSetting setting = tenantSettingMapper.toEntity(dto);
        setting.setTenantId(tenantId);

        // Link to system setting if exists
        systemSettingRepository.findBySettingKey(dto.getSettingKey())
                .ifPresent(setting::setSystemSetting);

        setting = tenantSettingRepository.save(setting);
        log.info("Created tenant setting: {} for tenant: {}", setting.getSettingKey(), tenantId);

        return tenantSettingMapper.toDto(setting);
    }

    @Transactional
    public TenantSettingDTO updateSetting(String settingKey, TenantSettingDTO dto) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return updateSetting(tenantId, settingKey, dto);
    }

    @Transactional
    @CacheEvict(value = "tenantSettings", key = "#tenantId + ':' + #settingKey")
    public TenantSettingDTO updateSetting(Long tenantId, String settingKey, TenantSettingDTO dto) {
        TenantSetting setting = tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, settingKey)
                .orElseThrow(() -> new NotFoundException("TenantSetting", "key", settingKey));

        tenantSettingMapper.updateEntity(dto, setting);
        setting = tenantSettingRepository.save(setting);
        log.info("Updated tenant setting: {} for tenant: {}", setting.getSettingKey(), tenantId);

        return tenantSettingMapper.toDto(setting);
    }

    @Transactional
    public TenantSettingDTO updateSettingValue(String settingKey, String value) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return updateSettingValue(tenantId, settingKey, value);
    }

    @Transactional
    @CacheEvict(value = "tenantSettings", key = "#tenantId + ':' + #settingKey")
    public TenantSettingDTO updateSettingValue(Long tenantId, String settingKey, String value) {
        TenantSetting setting = tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, settingKey)
                .orElseGet(() -> createNewTenantSetting(tenantId, settingKey));

        setting.setSettingValue(value);
        setting = tenantSettingRepository.save(setting);
        log.info("Updated tenant setting value: {} for tenant: {}", setting.getSettingKey(), tenantId);

        return tenantSettingMapper.toDto(setting);
    }

    private TenantSetting createNewTenantSetting(Long tenantId, String settingKey) {
        TenantSetting setting = new TenantSetting();
        setting.setTenantId(tenantId);
        setting.setSettingKey(settingKey);

        // Copy metadata from system setting if exists
        systemSettingRepository.findBySettingKey(settingKey).ifPresent(systemSetting -> {
            setting.setSystemSetting(systemSetting);
            setting.setDescription(systemSetting.getDescription());
            setting.setCategory(systemSetting.getCategory());
            setting.setValueType(systemSetting.getValueType());
            setting.setSensitive(systemSetting.isSensitive());
        });

        return setting;
    }

    @Transactional
    @CacheEvict(value = "tenantSettings", allEntries = true)
    public void updateSettings(Map<String, String> settings) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            updateSettingValue(tenantId, entry.getKey(), entry.getValue());
        }
    }

    @Transactional
    @CacheEvict(value = "tenantSettings", key = "#tenantId + ':' + #settingKey")
    public void deleteSetting(Long tenantId, String settingKey) {
        TenantSetting setting = tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, settingKey)
                .orElseThrow(() -> new NotFoundException("TenantSetting", "key", settingKey));

        setting.setActive(false);
        tenantSettingRepository.save(setting);
        log.info("Deactivated tenant setting: {} for tenant: {}", settingKey, tenantId);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getSettingsAsMap() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return tenantSettingRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .collect(Collectors.toMap(
                        TenantSetting::getSettingKey,
                        setting -> setting.getSettingValue() != null ? setting.getSettingValue() : ""
                ));
    }
}
