package com.hpc.jobagent.service;

import java.time.LocalDateTime;
import java.util.Locale;

import com.hpc.jobagent.domain.UserAccount;
import com.hpc.jobagent.dto.AuthRequest;
import com.hpc.jobagent.dto.AuthResponse;
import com.hpc.jobagent.mapper.UserAccountMapper;
import com.hpc.jobagent.security.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserDetailsService {

    private final UserAccountMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(AuthRequest request) {
        String username = normalizeUsername(request.username());
        if (userMapper.findByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(blankToNull(request.displayName()));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return response(new UserPrincipal(user));
    }

    public AuthResponse response(UserPrincipal principal) {
        return new AuthResponse(true, principal.id(), principal.getUsername(), principal.displayName());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userMapper.findByUsername(normalizeUsername(username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return new UserPrincipal(user);
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        String value = username.strip().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z0-9_@.\\-]{3,40}")) {
            throw new IllegalArgumentException("用户名只能包含字母、数字、下划线、点、横线或 @");
        }
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
