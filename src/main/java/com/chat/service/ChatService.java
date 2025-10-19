package com.chat.service;

import com.chat.dto.ChatDTO;
import com.chat.dto.CreateChatRequest;
import com.chat.entity.Chat;
import com.chat.entity.User;
import com.chat.repository.ChatRepository;
import com.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    public ChatDTO createChat(CreateChatRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        Chat chat = new Chat();
        chat.setName(request.getName());
        chat.setType(request.getType());
        chat.setCreator(creator);

        Set<User> members = new HashSet<>();
        members.add(creator);
        
        for (Long memberId : request.getMemberIds()) {
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));
            members.add(member);
        }
        
        chat.setMembers(members);
        chat = chatRepository.save(chat);
        
        return convertToDTO(chat);
    }

    public List<ChatDTO> getUserChats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return chatRepository.findAllByMember(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ChatDTO getChatById(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        return convertToDTO(chat);
    }

    private ChatDTO convertToDTO(Chat chat) {
        ChatDTO dto = new ChatDTO();
        dto.setId(chat.getId());
        dto.setName(chat.getName());
        dto.setType(chat.getType());
        dto.setAvatarUrl(chat.getAvatarUrl());
        dto.setCreatedAt(chat.getCreatedAt());
        
        try {
            List<Long> memberIds = chat.getMembers().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            logger.debug("Chat {} members: {}", chat.getId(), memberIds);
            dto.setMemberIds(memberIds);
        } catch (Exception e) {
            logger.error("Error loading members for chat {}: {}", chat.getId(), e.getMessage());
            dto.setMemberIds(List.of());
        }
        
        return dto;
    }
}

