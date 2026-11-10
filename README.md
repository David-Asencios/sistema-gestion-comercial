# Sistema de Gestión Comercial

Backend Java 21, Spring Boot, JPA y MySQL 8. El frontend Angular está pendiente.

## Ejecución local

La base de desarrollo configurada en IntelliJ es `sistema_gestion_comercial`.
Las credenciales se reciben mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.
`.env.example` documenta las variables; `.env` no se versiona ni se carga automáticamente por Spring.

En IntelliJ, configurar esas variables en la ejecución de `BackendApplication`.
Desde PowerShell, en la raíz del repositorio, cargar el `.env` sin ejecutar su contenido:

```powershell
Get-Content -LiteralPath .env | ForEach-Object {
    if ($_ -match '^\s*(DB_URL|DB_USERNAME|DB_PASSWORD)\s*=\s*(.*)\s*$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim().Trim('"').Trim("'"), 'Process')
    }
}
Set-Location backend
.\mvnw.cmd spring-boot:run
```

Antes del primer arranque con descuentos, aplicar una sola vez
`backend/src/main/resources/db/manual/001_descuentos_detalle.sql` sobre la base existente.
El script agrega cuatro columnas con valores iniciales `NINGUNO` y cero, sin recalcular
los importes históricos. MySQL hace commit implícito de este cambio de esquema.
No ejecutarlo si las columnas ya existen. La aplicación todavía usa `ddl-auto=update`;
el versionado automático de migraciones queda pendiente.

Para la numeración concurrente, aplicar también una vez
`backend/src/main/resources/db/manual/002_secuencia_cotizacion.sql`, con la aplicación
detenida. Crea e inicializa el contador sin cambiar los códigos existentes.

## Pruebas

```powershell
Set-Location backend
.\mvnw.cmd test
```

Las pruebas habituales usan H2 en memoria mediante el perfil `test`; no usan ni modifican MySQL.
Incluyen API HTTP, persistencia, descuentos, recálculos, bloqueo por estado y generación de PDF.
Los documentos de verificación se generan en `backend/target/pdf-verification/`.

Para comprobar el esquema MySQL y las consultas reales, cargar las variables de conexión,
establecer `$env:MYSQL_VERIFY = 'true'` y ejecutar
`.\mvnw.cmd '-Dtest=MySqlSchemaTests' test` desde `backend/`.
Esta comprobación fuerza `ddl-auto=validate` y solo lee datos. Después, eliminar la variable
con `Remove-Item Env:MYSQL_VERIFY`.

El contrato y las reglas de descuentos están documentados en [docs/descuentos.md](docs/descuentos.md).
Los DTO de clientes y oportunidades, sus campos editables y valores predeterminados
se describen en [docs/dto-clientes-oportunidades.md](docs/dto-clientes-oportunidades.md).
Los contratos de servicios y productos y los límites de sus precios están en
[docs/dto-catalogos.md](docs/dto-catalogos.md).
Las búsquedas, filtros y paginación de clientes, oportunidades, cotizaciones,
servicios y productos se describen en
[docs/busquedas-paginadas.md](docs/busquedas-paginadas.md).
Las reglas de clientes, oportunidades y envío/aceptación se explican en
[docs/validaciones-comerciales.md](docs/validaciones-comerciales.md).
La numeración y los bloqueos se explican en [docs/numeracion-y-concurrencia.md](docs/numeracion-y-concurrencia.md).
El recorrido HTTP integrado y la colección importable se describen en [postman/README.md](postman/README.md).
La propuesta de usuarios, roles y permisos está en
[docs/usuarios-roles-permisos.md](docs/usuarios-roles-permisos.md).
Todavía no está aplicada al backend; están confirmados los roles Administrador
y Comercial, con cartera compartida.
El diseño propuesto de login, cookies, caducidad y revocación de sesiones está en
[docs/autenticacion-sesiones.md](docs/autenticacion-sesiones.md); aún no está implementado.
