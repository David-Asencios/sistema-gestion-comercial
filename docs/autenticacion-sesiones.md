# Inicio de sesión y gestión de sesiones

Fecha: 5 de noviembre de 2026.
Estado: diseño propuesto para revisión; no implementado.
Base confirmada: roles ADMINISTRADOR y COMERCIAL, con cartera compartida.

## Elección para la primera versión

Propuesta: Spring Security para autenticar y autorizar; Spring Session JDBC
para guardar sesiones en el MySQL existente. Angular utilizará una cookie de
sesión enviada automáticamente por el navegador.

El navegador conserva un identificador opaco y el servidor conserva la sesión.
Esta elección permite cerrar sesiones desde el servidor y compartir el estado
entre instancias que usen la misma base. No requiere incorporar Redis.
No se propone JWT ni un flujo de refresh tokens para esta aplicación web.
La elección se podrá revisar si más adelante aparecen clientes móviles o
integraciones con necesidades diferentes.

La persistencia de HttpSession mediante JDBC está documentada en la
[guía oficial de Spring Session](https://docs.spring.io/spring-session/reference/guides/boot-jdbc.html).
Es una capacidad del framework; los tiempos y reglas siguientes son decisiones
propuestas para este proyecto, no valores que ya estén configurados.

## Reglas de la sesión

| Aspecto | Propuesta |
|---|---|
| Inicio de sesión | Username y contraseña; el servidor obtiene el rol. |
| Inactividad | Expirar tras 30 minutos sin actividad autenticada en la API. |
| Duración absoluta | Expirar a las 8 horas del login, aunque exista actividad. |
| Renovación | La actividad renueva solo el límite de inactividad. |
| Inicio automático permanente | No se incorpora «recordarme» en esta versión. |
| Varios dispositivos | Permitidos; cada navegador mantiene su propia sesión. |
| Cerrar sesión | Revoca la sesión del navegador actual. |
| Cerrar todas | Revoca todas las sesiones del usuario, incluida la actual. |
| Cambio de contraseña o restablecimiento | Revoca todas las sesiones. |
| Cambio de rol, desactivación o reactivación | Revoca las sesiones anteriores. |

Los límites serán configurables y se aplicarán en el servidor. El plazo absoluto
se calcula desde el login exitoso, no desde la creación de una sesión anónima.
No habrá un heartbeat de Angular destinado a mantener la sesión viva.

Recargar la página conserva el acceso si la sesión sigue válida. Las pestañas
del mismo navegador comparten la cookie: cerrar sesión afecta a todas ellas.
Cerrar el navegador no se considerará una revocación fiable, porque puede
restaurar cookies; los límites y la revocación del servidor siguen siendo la autoridad.

La revocación bloquea las peticiones posteriores a su confirmación. No cancela
automáticamente una transacción comercial que ya estaba en ejecución.

## Cookies, despliegue y protección de peticiones

Para producción se propone servir Angular y `/api` bajo el mismo origen HTTPS,
utilizando un proxy hacia Spring Boot. En desarrollo, Angular utilizará un proxy
local para `/api` hacia el backend. El host definitivo se configurará al desplegar.

| Cookie | Uso y atributos |
|---|---|
| __Host-SESSION en producción | Identificador de sesión, HttpOnly, Secure, SameSite=Lax, Path=/, sin Domain. |
| SESSION en desarrollo HTTP local | Identificador de sesión, HttpOnly, SameSite=Lax, Path=/; Secure desactivado únicamente en ese perfil. |
| XSRF-TOKEN | Token CSRF legible por Angular, SameSite=Lax, Path=/, Secure en producción. No autentica al usuario. |

La cookie de sesión no tendrá duración persistente de «recordarme». El ID nunca
se devolverá en JSON ni se enviará por URL. No se guardará una credencial de
autenticación en localStorage o sessionStorage.

Angular enviará `X-XSRF-TOKEN` en POST, PUT, PATCH y DELETE. La protección CSRF
incluye login, logout y cambio de contraseña. Las lecturas GET no modificarán
datos comerciales.

`GET /api/auth/csrf` permitirá obtener el token antes del login. Tras login y
logout se obtendrá uno nuevo antes de otra escritura: Spring Security elimina
el anterior en esas operaciones. Para SPA se debe configurar también el manejo
de tokens diferidos y codificados descrito en su
[documentación oficial de CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
La cookie CSRF legible y la cookie de sesión HttpOnly cumplen funciones distintas.

Si se habilita un origen separado, se usará una lista explícita de orígenes con
credenciales; no un comodín. Se deberá verificar también la política de cookies
y el envío del encabezado CSRF. Esa topología no es la base de esta propuesta.

## Endpoints de autenticación propuestos

Todos son futuros; actualmente no existen.

| Método y ruta | Acceso | Resultado |
|---|---|---|
| GET /api/auth/csrf | Público | 200; entrega cookie XSRF-TOKEN y nombre de encabezado. |
| POST /api/auth/login | Público, con CSRF válido | 200; sesión y perfil, o 401 por credenciales inválidas. |
| GET /api/auth/me | Sesión válida, también temporal | 200; perfil actual y plazos de sesión. |
| POST /api/auth/logout | Sesión válida, también temporal, con CSRF | 204; revoca sesión actual y elimina cookies. |
| POST /api/auth/logout-todas | Sesión válida, también temporal, con CSRF | 204; revoca todas las sesiones y elimina cookies actuales. |
| POST /api/auth/cambiar-clave | Sesión válida, también temporal, con CSRF | 204; cambia clave y revoca todas las sesiones. |

Sesión ausente, vencida o revocada en un endpoint protegido: 401 JSON, sin
redirección HTML. Logout de una sesión ya inválida también devuelve 401; Angular
limpia su estado local y lo trata como cierre efectivo, no como error recuperable.

### Inicio de sesión

```http
POST /api/auth/login
Content-Type: application/json
X-XSRF-TOKEN: valor obtenido previamente
```

```json
{
  "username": "comercial.demo",
  "password": "valor introducido por el usuario"
}
```

El ejemplo no representa una cuenta ni una contraseña precargadas.
Username se normaliza con espacios externos eliminados y minúsculas; la
contraseña no se recorta ni transforma. El body no admite elegir rol ni activo.

El servidor verifica credenciales, cuenta activa y vigencia de una clave
temporal. Un usuario inexistente, desactivado o una contraseña incorrecta o
temporal vencida reciben el mismo mensaje: «Usuario o contraseña incorrectos».
Un formato de request inválido devuelve 400.

Al autenticar, se rota el identificador de sesión, se persiste la autenticación
y se registra el inicio del plazo absoluto. Una cookie previa nunca conserva
el mismo ID autenticado.

Respuesta conceptual:

```json
{
  "usuario": {
    "id": 1,
    "username": "comercial.demo",
    "nombreCompleto": "Usuario de ejemplo",
    "rol": "COMERCIAL",
    "requiereCambioClave": false
  },
  "sesion": {
    "inactividadSegundos": 1800,
    "expiraPorInactividadEn": "2026-11-05T16:30:00Z",
    "expiraAbsolutamenteEn": "2026-11-06T00:00:00Z"
  }
}
```

Los instantes son ilustrativos. En la implementación se expresarán en UTC con
ISO 8601 y la interfaz los mostrará en hora local. La expiración por inactividad
informada corresponde a ese instante; otra petición puede renovarla hasta
el límite absoluto. GET me usa el mismo contrato, con datos actuales.

### Cambio de contraseña

```json
{
  "passwordActual": "contraseña actual",
  "passwordNueva": "nueva contraseña elegida"
}
```

Se propone una longitud de 12 a 128 caracteres para las nuevas contraseñas,
permitiendo frases y sin exigir combinaciones artificiales de tipos de caracteres.
Se valida en el backend, sin truncamiento silencioso. La nueva no puede coincidir
con la actual. Angular pedirá confirmación, pero el backend recibe un solo valor nuevo.
Una clave actual incorrecta o nueva inválida devuelve 400 y no cambia credenciales.

El éxito revoca todas las sesiones, elimina cookies actuales y requiere
obtener CSRF e iniciar sesión de nuevo. No hay un cambio de contraseña público.

## Contraseñas temporales y recuperación

Las cuentas nuevas y los restablecimientos administrativos utilizarán una clave
temporal de 24 horas, con `requiereCambioClave=true` y `claveTemporalExpiraEn`.
Un login válido con esa clave abre solo una sesión restringida: CSRF, me,
logout, logout-todas y cambiar-clave. El resto de la API devuelve 403 con código
`CAMBIO_CLAVE_REQUERIDO`.

El administrador establecerá la clave temporal desde un formulario protegido y
la comunicará por un canal acordado fuera de la aplicación. El backend recibe
ese valor solo en creación o restablecimiento, guarda su hash y nunca lo
devuelve en la respuesta. No habrá envío automático de correo en esta etapa.

El restablecimiento de otra cuenta será una operación administrativa futura,
`POST /api/usuarios/{id}/restablecer-clave`, no un endpoint público de recuperación.
No permite recuperar la contraseña anterior. El usuario que olvide su clave
solicitará un restablecimiento al administrador.

El primer administrador se preparará mediante configuración inicial privada,
exclusivamente cuando no existan usuarios, con cambio obligatorio de clave.
El proceso deberá impedir dos altas iniciales simultáneas, no sobrescribir cuentas
al reiniciar y permitir retirar la credencial de configuración tras el alta.
El procedimiento de recuperación si se pierde el único administrador será
operativo y controlado, sin una ruta pública que omita la autenticación.

## Almacenamiento y revocación efectiva

Se propone almacenar contraseñas con PBKDF2 adaptativo mediante PasswordEncoder,
con sal aleatoria por clave e identificador del algoritmo para permitir futuras
actualizaciones. El costo se calibrará en el entorno real. No se implementará
un algoritmo criptográfico propio. Spring Security documenta funciones adaptativas
y almacenamiento con formato versionado en
[Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).

El usuario incorporará `versionSeguridad`, además de los campos definidos en
[roles y permisos](usuarios-roles-permisos.md). La sesión conservará un principal
mínimo con ID de usuario, versión de seguridad e instante de autenticación;
no guardará una entidad JPA ni la contraseña en claro.

Antes de autorizar cada petición protegida, se comprobarán en el servidor:

1. Existencia de sesión y límites temporal y absoluto.
2. Existencia del usuario y su estado activo.
3. Coincidencia de versionSeguridad de la sesión con la vigente.
4. Restricción de cambio de clave y permisos del rol.

El servidor no confiará indefinidamente en un rol copiado al iniciar sesión.
Desactivación, reactivación, cambio de rol, cambio/restablecimiento de clave y
logout-todas incrementarán versionSeguridad de forma atómica. El perfil vigente
se consultará sin una caché que retrase esa revocación.

Spring Session permitirá eliminar además las sesiones indexadas del usuario.
La comprobación de versión evita que una petición concurrente que vuelva a
guardar una sesión antigua le devuelva validez. Login verificará que el estado y
la versión de credenciales no hayan cambiado entre comprobar la clave y emitir
la sesión, para no autenticar una clave recién restablecida con la versión nueva.

Los cambios de cuenta y versión se confirmarán en la misma transacción.
Logout actual invalidará HttpSession, limpiará SecurityContext y eliminará la
cookie con los mismos atributos de nombre, Path y Domain usados al crearla.

Para login JSON personalizado, guardar el contexto y aplicar la estrategia de
autenticación de sesión son pasos explícitos a implementar y probar, según
[Spring Security: persistencia y sesiones](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
No basta con asignar una variable en el controlador.

## Respuestas, intentos y experiencia de Angular

Los errores de autenticación tendrán `codigo` y `mensaje`. Los errores comerciales
existentes conservarán su contrato.

| Estado | Código propuesto | Comportamiento |
|---|---|---|
| 400 | REQUEST_INVALIDO | Datos de login o cambio de clave inválidos. |
| 401 | CREDENCIALES_INVALIDAS | Fallo de login con mensaje genérico. |
| 401 | SESION_NO_VALIDA | Sesión ausente, vencida o revocada; volver a login. |
| 403 | ACCESO_DENEGADO | Sesión válida sin permiso de rol. |
| 403 | CAMBIO_CLAVE_REQUERIDO | Ir al cambio de clave; no al flujo comercial. |
| 403 | CSRF_INVALIDO | Recuperar token y mostrar error; no repetir escrituras automáticamente. |
| 429 | DEMASIADOS_INTENTOS | Respetar Retry-After antes de reintentar. |

En operaciones protegidas, una sesión no válida debe producir 401, aunque también
falte CSRF. Login es público, pero un CSRF inválido produce 403. Los handlers de
seguridad deberán implementar y probar esta distinción.

Se propone limitar los intentos fallidos por username normalizado: cinco en
15 minutos producen una pausa de 15 minutos; las peticiones durante esa pausa no
la alargan. El control se aplica también a nombres inexistentes, sin revelar
cuentas válidas. Adicionalmente se propone un máximo de 30 intentos de login por
minuto y origen IP. Los límites serán configurables y tendrán estado compartido
en MySQL con limpieza de entradas vencidas; no crearán cuentas ficticias.
La IP solo se obtendrá de encabezados de proxy cuando el proxy sea confiable.

Angular obtendrá me al arrancar y usará ese perfil para la navegación. Un 401
limpiará datos locales del usuario y mostrará login con el mensaje de sesión
expirada. No reenviará automáticamente un POST/PUT/PATCH/DELETE tras autenticar.
Cerrar sesión también limpiará datos comerciales en memoria; no se guardarán
contraseñas ni se registrarán en consola.

Respuestas de autenticación, perfiles y PDF privado llevarán Cache-Control:
no-store. La descarga PDF conservará la autorización de la cookie; la interfaz
comprobará el estado HTTP antes de guardar una respuesta como archivo.
No se registrarán cookies, contraseñas ni tokens CSRF en los logs.

## Cambios previstos al implementar

- Agregar las dependencias de Spring Security, Spring Session JDBC y sus pruebas,
  usando las versiones administradas por Spring Boot.
- Crear usuarios y campos de seguridad, además de las tablas JDBC de sesiones
  e índices para principal y expiración. Preparar SQL explícito para MySQL;
  no depender de ddl-auto para generar tablas de Spring Session.
- Agregar configuración de cookies por entorno y los tiempos de sesión.
- Incorporar filtros, autenticación, handlers JSON y permisos de los dos roles.
- Aplicar denegación por defecto: públicas solo las rutas de autenticación
  declaradas y los recursos necesarios para servir la interfaz.
- Adaptar Postman a cookies y CSRF y mantener las pruebas comerciales.

Este diseño no cambia todavía el pom, la base de datos ni la configuración en
ejecución. Los requests de esta página son contratos propuestos, no endpoints
disponibles para probar hoy.

## Casos de aceptación para la implementación

1. Login de ambos roles, contraseña incorrecta, cuenta inexistente e inactiva.
2. ID de sesión rotado en login y autenticación persistente entre peticiones.
3. Cookie HttpOnly, HTTPS y atributos correctos por entorno.
4. CSRF requerido también en login/logout y token renovado tras ambos.
5. Caducidad por inactividad y por límite absoluto con reloj controlado.
6. Recargar página conserva la sesión; logout revoca y la cookie antigua falla.
7. Logout actual mantiene otro navegador; logout-todas invalida ambos.
8. Desactivar, cambiar rol o contraseña invalida sesiones antiguas incluso si
   una petición concurrente intenta volver a guardarlas.
9. Login concurrente con restablecimiento no autentica credenciales antiguas.
10. Clave temporal expirada rechazada; vigente restringida hasta cambio.
11. Cambiar clave exige la actual, revoca sesiones y permite nuevo login.
12. Comercial no puede administrar cuentas o catálogos; ambos consultan y
    operan cotizaciones conforme a sus permisos y reglas comerciales.
13. Límites de intentos, Retry-After y expiración del bloqueo temporal.
14. Inicialización única del primer administrador y protección del último activo.
15. Contratos 400/401/403/429 JSON y acceso autorizado al PDF.
16. Pruebas HTTP y Postman con cookies y CSRF sobre una base aislada.

La validación de este documento es de diseño y coherencia con la API actual.
No se han ejecutado pruebas de autenticación, porque su código aún no existe.
