CREATE TABLE listings (
      id BIGSERIAL PRIMARY KEY,
      title VARCHAR(255) NOT NULL,
      description TEXT,
      price NUMERIC(10,2) NOT NULL,
      location VARCHAR(255) NOT NULL,
      bedrooms INT NOT NULL,
      pet_friendly BOOLEAN NOT NULL DEFAULT FALSE,
      accessible BOOLEAN NOT NULL DEFAULT FALSE,
      status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
      owner_id BIGINT NOT NULL REFERENCES users(id),
      created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_listings_owner ON listings(owner_id);
CREATE INDEX idx_listings_status ON listings(status);