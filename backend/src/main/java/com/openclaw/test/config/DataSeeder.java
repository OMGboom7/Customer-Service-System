package com.openclaw.test.config;

import com.openclaw.test.entity.User;
import com.openclaw.test.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("用户表已有数据，跳过预置账号创建");
            return;
        }

        log.info("创建预置账号...");

        userRepository.save(new User("admin", passwordEncoder.encode("admin123"), "管理员", "admin"));

        for (int i = 1; i <= 9; i++) {
            String username = "user" + i;
            String displayName = "用户" + i;
            userRepository.save(new User(username, passwordEncoder.encode("user123"), displayName, "user"));
        }

        log.info("已创建 10 个预置账号（admin + user1~user9）");
    }
}
