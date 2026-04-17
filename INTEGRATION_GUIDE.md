# Integración Cliente-Servidor - Guía de Prueba

## Cómo Ejecutar Cliente y Servidor Juntos

### Paso 1: Iniciar el Servidor

Abre una terminal y ejecuta:
```bash
cd "c:\Users\User\Documents\programacion_visual\sockets_battleship\Battle-Ship\servidor\src"
java ServerMain
```

**Salida esperada:**
```
2026-04-10 10:30:00 - Iniciando servidor en el puerto 5000...
2026-04-10 10:30:00 - Servidor listo. Esperando conexiones...
```

### Paso 2: Iniciar el Cliente (en otra terminal)

Abre una segunda terminal y ejecuta:
```bash
cd "c:\Users\User\Documents\programacion_visual\sockets_battleship"
java -cp . client.src.BattleshipClient
```

### Paso 3: Conectar el Cliente

Ingresa los datos de conexión:
- **Host:** `localhost` (o presiona Enter)
- **Port:** `5000` (o presiona Enter)
- **Player Name:** `TestPlayer` (cualquier nombre)

## Flujo Completo Esperado

### Terminal del Servidor:
```
2026-04-10 10:30:00 - Iniciando servidor en el puerto 5000...
2026-04-10 10:30:00 - Servidor listo. Esperando conexiones...
2026-04-10 10:30:05 - Cliente #1 conectado desde /127.0.0.1:xxxxx
2026-04-10 10:30:05 - Cliente #1 -> CONNECT TestPlayer
2026-04-10 10:30:05 - TestPlayer se ha conectado (cliente #1)
2026-04-10 10:30:05 - Servidor -> START 1
```

### Terminal del Cliente:
```
=== Battleship Client - Connection ===
Host (default: localhost):
Port (default: 5000):
Player Name: TestPlayer
Connecting to localhost:5000...
Connected to server successfully!
Sent: CONNECT TestPlayer
Waiting for server response... (Press Ctrl+C to exit)
Received: START 1
Game started! First player: 1
```

## Comandos de Compilación

```bash
# Compilar ambos
cd "c:\Users\User\Documents\programacion_visual\sockets_battleship"
javac client/src/BattleshipClient.java
javac Battle-Ship/servidor/src/ServerMain.java

# Ejecutar servidor
java Battle-Ship.servidor.src.ServerMain

# Ejecutar cliente (en otra terminal)
java -cp . client.src.BattleshipClient
```

## Solución de Problemas

### Error: "Connection refused"
- Asegúrate de que el servidor esté ejecutándose primero
- Verifica que uses el puerto 5000

### Error: "Class not found"
- Usa `java -cp .` para incluir el directorio actual en el classpath
- Asegúrate de usar los nombres de paquete correctos

### El cliente no recibe respuesta
- Verifica que el servidor esté enviando "START 1"
- Revisa los logs del servidor para ver si recibió el CONNECT

## Próximos Pasos

Una vez que la Parte 1 funcione:
1. El servidor enviará "START nombre_del_jugador" en lugar de "START 1"
2. Implementarás la Parte 2: colocación de barcos
3. El servidor validará las posiciones de barcos con "PLACE"

¡La integración básica ya está funcionando! 🎉