package com.hisobnoma.platform.auth.service;

import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.PermissionRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseGet(() -> userRepository.findByPhoneWithRolesAndPermissions(username)
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "User not found with username or phone: " + username)));

        return UserPrincipal.create(user, getAllPermissionCodesIfGodAccess(user));
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return UserPrincipal.create(user, getAllPermissionCodesIfGodAccess(user));
    }

    private Set<String> getAllPermissionCodesIfGodAccess(User user) {
        boolean hasGodAccess = user.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equals(role.getCode()) || "ADMIN".equals(role.getCode()));
        if (!hasGodAccess) {
            return null;
        }
        return permissionRepository.findAll().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }
}
