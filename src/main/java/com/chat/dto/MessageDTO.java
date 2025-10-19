package com.chat.dto;

import com.chat.entity.Message;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageDTO {
    private Long id;
    private Long chatId;
    private Long senderId;
    private String senderName;
    private String content; // Расшифрованный контент
    private Message.MessageType type;
    private LocalDateTime sentAt;
    private Boolean isRead;
    
    // Вложения
    private List<AttachmentDTO> attachments;
    
    // Ответ на сообщение
    private Long replyToMessageId;
    private MessagePreviewDTO replyToMessage;
    
    // Пересылка
    private Long forwardedFromId;
    private String forwardedFromUser;
    
    // Редактирование
    private LocalDateTime editedAt;
    
    // Удаление
    private Boolean isDeleted;
}





