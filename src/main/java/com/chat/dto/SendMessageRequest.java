package com.chat.dto;

import com.chat.entity.Message;
import lombok.Data;

@Data
public class SendMessageRequest {
    private Long chatId;
    private String content;
    private Message.MessageType type;
    private Long replyToMessageId; // ID сообщения, на которое отвечаем
    private Long forwardFromMessageId; // ID сообщения, которое пересылаем
}




