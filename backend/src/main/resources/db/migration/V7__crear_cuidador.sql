CREATE TABLE cuidador (
    id                  BIGINT PRIMARY KEY,
    dni_nie             VARCHAR(9)  NOT NULL,
    empresa_id          BIGINT,
    estado_en_empresa   VARCHAR(20),
    estado_verificacion VARCHAR(20) NOT NULL DEFAULT 'SIN_SUBIR',

    CONSTRAINT fk_cuidador_usuario FOREIGN KEY (id) REFERENCES usuario (id) ON DELETE CASCADE,
    -- No se puede borrar una empresa que aún tiene cuidadores
    CONSTRAINT fk_cuidador_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT uk_cuidador_dni_nie UNIQUE (dni_nie),
    CONSTRAINT ck_cuidador_dni_nie CHECK (dni_nie ~ '^([0-9]{8}|[XYZ][0-9]{7})[TRWAGMYFPDXBNJZSQVHLCKE]$'),
    CONSTRAINT ck_cuidador_estado_verificacion CHECK (estado_verificacion IN ('SIN_SUBIR','PENDIENTE', 'VERIFICADO', 'RECHAZADO')),
    CONSTRAINT ck_cuidador_estado_en_empresa CHECK (estado_en_empresa IN ('PENDIENTE', 'ACTIVO', 'BAJA')),
    -- Autonomo: sin empresa y sin estado. Con empresa: siempre con estado.
    CONSTRAINT ck_cuidador_empresa_y_estado CHECK ((empresa_id IS NULL) = (estado_en_empresa IS NULL))
);

CREATE INDEX idx_cuidador_empresa ON cuidador (empresa_id);

CREATE TABLE cuidador_documento (
    cuidador_id    BIGINT       NOT NULL,
    tipo           VARCHAR(30)  NOT NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    clave_archivo  VARCHAR(255) NOT NULL,
    motivo_rechazo VARCHAR(500),
    fecha_subida   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_cuidador_documento_cuidador FOREIGN KEY (cuidador_id) REFERENCES cuidador (id) ON DELETE CASCADE,
    CONSTRAINT ck_cuidador_documento_tipo CHECK (tipo IN ('DNI_NIE', 'CERTIFICADO_PROFESIONAL', 'CIF', 'OTRO')),
    CONSTRAINT ck_cuidador_documento_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    CONSTRAINT ck_cuidador_documento_motivo CHECK (estado <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL)
);

CREATE INDEX idx_cuidador_documento_cuidador ON cuidador_documento (cuidador_id);

CREATE TABLE cuidador_franja_horaria (
    cuidador_id BIGINT      NOT NULL,
    dia_semana  VARCHAR(10) NOT NULL,
    hora_inicio TIME        NOT NULL,
    hora_fin    TIME        NOT NULL,

    CONSTRAINT fk_cuidador_franja_cuidador FOREIGN KEY (cuidador_id) REFERENCES cuidador (id) ON DELETE CASCADE,
    CONSTRAINT ck_cuidador_franja_dia CHECK (dia_semana IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT ck_cuidador_franja_horas CHECK (hora_fin > hora_inicio)
);

CREATE INDEX idx_cuidador_franja_cuidador ON cuidador_franja_horaria (cuidador_id);