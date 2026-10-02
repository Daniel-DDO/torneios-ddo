-- Disponibilidade de horários dos jogadores
-- Assume tabela "jogador" com id VARCHAR(255). Datas em horário de Brasília (gravadas pela aplicação).

CREATE TABLE IF NOT EXISTS disponibilidade_jogador (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogador_id  VARCHAR(255) NOT NULL REFERENCES jogador (id) ON DELETE CASCADE,
    mascara     INTEGER      NOT NULL CHECK (mascara BETWEEN 0 AND 268435455),
    criado_em   TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_disp_jogador_criado_em
    ON disponibilidade_jogador (jogador_id, criado_em);

CREATE TABLE IF NOT EXISTS observacao_disponibilidade (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogador_id  VARCHAR(255) NOT NULL REFERENCES jogador (id) ON DELETE CASCADE,
    texto       TEXT         NOT NULL CHECK (char_length(texto) BETWEEN 1 AND 500),
    criado_em   TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_obs_disp_jogador_criado_em
    ON observacao_disponibilidade (jogador_id, criado_em);

-- Log append-only: só jogador_id pode mudar (necessário pro mesclarContas). Nada mais é editável.
CREATE OR REPLACE FUNCTION fn_disponibilidade_append_only() RETURNS trigger AS $$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
       OR NEW.mascara IS DISTINCT FROM OLD.mascara
       OR NEW.criado_em IS DISTINCT FROM OLD.criado_em THEN
        RAISE EXCEPTION 'disponibilidade_jogador é append-only: só jogador_id pode mudar (mesclagem de contas)';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_disponibilidade_append_only ON disponibilidade_jogador;
CREATE TRIGGER trg_disponibilidade_append_only
    BEFORE UPDATE ON disponibilidade_jogador
    FOR EACH ROW EXECUTE FUNCTION fn_disponibilidade_append_only();

CREATE OR REPLACE FUNCTION fn_observacao_disponibilidade_append_only() RETURNS trigger AS $$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
       OR NEW.texto IS DISTINCT FROM OLD.texto
       OR NEW.criado_em IS DISTINCT FROM OLD.criado_em THEN
        RAISE EXCEPTION 'observacao_disponibilidade é append-only: só jogador_id pode mudar (mesclagem de contas)';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_observacao_disponibilidade_append_only ON observacao_disponibilidade;
CREATE TRIGGER trg_observacao_disponibilidade_append_only
    BEFORE UPDATE ON observacao_disponibilidade
    FOR EACH ROW EXECUTE FUNCTION fn_observacao_disponibilidade_append_only();
