# Validaciones de clientes, oportunidades y estados

## Clientes

El RUC sigue siendo opcional. Al crear o actualizar un cliente, un RUC nulo,
vacío o compuesto solo por espacios se guarda como `null`. Pueden existir varios
clientes sin RUC. Los valores informados se guardan sin espacios en los extremos.

Actualizar un cliente que no tenía RUC ya no provoca un error interno.
Se permite asignar, conservar o quitar su RUC. Un RUC ya utilizado por otro
cliente devuelve HTTP 409 y no modifica el registro existente.
Se mantiene la validación actual de longitud máxima; no se introduce validación
del dígito verificador ni consultas a servicios externos.

## Oportunidades

Los endpoints siguen recibiendo la referencia `"cliente": {"id": 1}`.
Se aplica la misma validación al crear y actualizar:

- Referencia ausente, nula, sin ID o con ID no positivo: HTTP 400.
- ID positivo que no corresponde a un cliente: HTTP 404.
- Referencia válida: se carga el cliente existente desde el repositorio.

Los demás atributos enviados dentro de esa referencia no actualizan al cliente.
Una petición inválida no crea una oportunidad ni altera la existente.

## Envío y aceptación de cotizaciones

Se conserva `PATCH /api/cotizaciones/{id}/estado`, cuyo cuerpo es una cadena JSON,
por ejemplo `"ENVIADA"`.

| Transición | Condiciones adicionales |
|---|---|
| BORRADOR → ENVIADA | Tener al menos un detalle y una fecha de vencimiento vigente. |
| ENVIADA → ACEPTADA | Tener una fecha de vencimiento vigente. |
| ENVIADA → VENCIDA | Tener una fecha de vencimiento anterior al día actual. |
| ENVIADA → RECHAZADA | Se conserva el comportamiento previo, incluso si pasó la fecha. |

La fecha de vencimiento es inclusiva: durante ese día todavía se puede enviar
y aceptar. Se utiliza la fecha local del servidor, igual que en la creación de
cotizaciones. Un fallo de estas reglas devuelve HTTP 400 sin cambiar el estado.
ACEPTADA, RECHAZADA y VENCIDA siguen siendo estados finales.

Una cotización con detalles puede tener total cero, por ejemplo con un descuento
del 100 %. No se confunde ese caso con una cotización vacía.

Esta etapa no agrega tareas automáticas de vencimiento: una cotización enviada
cuya fecha pasó permanece ENVIADA hasta que se marque VENCIDA o RECHAZADA;
su aceptación se bloquea inmediatamente por fecha. Un borrador vencido puede
corregir su fecha o vigencia antes del envío.

## Verificación y alcance

Las pruebas HTTP comprueban respuestas, conservación del estado ante errores,
RUC opcional y duplicado, referencias de clientes y límites del vencimiento.
Se ejecutan con H2 en memoria; sus datos se revierten al terminar cada prueba.
No requiere cambios de esquema ni modificaciones de los registros de MySQL.
La numeración y los cambios simultáneos se explican en
[numeracion-y-concurrencia.md](numeracion-y-concurrencia.md).
