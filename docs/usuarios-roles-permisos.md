# Usuarios, roles y permisos

Fecha de propuesta: 5 de noviembre de 2026.
Estado: roles ADMINISTRADOR y COMERCIAL y cartera compartida confirmados por el
usuario; detalle de permisos propuesto para revisión y todavía no aplicado al backend.

## Situación actual y alcance

Existen seis recursos comerciales: clientes, oportunidades, cotizaciones,
detalles, servicios y productos. Actualmente no hay entidades de usuario,
autenticación, roles ni responsables asignados a registros.

Esta definición propone los permisos de la primera versión. No cambia endpoints,
dependencias, esquema ni datos. El mecanismo de autenticación y sus contratos
se describen como propuesta en [autenticación y sesiones](autenticacion-sesiones.md),
tomando como base la cartera compartida.

## Roles definidos

Cada usuario tendrá exactamente un rol:

- ADMINISTRADOR: administra cuentas, catálogos y operaciones comerciales.
- COMERCIAL: registra clientes y oportunidades, prepara cotizaciones y realiza
  su seguimiento comercial.

COMERCIAL será el rol habitual para el trabajo diario. Ambos roles tendrán
usuario y contraseña y podrán consultar información y descargar PDF.

Los permisos serán fijos por rol en la primera versión. El formulario de cuentas
permitirá asignar un rol, sin un editor de permisos individuales.

## Matriz de operaciones comerciales

Sí permite la operación únicamente si también cumple las reglas del recurso.

| Operación | ADMINISTRADOR | COMERCIAL |
|---|---|---|
| Listar, buscar y consultar clientes | Sí | Sí |
| Crear y editar clientes, incluido su activo | Sí | Sí |
| Eliminar clientes | Sí | No |
| Listar, buscar y consultar oportunidades | Sí | Sí |
| Crear y editar oportunidades, incluida su etapa | Sí | Sí |
| Eliminar oportunidades | Sí | No |
| Listar, buscar y consultar cotizaciones | Sí | Sí |
| Crear cotizaciones | Sí | Sí |
| Editar cabeceras en BORRADOR | Sí | Sí |
| Eliminar BORRADOR sin detalles | Sí | Sí |
| Listar y consultar detalles | Sí | Sí |
| Crear, editar y eliminar detalles en BORRADOR | Sí | Sí |
| Registrar envío, aceptación, rechazo o vencimiento | Sí | Sí |
| Descargar PDF | Sí | Sí |
| Listar, buscar y consultar servicios y productos | Sí | Sí |
| Crear o editar servicios y productos, incluido su activo | Sí | No |
| Eliminar servicios y productos | Sí | No |

La eliminación de clientes y oportunidades se reserva al administrador para
reducir eliminaciones accidentales de información compartida. Un comercial puede
desactivar un cliente mediante su campo activo, sin borrar su historial.

Mantener catálogos corresponde al administrador. El comercial puede negociar el
precio unitario y los descuentos del detalle con las reglas actuales; eso no
modifica el precio referencial del catálogo. No se propone un límite de descuento
adicional ni un circuito de aprobación en esta etapa.

Registrar ACEPTADA significa dejar constancia de la respuesta del cliente.
La autenticación de empleados no convierte esa acción en una firma del cliente.
No se incorpora un portal de clientes ni un envío de correo automático.

## Reglas comerciales que ningún rol puede saltarse

- Una cotización se crea como BORRADOR, con código y cálculos del servidor.
- Cabecera y detalles solo se modifican mientras sea BORRADOR.
- Un borrador solo se elimina si no tiene detalles asociados.
- El envío requiere detalles y una vigencia válida.
- Solo una ENVIADA puede pasar a ACEPTADA, RECHAZADA o VENCIDA, según sus reglas.
- ACEPTADA, RECHAZADA y VENCIDA no se reabren.
- El vencimiento se registra según las fechas existentes; no se vuelve automático.
- Los descuentos, los importes y la numeración conservan sus validaciones y bloqueos.
- No se permiten eliminaciones que violen relaciones existentes.

El administrador tendrá permisos para operar, sin una ruta que ignore estas reglas.
Los detalles se podrán trasladar solo cuando ambas cotizaciones sean editables,
igual que en el servicio actual.

## Alcance de la cartera: compartida, confirmado

El usuario confirmó el 5 de noviembre de 2026 la cartera compartida del equipo
para la primera versión.

Los dos roles consultan la cartera completa. Administrador y Comercial operan
los registros de todo el equipo según la matriz, independientemente de quién
los creó.

No se agregarán responsables ni restricciones por propietario en esta versión.
Registrar quién crea o modifica un registro es una necesidad de trazabilidad
distinta de asignar quién puede verlo o gestionarlo; la cartera compartida
permite incorporar esa trazabilidad posteriormente.

## Gestión de cuentas propuesta

| Acción | ADMINISTRADOR | COMERCIAL |
|---|---|---|
| Consultar su propio perfil | Sí | Sí |
| Cambiar su propia contraseña | Sí | Sí |
| Listar y consultar otras cuentas | Sí | No |
| Crear cuentas y asignar roles | Sí | No |
| Editar nombre y rol de otras cuentas | Sí | No |
| Activar o desactivar otras cuentas | Sí | No |
| Restablecer credenciales de otra cuenta | Sí | No |
| Cambiar su propio rol desde el perfil | No | No |

Se propone desactivar cuentas en lugar de eliminarlas físicamente. El nombre
de acceso será único y estable, sin reutilizarlo para otra persona.
Un administrador no podrá desactivar su propia cuenta ni quitarse su propio
rol de administrador. Tampoco se permitirá dejar el sistema sin administrador
activo, incluso ante operaciones simultáneas.

Las cuentas inactivas no podrán iniciar sesión ni seguir accediendo con una
sesión previamente emitida. Un cambio de rol deberá afectar también al acceso
posterior de sesiones existentes; no bastará con ocultar botones en Angular.

No habrá registro público: las cuentas las crea el administrador. La primera
cuenta de administrador se creará mediante un procedimiento inicial controlado,
sin contraseña predeterminada publicada en el código ni en Postman.

El administrador establecerá una clave temporal, la comunicará por un canal
acordado y se exigirá cambiarla antes de permitir operaciones comerciales.
La propuesta de autenticación define su vigencia de 24 horas y la sesión restringida.

## Datos mínimos del usuario

Propuesta de campos para la entidad futura:

| Campo | Finalidad |
|---|---|
| id | Identificador interno del servidor. |
| nombreCompleto | Identificar a la persona. |
| username | Nombre único de acceso, normalizado sin espacios externos y en minúsculas. |
| passwordHash | Guardar el hash; nunca devolverlo en respuestas. |
| rol | ADMINISTRADOR o COMERCIAL. |
| activo | Permitir o bloquear acceso. |
| fechaCreacion | Fecha asignada por el servidor. |
| requiereCambioClave | Restringir credenciales temporales al cambio de contraseña. |

La propuesta de autenticación agrega versionSeguridad y claveTemporalExpiraEn
para revocación y claves temporales. El cliente HTTP no podrá asignarse un rol mediante
login, perfil o un request comercial. Cada operación administrativa validará
que quien la solicita tiene el rol correspondiente.

## Aplicación a las rutas existentes

| Recurso o ruta | Lectura | Escritura |
|---|---|---|
| /api/clientes | Ambos roles | POST y PUT: Administrador/Comercial. DELETE: Administrador. |
| /api/oportunidades | Ambos roles | POST y PUT: Administrador/Comercial. DELETE: Administrador. |
| /api/cotizaciones | Ambos roles | POST, PUT y DELETE: Administrador/Comercial, con reglas de estado. |
| /api/cotizaciones/{id}/estado | No aplica | PATCH: Administrador/Comercial. |
| /api/cotizaciones/{codigo}/pdf | Ambos roles | No aplica. |
| /api/detalles-cotizacion | Ambos roles | POST, PUT y DELETE: Administrador/Comercial, con reglas de borrador. |
| /api/servicios y /api/productos | Ambos roles | POST, PUT y DELETE: Administrador. |

La lectura comprende listado, búsqueda paginada, consulta por ID, por código y
detalles por cotización cuando estén disponibles. El alcance es toda la cartera.

Las rutas futuras de usuarios y autenticación todavía no existen.
Su implementación deberá proteger el backend aunque se invoque directamente
desde Postman, sin pasar por Angular.

## Respuestas y casos que se deberán probar al implementar

- Sin autenticación válida en un recurso protegido: HTTP 401.
- Usuario autenticado sin permiso para la acción: HTTP 403.
- Permiso suficiente, pero datos o transición inválidos: conservar los errores
  de negocio 400, 404 o 409 que correspondan.
- Ambos roles pueden consultar la cartera y descargar PDF.
- Comercial no puede modificar catálogos ni administrar usuarios.
- Ningún request de perfil o comercial puede aumentar privilegios.
- Cuenta desactivada y cambios de rol afectan a sesiones existentes.
- Todos los roles mantienen las reglas de descuentos, estados y concurrencia.
- No se revela passwordHash ni información de credenciales en respuestas.
- Una credencial temporal no permite entrar al flujo comercial sin cambiarla.
- Comprobar que Comercial accede a registros creados por otros usuarios según
  sus permisos sobre la cartera compartida.

Las pruebas existentes y la colección Postman deberán adaptarse a autenticación
cuando se implemente. En esta etapa siguen comprobando el backend actual,
sin permisos aplicados.

## Orden de implementación posterior

1. Revisar el detalle de permisos; los dos roles y la cartera compartida ya están confirmados.
2. Revisar el diseño de autenticación y sesiones documentado, manteniendo
   Java, Spring Boot y MySQL.
3. Implementar usuarios, roles y preparación controlada del primer administrador.
4. Implementar autenticación y aplicar la matriz en los endpoints.
5. Verificar permisos por rol, reglas comerciales y colección Postman.
6. Incorporar inicio de sesión y navegación según rol en Angular.
