package com.rominiki.waytohome.repository;

import com.rominiki.waytohome.entity.ChatMessage;
import com.rominiki.waytohome.entity.Conversation;
import com.rominiki.waytohome.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationOrderByCreatedAtAsc(Conversation conversation);
    Page<ChatMessage> findByConversationOrderByCreatedAtAsc(Conversation conversation,Pageable pageable);
    List<ChatMessage> findByRecipientAndReadFalse(User recipient);
}