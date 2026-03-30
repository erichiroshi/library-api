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