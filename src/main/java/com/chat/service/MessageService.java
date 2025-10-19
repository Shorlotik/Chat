package com.chat.service;

import com.chat.dto.AttachmentDTO;
import com.chat.dto.MessageDTO;
import com.chat.dto.MessagePreviewDTO;
import com.chat.dto.SendMessageRequest;
import com.chat.entity.Attachment;
import com.chat.entity.Chat;
import com.chat.entity.Message;
import com.chat.entity.User;
import com.chat.repository.ChatRepository;
import com.chat.repository.MessageRepository;
import com.chat.repository.UserRepository;
import com.chat.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public MessageDTO sendMessage(SendMessageRequest request, Long senderId) {
        Chat chat = chatRepository.findByIdWithMembers(request.getChatId())
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        // Проверка, что отправитель является членом чата
        if (!chat.getMembers().contains(sender)) {
            throw new RuntimeException("User is not a member of this chat");
        }

        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setType(request.getType());
        
        // Шифрование контента
        if (request.getContent() != null && !request.getContent().isEmpty()) {
            String encryptedContent = encryptionUtil.encrypt(request.getContent());
            message.setEncryptedContent(encryptedContent);
        }
        
        // Обработка ответа на сообщение
        if (request.getReplyToMessageId() != null) {
            Message replyTo = messageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new RuntimeException("Reply message not found"));
            message.setReplyToMessage(replyTo);
        }
        
        // Обработка пересылки сообщения
        if (request.getForwardFromMessageId() != null) {
            Message forwardFrom = messageRepository.findById(request.getForwardFromMessageId())
                    .orElseThrow(() -> new RuntimeException("Forward message not found"));
            message.setForwardedFrom(forwardFrom);
            message.setForwardedFromUser(forwardFrom.getSender());
        }

        message = messageRepository.save(message);
        
        MessageDTO messageDTO = convertToDTO(message);
        
        // Отправка через WebSocket всем участникам чата
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getId(), messageDTO);
        
        return messageDTO;
    }

    public List<MessageDTO> getChatMessages(Long chatId) {
        return messageRepository.findByChatIdOrderBySentAtDesc(chatId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public MessageDTO editMessage(Long messageId, String newContent, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        // Проверка, что пользователь - автор сообщения
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("You can only edit your own messages");
        }
        
        // Проверка, что сообщение не удалено
        if (message.getIsDeleted()) {
            throw new RuntimeException("Cannot edit deleted message");
        }
        
        // Шифрование нового контента
        String encryptedContent = encryptionUtil.encrypt(newContent);
        message.setEncryptedContent(encryptedContent);
        message.setEditedAt(LocalDateTime.now());
        
        message = messageRepository.save(message);
        
        MessageDTO messageDTO = convertToDTO(message);
        
        // Отправка обновления через WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(), messageDTO);
        
        return messageDTO;
    }
    
    public MessageDTO deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        // Проверка, что пользователь - автор сообщения
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own messages");
        }
        
        // Мягкое удаление
        message.setIsDeleted(true);
        message.setEncryptedContent(encryptionUtil.encrypt("Сообщение удалено"));
        
        message = messageRepository.save(message);
        
        MessageDTO messageDTO = convertToDTO(message);
        
        // Отправка обновления через WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(), messageDTO);
        
        return messageDTO;
    }

    public MessageDTO convertMessageToDTO(Message message) {
        return convertToDTO(message);
    }
    
    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setChatId(message.getChat().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getDisplayName() != null ? 
                message.getSender().getDisplayName() : message.getSender().getUsername());
        
        // Расшифровка контента
        if (message.getEncryptedContent() != null && !message.getEncryptedContent().isEmpty()) {
            String decryptedContent = encryptionUtil.decrypt(message.getEncryptedContent());
            dto.setContent(decryptedContent);
        }
        
        dto.setType(message.getType());
        dto.setSentAt(message.getSentAt());
        dto.setIsRead(message.getIsRead());
        dto.setIsDeleted(message.getIsDeleted());
        dto.setEditedAt(message.getEditedAt());
        
        // Ответ на сообщение
        if (message.getReplyToMessage() != null) {
            Message replyTo = message.getReplyToMessage();
            dto.setReplyToMessageId(replyTo.getId());
            
            // Создаем preview для реплая
            MessagePreviewDTO preview = new MessagePreviewDTO();
            preview.setId(replyTo.getId());
            preview.setSenderName(replyTo.getSender().getDisplayName() != null ?
                    replyTo.getSender().getDisplayName() : replyTo.getSender().getUsername());
            
            // Расшифровываем контент для превью
            if (replyTo.getEncryptedContent() != null && !replyTo.getEncryptedContent().isEmpty()) {
                String decryptedContent = encryptionUtil.decrypt(replyTo.getEncryptedContent());
                // Обрезаем длинные сообщения для превью
                if (decryptedContent.length() > 100) {
                    decryptedContent = decryptedContent.substring(0, 100) + "...";
                }
                preview.setContent(decryptedContent);
            }
            preview.setType(replyTo.getType());
            
            dto.setReplyToMessage(preview);
        }
        
        // Пересылка
        if (message.getForwardedFrom() != null) {
            dto.setForwardedFromId(message.getForwardedFrom().getId());
            if (message.getForwardedFromUser() != null) {
                dto.setForwardedFromUser(message.getForwardedFromUser().getDisplayName() != null ?
                        message.getForwardedFromUser().getDisplayName() : 
                        message.getForwardedFromUser().getUsername());
            }
        }
        
        // Вложения
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            dto.setAttachments(message.getAttachments().stream()
                    .map(this::convertAttachmentToDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private AttachmentDTO convertAttachmentToDTO(Attachment attachment) {
        AttachmentDTO dto = new AttachmentDTO();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setFileSize(attachment.getFileSize());
        dto.setMimeType(attachment.getFileType());
        // Формируем URL для скачивания файла
        dto.setFileUrl("/api/files/" + attachment.getId());
        return dto;
    }
}

