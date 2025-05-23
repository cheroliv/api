set search_path = "test";
CREATE TABLE IF NOT EXISTS "user"(
"id"       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
"login"    TEXT,
"password" TEXT,
"email"    TEXT,
"lang_key" VARCHAR,
"version"  BIGINT);

CREATE UNIQUE INDEX IF NOT EXISTS "uniq_idx_user_login" ON "user" ("login");
CREATE UNIQUE INDEX IF NOT EXISTS "uniq_idx_user_email" ON "user" ("email");CREATE TABLE IF NOT EXISTS "authority" (
    "role" VARCHAR(50) PRIMARY KEY
);

INSERT INTO authority ("role")
VALUES ('ADMIN'),
       ('USER'),
       ('ANONYMOUS')
ON CONFLICT ("role") DO NOTHING;CREATE SEQUENCE IF NOT EXISTS user_authority_seq
START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS "user_authority"(
    "id"  BIGINT DEFAULT nextval('user_authority_seq') PRIMARY KEY,
    "user_id"      UUID,
    "role"       VARCHAR,
    FOREIGN KEY ("user_id")
    REFERENCES "user" (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY ("role")
    REFERENCES authority ("role")
    ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_idx_user_authority
ON "user_authority" ("role", "user_id");CREATE TABLE IF NOT EXISTS "user_activation" (
    "id" UUID PRIMARY KEY,
    "activation_key" VARCHAR,
    "created_date" TIMESTAMP,
    "activation_date" TIMESTAMP,
    FOREIGN KEY ("id")
    REFERENCES "user" ("id")
    ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_idx_user_activation_key
ON "user_activation" ("activation_key");

CREATE INDEX IF NOT EXISTS idx_user_activation_date
ON "user_activation" ("activation_date");

CREATE INDEX IF NOT EXISTS idx_user_activation_creation_date
ON "user_activation" ("created_date");