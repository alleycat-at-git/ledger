CREATE TABLE IF NOT EXISTS users (
    id SERIAL,
    uuid UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(26) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

SELECT manage_updated_at('users');
CREATE UNIQUE INDEX users_uuid_idx ON users (uuid);
CREATE INDEX users_email_idx ON users (email);
