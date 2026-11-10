# Contrato HTTP de clientes y oportunidades

Los endpoints usan DTO de entrada y salida. Los servicios conservan la lógica de
negocio y los repositorios siguen trabajando con entidades JPA. No se requiere
una migración de MySQL.

## Clientes

Se mantienen las rutas de listado, consulta, creación, actualización y eliminación
en `/api/clientes`. POST y PUT reciben:

```json
{
  "ruc": "20123456789",
  "razonSocial": "Empresa de ejemplo",
  "nombreContacto": "Contacto comercial",
  "telefono": "999999999",
  "correo": "contacto@example.com",
  "direccion": "Lima",
  "activo": true
}
```

- Obligatorios: razón social y nombre de contacto, no vacíos, máximo 100 caracteres.
- Opcionales: RUC (máximo 11), teléfono (máximo 9), correo (formato de email,
  máximo 100) y dirección (máximo 250). Se conservan las reglas anteriores de RUC.
- `activo` vale `true` si se omite. Un `null` explícito devuelve HTTP 400.
- La respuesta contiene esos campos y el `id` generado por el servidor.

## Oportunidades

Se mantienen las rutas en `/api/oportunidades`. POST y PUT reciben:

```json
{
  "titulo": "Solicitud de alquiler de equipo",
  "descripcion": "Información de la solicitud",
  "canalOrigen": "WHATSAPP",
  "etapa": "NUEVA",
  "observaciones": "Coordinar visita",
  "cliente": { "id": 1 }
}
```

- Título obligatorio, no vacío, máximo 150 caracteres.
- Canal obligatorio: FACEBOOK_MARKETPLACE, WHATSAPP, LLAMADA, PAGINA_WEB,
  REFERIDO u OTRO.
- Etapa: NUEVA, EN_NEGOCIACION, GANADA o PERDIDA. Si se omite se usa NUEVA;
  un `null` explícito devuelve HTTP 400.
- Cliente obligatorio con ID positivo. Una referencia incompleta devuelve 400;
  un ID positivo inexistente devuelve 404.
- Descripción y observaciones son opcionales.
- La respuesta incluye esos campos, `id`, `fechaCreacion` y los datos del cliente
  cargados desde la base; no solo su referencia.

## Campos controlados por el servidor

Los DTO de entrada no contienen `id` ni `fechaCreacion`. Si se envían como campos
adicionales, se ignoran. POST siempre crea un registro nuevo; PUT usa el ID de la
ruta y conserva la fecha de creación. Los atributos adicionales dentro de
`cliente` no modifican los datos del cliente.

PUT conserva la semántica de reemplazo actual: se deben enviar los campos
obligatorios y los opcionales que se quieran conservar. No es una actualización
parcial; omitir `activo` o `etapa` aplica los valores predeterminados descritos.

Las validaciones de entrada devuelven HTTP 400 con un mapa de campos y mensajes,
por ejemplo `{"cliente.id":"..."}` o `{"correo":"..."}`. Los errores de negocio
mantienen `{"mensaje":"..."}`; los valores de enum inválidos o JSON mal formado
también devuelven HTTP 400.

## Verificación

`ClientesOportunidadesDtoIntegrationTests` verifica creación, edición, consulta,
listado y eliminación sin una transacción de prueba abierta, además de campos
internos protegidos, valores predeterminados y entradas inválidas.
`ValidacionesComercialesIntegrationTests` conserva los casos de RUC y referencias.
El recorrido HTTP y la colección Postman comprueban la compatibilidad con las
cotizaciones y sus detalles. Las pruebas usan H2 aislada; no escriben en MySQL.
