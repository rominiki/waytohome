package com.rominiki.waytohome.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_conversation_listing_student_landlord",
                columnNames = {"listing_id", "student_id", "landlord_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean hasParticipant(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }

        return user.getId().equals(student.getId()) || user.getId().equals(landlord.getId());
    }


    public User otherParticipant(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (user.getId().equals(student.getId())) {
            return landlord;
        }

        if (user.getId().equals(landlord.getId())) {
            return student;
        }

        throw new IllegalArgumentException("User is not a participant in this conversation");
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}