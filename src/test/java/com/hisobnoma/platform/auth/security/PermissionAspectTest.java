package com.hisobnoma.platform.auth.security;

import com.hisobnoma.platform.common.exception.ForbiddenException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionAspectTest {

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private PermissionAspect permissionAspect;

    private UserPrincipal userWithPermissions;
    private UserPrincipal userWithoutPermissions;

    @BeforeEach
    void setUp() {
        userWithPermissions = new UserPrincipal(
                1L,
                "admin",
                "password",
                1L,
                true,
                true,
                Set.of(
                        new SimpleGrantedAuthority("ADMIN_USER_MANAGE"),
                        new SimpleGrantedAuthority("ADMIN_ROLE_MANAGE"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        );

        userWithoutPermissions = new UserPrincipal(
                2L,
                "viewer",
                "password",
                1L,
                true,
                true,
                Set.of(new SimpleGrantedAuthority("ROLE_VIEWER"))
        );
    }

    @Test
    void shouldAllowAccessWhenUserHasRequiredPermission() throws Throwable {
        // Given
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getAnnotatedMethod("methodWithSinglePermission"));
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userWithPermissions);
        when(joinPoint.proceed()).thenReturn("success");

        // When
        Object result = permissionAspect.checkPermission(joinPoint);

        // Then
        assertEquals("success", result);
        verify(joinPoint).proceed();
    }

    @Test
    void shouldDenyAccessWhenUserLacksPermission() throws Throwable {
        // Given
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getAnnotatedMethod("methodWithSinglePermission"));
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userWithoutPermissions);

        // When/Then
        assertThrows(ForbiddenException.class, () -> permissionAspect.checkPermission(joinPoint));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void shouldAllowAccessWhenUserHasAnyOfRequiredPermissions() throws Throwable {
        // Given
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getAnnotatedMethod("methodWithAnyOfPermissions"));
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userWithPermissions);
        when(joinPoint.proceed()).thenReturn("success");

        // When
        Object result = permissionAspect.checkPermission(joinPoint);

        // Then
        assertEquals("success", result);
    }

    @Test
    void shouldRequireAllPermissionsWhenAnyOfIsFalse() throws Throwable {
        // Given - user has ADMIN_USER_MANAGE but not FINANCE_MANAGE
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getAnnotatedMethod("methodWithAllPermissions"));
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userWithPermissions);

        // When/Then
        assertThrows(ForbiddenException.class, () -> permissionAspect.checkPermission(joinPoint));
    }

    // Helper methods for test annotations
    @RequiresPermission("ADMIN_USER_MANAGE")
    public void methodWithSinglePermission() {}

    @RequiresPermission(value = {"ADMIN_USER_MANAGE", "FINANCE_MANAGE"}, anyOf = true)
    public void methodWithAnyOfPermissions() {}

    @RequiresPermission(value = {"ADMIN_USER_MANAGE", "FINANCE_MANAGE"}, anyOf = false)
    public void methodWithAllPermissions() {}

    private Method getAnnotatedMethod(String methodName) throws NoSuchMethodException {
        return this.getClass().getMethod(methodName);
    }
}
