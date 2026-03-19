-- ===============================================
-- V006__add_cover_image_url_tb_book.sql
-- Adiciona colunas cover_image_url na tabela book
-- ===============================================

-- ============================
-- BOOK
-- ============================

ALTER TABLE if EXISTS tb_book 
ADD COLUMN cover_image_url varchar(255);

CREATE INDEX idx_book_cover_image_url 
ON tb_book(cover_image_url);
