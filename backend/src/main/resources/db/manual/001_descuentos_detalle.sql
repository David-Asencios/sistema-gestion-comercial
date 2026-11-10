-- Ejecutar UNA VEZ sobre la base existente antes de arrancar esta versión.
-- MySQL 8: ALTER TABLE hace commit implícito. No elimina ni recalcula importes históricos.
-- Las cuatro columnas deben estar ausentes; comprobar information_schema antes de ejecutar.
ALTER TABLE detalles_cotizacion
    ADD COLUMN tipo_descuento ENUM('MONTO', 'NINGUNO', 'PORCENTAJE') NOT NULL DEFAULT 'NINGUNO',
    ADD COLUMN valor_descuento DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN descuento_unitario DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN descuento_total DECIMAL(12,2) NOT NULL DEFAULT 0.00;
