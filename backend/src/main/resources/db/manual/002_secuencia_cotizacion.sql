-- Ejecutar UNA VEZ con la aplicación detenida. No cambia códigos ni importes existentes.
-- El correlativo se inicializa desde el mayor sufijo NUMÉRICO, sin orden alfabético.
-- CREATE TABLE provoca commit implícito en MySQL 8.
CREATE TABLE secuencia_cotizacion (
    id BIGINT NOT NULL PRIMARY KEY,
    ultimo_numero BIGINT NOT NULL,
    CONSTRAINT ck_secuencia_cotizacion_id CHECK (id = 1),
    CONSTRAINT ck_secuencia_cotizacion_numero CHECK (ultimo_numero >= 0)
) ENGINE=InnoDB;

INSERT INTO secuencia_cotizacion (id, ultimo_numero)
SELECT 1, COALESCE(MAX(CAST(SUBSTRING(codigo, 5) AS UNSIGNED)), 0)
FROM cotizaciones
WHERE codigo REGEXP '^COT-[0-9]+$';
