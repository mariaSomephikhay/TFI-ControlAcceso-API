# TFI-ControlAcceso-API

API REST de gestión de usuarios y control de acceso, desarrollada con Spring Boot. La implementación proviene del proyecto `gestionUsuario` y conserva su lógica de negocio.

## Funcionalidades

- Alta de usuarios con estado activo y contador de intentos fallidos en cero.
- Inicio de sesión mediante identificador y contraseña.
- Bloqueo del usuario al acumular tres contraseñas incorrectas.
- Registro de los inicios de sesión exitosos en la tabla `user_logs`.
- Alta individual e importación CSV de patentes autorizadas.

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

Las tablas de la aplicación se llaman `license_plates`, `users` y `user_logs`. En una base nueva, Hibernate las crea al iniciar la aplicación. `ddl-auto=update` conserva los datos entre ejecuciones, pero no renombra tablas ni columnas de versiones anteriores; una base existente con el esquema viejo requiere una migración manual para conservar sus registros.

`license_plates.id` y `user_logs.id` se generan automáticamente. En `users`, el identificador de acceso `user_id` ya es la clave primaria. Las tres entidades completan `created_at` y `updated_at` mediante `@CreationTimestamp` y `@UpdateTimestamp`.

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

Ambos endpoints reciben JSON con los campos `id` y `password` y devuelven un mensaje de texto.

```json
{
  "id": "usuario_demo",
  "password": "clave_demo"
}
```

| Método | Ruta | Resultado exitoso | Error manejado por el controlador |
| --- | --- | --- | --- |
| POST | `/users/new` | `201 Created` | `400 Bad Request` |
| POST | `/users/login` | `200 OK` | `401 Unauthorized` |

### Crear un usuario

Ejemplo en PowerShell:

```powershell
$body = @{ id = 'usuario_demo'; password = 'clave_demo' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/users/new' -ContentType 'application/json' -Body $body
```

Respuesta exitosa:

```text
Usuario creado exitosamente
```

### Iniciar sesión

```powershell
$body = @{ id = 'usuario_demo'; password = 'clave_demo' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/users/login' -ContentType 'application/json' -Body $body
```

Respuesta exitosa:

```text
Usuario logeado correctamente
```

Si el usuario no existe, la contraseña es incorrecta o el usuario está bloqueado, el controlador responde con `401` y un mensaje que comienza con `Error al iniciar sesion, `.

### Comportamiento del bloqueo

Cada contraseña incorrecta incrementa `blockAmount`. En el tercer fallo se cambia `state` a `false`; esa petición todavía informa contraseña incorrecta. Las siguientes peticiones informan que el usuario está bloqueado, incluso si la contraseña es correcta.

Los intentos fallidos son acumulativos: un login exitoso no reinicia el contador. Solo los accesos exitosos generan un registro en `user_logs`, con evento `0`, usuario y fecha de creación.

### Cargar patentes

Las clases y la tabla usan `LicensePlate` y `license_plates`. La ruta HTTP y los campos JSON mantienen los nombres anteriores para conservar la compatibilidad con los clientes existentes.

La carga individual recibe una patente en JSON y devuelve el número guardado, normalizado en mayúsculas:

```powershell
$body = @{ numero = 'ab123cd' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/patentes' -ContentType 'application/json' -Body $body
```

`POST /patentes` responde `201 Created` con `{"numero":"AB123CD"}`. Por defecto acepta letras latinas A-Z y números 0-9, de 1 a 16 caracteres. Elimina espacios al principio y al final; rechaza espacios internos, guiones, símbolos y valores vacíos con `400 Bad Request`. Una patente ya cargada, incluso con otra combinación de mayúsculas y minúsculas, devuelve `409 Conflict`.

Los formatos permitidos se configuran como una lista en `application.properties`, usando índices consecutivos. La patente debe coincidir por completo con **al menos uno** de los patrones, después de normalizarla a mayúsculas. El valor predeterminado es `license-plates.patterns[0]=[A-Z0-9]{1,16}`. Para permitir dos formatos específicos, reemplazarlo por:

```properties
license-plates.patterns[0]=[A-Z]{2}[0-9]{3}
license-plates.patterns[1]=[0-9]{3}[A-Z]{2}
```

La aplicación no inicia si la lista está vacía o contiene una expresión regular inválida. Esta misma configuración se aplica a la carga individual y a cada fila del CSV.

Para importar varias patentes, enviar un archivo CSV UTF-8 de **una columna** al campo `archivo` de `POST /patentes/importar`. Puede incluir una primera línea `patente`; después lleva una patente por línea:

```text
patente
ABC123
AB123CD
```

Ejemplo en PowerShell:

```powershell
curl.exe -F "archivo=@patentes.csv;type=text/csv" http://localhost:8080/patentes/importar
```

La respuesta exitosa es `201 Created` con `{"cantidad":2}`. Por defecto, el archivo admite hasta 100000 bytes y 1000 patentes; esos límites se ajustan mediante `license-plates.import.max-file-bytes` y `license-plates.import.max-rows` en `application.properties`. Se validan todas las filas y se detectan duplicados tanto dentro del archivo como en la base antes de guardar; si alguna falla, no se importa ninguna. Los errores de formato devuelven `400`, los duplicados `409` y los archivos que exceden el límite de carga de Spring `413`.

La carga solo registra patentes. La consulta, modificación, baja y validación de acceso se implementarán por separado. Estos endpoints aún no tienen protección de autenticación.

## Compilación y pruebas

En Windows:

```powershell
.\mvnw.cmd clean verify
```

En Linux o macOS:

```bash
sh mvnw clean verify
```

Las pruebas de usuarios verifican el alta con estado inicial, el registro de un login exitoso, el bloqueo tras tres fallos, el rechazo de usuarios inexistentes y el mapper de credenciales. Las pruebas de patentes cubren normalización, formatos inválidos, duplicados, importación completa y respuestas HTTP. Utilizan repositorios simulados y no requieren MySQL; no prueban la conexión real a la base.

El proceso genera un JAR ejecutable:

```powershell
java -jar target/TFI-ControlAcceso-API-0.0.1-SNAPSHOT.jar
```

## Estructura

```text
src/main/java/com/unla/gestionUsuario/
  controller/                 Endpoints REST
  dtos/                       Datos de las peticiones
  entities/                   Entidades User, UserLog y LicensePlate
  exceptions/                 Errores de negocio
  mapper/                     Conversión entre DTO y entidad
  repository/                 Acceso a datos con Spring Data
  service/                    Interfaz del servicio
  services/implementations/   Lógica de usuarios y acceso
src/main/resources/
  application.properties      Configuración de la aplicación
src/test/java/com/unla/gestionUsuario/
  GestionUsuarioApplicationTests.java
```

Los paquetes Java y la clase principal conservan el nombre `gestionUsuario` del proyecto de origen.

## Alcance actual

- Las contraseñas se almacenan y comparan en texto plano.
- El login valida credenciales y registra el acceso; no emite tokens ni crea una sesión de autenticación.
- No existe un endpoint de desbloqueo y el método de reinicio del contador está pendiente de implementación.
- El alta no comprueba si el identificador ya existe; guardar uno existente puede actualizar sus datos y reiniciar su estado y contador.
- Los campos de entrada no tienen restricciones de validación declaradas en el DTO.

Estas características describen la implementación migrada y deben contemplarse antes de utilizarla como sistema de autenticación en producción.
