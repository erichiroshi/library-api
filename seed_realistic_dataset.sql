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

-- ============================
-- Usuários
-- ============================
INSERT INTO tb_user (name, email, phone, password) VALUES
('João Silva', 'joao.silva@email.com', '11999999999', '{bcrypt}$2a$12$HKylhM2ZGJ6fmQa2mr2eHuSZ4htjXOlD3EjpFP7L3FQmzr1ZBUdky'),
('Maria Souza', 'maria.souza@email.com', '11888888888', '{bcrypt}$2a$12$HKylhM2ZGJ6fmQa2mr2eHuSZ4htjXOlD3EjpFP7L3FQmzr1ZBUdky'),
('Admin', 'admin@admin.com', '11999999899', '{bcrypt}$2a$12$oqR2CMQMn03KTkO6sHr8y.t//zDSetEIueMq4mPBfLD8XaSVkbIzS'),
('User', 'user@user.com', '11999999997', '{bcrypt}$2a$12$duyl2ugEqZovBPIIhlkzueeMd5Qccz.etOCo.ceI1/yk7sr.7Gamq');

INSERT INTO tb_user_roles (user_id, role) VALUES
(1, 'ROLE_ADMIN'),
(1, 'ROLE_USER'),
(2, 'ROLE_USER'),
(3, 'ROLE_ADMIN'),
(4, 'ROLE_USER');

-- ============================
-- Empréstimos (25 variados)
-- ============================
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-21', '2026-01-28', NULL, 'OVERDUE', 1);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2025-12-21', '2025-12-28', NULL, 'WAITING_RETURN', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-21', '2026-01-28', '2026-01-28', 'RETURNED', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2025-12-30', '2026-01-06', NULL, 'CANCELED', 1);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-12', '2026-01-19', NULL, 'CANCELED', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-02-06', '2026-02-13', NULL, 'WAITING_RETURN', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-02', '2026-01-09', NULL, 'WAITING_RETURN', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2025-12-27', '2026-01-03', NULL, 'OVERDUE', 1);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-06', '2026-01-13', NULL, 'OVERDUE', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-29', '2026-02-05', NULL, 'CANCELED', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-03', '2026-01-10', NULL, 'CANCELED', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2025-12-30', '2026-01-06', NULL, 'CANCELED', 1);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-02-18', '2026-02-25', '2026-02-24', 'RETURNED', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2025-12-29', '2026-01-05', NULL, 'OVERDUE', 1);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-02-12', '2026-02-19', '2026-02-17', 'RETURNED', 1);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-02-07', '2026-02-14', NULL, 'WAITING_RETURN', 1);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2025-12-28', '2026-01-04', NULL, 'WAITING_RETURN', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-20', '2026-01-27', NULL, 'CANCELED', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-05', '2026-01-12', NULL, 'OVERDUE', 1);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2025-12-23', '2025-12-30', NULL, 'WAITING_RETURN', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-09', '2026-01-16', NULL, 'WAITING_RETURN', 1);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-02', '2026-01-09', '2026-01-09', 'RETURNED', 1);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-15', '2026-01-22', NULL, 'WAITING_RETURN', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-05', '2026-01-12', '2026-01-09', 'RETURNED', 2);
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES ('2026-01-03', '2026-01-10', NULL, 'CANCELED', 1);

-- ============================
-- Itens dos Empréstimos
-- ============================
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (1, 16, 1);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (2, 1, 2);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (3, 1, 1);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (4, 15, 2);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (5, 12, 3);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (6, 10, 3);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (7, 8, 2);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (8, 13, 3);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (9, 6, 1);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (10, 6, 1);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (11, 16, 3);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (12, 2, 3);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (13, 17, 1);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (14, 15, 2);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (15, 7, 2);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (16, 4, 3);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (17, 11, 1);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (18, 10, 3);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (19, 19, 1);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (20, 12, 2);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (21, 15, 2);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (22, 10, 2);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (23, 13, 2);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (24, 4, 2);
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES (25, 21, 2);
