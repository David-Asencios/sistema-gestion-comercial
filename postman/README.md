# Recorrido comercial en Postman

## Archivos

- `flujo-comercial.postman_collection.json`: colección v2.1 con 45 solicitudes y scripts de verificación.
- `local.postman_environment.json`: entorno local con `baseUrl=http://localhost:8080`.

La colección no contiene credenciales ni IDs fijos de la base del proyecto.

Los requests de clientes y oportunidades mantienen el mismo JSON tras incorporar
DTO. Consultar [el contrato de entrada y salida](../docs/dto-clientes-oportunidades.md)
para los campos editables, valores predeterminados y errores por campo. No enviar
IDs de creación ni fechas de creación: el servidor los administra.

Servicios y productos también utilizan DTO y conservan los requests de esta
colección. Su precio referencial debe estar entre 0 y 99 999 999,99, con máximo
dos decimales; un valor inválido devuelve HTTP 400. Ver
[el contrato de catálogos](../docs/dto-catalogos.md) para ejemplos y respuestas.

## Ejecución manual

1. Arrancar el backend con MySQL y las migraciones 001 y 002 ya aplicadas.
2. Importar ambos archivos en Postman y seleccionar el entorno **Gestión Comercial - Local**.
3. Ejecutar la colección completa en el Collection Runner, en su orden original, con una iteración.
4. Revisar los resultados de los tests. Si una solicitud falla, corregir la causa y reiniciar
   desde la primera: ella limpia los IDs anteriores y genera un identificador nuevo de ejecución.

Los scripts capturan los IDs y el código comercial de las respuestas en variables de colección.
Las referencias usan esos valores en cada solicitud posterior. No ejecutar una carpeta aislada
sin haber preparado sus dependencias.

La primera ejecución genera un cliente, una oportunidad, un servicio, un producto,
tres cotizaciones y cuatro detalles. Cada nueva ejecución genera otro conjunto de datos
identificado por `runId`. No se eliminan los registros al finalizar: las cotizaciones aceptadas
y rechazadas se conservan según las reglas del sistema. Usar una base de desarrollo.

## Casos verificados

| Recorrido | Resultado esperado |
|---|---|
| Cliente → oportunidad → catálogos → cotización | IDs válidos, código COT y borrador con vigencia de 15 días. |
| Envío sin detalles | HTTP 400. |
| Alquiler: 7 × 180, descuento de 30 por unidad | Descuento 210 e importe 1,050 soles. |
| Producto: 2 × 250, descuento del 10 % | Descuento 50 e importe 450 soles. |
| Totales combinados | Subtotal 1,500, IGV 270 y total 1,770 soles. |
| Descuento superior al precio | HTTP 400; se conservan dos líneas y los totales. |
| Consulta de catálogos | Conservan precios 180 y 250. |
| Descarga PDF | HTTP 200, tipo application/pdf, firma %PDF- y nombre por código. |
| Envío | ENVIADA; edición de cabecera y creación/edición/eliminación de líneas bloqueadas. |
| Aceptación | ACEPTADA; importes conservados y reapertura bloqueada. |
| Segunda cotización enviada y rechazada | RECHAZADA; reapertura bloqueada. |
| Borrador creado hace 45 días con vigencia de 15 | No puede enviarse; conserva BORRADOR. |
| Intento de marcar VENCIDA una cotización vigente | HTTP 400. |
| Buscar cliente por texto y estado activo | Una página con el cliente de la ejecución y metadatos. |
| Buscar oportunidades por texto, cliente, etapa y canal | Una página con la oportunidad de la ejecución y su cliente. |
| Consultar una página fuera del resultado | HTTP 200, contenido vacío y total conservado. |
| Consultar una etapa desconocida | HTTP 400 controlado. |
| Cotizaciones por código, cliente, oportunidad y estado | Devuelve la cotización aceptada de la ejecución. |
| Rango de emisión con ambos extremos iguales | Incluye la cotización emitida en esa fecha. |
| Página de cotizaciones sin contenido | HTTP 200, total filtrado conservado. |
| Fecha inexistente o rango invertido | HTTP 400 controlado. |
| Servicios y productos por nombre y activo | Conceptos de la ejecución con precios y totales conservados. |
| Página de servicios fuera del resultado | HTTP 200, contenido vacío y total conservado. |
| Orden de producto no permitido | HTTP 400 controlado. |

Las solicitudes 33 a 45 consultan las rutas `/buscar`. Ver
[parámetros, orden y ejemplos](../docs/busquedas-paginadas.md).

Para guardar el PDF, ejecutar la solicitud **14 Descargar PDF** después de preparar sus datos
y usar la opción de guardar la respuesta como archivo en Postman.

## Vencimiento después del envío

No existe una ruta para retroceder la fecha de una cotización enviada. Por eso la colección
no simula `ENVIADA → VENCIDA` mediante un endpoint artificial ni espera quince días.

`FlujoComercialHttpTests` cubre ese caso: registra y envía por HTTP una cotización válida,
ajusta las fechas en su H2 aislada para representar una vigencia terminada ayer y luego
comprueba por HTTP que aceptar devuelve 400 y marcar VENCIDA devuelve 200.
El único acceso directo a la base de esa prueba es la preparación temporal; el resto del
recorrido usa peticiones HTTP reales con transacciones independientes.

## Verificar la colección automáticamente

Además de las tres pruebas HTTP habituales (aceptación, rechazo y vencimiento), se puede
ejecutar exactamente este JSON y sus scripts con Newman. Se instala exclusivamente en
`backend/target/`, sin dependencias adicionales de producción ni instalación global:

```powershell
# Desde la raíz del repositorio:
npm install --prefix backend/target/newman --no-audit --no-fund newman@6.2.3
Set-Location backend
.\mvnw.cmd test '-Drun.postman=true'
```

Java arranca el servidor solo en loopback, en un puerto libre, sobre H2 en memoria y pasa
esa URL al ejecutor. La URL local del archivo de entorno se reemplaza solo durante la prueba.
No arranca el backend sobre MySQL ni modifica los datos de desarrollo.
Se generan `backend/target/postman-results/newman.log` y `newman-report.json`.
La ejecución termina con error si falla una solicitud o una aserción.

Sin `-Drun.postman=true`, la prueba opcional de Newman se omite; las tres pruebas Java
del recorrido se ejecutan normalmente y no requieren Node ni descargas de npm.

La ejecución v2.1 por consola y el uso de archivos de entorno están descritos en la
[documentación oficial de Newman](https://learning.postman.com/docs/reference/newman-cli/command-line-integration-with-newman/).
Los tests usan las funciones documentadas
[pm.test y pm.expect](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-test-expect/).

## Resultado verificado

El 4 de noviembre de 2026 se ejecutó la suite con `-Drun.postman=true` tras
incorporar los DTO y las búsquedas paginadas de los cinco módulos:
248 pruebas correctas, cero fallos y una comprobación opcional de MySQL omitida.
La colección ejecutó las 45 solicitudes y 102 aserciones sin errores sobre el servidor
HTTP de pruebas y H2 en memoria. No se escribieron registros en MySQL durante esta verificación.
