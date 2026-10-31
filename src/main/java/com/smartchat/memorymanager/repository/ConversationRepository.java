package com.smartchat.memorymanager.repository;

import com.smartchat.memorymanager.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findAllByOwnerUsernameOrderByUpdatedAtDesc(String username);
    Optional<Conversation> findByIdAndOwnerUsername(Long id, String username);
}
