-- ===============================================
-- V005__add_audit_and_version_columns.sql
-- Adiciona colunas de auditoria e controle otimista
-- ===============================================

-- ============================
-- AUTHOR
-- ============================

ALTER TABLE IF EXISTS tb_author
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================
-- BOOK
-- ============================

ALTER TABLE IF EXISTS tb_book
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================
-- CATEGORY
-- ============================

ALTER TABLE IF EXISTS tb_category
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================
-- LOAN
-- ============================

ALTER TABLE IF EXISTS tb_loan
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================
-- LOAN ITEM
-- ============================

ALTER TABLE IF EXISTS tb_loan_item
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS version BIGINT  DEFAULT 0;

-- ============================
-- USER
-- ============================

ALTER TABLE IF EXISTS tb_user
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS version BIGINT  DEFAULT 0;
