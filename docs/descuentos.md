# Cotizaciones y descuentos por detalle

## Contrato HTTP

Las rutas no cambian. Cotizaciones y detalles reciben DTO y devuelven DTO;
las referencias conservan la forma `{"id": 1}`. Los demás módulos siguen usando entidades.

Crear cotización: `POST /api/cotizaciones`.

```json
{
  "oportunidad": {"id": 1},
  "fechaEmision": "2026-11-01",
  "vigenciaDias": 15,
  "alcanceServicio": "Condiciones comerciales"
}
```

La fecha y la vigencia son opcionales. Al crear, se usan la fecha actual y 15 días;
al actualizar, su omisión conserva los valores existentes. El código, estado inicial,
vencimiento y totales los determina el servidor.

Crear detalle: `POST /api/detalles-cotizacion`.
Actualizar: `PUT /api/detalles-cotizacion/{id}`, con el mismo formato.

```json
{
  "cotizacion": {"id": 1},
  "servicio": {"id": 1},
  "descripcion": "Alquiler de equipo por día",
  "cantidad": 7,
  "precioUnitario": 180.00,
  "tipoDescuento": "MONTO",
  "valorDescuento": 30.00
}
```

Indicar exactamente uno de `servicio` o `producto`. Los alquileres pueden usar
un concepto del catálogo y una cantidad de días, como en el ejemplo; no se introduce
un módulo de alquileres ni se cambia su modelo en esta etapa.

`precioUnitario` es el precio comercial explícito de la línea, independiente del catálogo.
La descripción y el precio se guardan en el detalle y no cambian al editar el catálogo.
Los campos `id`, `subtotal`, `descuentoUnitario`, `descuentoTotal`, `igv` y `total`
no son entradas comerciales; enviarlos no permite controlar sus valores persistidos.

## Reglas y redondeo

- `NINGUNO`: valor omitido, nulo o cero. Otro valor se rechaza.
- `MONTO`: descuento por unidad en soles; entre cero y el precio unitario.
- `PORCENTAJE`: porcentaje por unidad; entre cero y 100.
- Omitir el tipo equivale a `NINGUNO`. MONTO y PORCENTAJE requieren valor.
- Precio no negativo, cantidad positiva y valores con hasta dos decimales.
- Descuento del 100 % o igual al precio está permitido y produce importe cero.
- Solo se crean, actualizan o eliminan detalles de cotizaciones `BORRADOR`.

Se usa `BigDecimal` y redondeo `HALF_UP` a dos decimales:

1. Descuento unitario: monto o `precio × porcentaje / 100`, redondeado a céntimos.
2. Bruto de línea: `precio × cantidad`, redondeado.
3. Descuento total: `descuento unitario × cantidad`, redondeado.
4. Importe: `bruto de línea − descuento total`.
5. Subtotal de cotización: suma de importes; IGV: 18 % de ese subtotal, redondeado;
   total: subtotal más IGV.

Restar los dos importes redondeados mantiene la igualdad visible entre el bruto,
el descuento y el importe, también con cantidades fraccionarias. Por ejemplo,
precio 0.05, cantidad 1.50 y 33.33 % producen descuento unitario 0.02,
bruto 0.08, descuento total 0.03 e importe 0.05.

Ejemplo principal: 180 × 7, descuento de 30 por unidad → descuento total 210,
subtotal 1,050, IGV 189 y total 1,239 soles.
Los importes que excedan DECIMAL(12,2) se rechazan y la transacción se revierte.

## PDF y límites de esta etapa

`GET /api/cotizaciones/{codigo}/pdf` muestra ÍTEM, DESCRIPCIÓN, CANTIDAD,
PRECIO UNITARIO, DESCUENTO total por línea e IMPORTE. La vigencia usa el valor guardado.
Un código inexistente devuelve 404. Los detalles se ordenan por ID. Las tablas repiten
su encabezado al continuar en otra página y mantienen cada fila completa;
el pie aparece desde la primera página.

La fotografía de esta etapa cubre los valores comerciales del detalle. Los datos del cliente
siguen consultándose desde su registro actual. El vencimiento automático y una fotografía
completa del documento permanecen pendientes. La numeración y la coordinación de escrituras
se explican en [numeracion-y-concurrencia.md](numeracion-y-concurrencia.md).

## Verificación local del 1 de noviembre de 2026

Se confirmó MySQL 8.0.34 y la base `sistema_gestion_comercial`, usada por IntelliJ.
Se corrigió la URL del `.env` local y se aplicó `001_descuentos_detalle.sql`.
Los dos detalles previos conservaron su subtotal conjunto de S/ 700.00;
las nuevas columnas quedaron en `NINGUNO` y cero. No volver a aplicar el script en esa base.
La comprobación `MySqlSchemaTests` pasó con `ddl-auto=validate` y lectura de los DTO.
Las pruebas de negocio usan H2; la comprobación de MySQL no prueba escrituras reales.
