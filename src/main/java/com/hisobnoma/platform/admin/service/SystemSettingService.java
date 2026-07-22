package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.SystemSettingDTO;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.mapper.SystemSettingMapper;
import com.hisobnoma.platform.admin.repository.SystemSettingRepository;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.BusinessException;
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
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final SystemSettingMapper systemSettingMapper;

    @Transactional(readOnly = true)
    public List<SystemSettingDTO> getAllSettings() {
        List<SystemSetting> settings = systemSettingRepository.findAllActiveOrderedByCategoryAndSortOrder();
        return systemSettingMapper.toDtoList(settings);
    }

    @Transactional(readOnly = true)
    public List<SystemSettingDTO> getSettingsByCategory(String category) {
        List<SystemSetting> settings = systemSettingRepository.findByCategoryAndActiveTrue(category);
        return systemSettingMapper.toDtoList(settings);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "systemSettings", key = "#settingKey")
    public SystemSettingDTO getSetting(String settingKey) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new NotFoundException("SystemSetting", "key", settingKey));
        return systemSettingMapper.toDto(setting);
    }

    @Transactional(readOnly = true)
    public String getSettingValue(String settingKey) {
        return systemSettingRepository.findBySettingKey(settingKey)
                .map(SystemSetting::getEffectiveValue)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public String getSettingValue(String settingKey, String defaultValue) {
        return systemSettingRepository.findBySettingKey(settingKey)
                .map(SystemSetting::getEffectiveValue)
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public boolean getBooleanSetting(String settingKey, boolean defaultValue) {
        return systemSettingRepository.findBySettingKey(settingKey)
                .map(SystemSetting::getBooleanValue)
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public Integer getIntegerSetting(String settingKey, Integer defaultValue) {
        return systemSettingRepository.findBySettingKey(settingKey)
                .map(SystemSetting::getIntegerValue)
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return systemSettingRepository.findDistinctCategories();
    }

    @Transactional
    @CacheEvict(value = "systemSettings", key = "#dto.settingKey")
    public SystemSettingDTO createSetting(SystemSettingDTO dto) {
        if (systemSettingRepository.existsBySettingKey(dto.getSettingKey())) {
            throw new DuplicateResourceException("SystemSetting", "key", dto.getSettingKey());
        }

        SystemSetting setting = systemSettingMapper.toEntity(dto);
        setting = systemSettingRepository.save(setting);
        log.info("Created system setting: {}", setting.getSettingKey());

        return systemSettingMapper.toDto(setting);
    }

    @Transactional
    @CacheEvict(value = "systemSettings", key = "#settingKey")
    public SystemSettingDTO updateSetting(String settingKey, SystemSettingDTO dto) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new NotFoundException("SystemSetting", "key", settingKey));

        if (setting.isReadonly()) {
            throw new BusinessException("Cannot modify read-only setting: " + settingKey, "SETTING_READONLY");
        }

        systemSettingMapper.updateEntity(dto, setting);
        setting = systemSettingRepository.save(setting);
        log.info("Updated system setting: {}", setting.getSettingKey());

        return systemSettingMapper.toDto(setting);
    }

    @Transactional
    @CacheEvict(value = "systemSettings", key = "#settingKey")
    public SystemSettingDTO updateSettingValue(String settingKey, String value) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new NotFoundException("SystemSetting", "key", settingKey));

        if (setting.isReadonly()) {
            throw new BusinessException("Cannot modify read-only setting: " + settingKey, "SETTING_READONLY");
        }

        setting.setSettingValue(value);
        setting = systemSettingRepository.save(setting);
        log.info("Updated system setting value: {}", setting.getSettingKey());

        return systemSettingMapper.toDto(setting);
    }

    @Transactional
    @CacheEvict(value = "systemSettings", allEntries = true)
    public void updateSettings(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            SystemSetting setting = systemSettingRepository.findBySettingKey(entry.getKey())
                    .orElseGet(() -> {
                        // Upsert: a missing key must not make the save a silent no-op
                        // (mirrors TenantSettingService.updateSettingValue).
                        SystemSetting created = new SystemSetting();
                        created.setSettingKey(entry.getKey());
                        created.setCategory(entry.getKey().contains(".")
                                ? entry.getKey().substring(0, entry.getKey().indexOf('.')).toUpperCase()
                                : "GENERAL");
                        return created;
                    });
            if (!setting.isReadonly()) {
                setting.setSettingValue(entry.getValue());
                systemSettingRepository.save(setting);
                log.info("Updated system setting: {}", setting.getSettingKey());
            }
        }
    }

    @Transactional
    @CacheEvict(value = "systemSettings", key = "#settingKey")
    public void deleteSetting(String settingKey) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new NotFoundException("SystemSetting", "key", settingKey));

        if (setting.isReadonly()) {
            throw new BusinessException("Cannot delete read-only setting: " + settingKey, "SETTING_READONLY");
        }

        setting.setActive(false);
        systemSettingRepository.save(setting);
        log.info("Deactivated system setting: {}", setting.getSettingKey());
    }

    @Transactional(readOnly = true)
    public Map<String, String> getSettingsAsMap() {
        return systemSettingRepository.findByActiveTrue().stream()
                .collect(Collectors.toMap(
                        SystemSetting::getSettingKey,
                        setting -> setting.getEffectiveValue() != null ? setting.getEffectiveValue() : ""
                ));
    }
}
