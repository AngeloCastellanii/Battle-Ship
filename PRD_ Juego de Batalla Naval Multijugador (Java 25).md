# **PRD: Juego de Batalla Naval Multijugador (Java 25\)**

## **1\. Resumen del Proyecto**

Desarrollo de una aplicación distribuida del juego "Batalla Naval" utilizando **Java 25**. El proyecto se divide en dos módulos principales: un **Servidor** (encargado de la orquestación y arbitraje) y un **Cliente** (encargado de la interfaz de usuario y la interacción).

## **2\. División de Responsabilidades**

Este proyecto está diseñado para ser realizado por dos estudiantes con tareas claramente delimitadas pero interdependientes.

### **2.1. Estudiante A: Especialista en Servidor y Red**

* **Gestión de Conexiones:** Implementación de `ServerSocket` utilizando **Virtual Threads** (Java 25\) para manejar múltiples peticiones sin bloquear el hilo principal.  
* **Lógica de Arbitraje:** El servidor debe validar si un movimiento es legal, determinar si un impacto fue exitoso y llevar el conteo de barcos restantes para declarar un ganador.  
* **Protocolo de Mensajería:** Implementación del lado del servidor para procesar y enviar comandos estructurados.  
* **Logs del Servidor:** Registro en consola de todos los eventos de red y cambios de estado del juego.

### **2.2. Estudiante B: Especialista en Cliente e Interfaz (GUI)**

* **Interfaz Gráfica (JavaFX):** Diseño de las pantallas de inicio, posicionamiento de barcos y tablero de combate.  
* **Gestión de Estados en UI:** Controlar visualmente cuándo es el turno del jugador y cuándo debe esperar.  
* **Interactividad:** Manejo de eventos de ratón para disparar y colocar barcos.  
* **Comunicación Cliente-Servidor:** Implementación de la clase que se conecta al Socket del servidor y escucha los mensajes entrantes para actualizar la pantalla.

## **3\. Especificaciones Técnicas (Java 25\)**

Para aprovechar las capacidades modernas de Java 25, se requieren los siguientes estándares:

* **Concurrencia:** Uso de `Executors.newVirtualThreadPerTaskExecutor()` para la escucha de sockets.  
* **Estructura de Datos:** Uso de `records` para representar coordenadas (ej. `record Point(int x, int y) {}`) y resultados de ataques.  
* **Pattern Matching:** Uso de `switch` con pattern matching para procesar los diferentes comandos recibidos por el socket de forma limpia.  
* **Interfaz de Red:** `java.net.StandardSocketOptions` para configurar timeouts y mantener la conexión activa.

## **4\. El Contrato de Comunicación (Protocolo)**

Para que ambos estudiantes puedan trabajar por separado, deben adherirse estrictamente a este protocolo basado en texto:

| Comando | Dirección | Formato | Acción |
| ----- | ----- | ----- | ----- |
| `CONNECT` | C \-\> S | `CONNECT <PlayerName>` | El cliente solicita unirse a la partida. |
| `START` | S \-\> C | `START <FirstPlayer>` | El servidor indica que ambos están conectados y quién inicia. |
| `PLACE` | C \-\> S | `PLACE <ShipName> <x> <y> <H/V>` | Notifica al servidor la posición de un barco para validación. |
| `ATTACK` | C \-\> S | `ATTACK <x> <y>` | El jugador envía coordenadas de disparo. |
| `RESULT` | S \-\> C | `RESULT <x> <y> <HIT/MISS/SUNK>` | El servidor informa el resultado del último ataque. |
| `TURN` | S \-\> C | `TURN <PlayerID>` | Indica a quién le corresponde mover ahora. |
| `CHAT` | C \<-\> S | `CHAT <mensaje>` | Envío de mensajes de texto entre jugadores. |
| `VICTORY` | S \-\> C | `VICTORY <PlayerID>` | El servidor anuncia el fin de la partida. |

## **5\. Requisitos de Funcionalidad**

### **5.1. Flujo de Juego**

1. **Fase de Conexión:** El Servidor inicia y espera. El Cliente se conecta vía IP/Puerto.  
2. **Fase de Preparación:** Ambos jugadores colocan sus 5 barcos. El Cliente envía las posiciones al Servidor. El Servidor confirma cuando ambos están listos (`READY`).  
3. **Fase de Combate:** \* Jugador A dispara \-\> Servidor valida \-\> Servidor informa a ambos \-\> Si acertó, sigue tirando (opcional) o cambia el turno.  
4. **Fase de Finalización:** El Servidor detecta que un jugador tiene 0 celdas de barco y envía el comando `VICTORY`.

### **5.2. Manejo de Errores**

* Si el cliente se cierra inesperadamente, el servidor debe notificar al otro jugador y cerrar la sesión limpiamente.  
* Validación de coordenadas: El servidor debe rechazar ataques fuera del rango 0-9 o ataques a celdas ya disparadas.

## **6\. Entregables y Pruebas de Integración**

### **Paso 1: Prueba de Ping**

Los estudiantes deben demostrar que un mensaje enviado desde el Cliente es recibido y mostrado en la consola del Servidor.

### **Paso 2: Validación de Lógica**

El Estudiante A (Servidor) debe tener una suite de pruebas (JUnit) que verifique que la lógica de "Hundido" funciona correctamente sin necesidad de la GUI.

### **Paso 3: Integración Final**

Ambos módulos se unen. El Cliente debe poder refrescar su tablero basado exclusivamente en los comandos que recibe del Servidor.

## **7\. Criterios de Evaluación**

1. **Sincronización:** ¿El estado visual del cliente coincide con el estado lógico del servidor?  
2. **Uso de Java 25:** ¿Se implementaron Virtual Threads para la concurrencia?  
3. **Estabilidad:** ¿La aplicación maneja desconexiones sin lanzar excepciones fatales?  
4. **Interfaz:** ¿Es claro para el usuario cuándo es su turno y dónde están sus barcos?

