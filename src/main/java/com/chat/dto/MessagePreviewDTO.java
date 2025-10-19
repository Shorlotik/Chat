package com.chat.dto;

import com.chat.entity.Message;
import lombok.Data;

@Data
public class MessagePreviewDTO {
    private Long id;
    private String senderName;
    private String content;
    private Message.MessageType type;
}

