# Búsquedas paginadas comerciales

Las nuevas consultas se realizan con GET, sin body:

- `/api/clientes/buscar`
- `/api/oportunidades/buscar`
- `/api/cotizaciones/buscar`
- `/api/servicios/buscar`
- `/api/productos/buscar`

Las rutas originales de listado de los cinco módulos siguen devolviendo arreglos
completos. Los filtros y la paginación se usan en las rutas
`/buscar`; así los consumidores actuales conservan su contrato.

## Parámetros comunes

| Parámetro | Predeterminado | Regla |
|---|---|---|
| texto | Sin filtro | Coincidencia parcial sin distinguir mayúsculas; se recortan espacios externos. Máximo 200 caracteres. |
| pagina | 0 | Entero no negativo; la primera página es cero. |
| tamano | 20 | Entre 1 y 100 registros. |
| orden | id | Un campo de la lista permitida del módulo. |
| direccion | asc | asc o desc, sin distinguir mayúsculas. |

Un texto vacío o formado solo por espacios no filtra. Los caracteres `%`, `_`
y `!` se buscan literalmente. Las consultas usan parámetros enlazados.
El desplazamiento `pagina * tamano` no puede superar 2 147 483 647.

El orden se completa con ID en la misma dirección para desempatar. La paginación
se ejecuta en la base de datos, no sobre una lista completa en memoria.
No representa una fotografía inmutable entre requests: si otros usuarios crean
o modifican registros entre páginas, los resultados pueden cambiar.

## Clientes

`texto` busca en razón social, RUC o nombre del contacto. Se puede combinar con
`activo=true` o `activo=false`; omitir activo incluye ambos estados.

Orden permitido: `id`, `razonSocial`, `ruc`, `nombreContacto`, `activo`.

Ejemplo para Postman:

```http
GET http://localhost:8080/api/clientes/buscar?texto=empresa&activo=true&pagina=0&tamano=10&orden=razonSocial&direccion=asc
```

## Oportunidades

`texto` busca en el título. Se puede combinar con:

- `clienteId`: ID positivo; uno inexistente produce una página vacía.
- `etapa`: NUEVA, EN_NEGOCIACION, GANADA o PERDIDA.
- `canalOrigen`: FACEBOOK_MARKETPLACE, WHATSAPP, LLAMADA, PAGINA_WEB,
  REFERIDO u OTRO.

Los enums se envían con los nombres exactos en mayúsculas.
Todos los filtros informados se cumplen a la vez.
Orden permitido: `id`, `titulo`, `fechaCreacion`, `etapa`, `canalOrigen`.

```http
GET http://localhost:8080/api/oportunidades/buscar?texto=alquiler&clienteId=1&etapa=NUEVA&canalOrigen=WHATSAPP&pagina=0&tamano=10&orden=fechaCreacion&direccion=desc
```

Reemplazar el ID de ejemplo por un cliente existente. Los registros devueltos
incluyen los datos del cliente, como las consultas actuales. La consulta paginada
carga esa relación y realiza un conteo sin fetch para los totales.

## Cotizaciones

En este módulo se utiliza `codigo` en lugar de `texto`: busca una parte del
código, sin distinguir mayúsculas, recortando espacios externos y con un máximo
de 200 caracteres. Los caracteres especiales se interpretan literalmente.

Los filtros opcionales son:

| Parámetro | Regla |
|---|---|
| clienteId | ID positivo del cliente de la oportunidad. |
| oportunidadId | ID positivo de la oportunidad. |
| estado | BORRADOR, ENVIADA, ACEPTADA, RECHAZADA o VENCIDA. |
| fechaDesde | Fecha de emisión mínima, inclusiva, en formato yyyy-MM-dd. |
| fechaHasta | Fecha de emisión máxima, inclusiva, en formato yyyy-MM-dd. |

Los filtros se combinan con AND. Se puede indicar solo una de las fechas.
Si ambas están presentes, desde no puede ser posterior a hasta. Las fechas
filtran la emisión, no el vencimiento.

Un ID positivo inexistente o una combinación de cliente y oportunidad que no
coinciden devuelve una página vacía. Un ID no positivo, estado desconocido, fecha
inválida o rango invertido devuelve HTTP 400.

Se reutilizan `pagina`, `tamano`, `orden` y `direccion` con los mismos límites
y valores predeterminados. Orden permitido: `id`, `codigo`, `fechaEmision`,
`fechaVencimiento`, `estado`, `subtotal`, `igv`, `total`.
El orden de código es textual; el orden por ID permite seguir la secuencia de
registro. Los empates se resuelven por ID.

```http
GET http://localhost:8080/api/cotizaciones/buscar?codigo=COT-&clienteId=1&estado=ACEPTADA&fechaDesde=2026-01-01&fechaHasta=2026-12-31&pagina=0&tamano=10&orden=fechaEmision&direccion=desc
```

Reemplazar el cliente y las fechas por los valores que se quieran consultar.
No enviar body. Cada fila conserva el DTO de cotización con importes, oportunidad
y cliente. Las relaciones se cargan con la consulta; el conteo paginado no usa fetch.

La búsqueda no cambia estados ni recalcula importes. `estado=VENCIDA` busca el
estado almacenado; una ENVIADA cuya fecha ya pasó sigue siendo ENVIADA hasta
que se cambie su estado según las reglas existentes.

Se conservan la consulta por ID, la consulta exacta por código y
`GET /api/cotizaciones/{codigo}/pdf`.

## Servicios y productos

Ambos catálogos reciben los mismos parámetros:

- `texto`: busca una parte del nombre, sin distinguir mayúsculas. No busca en
  la descripción. Se recortan espacios externos y se permiten hasta 200 caracteres.
- `activo`: true o false; omitirlo incluye ambos estados.
- `pagina`, `tamano`, `orden` y `direccion`: las reglas comunes anteriores.

Orden permitido: `id`, `nombre` o `precioReferencial`, con desempate por ID.
El precio se ordena numéricamente. Los filtros de nombre y activo se cumplen a la vez.

```http
GET http://localhost:8080/api/servicios/buscar?texto=alquiler&activo=true&pagina=0&tamano=10&orden=precioReferencial&direccion=asc
GET http://localhost:8080/api/productos/buscar?texto=equipo&activo=false&pagina=0&tamano=10&orden=nombre&direccion=asc
```

Usar GET sin body. Los resultados conservan los DTO con ID, nombre, descripción,
precio referencial y activo. La búsqueda solo lee datos: no modifica catálogos
ni los valores históricos de las cotizaciones.

Los listados completos y las consultas por ID siguen disponibles. No se requiere
migración ni cambio de los registros existentes.

## Respuesta

`contenido` contiene los DTO actuales del módulo:

```json
{
  "contenido": [],
  "pagina": 0,
  "tamano": 20,
  "totalElementos": 0,
  "totalPaginas": 0,
  "primera": true,
  "ultima": true
}
```

Los totales corresponden a los filtros, no al listado completo. Una página más
allá de la última devuelve HTTP 200 con contenido vacío y los totales conservados.
No se crean registros ni se requiere un cambio de esquema.

Parámetros de tipo incorrecto, tamaños fuera de rango, enums desconocidos,
IDs no positivos, texto demasiado largo y campos de orden no permitidos devuelven
HTTP 400 con `{"mensaje":"..."}`.

## Verificación y referencias

`BusquedasComercialesIntegrationTests` comprueba filtros individuales y
combinados, conteos, páginas vacías, valores predeterminados, orden estable,
caracteres especiales y errores HTTP. Usa H2 aislada sin transacción de prueba
abierta, por lo que también verifica el acceso al cliente tras cerrar la sesión.

Las solicitudes 33 a 36 de la colección Postman usan los IDs de su propia
preparación y verifican filtros, respuesta paginada y parámetros inválidos.
Ejecutar la colección completa en orden.

`BusquedasCotizacionesIntegrationTests` verifica todos los filtros, fechas
inclusivas e independientes, ordenamiento, conteos, relaciones cargadas sin
sesión abierta y errores de parámetros. También comprueba la compatibilidad
de las consultas anteriores y la firma del PDF. Sus datos históricos de prueba
se preparan en H2 aislada; no se alteran registros de MySQL.

Las solicitudes 37 a 41 de Postman comprueban cotizaciones por filtros combinados,
fechas de emisión, página vacía y fechas inválidas.

`BusquedasCatalogosIntegrationTests` comprueba nombre, activo, caracteres
literales, precios cero, orden, páginas con empates, conteos y parámetros
inválidos en ambos catálogos sobre H2 aislada. Los datos de preparación se crean
por HTTP sin transacción de prueba abierta.

Las solicitudes 42 a 45 de Postman verifican los catálogos de la misma ejecución,
sus precios, los totales filtrados, una página vacía y un orden inválido.

La primera página de Spring Data es cero y el tamaño debe ser positivo, según
la [API oficial de PageRequest](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/domain/PageRequest.html).
Los campos de Sort deben corresponder al modelo o a un alias de la consulta,
según [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html);
la API restringe esos campos a las listas anteriores.
