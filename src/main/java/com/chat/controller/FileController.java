package com.chat.controller;

import com.chat.dto.AttachmentDTO;
import com.chat.entity.Attachment;
import com.chat.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<AttachmentDTO> uploadFile(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("messageId") Long messageId) throws IOException {
        Attachment attachment = fileService.saveFile(file, messageId);
        
        // Преобразуем в DTO чтобы избежать циклических ссылок
        AttachmentDTO dto = new AttachmentDTO();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setFileSize(attachment.getFileSize());
        dto.setMimeType(attachment.getFileType());
        dto.setFileUrl("/api/files/" + attachment.getId());
        
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long attachmentId) throws IOException {
        Attachment attachment = fileService.getAttachment(attachmentId);
        byte[] data = fileService.loadFile(attachmentId);
        
        // Определяем MIME-тип
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String disposition = "attachment";
        
        if (attachment.getFileType() != null) {
            try {
                mediaType = MediaType.parseMediaType(attachment.getFileType());
                // Для изображений и видео показываем inline (в браузере)
                if (attachment.getFileType().startsWith("image/") || 
                    attachment.getFileType().startsWith("video/")) {
                    disposition = "inline";
                }
            } catch (Exception e) {
                // Если не можем распарсить, используем default
            }
        }
        
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + attachment.getFileName() + "\"")
                .body(data);
    }
}




