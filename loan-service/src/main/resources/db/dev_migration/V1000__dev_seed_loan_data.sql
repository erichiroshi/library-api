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