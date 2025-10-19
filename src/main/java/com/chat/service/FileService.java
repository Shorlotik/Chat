package com.chat.service;

import com.chat.dto.MessageDTO;
import com.chat.entity.Attachment;
import com.chat.entity.Message;
import com.chat.repository.AttachmentRepository;
import com.chat.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Attachment saveFile(MultipartFile file, Long messageId) throws IOException {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        Attachment attachment = new Attachment();
        attachment.setMessage(message);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFilePath(filePath.toString());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());

        attachment = attachmentRepository.save(attachment);
        
        // Перезагружаем сообщение с вложениями и отправляем через WebSocket
        message = messageRepository.findByIdWithAttachments(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        MessageDTO messageDTO = messageService.convertMessageToDTO(message);
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(), messageDTO);
        
        return attachment;
    }

    public Attachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
    }

    public byte[] loadFile(Long attachmentId) throws IOException {
        Attachment attachment = getAttachment(attachmentId);
        Path filePath = Paths.get(attachment.getFilePath());
        return Files.readAllBytes(filePath);
    }
}




