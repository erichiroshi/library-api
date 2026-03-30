-- =============================================
-- Criação dos schemas por domínio
-- =============================================

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS lending;

COMMENT ON SCHEMA auth    IS 'Domain: authentication, users and refresh tokens';
COMMENT ON SCHEMA catalog IS 'Domain: books, authors and categories';
COMMENT ON SCHEMA lending IS 'Domain: loans and loan items';