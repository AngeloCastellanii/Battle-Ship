# Contrato del Protocolo - Servidor

## Mensajes del cliente al servidor
- `CONNECT <Nombre>`
- `PLACE <Barco> <x> <y> <H|V>`
- `ATTACK <x> <y>`

## Mensajes del servidor al cliente
- `CONNECTED <ID>`
- `WAITING_FOR_OPPONENT`
- `START 1`
- `PLACE_OK <Barco>`
- `READY <ID>`
- `TURN <ID>`
- `RESULT <x> <y> <HIT|MISS|SUNK>`
- `VICTORY <ID>`
- `ERROR <CODIGO> <MENSAJE>`

## Barcos validos
- `PORTAAVIONES` (5)
- `ACORAZADO` (4)
- `SUBMARINO` (3)
- `DESTRUCTOR` (2)
- `LANCHA` (1)

## Codigos de error actuales
- `BAD_CONNECT`
- `BAD_PLACE`
- `BAD_ATTACK`
- `ALREADY_CONNECTED`
- `ROOM_FULL`
- `NOT_CONNECTED`
- `WAITING_PLAYER`
- `BATTLE_STARTED`
- `INVALID_SHIP`
- `DUPLICATE_SHIP`
- `BAD_COORDINATE`
- `BAD_ORIENTATION`
- `INVALID_PLACEMENT`
- `SHIP_OVERLAP`
- `BATTLE_NOT_READY`
- `NOT_YOUR_TURN`
- `OUT_OF_BOARD`
- `NO_OPPONENT`
- `ALREADY_ATTACKED`
- `OPPONENT_DISCONNECTED`
- `SERVER_SHUTDOWN`
