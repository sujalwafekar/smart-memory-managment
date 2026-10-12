package com.smartchat.memorymanager.config;

import com.smartchat.memorymanager.model.Memory;
import com.smartchat.memorymanager.model.MemoryType;
import com.smartchat.memorymanager.repository.MemoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class SeedData {
    @Bean
    CommandLineRunner seedMemories(MemoryRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                        new Memory(MemoryType.SEMANTIC, "User is building a React + Firebase mess management system.", 2.0),
                        new Memory(MemoryType.SEMANTIC, "User knows JavaScript.", 1.5),
                        new Memory(MemoryType.SEMANTIC, "User prefers a simple UI.", 1.0),
                        new Memory(MemoryType.EPISODIC, "User participated in a hackathon.", 1.0),
                        new Memory(MemoryType.EPISODIC, "User completed the login module.", 2.0),
                        new Memory(MemoryType.WORKING, "User previously asked how authentication should be implemented.", 2.5)
                ));
            }
        };
    }
}
