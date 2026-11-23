# TFI-ControlAcceso-API

API REST de gestión de usuarios y control de acceso, desarrollada con Spring Boot. La implementación proviene del proyecto `gestionUsuario` y conserva su lógica de negocio.

## Funcionalidades

- Alta de usuarios con estado activo y contador de intentos fallidos en cero.
- Inicio de sesión del administrador de acceso desde el embebido, mediante tarjeta y PIN.
- Bloqueo del usuario al acumular tres contraseñas incorrectas.
- Registro de los inicios de sesión exitosos en `authentication_events`.
- Alta individual e importación CSV de patentes autorizadas.
- Registro y consulta de titulares de autos.

## Tecnologías y requisitos

- JDK 17 como versión objetivo del proyecto. La compilación también fue validada con JDK 21.
- Spring Boot 3.5.0 y Spring Data JPA.
- MySQL disponible para ejecutar la aplicación.
- Lombok y MapStruct para generación de código.
- Maven Wrapper incluido: no es necesario instalar Maven por separado.

La primera ejecución del wrapper requiere acceso a Internet para descargar Maven y las dependencias. Java debe estar disponible en `PATH`; si se define `JAVA_HOME`, debe apuntar al JDK.

## Configuración de la base de datos

La configuración se encuentra en [application.properties](src/main/resources/application.properties). Por defecto utiliza:

| Propiedad | Valor |
| --- | --- |
| Servidor | `localhost:3306` |
| Base de datos | `gestionusuario` |
| Usuario | `root` |
| Contraseña | `root` |
| Gestión del esquema | `spring.jpa.hibernate.ddl-auto=update` |

La URL incluye `createDatabaseIfNotExist=true`: la conexión puede crear la base si el usuario tiene permisos. Hibernate crea o actualiza las tablas al iniciar la aplicación. MySQL debe estar ejecutándose antes de iniciar la API.

El esquema usa `users.id` autogenerado y un `card_uid` único para operadores. Cada auto en `cars` apunta a su titular en `people`. Para migrar datos desde el esquema anterior, seguí las dos etapas de la [guía de migración](docs/migration.md).

Para usar otra conexión sin modificar el archivo, configurar las variables de entorno en la misma terminal de PowerShell desde la que se ejecutará la aplicación:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/gestionusuario?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC'
$env:SPRING_DATASOURCE_USERNAME = 'tu_usuario'
$env:SPRING_DATASOURCE_PASSWORD = 'tu_clave'
```

## Ejecutar la aplicación

Clonar el repositorio y entrar en la rama `master`:

```powershell
git clone --branch master https://github.com/mariaSomephikhay/TFI-ControlAcceso-API.git
cd TFI-ControlAcceso-API
```

En Windows, desde PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```bash
sh mvnw spring-boot:run
```

La API utiliza por defecto `http://localhost:8080`. No incluye una interfaz web; sus operaciones se consumen mediante HTTP.

## Endpoints

Los endpoints de operadores reciben JSON y devuelven un mensaje de texto.

| Método | Ruta | Resultado exitoso | Error manejado por el controlador |
| --- | --- | --- | --- |
| POST | `/users/new` | `201 Created` | `400 Bad Request`; `409 Conflict` si el usuario ya existe |
| POST | `/users/login` | `200 OK` | `401 Unauthorized` |

### Crear un usuario

Ejemplo en PowerShell:

```powershell
$body = @{ username = 'operador_demo'; cardUid = 'A01B02CD'; password = '0123' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/users/new' -ContentType 'application/json' -Body $body
```

Respuesta exitosa:

```text
Usuario creado exitosamente
```

Si el nombre de usuario o el UID ya existe, responde `409 Conflict` y conserva los datos del operador registrado.

### Iniciar sesión

```powershell
$body = @{ cardUid = 'A01B02CD'; password = '0123' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/users/login' -ContentType 'application/json' -Body $body
```

Respuesta exitosa:

```text
Usuario logeado correctamente
```

Si el usuario no existe, la contraseña es incorrecta o el usuario está bloqueado, el controlador responde con `401` y un mensaje que comienza con `Error al iniciar sesion, `.

El contrato anterior con `id` sigue disponible para el embebido. `id` admite el UID o un identificador migrado. Enviá el PIN como texto para conservar los ceros iniciales.

### Comportamiento del bloqueo

Cada contraseña incorrecta incrementa `failedAttempts`. En el tercer fallo se desactiva al operador. Las siguientes peticiones informan que el operador está bloqueado.

Los intentos fallidos se acumulan incluso después de un login exitoso. Cada login exitoso genera un registro en `authentication_events`.

### Registrar titulares

`POST /people` recibe `firstName`, `lastName`, `nationalId` y un `email` opcional. Devuelve `201` con el titular y su ID. Un documento ya registrado devuelve `409`. `GET /people` lista titulares y `GET /people/{id}` consulta uno.

### Cargar patentes

La carga individual guarda un `Car` con el titular indicado por `personId`. El campo `description` es opcional.

```powershell
$body = @{ numero = 'ab123cd'; personId = 1 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/patentes' -ContentType 'application/json' -Body $body
```

`POST /patentes` responde `201 Created` con `{"numero":"AB123CD"}`. Una patente inválida o la falta de `personId` devuelve `400`, un titular inexistente devuelve `404` y una patente duplicada devuelve `409`. Por defecto se aceptan letras A–Z y números 0–9, de 1 a 16 caracteres, después de quitar espacios externos y convertir a mayúsculas.

Los formatos permitidos se configuran como una lista en `application.properties`, usando índices consecutivos. La patente debe coincidir por completo con **al menos uno** de los patrones, después de normalizarla a mayúsculas. El valor predeterminado es `license-plates.patterns[0]=[A-Z0-9]{1,16}`. Para permitir dos formatos específicos, reemplazarlo por:

```properties
license-plates.patterns[0]=[A-Z]{2}[0-9]{3}
license-plates.patterns[1]=[0-9]{3}[A-Z]{2}
```

La aplicación no inicia si la lista está vacía o contiene una expresión regular inválida. Esta misma configuración se aplica a la carga individual y a cada fila del CSV.

Para importar varias patentes, enviar un CSV UTF-8 de **una columna** en `archivo` y el `personId` del titular de todo el lote a `POST /patentes/importar`. El archivo admite una cabecera opcional `patente`.

```text
patente
ABC123
AB123CD
```

Ejemplo en PowerShell:

```powershell
curl.exe -F "archivo=@patentes.csv;type=text/csv" -F "personId=1" http://localhost:8080/patentes/importar
```

La respuesta exitosa es `201 Created` con `{"cantidad":2}`. Por defecto, el archivo admite hasta 100000 bytes y 1000 patentes; esos límites se ajustan mediante `license-plates.import.max-file-bytes` y `license-plates.import.max-rows` en `application.properties`. Se validan todas las filas y se detectan duplicados tanto dentro del archivo como en la base antes de guardar; si alguna falla, no se importa ninguna. Los errores de formato devuelven `400`, los duplicados `409` y los archivos que exceden el límite de carga de Spring `413`.

### Consultar, modificar y dar de baja patentes

`GET /patentes` lista las patentes con `id`, `numero`, `createdAt` y `updatedAt`; `GET /patentes/{id}` devuelve una sola. La lista vacía responde `200 OK` con `[]`, y un ID inexistente responde `404 Not Found`.

`PUT /patentes/{id}` recibe `{"numero":"AB123CD"}`, normaliza y actualiza el número. Conserva el titular y responde `400` para un formato inválido, `409` por duplicado y `404` si el ID no existe. `DELETE /patentes/{id}` responde `204` al borrar y `404` si el ID no existe. La baja desvincula el auto de las estadías y conserva la patente detectada, las fechas y la marca que indica si estaba registrada.

La validación automática de acceso y el archivo de estadías siguen pendientes. Estos endpoints aún no tienen protección de autenticación.

## Compilación y pruebas

En Windows:

```powershell
.\mvnw.cmd clean verify
```

En Linux o macOS:

```bash
sh mvnw clean verify
```

Las pruebas de operadores verifican el alta, duplicados, la auditoría, el bloqueo y la compatibilidad de las peticiones del embebido. Las pruebas de patentes cubren normalización, formatos inválidos, duplicados, importación, consulta, modificación, baja y respuestas HTTP. Estas pruebas utilizan repositorios simulados.

El proceso genera un JAR ejecutable:

```powershell
java -jar target/TFI-ControlAcceso-API-0.0.1-SNAPSHOT.jar
```

## Estructura

```text
src/main/java/com/unla/gestionUsuario/
  config/                     Configuración de patentes
  dtos/                       Contratos HTTP de patentes
  exceptions/                 Errores de negocio
src/main/java/com/unla/accesscontrol/
  controller/                 Endpoints de operadores, titulares y patentes
  entities/                   Entidades del nuevo modelo
  repository/                 Acceso a datos con Spring Data
  service/                    Alta, autenticación y gestión de patentes
src/main/resources/
  application.properties      Configuración de la aplicación
src/test/java/com/unla/gestionUsuario/
  OperatorServiceTests.java
```

Los paquetes Java y la clase principal conservan el nombre `gestionUsuario` del proyecto de origen.

## Alcance actual

- Las contraseñas se almacenan y comparan en texto plano.
- El login valida credenciales y registra el acceso; no emite tokens ni crea una sesión de autenticación.
- No existe un endpoint de desbloqueo y el método de reinicio del contador está pendiente de implementación.

Estas características describen la implementación migrada y deben contemplarse antes de utilizarla como sistema de autenticación en producción.
