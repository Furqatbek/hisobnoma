package com.hisobnoma.platform.auth.service;

import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        }
)
@ActiveProfiles("test")
@Import(PermissionCacheServiceTest.TestCacheConfig.class)
class PermissionCacheServiceTest {

    @TestConfiguration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("user-permissions");
        }
    }

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
        assertNotNull(cacheManager.getCache(CACHE_NAME).get(USER_ID));
    }

    @Test
    void getPermissions_cacheHit_returnsCachedValueWithoutDbCall() {
        // Given
        Permission p1 = Permission.builder().code("INVENTORY_READ").build();
        when(permissionRepository.findAllByUserId(USER_ID)).thenReturn(Set.of(p1));

        permissionCacheService.getUserPermissions(USER_ID);
        verify(permissionRepository, times(1)).findAllByUserId(USER_ID);

        // When
        Set<String> result = permissionCacheService.getUserPermissions(USER_ID);

        // Then
        assertEquals(1, result.size());
        verify(permissionRepository, times(1)).findAllByUserId(USER_ID);
    }

    @Test
    void evictUserPermissions_removesEntryAndForcesDbOnNextCall() {
        // Given
        Permission p1 = Permission.builder().code("INVENTORY_READ").build();
        when(permissionRepository.findAllByUserId(USER_ID)).thenReturn(Set.of(p1));

        permissionCacheService.getUserPermissions(USER_ID);
        assertNotNull(cacheManager.getCache(CACHE_NAME).get(USER_ID));

        // When
        permissionCacheService.evictUserPermissions(USER_ID);

        // Then
        assertNull(cacheManager.getCache(CACHE_NAME).get(USER_ID));
        permissionCacheService.getUserPermissions(USER_ID);
        verify(permissionRepository, times(2)).findAllByUserId(USER_ID);
    }
}
