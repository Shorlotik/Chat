package com.chat.dto;

import com.chat.entity.Chat;
import lombok.Data;
import java.util.List;

@Data
public class CreateChatRequest {
    private String name;
    private Chat.ChatType type;
    private List<Long> memberIds;
}




