package com.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String encryptedContent; // Зашифрованный текст

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type; // TEXT, IMAGE, GIF, VOICE, LINK

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean isRead = false;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private Set<Attachment> attachments = new HashSet<>();
    
    // Ответ на сообщение
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id", nullable = true)
    private Message replyToMessage;
    
    // Пересланное сообщение
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarded_from_id", nullable = true)
    private Message forwardedFrom;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarded_from_user_id", nullable = true)
    private User forwardedFromUser;
    
    // Редактирование
    @Column(nullable = true)
    private LocalDateTime editedAt;
    
    // Удаление
    @Column(nullable = false)
    private Boolean isDeleted = false;

    public enum MessageType {
        TEXT, IMAGE, VIDEO, GIF, VOICE, DOCUMENT, LINK
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


