-- ============================
-- Categorias
-- ============================
INSERT INTO tb_category (id, name) VALUES
    (1, 'Tecnologia'),
    (2, 'Literatura'),
    (3, 'História');

-- ============================
-- Autores
-- ============================
INSERT INTO tb_author (id, name, biography) VALUES
    (1, 'Robert C. Martin', 'Autor e engenheiro de software'),
    (2, 'Joshua Bloch', 'Especialista em Java e APIs'),
    (3, 'George Orwell', 'Escritor e jornalista');

-- ============================
-- Livros
-- ============================
INSERT INTO tb_book (
    id,
    title,
    isbn,
    publication_year,
    available_copies,
    category_id
) VALUES
    (1, 'Clean Code', '9780132350884', 2008, 5, 1),
    (2, 'Effective Java', '9780134685991', 2018, 3, 1),
    (3, '1984', '9780451524935', 1949, 10, 2);

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
INSERT INTO tb_user (id, name, email, phone) VALUES
    (1, 'João Silva', 'joao.silva@email.com', '11999999999'),
    (2, 'Maria Souza', 'maria.souza@email.com', '11888888888');

-- ============================
-- Empréstimos
-- ============================
INSERT INTO tb_loan (
    id,
    loan_date,
    due_date,
    return_date,
    status,
    user_id
) VALUES
    (
        1,
        CURRENT_DATE,
        CURRENT_DATE + 7,
        NULL,
        'WAITING_RETURN',
        1
    );

-- ============================
-- Itens do Empréstimo
-- ============================
INSERT INTO tb_loan_item (
    loan_id,
    book_id,
    quantity
) VALUES
    (1, 1, 1);
