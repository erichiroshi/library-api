-- ============================
-- Alter table: tb_user
-- ============================
ALTER TABLE tb_user
ADD COLUMN password VARCHAR(255) NOT NULL;

-- ============================
-- Table: tb_user_roles
-- ============================
CREATE TABLE tb_user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,

    CONSTRAINT pk_user_roles
        PRIMARY KEY (user_id, role),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES tb_user (id)
        ON DELETE CASCADE
);