-- ============================
-- Categorias
-- ============================
INSERT INTO tb_category (name) VALUES
    ('Tecnologia'),
    ('Literatura'),
    ('História');

-- ============================
-- Autores
-- ============================
INSERT INTO tb_author (name, biography) VALUES
    ('Robert C. Martin', 'Autor e engenheiro de software'),
    ('Joshua Bloch', 'Especialista em Java e APIs'),
    ('George Orwell', 'Escritor e jornalista');

-- ============================
-- Livros
-- ============================
INSERT INTO tb_book (title, isbn, publication_year, available_copies, category_id) VALUES
    ('Clean Code', '9780132350884', 2008, 5, 1),
    ('Effective Java', '9780134685991', 2018, 3, 1),
    ('1984', '9780451524935', 1949, 10, 2);

-- ============================
-- Relacionamento Livro x Autor
-- ============================
INSERT INTO tb_book_author (book_id, author_id) VALUES
    (1, 1),
    (2, 2),
    (3, 3);

-- ============================
-- Usuários
-- ============================
INSERT INTO tb_user (name, email, phone, password) VALUES
    ('João Silva', 'joao.silva@email.com', '11999999999', '{bcrypt}$2a$10$HGId/fYcy8WU7UZ4NLol2ejTGGi/ZnVpZ9OdQTxeW3PaDz.09cnaa'),
    ('Maria Souza', 'maria.souza@email.com', '11888888888', '{bcrypt}$2a$10$HGId/fYcy8WU7UZ4NLol2ejTGGi/ZnVpZ9OdQTxeW3PaDz.09cnaa');

-- João Silva → ADMIN + USER
INSERT INTO tb_user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN'
FROM tb_user
WHERE email = 'joao.silva@email.com';

INSERT INTO tb_user_roles (user_id, role)
SELECT id, 'ROLE_USER'
FROM tb_user
WHERE email = 'joao.silva@email.com';

-- Maria Souza → USER
INSERT INTO tb_user_roles (user_id, role)
SELECT id, 'ROLE_USER'
FROM tb_user
WHERE email = 'maria.souza@email.com';

-- ============================
-- Empréstimos
-- ============================
INSERT INTO tb_loan (loan_date, due_date, return_date, status, user_id) VALUES
    (CURRENT_DATE, CURRENT_DATE + 7, NULL, 'WAITING_RETURN', 1);

-- ============================
-- Itens do Empréstimo
-- ============================
INSERT INTO tb_loan_item (loan_id, book_id, quantity) VALUES
    (1, 1, 1);
