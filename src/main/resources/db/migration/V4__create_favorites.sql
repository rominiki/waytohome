CREATE TABLE favorites (
   id BIGSERIAL PRIMARY KEY,
   user_id BIGINT NOT NULL REFERENCES users(id),
   listing_id BIGINT NOT NULL REFERENCES listings(id),
   created_at TIMESTAMP NOT NULL DEFAULT NOW(),
   CONSTRAINT uq_favorites_user_listing
       UNIQUE (user_id, listing_id)
);
CREATE INDEX idx_favorites_user ON favorites(user_id);