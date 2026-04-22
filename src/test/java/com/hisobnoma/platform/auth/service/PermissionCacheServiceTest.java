package com.hisobnoma.platform.auth.service;

import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class PermissionCacheServiceTest {

    @Autowired
    private PermissionCacheService permissionCacheService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private PermissionRepository permissionRepository;

    private static final Long USER_ID = 10L;
    private static final String CACHE_NAME = "user-permissions";

    @BeforeEach
    void setUp() {
        // Ensure cache is clean before each test
        if (cacheManager.getCache(CACHE_NAME) != null) {
            cacheManager.getCache(CACHE_NAME).clear();
        }
    }

    @Test
    void getPermissions_cacheMiss_loadsFromDbAndCaches() {
        // Given
        Permission p1 = Permission.builder().code("INVENTORY_READ").build();
        Permission p2 = Permission.builder().code("POS_READ").build();
        when(permissionRepository.findAllByUserId(USER_ID)).thenReturn(Set.of(p1, p2));

        // When
        Set<String> result = permissionCacheService.getUserPermissions(USER_ID);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains("INVENTORY_READ"));
        assertTrue(result.contains("POS_READ"));
        verify(permissionRepository, times(1)).findAllByUserId(USER_ID);

        // Verify cache was populated
        assertNotNull(cacheManager.getCache(CACHE_NAME).get(USER_ID));
    }

    @Test
    void getPermissions_cacheHit_returnsCachedValueWithoutDbCall() {
        // Given — prime the cache
        Permission p1 = Permission.builder().code("INVENTORY_READ").build();
        when(permissionRepository.findAllByUserId(USER_ID)).thenReturn(Set.of(p1));

        // First call — populates cache
        permissionCacheService.getUserPermissions(USER_ID);
        verify(permissionRepository, times(1)).findAllByUserId(USER_ID);

        // When — second call should hit cache
        Set<String> result = permissionCacheService.getUserPermissions(USER_ID);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.contains("INVENTORY_READ"));
        // Repository not called again — still 1 total invocation
        verify(permissionRepository, times(1)).findAllByUserId(USER_ID);
    }

    @Test
    void evictUserPermissions_removesEntryAndForcesDbOnNextCall() {
        // Given — prime the cache
        Permission p1 = Permission.builder().code("INVENTORY_READ").build();
        when(permissionRepository.findAllByUserId(USER_ID)).thenReturn(Set.of(p1));

        permissionCacheService.getUserPermissions(USER_ID);
        assertNotNull(cacheManager.getCache(CACHE_NAME).get(USER_ID));

        // When
        permissionCacheService.evictUserPermissions(USER_ID);

        // Then — cache entry removed
        assertNull(cacheManager.getCache(CACHE_NAME).get(USER_ID));

        // Subsequent call triggers a fresh DB query
        permissionCacheService.getUserPermissions(USER_ID);
        verify(permissionRepository, times(2)).findAllByUserId(USER_ID);
    }
}
