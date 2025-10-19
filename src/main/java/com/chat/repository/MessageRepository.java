package com.chat.repository;

import com.chat.entity.Chat;
import com.chat.entity.Message;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatOrderBySentAtDesc(Chat chat);
    
    @EntityGraph(attributePaths = {"attachments"})
    List<Message> findByChatIdOrderBySentAtDesc(Long chatId);
    
    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.id = :id")
    Optional<Message> findByIdWithAttachments(@Param("id") Long id);
}


