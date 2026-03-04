-- =============================================
-- ShedLock — tabela de lock distribuído para scheduled jobs
-- Garante que apenas uma instância execute cada job por vez
-- =============================================

CREATE TABLE shedlock (
    name        VARCHAR(64)  NOT NULL,
    lock_until  TIMESTAMP    NOT NULL,
    locked_at   TIMESTAMP    NOT NULL,
    locked_by   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_shedlock PRIMARY KEY (name)
);

COMMENT ON TABLE shedlock
    IS 'Distributed lock table used by ShedLock to prevent concurrent scheduled job execution across multiple instances';