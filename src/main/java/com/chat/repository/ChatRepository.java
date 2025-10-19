package com.chat.repository;

import com.chat.entity.Chat;
import com.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    @EntityGraph(attributePaths = {"members"})
    @Query("SELECT c FROM Chat c JOIN c.members m WHERE m = :user")
    List<Chat> findAllByMember(User user);
    
    @EntityGraph(attributePaths = {"members"})
    @Query("SELECT c FROM Chat c WHERE c.id = :id")
    Optional<Chat> findByIdWithMembers(Long id);
}


