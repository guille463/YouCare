CREATE TABLE
    cliente (
        id BIGINT PRIMARY KEY,
        fecha_nacimiento DATE NOT NULL,
        direccion VARCHAR(200),
        ciudad VARCHAR(100),
        CONSTRAINT fk_cliente_usuario FOREIGN KEY (id) REFERENCES usuario (id) ON DELETE CASCADE
    );

CREATE TABLE
    admin (
        id BIGINT PRIMARY KEY,
        departamento VARCHAR(100),
        CONSTRAINT fk_admin_usuario FOREIGN KEY (id) REFERENCES usuario (id) ON DELETE CASCADE
    );