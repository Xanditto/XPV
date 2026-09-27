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
    UNIQUE (conta_plataforma_id, app_id)
);
