package com.chat.dto;

import lombok.Data;

@Data
public class AttachmentDTO {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String fileUrl;
    private String mimeType;
}

