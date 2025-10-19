package com.chat.controller;

import com.chat.dto.MessageDTO;
import com.chat.dto.SendMessageRequest;
import com.chat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.chat.repository.UserRepository;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody SendMessageRequest request,
                                                   Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(messageService.sendMessage(request, userId));
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<MessageDTO>> getChatMessages(@PathVariable Long chatId) {
        return ResponseEntity.ok(messageService.getChatMessages(chatId));
    }
    
    @PutMapping("/{messageId}")
    public ResponseEntity<MessageDTO> editMessage(@PathVariable Long messageId,
                                                    @RequestBody MessageEditRequest request,
                                                    Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(messageService.editMessage(messageId, request.getContent(), userId));
    }
    
    @DeleteMapping("/{messageId}")
    public ResponseEntity<MessageDTO> deleteMessage(@PathVariable Long messageId,
                                                      Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(messageService.deleteMessage(messageId, userId));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}

class MessageEditRequest {
    private String content;
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
}

