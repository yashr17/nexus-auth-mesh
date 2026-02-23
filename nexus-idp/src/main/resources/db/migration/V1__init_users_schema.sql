-- V1: Initial Schema for Identity & Access Management

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Insert the default baseline roles so they exist immediately upon startup
INSERT INTO roles (id, name, description) 
VALUES 
    ('018e47b1-4b1a-7b5a-9b1a-000000000001', 'ROLE_USER', 'Standard authenticated user'),
    ('018e47b1-4b1a-7b5a-9b1a-000000000002', 'ROLE_ADMIN', 'System administrator with full access');

-- Insert a dummy admin user for testing (Password: password123)
-- The hash below is the BCrypt representation of 'password123' with a cost factor of 10
INSERT INTO users (id, email, password_hash, is_locked) 
VALUES 
    ('018e47b1-4b1a-7b5a-9b1a-000000000003', 'admin@nexus.com', '$2a$10$rGJfll8cbIbXEQNarHagsOmXPNoaSzombhnsCVHGyhZJiFjzWZCj6', FALSE);

-- Map the admin user to both ROLE_USER and ROLE_ADMIN using the hardcoded UUIDs
INSERT INTO user_roles (user_id, role_id) 
VALUES 
    ('018e47b1-4b1a-7b5a-9b1a-000000000003', '018e47b1-4b1a-7b5a-9b1a-000000000001'), -- ROLE_USER
    ('018e47b1-4b1a-7b5a-9b1a-000000000003', '018e47b1-4b1a-7b5a-9b1a-000000000002'); -- ROLE_ADMIN