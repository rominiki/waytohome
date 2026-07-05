package com.rominiki.waytohome.repository;

import com.rominiki.waytohome.entity.Conversation;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByListingAndStudentAndLandlord(Listing listing, User student, User landlord);
    List<Conversation> findByStudentOrLandlordOrderByUpdatedAtDesc(User student, User landlord);
}