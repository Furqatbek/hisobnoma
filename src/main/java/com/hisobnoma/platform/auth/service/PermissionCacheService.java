package com.hisobnoma.platform.auth.service;

import com.hisobnoma.platform.auth.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final PermissionRepository permissionRepository;

    private static final String CACHE_NAME = "user-permissions";

    @Cacheable(value = CACHE_NAME, key = "#userId")
    public Set<String> getUserPermissions(Long userId) {
        log.debug("Loading permissions from database for user: {}", userId);
        return permissionRepository.findAllByUserId(userId).stream()
                .map(p -> p.getCode())
                .collect(Collectors.toSet());
    }

    @CacheEvict(value = CACHE_NAME, key = "#userId")
    public void evictUserPermissions(Long userId) {
        log.debug("Evicting permission cache for user: {}", userId);
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void evictAllPermissions() {
        log.debug("Evicting all permission caches");
    }

    public boolean hasPermission(Long userId, String permissionCode) {
        return getUserPermissions(userId).contains(permissionCode);
    }

    public boolean hasAnyPermission(Long userId, String... permissionCodes) {
        Set<String> userPermissions = getUserPermissions(userId);
        for (String code : permissionCodes) {
            if (userPermissions.contains(code)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllPermissions(Long userId, String... permissionCodes) {
        Set<String> userPermissions = getUserPermissions(userId);
        for (String code : permissionCodes) {
            if (!userPermissions.contains(code)) {
                return false;
            }
        }
        return true;
    }
}
