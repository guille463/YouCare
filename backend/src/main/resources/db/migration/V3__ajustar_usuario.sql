ALTER TABLE usuario
    ADD COLUMN cantidad_strikes INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_usuario_strikes CHECK (cantidad_strikes >= 0);

-- Teléfono obligatorio para contactar. No es UNIQUE: hay hogares que lo comparten.
ALTER TABLE usuario
    ALTER COLUMN telefono SET NOT NULL;
-- El email se guarda en minúsculas (EmailUtil.normalizar).
-- Así uk_usuario_email impide duplicados como Ana@ / ana@, también desde SQL.
ALTER TABLE usuario
    ADD CONSTRAINT ck_usuario_email_minusculas CHECK (email = lower(email));