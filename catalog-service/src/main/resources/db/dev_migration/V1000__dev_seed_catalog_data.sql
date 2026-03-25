-- ============================================================
-- DEV SEED — Flyway managed (perfil dev)
-- Executado automaticamente ao subir o ambiente de desenvolvimento.
-- ============================================================

-- ============================
-- Categorias
-- ============================
INSERT INTO tb_category (name) VALUES ('Tecnologia');
INSERT INTO tb_category (name) VALUES ('Literatura');
INSERT INTO tb_category (name) VALUES ('História');
INSERT INTO tb_category (name) VALUES ('Ciência');
INSERT INTO tb_category (name) VALUES ('Filosofia');
INSERT INTO tb_category (name) VALUES ('Fantasia');
INSERT INTO tb_category (name) VALUES ('Negócios');

-- ============================
-- Autores
-- ============================
INSERT INTO tb_author (name, biography) VALUES ('Robert C. Martin', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Joshua Bloch', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('George Orwell', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('J.K. Rowling', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Stephen King', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Yuval Noah Harari', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Sun Tzu', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Adam Smith', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Daniel Kahneman', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Carl Sagan', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Isaac Asimov', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Jane Austen', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Ernest Hemingway', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Fiódor Dostoiévski', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Machado de Assis', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Clarice Lispector', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Neil Gaiman', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Brandon Sanderson', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Malcolm Gladwell', 'Autor reconhecido internacionalmente.');
INSERT INTO tb_author (name, biography) VALUES ('Nassim Taleb', 'Autor reconhecido internacionalmente.');

-- ============================
-- Livros (Realistas)
-- ============================
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Clean Code', '9780000000001', 2008, 7, 1);
INSERT INTO tb_book_author (book_id, author_id) VALUES (1, 17);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Effective Java', '9780000000002', 2018, 7, 1);
INSERT INTO tb_book_author (book_id, author_id) VALUES (2, 18);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('1984', '9780000000003', 1949, 13, 2);
INSERT INTO tb_book_author (book_id, author_id) VALUES (3, 9);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Animal Farm', '9780000000004', 1945, 11, 2);
INSERT INTO tb_book_author (book_id, author_id) VALUES (4, 16);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Harry Potter and the Sorcerers Stone', '9780000000005', 1997, 1, 6);
INSERT INTO tb_book_author (book_id, author_id) VALUES (5, 19);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('The Shining', '9780000000006', 1977, 10, 2);
INSERT INTO tb_book_author (book_id, author_id) VALUES (6, 8);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Sapiens', '9780000000007', 2011, 10, 3);
INSERT INTO tb_book_author (book_id, author_id) VALUES (7, 9);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('The Art of War', '9780000000008', -500, 3, 3);
INSERT INTO tb_book_author (book_id, author_id) VALUES (8, 8);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('The Wealth of Nations', '9780000000009', 1776, 12, 7);
INSERT INTO tb_book_author (book_id, author_id) VALUES (9, 20);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Thinking, Fast and Slow', '9780000000010', 2011, 2, 4);
INSERT INTO tb_book_author (book_id, author_id) VALUES (10, 7);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Cosmos', '9780000000011', 1980, 3, 4);
INSERT INTO tb_book_author (book_id, author_id) VALUES (11, 18);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Foundation', '9780000000012', 1951, 2, 6);
INSERT INTO tb_book_author (book_id, author_id) VALUES (12, 16);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Pride and Prejudice', '9780000000013', 1813, 2, 2);
INSERT INTO tb_book_author (book_id, author_id) VALUES (13, 12);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('The Old Man and the Sea', '9780000000014', 1952, 5, 2);
INSERT INTO tb_book_author (book_id, author_id) VALUES (14, 2);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Crime and Punishment', '9780000000015', 1866, 5, 2);
INSERT INTO tb_book_author (book_id, author_id) VALUES (15, 3);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Dom Casmurro', '9780000000016', 1899, 7, 2);
INSERT INTO tb_book_author (book_id, author_id) VALUES (16, 7);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('A Hora da Estrela', '9780000000017', 1977, 12, 2);
INSERT INTO tb_book_author (book_id, author_id) VALUES (17, 1);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('American Gods', '9780000000018', 2001, 11, 6);
INSERT INTO tb_book_author (book_id, author_id) VALUES (18, 15);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Mistborn', '9780000000019', 2006, 11, 6);
INSERT INTO tb_book_author (book_id, author_id) VALUES (19, 3);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('Outliers', '9780000000020', 2008, 9, 7);
INSERT INTO tb_book_author (book_id, author_id) VALUES (20, 7);
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES ('The Black Swan', '9780000000021', 2007, 3, 7);
INSERT INTO tb_book_author (book_id, author_id) VALUES (21, 16);