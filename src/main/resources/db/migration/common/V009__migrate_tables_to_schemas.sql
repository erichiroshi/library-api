-- =============================================
-- Migração das tabelas para seus schemas
-- =============================================

-- AUTH
ALTER TABLE tb_user           SET SCHEMA auth;
ALTER TABLE tb_user_roles     SET SCHEMA auth;
ALTER TABLE tb_refresh_tokens SET SCHEMA auth;

-- CATALOG
ALTER TABLE tb_book        SET SCHEMA catalog;
ALTER TABLE tb_author      SET SCHEMA catalog;
ALTER TABLE tb_category    SET SCHEMA catalog;
ALTER TABLE tb_book_author SET SCHEMA catalog;

-- LENDING
ALTER TABLE tb_loan      SET SCHEMA lending;
ALTER TABLE tb_loan_item SET SCHEMA lending;

-- SHEDLOCK permanece em public (infraestrutura, não domínio)