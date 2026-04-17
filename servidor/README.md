# Servidor - Batalla Naval

## Requisitos
- Java 25

## Compilar
```powershell
Set-Location "c:\Users\Angelo\Desktop\Programacion Visual\Proyecto 5 Sockets\servidor\src"
javac *.java
```

## Ejecutar
```powershell
java ServerMain
```

## Ejecutar con puerto custom
```powershell
java ServerMain 6000
```

## Caracteristicas implementadas
- Conexion de hasta 2 jugadores.
- Asignacion de jugador 1 o 2 con `CONNECTED`.
- Inicio de partida con `START 1` al conectarse ambos.
- Validacion de colocacion de barcos (`PLACE`).
- Ataques por turno (`ATTACK`) con `RESULT`, `TURN` y `VICTORY`.
- Timeout de lectura por cliente para evitar conexiones colgadas.
- Cierre ordenado de conexiones en apagado del servidor.

## Notas de integracion
- Todos los errores salen como `ERROR <CODIGO> <MENSAJE>`.
- Ver contrato de protocolo en `SERVER_PROTOCOL.md`.
