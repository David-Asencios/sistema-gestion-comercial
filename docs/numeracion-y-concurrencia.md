# Numeración y operaciones simultáneas de cotizaciones

## Formato comercial

Se conserva `COT-00001`: COT identifica la cotización y el número es un correlativo
global, sin reinicio anual. Cinco dígitos son el ancho mínimo, no un límite:
después de `COT-99999` sigue `COT-100000`.

El código comercial se mantiene separado del ID de persistencia. Los códigos
existentes no se renumeran. Eliminar un borrador no devuelve su número al contador.
Una creación revertida no reserva un número, porque contador y cotización se
guardan dentro de la misma transacción.

## Contador persistente

`secuencia_cotizacion` contiene una fila, con ID 1 y el último número utilizado.
La creación bloquea esa fila mediante `PESSIMISTIC_WRITE`, incrementa el número y
guarda la cotización. El bloqueo se libera al confirmar o revertir la transacción.
Esto coordina también procesos distintos que usen la misma base de datos.
Se conserva la restricción única de `cotizaciones.codigo` como protección adicional.

Aplicar una vez `backend/src/main/resources/db/manual/002_secuencia_cotizacion.sql`
con la aplicación detenida. Inicializa el contador desde el mayor sufijo numérico
de los códigos existentes; no utiliza el orden alfabético de los textos.
No ejecutar si la tabla ya existe ni reinicializar el contador al reiniciar la aplicación.
En una instalación vacía, crear primero las tablas JPA y luego inicializar esta fila
antes de registrar cotizaciones. Las pruebas H2 la inicializan con `data-test.sql`.

## Integridad de detalles, totales y estados

Crear, actualizar y eliminar detalles bloquea primero su cotización. Cambiar el
estado, editar la cabecera o eliminar una cotización utiliza el mismo bloqueo.
Por eso, un envío no puede intercalarse en mitad de un cambio de sus detalles.
Las cotizaciones diferentes pueden modificarse de manera independiente.

Al trasladar un detalle se bloquean origen y destino en orden creciente de ID.
Después se vuelve a leer el detalle. Si otra transacción ya lo trasladó a una
cotización diferente de la detectada al empezar, la operación falla con 409;
si lo eliminó, devuelve 404. No se modifica una cotización cuyo bloqueo no se tomó.

Las escrituras de detalles usan `READ_COMMITTED`: las sumas ven los cambios
confirmados por la transacción anterior después de esperar el bloqueo, incluso
en MySQL, cuya configuración predeterminada local es REPEATABLE_READ.
Un conflicto o fallo de bloqueo traducido por Spring devuelve HTTP 409 y permite
al consumidor actualizar sus datos y volver a intentar la operación.

Los bloqueos protegen cada transacción. Todavía no existe una versión enviada por
el frontend para detectar formularios abiertos hace tiempo: dos ediciones válidas
del mismo campo se ejecutan en orden y prevalece la última. Esa detección requiere
un contrato de versiones y se puede incorporar al desarrollar el editor Angular.

## Pruebas

La suite habitual prueba ocho creaciones y ocho altas de detalles simultáneas,
actualizaciones, eliminaciones, traslados opuestos y carreras entre envío y edición
o eliminación del último detalle. Comprueba códigos, sumas, IGV, total y estados.
También verifica el paso a seis dígitos, no reutilizar números eliminados y rollback.

Para ejecutar los mismos diez casos sobre MySQL, preparar un esquema local vacío
llamado `gestion_comercial_concurrency_test` y establecer estas variables:

```powershell
$env:MYSQL_CONCURRENCY_URL = 'jdbc:mysql://localhost:3306/gestion_comercial_concurrency_test'
$env:MYSQL_TEST_USERNAME = 'usuario_de_pruebas'
$env:MYSQL_TEST_PASSWORD = 'clave_de_pruebas'
Set-Location backend
.\mvnw.cmd test '-Dtest=ConcurrenciaCotizacionIntegrationTests'
```

Las pruebas crean y eliminan tablas, por eso la URL admite exclusivamente ese
nombre de esquema local. No apuntarlas a una base con datos que se deban conservar.
Al terminar, retirar las tres variables de la sesión PowerShell.

## Resultado de la verificación local

El 1 de noviembre de 2026 pasaron 67 pruebas sobre H2 y los diez casos de concurrencia
también pasaron sobre MySQL 8.0.34 en el esquema aislado. Se aplicó el script 002 a
`sistema_gestion_comercial`: el contador quedó en 6, conservando los códigos,
subtotales, IGV y totales de las seis cotizaciones existentes. El siguiente número
será 7 si no se registran otras cotizaciones antes. No volver a aplicar el script
002 en esa base ni ejecutar en ella `data-test.sql`.
