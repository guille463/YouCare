CREATE TABLE empresa (
    id                  BIGINT PRIMARY KEY,
    razon_social        VARCHAR(150) NOT NULL,
    cif                 VARCHAR(9)   NOT NULL,
    estado_verificacion VARCHAR(20)  NOT NULL DEFAULT 'SIN_SUBIR',

    CONSTRAINT fk_empresa_usuario FOREIGN KEY (id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT uk_empresa_cif UNIQUE (cif),
    CONSTRAINT ck_empresa_cif CHECK (cif ~ '^[ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J]$'),
    CONSTRAINT ck_empresa_estado_verificacion CHECK (estado_verificacion IN ('SIN_SUBIR', 'PENDIENTE', 'VERIFICADO', 'RECHAZADO'))
);

CREATE TABLE empresa_documento (
    empresa_id     BIGINT       NOT NULL,
    tipo           VARCHAR(30)  NOT NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    clave_archivo  VARCHAR(255) NOT NULL,
    motivo_rechazo VARCHAR(500),
    fecha_subida   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_empresa_documento_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE,
    CONSTRAINT ck_empresa_documento_tipo CHECK (tipo IN ('DNI_NIE', 'CERTIFICADO_PROFESIONAL', 'CIF', 'OTRO')),
    CONSTRAINT ck_empresa_documento_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    CONSTRAINT ck_empresa_documento_motivo CHECK (estado <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL)
);

CREATE INDEX idx_empresa_documento_empresa ON empresa_documento (empresa_id);