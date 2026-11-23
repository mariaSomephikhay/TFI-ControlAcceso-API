# Migración a operadores y autos con titular

## 1. Operadores y auditoría

1. Verificar un respaldo y restaurarlo en MySQL. Usar esa copia como origen de ensayo.
   Reservar otra base vacía en el mismo servidor como destino, con el mismo juego de caracteres y collation.
2. Iniciar el código del PR de operadores con `SPRING_DATASOURCE_URL` apuntando al destino.
   Hibernate crea sus tablas y la tabla de patentes del contrato anterior. Detener la API antes de copiar datos.
3. En una sesión MySQL sobre el destino, cargar `sql/01-migrate-operators.sql`.
   Completar `migration_operator_map (legacy_id, username, card_uid)` con el identificador previo,
   el nombre de usuario y el UID real de cada operador. Usar datos verificados por el equipo.
4. Ejecutar `CALL migrate_legacy_operators('nombre_de_la_base_origen');`. La operación copia usuarios,
   alias de login, eventos y patentes en una transacción. Rechaza correspondencias faltantes,
   identificadores ambiguos y un destino con datos en esas tablas.
5. Conciliar conteos y valores con el origen. Probar un login con `cardUid`, otro con `id`,
   un operador bloqueado y los IDs de patentes existentes.

Para un corte real, mantener el origen sin escrituras desde que se toma la copia final.
Arrancar contra el destino únicamente después de conciliar los registros. Conservar el origen
y el respaldo. Para volver atrás antes de admitir escrituras, volver a la versión anterior
conectada al origen. Si el destino ya recibió escrituras, reconciliarlas primero para evitar pérdida.

`POST /users/new` recibe `username`, `cardUid` y `password`. La base genera el ID numérico.
`POST /users/login` admite `cardUid` y el campo previo `id`, que resuelve un UID o un alias migrado.
Para conservar ceros iniciales, el PIN se envía como cadena JSON. Los eventos de inicio de sesión
se guardan en `authentication_events`, incluidos los registros copiados de `user_logs`.

## 2. Patentes y titulares

1. Con la API detenida, respaldar el destino de la etapa 1.
2. En esa base, registrar los titulares reales en `people` con nombre, apellido, documento único
   y las fechas de alta y actualización. Registrar el correo si está disponible.
3. Cargar `sql/02-migrate-plates.sql` y completar `migration_plate_owner_map (plate_id, person_id)`
   con un titular por cada patente de origen.
4. Ejecutar `CALL migrate_legacy_plates();`. Verifica la correspondencia, conserva los IDs y
   las fechas, y deja la tabla anterior como `legacy_license_plates`.
5. Iniciar la versión del PR de patentes contra el destino y verificar las seis rutas de patentes.
   El alta y la importación requieren `personId`. La consulta, la edición del número y la baja
   conservan sus rutas y sus respuestas.

Para revertir la etapa 2 antes de aceptar escrituras, restaurar el respaldo del destino en otra base
y conectar la versión de la etapa 1. Si hubo escrituras, reconciliarlas antes de volver a esa versión.
El esquema viejo y los respaldos pueden conservarse hasta terminar la conciliación.
