CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,

    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id),
    landlord_id BIGINT NOT NULL REFERENCES users(id),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_conversation_listing_student_landlord
       UNIQUE (listing_id, student_id, landlord_id)
);

CREATE INDEX idx_conversations_student ON conversations(student_id);
CREATE INDEX idx_conversations_landlord ON conversations(landlord_id);
CREATE INDEX idx_conversations_listing ON conversations(listing_id);
CREATE INDEX idx_conversations_updated_at ON conversations(updated_at);


CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,

    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    recipient_id BIGINT NOT NULL REFERENCES users(id),

    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_conversation ON chat_messages(conversation_id);
CREATE INDEX idx_chat_messages_sender ON chat_messages(sender_id);
CREATE INDEX idx_chat_messages_recipient ON chat_messages(recipient_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);