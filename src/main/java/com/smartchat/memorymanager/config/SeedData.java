package com.smartchat.memorymanager.config;

import com.smartchat.memorymanager.model.AppUser;
import com.smartchat.memorymanager.model.Memory;
import com.smartchat.memorymanager.model.MemoryType;
import com.smartchat.memorymanager.repository.MemoryRepository;
import com.smartchat.memorymanager.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class SeedData {
    public static final String DEMO_USERNAME = "demo_user";

    @Bean
    CommandLineRunner seedDemoAccount(UserRepository users, MemoryRepository memories, PasswordEncoder passwordEncoder) {
        return args -> {
            if (users.existsByUsername(DEMO_USERNAME)) return;

            AppUser demoUser = users.save(new AppUser(DEMO_USERNAME, passwordEncoder.encode("demo12345")));
            memories.saveAll(List.of(
                    new Memory(MemoryType.SEMANTIC, "User is building a React + Firebase mess management system.", 2.0, demoUser),
                    new Memory(MemoryType.SEMANTIC, "User knows JavaScript.", 1.5, demoUser),
                    new Memory(MemoryType.SEMANTIC, "User prefers a simple UI.", 1.0, demoUser),
                    new Memory(MemoryType.EPISODIC, "User participated in a hackathon.", 1.0, demoUser),
                    new Memory(MemoryType.EPISODIC, "User completed the login module.", 2.0, demoUser),
                    new Memory(MemoryType.WORKING, "User previously asked how authentication should be implemented.", 2.5, demoUser)
            ));
        };
    }
}
