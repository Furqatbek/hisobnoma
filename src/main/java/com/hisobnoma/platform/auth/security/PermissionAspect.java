package com.hisobnoma.platform.auth.security;

import com.hisobnoma.platform.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final SecurityContextHelper securityContextHelper;

    @Around("@annotation(com.hisobnoma.platform.auth.security.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        String[] requiredPermissions = annotation.value();
        boolean anyOf = annotation.anyOf();

        UserPrincipal user = securityContextHelper.getRequiredCurrentUser();

        boolean hasAccess;
        if (anyOf) {
            hasAccess = hasAnyPermission(user, requiredPermissions);
        } else {
            hasAccess = hasAllPermissions(user, requiredPermissions);
        }

        if (!hasAccess) {
            log.warn("User {} denied access to {} - missing permissions: {}",
                    user.getUsername(), method.getName(), String.join(", ", requiredPermissions));
            throw new ForbiddenException("You don't have permission to perform this action");
        }

        return joinPoint.proceed();
    }

    private boolean hasAnyPermission(UserPrincipal user, String[] permissions) {
        for (String permission : permissions) {
            if (user.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAllPermissions(UserPrincipal user, String[] permissions) {
        for (String permission : permissions) {
            if (!user.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }
}
