# DTO de servicios y productos

Los endpoints de `/api/servicios` y `/api/productos` reciben DTO de entrada y
devuelven DTO de salida. Se conservan las rutas, los nombres de los campos y los
servicios existentes; no se necesita una migración de MySQL.

## Requests de creación y actualización

POST y PUT utilizan el mismo formato para ambos catálogos:

```json
{
  "nombre": "Alquiler de equipo",
  "descripcion": "Descripción del concepto",
  "precioReferencial": 180.00,
  "activo": true
}
```

- `nombre`: obligatorio, no vacío y máximo 150 caracteres.
- `descripcion`: opcional.
- `precioReferencial`: obligatorio, no negativo, hasta ocho cifras enteras y dos
  decimales. El intervalo permitido es de 0 a 99 999 999,99, según la columna
  existente DECIMAL(10,2).
- `activo`: true si se omite; false permite desactivar el concepto. Un null
  explícito devuelve HTTP 400.

La validación de precisión evita guardar importes fuera de capacidad o redondear
silenciosamente un precio con más de dos decimales. Las entradas inválidas
devuelven HTTP 400 con un mapa de campos y mensajes, por ejemplo
`{"precioReferencial":"..."}`, sin crear registros ni modificar el registro de PUT.

PUT conserva la semántica de reemplazo actual: enviar todos los campos que se
quieran conservar. Omitir descripción la elimina y omitir activo aplica true.

El ID no forma parte del DTO de entrada. Si se incluye como campo adicional,
se ignora: POST genera un ID nuevo y PUT usa exclusivamente el ID de la ruta.

## Respuestas y consultas

La respuesta contiene `id`, `nombre`, `descripcion`, `precioReferencial` y
`activo`. GET de listado sigue devolviendo un arreglo JSON. POST devuelve 201,
GET y PUT devuelven 200 y DELETE devuelve 204. Un registro inexistente devuelve
404. Eliminar un concepto utilizado por un detalle devuelve 409 por su relación.

Los DTO no agregan reglas nuevas sobre el uso de conceptos inactivos en detalles;
ese comportamiento comercial queda como estaba antes de esta etapa.

## Conservación de cotizaciones

Editar el nombre, precio referencial o estado del catálogo no cambia los campos
comerciales almacenados en cada detalle: descripción, cantidad, precio unitario,
tipo y valor de descuento, descuento unitario y total, e importe final.

La referencia anidada `servicio` o `producto` de la respuesta de un detalle sigue
mostrando los datos actuales del catálogo. Los valores históricos son los campos
propios del detalle; por eso su precio unitario puede diferir del precio referencial.

## Verificación

`CatalogosDtoIntegrationTests` usa una H2 aislada y peticiones sin transacción de
prueba abierta. Comprueba ambos catálogos: CRUD, protección de IDs, valores
predeterminados, precisión de precios y entradas inválidas. También registra
cotizaciones con descuentos, las acepta, modifica el catálogo y comprueba que
sus valores comerciales y totales se conservan.

La colección Postman existente mantiene sus requests y verifica el recorrido
comercial completo. Las pruebas no escriben en MySQL.
