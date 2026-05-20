package com.openclaw.test.controller;

import com.openclaw.test.dto.AuthResponse;
import com.openclaw.test.dto.LoginRequest;
import com.openclaw.test.dto.RegisterRequest;
import com.openclaw.test.entity.User;
import com.openclaw.test.repository.UserRepository;
import com.openclaw.test.config.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "用户认证", description = "用户注册、登录与管理员管理接口")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    private boolean isAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return false;
        return userRepository.findById(userId)
            .map(u -> "admin".equals(u.getRole()))
            .orElse(false);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名不能为空"));
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "密码至少6位"));
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名已存在"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());
        user.setRole("user");
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());

        return ResponseEntity.ok(new AuthResponse(token, userInfo));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名或密码错误"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名或密码错误"));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());

        return ResponseEntity.ok(new AuthResponse(token, userInfo));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
        }

        User user = userOpt.get();
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());

        return ResponseEntity.ok(Map.of("user", userInfo));
    }

    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "管理员获取所有用户列表")
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可查看"));
        }
        List<User> users = userRepository.findAll();
        var result = users.stream().map(u -> Map.of(
            "id", u.getId(),
            "username", u.getUsername(),
            "displayName", u.getDisplayName() != null ? u.getDisplayName() : "",
            "role", u.getRole(),
            "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        )).collect(Collectors.toList());
        log.info("Admin {} listed all users ({} total)", request.getAttribute("username"), result.size());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "删除用户", description = "管理员删除指定用户")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可操作"));
        }
        var userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User targetUser = userOpt.get();
        // Prevent admin from deleting themselves
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId != null && currentUserId.equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "不能删除自己的账号"));
        }
        userRepository.deleteById(id);
        log.info("Admin {} deleted user {} (id={}, role={})",
            request.getAttribute("username"), targetUser.getUsername(), id, targetUser.getRole());
        return ResponseEntity.ok(Map.of("message", "删除成功", "userId", id));
    }
}
