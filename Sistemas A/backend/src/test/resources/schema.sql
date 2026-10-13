-- Cria a tabela "jogos" manualmente porque a geração automática de DDL do
-- Hibernate (community SQLiteDialect, 8 colunas) grava a coluna "id" sem
-- tipo (viraria "id," em vez de "id integer") nessa tabela especificamente,
-- o que quebra o alias de rowid do SQLite e faz o autoincremento nunca
-- gravar um valor - toda linha inserida fica com id NULL no banco. Criando
-- a tabela nós mesmos (com IF NOT EXISTS) evitamos esse bug: o
-- spring.jpa.hibernate.ddl-auto=update do Hibernate não recria tabelas que
-- já existem, só adiciona colunas que faltarem.
CREATE TABLE IF NOT EXISTS jogos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conta_plataforma_id BIGINT NOT NULL,
    app_id BIGINT NOT NULL,
    nome VARCHAR(255),
    imagem VARCHAR(255),
    horas_jogadas FLOAT NOT NULL,
    conquistas_obtidas INTEGER NOT NULL,
    conquistas_totais INTEGER NOT NULL,
    ultimo_acesso TIMESTAMP,
    horas_windows FLOAT NOT NULL DEFAULT 0,
    horas_mac FLOAT NOT NULL DEFAULT 0,
    horas_linux FLOAT NOT NULL DEFAULT 0,
    horas_deck FLOAT NOT NULL DEFAULT 0,
    UNIQUE (conta_plataforma_id, app_id)
);

-- Mesmo motivo acima (Issue #10): criamos "destaques" e "destaque_jogos" na
-- mão para não depender da geração automática de DDL do Hibernate, que já
-- se mostrou pouco confiável nesse projeto para tabelas com várias colunas.
CREATE TABLE IF NOT EXISTS destaques (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(255) NOT NULL,
    posicao INTEGER NOT NULL,
    plataforma VARCHAR(255),
    imagem TEXT,
    texto TEXT
);

CREATE TABLE IF NOT EXISTS destaque_jogos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    destaque_id BIGINT NOT NULL,
    conta_plataforma_id BIGINT NOT NULL,
    app_id BIGINT NOT NULL,
    ordem INTEGER NOT NULL,
    conquista_chave VARCHAR(255),
    conquista_nome VARCHAR(255),
    conquista_icone TEXT
);
