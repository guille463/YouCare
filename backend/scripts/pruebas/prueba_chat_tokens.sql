

------------ DATOS BASE----------
INSERT INTO usuario (id,email,hash_contrasenia,nombre,apellidos,telefono,rol) OVERRIDING SYSTEM VALUE VALUES
 (9001,'test.cliente@x.es','h','Ana','Prueba','600000000','CLIENTE'),
 (9002,'test.cuidador@x.es','h','Luis','Prueba','600000001','CUIDADOR'),
 (9003,'test.empresa@x.es','h','Emp','Prueba','600000002','EMPRESA');
INSERT INTO cliente (id,fecha_nacimiento) VALUES (9001,'1980-01-01');
INSERT INTO cuidador (id,dni_nie) VALUES (9002,'12345678Z');
INSERT INTO empresa (id,razon_social,cif) VALUES (9003,'Prueba SL','B12345678');

------------ CHAT----------
-- OK: chat con cuidador autónomo
INSERT INTO chat (id,cliente_id,cuidador_id) OVERRIDING SYSTEM VALUE VALUES (9001,9001,9002);
-- OK: chat con empresa
INSERT INTO chat (id,cliente_id,empresa_id) OVERRIDING SYSTEM VALUE VALUES (9002,9001,9003);
-- ERROR ck_chat_un_proveedor: los dos proveedores
INSERT INTO chat (cliente_id,cuidador_id,empresa_id) VALUES (9001,9002,9003);
-- ERROR ck_chat_un_proveedor: sin proveedor
INSERT INTO chat (cliente_id) VALUES (9001);
-- ERROR uk_chat_cliente_cuidador: chat repetido
INSERT INTO chat (cliente_id,cuidador_id) VALUES (9001,9002);
-- ERROR uk_chat_cliente_empresa: chat repetido
INSERT INTO chat (cliente_id,empresa_id) VALUES (9001,9003);

------------ MENSAJE----------
-- OK
INSERT INTO mensaje (chat_id,emisor_id,contenido) VALUES (9001,9001,'Hola');
-- ERROR ck_mensaje_contenido: solo espacios
INSERT INTO mensaje (chat_id,emisor_id,contenido) VALUES (9001,9001,'   ');
-- ERROR ck_mensaje_fecha_lectura: leído antes de enviarse
INSERT INTO mensaje (chat_id,emisor_id,contenido,fecha_envio,fecha_lectura) VALUES (9001,9001,'x',now(),now()-interval '1 hour');

------------ REFRESH_TOKEN ----------
-- OK
INSERT INTO refresh_token (usuario_id,hash_token,fecha_expiracion) VALUES (9001,repeat('a',64),now()+interval '7 days');
-- ERROR uk_refresh_token_hash: hash repetido
INSERT INTO refresh_token (usuario_id,hash_token,fecha_expiracion) VALUES (9001,repeat('a',64),now()+interval '7 days');
-- ERROR ck_refresh_token_hash: hash en mayúsculas
INSERT INTO refresh_token (usuario_id,hash_token,fecha_expiracion) VALUES (9001,repeat('A',64),now()+interval '7 days');
-- ERROR ck_refresh_token_fechas: caduca antes de crearse
INSERT INTO refresh_token (usuario_id,hash_token,fecha_expiracion) VALUES (9001,repeat('b',64),now()-interval '1 hour');

-- ---------- EMAIL_TOKEN ----------
-- OK
INSERT INTO email_token (usuario_id,hash_token,tipo,fecha_expiracion) VALUES (9001,repeat('c',64),'VERIFICACION',now()+interval '24 hours');
-- ERROR ck_email_token_tipo: tipo inexistente
INSERT INTO email_token (usuario_id,hash_token,tipo,fecha_expiracion) VALUES (9001,repeat('d',64),'OTRO',now()+interval '1 hour');

-- ---------- LIMPIEZA EN CASCADA ----------
DELETE FROM usuario WHERE id IN (9001,9002,9003);

SELECT (SELECT count(*) FROM chat WHERE cliente_id=9001) AS chats,
       (SELECT count(*) FROM mensaje WHERE emisor_id=9001) AS mensajes,
       (SELECT count(*) FROM refresh_token WHERE usuario_id=9001) AS refresh,
       (SELECT count(*) FROM email_token WHERE usuario_id=9001) AS email;