package com.chat.dto;

import com.chat.entity.Chat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatDTO {
    private Long id;
    private String name;
    private Chat.ChatType type;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private List<Long> memberIds;
}




