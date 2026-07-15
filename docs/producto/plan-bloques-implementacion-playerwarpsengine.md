# Plan de bloques de implementacion - PlayerWarpsEngine

Este plan organiza la implementacion de `PlayerWarpsEngine` en pocos bloques secuenciales, con dependencias claras entre etapas. El objetivo es evitar empezar funcionalidades que luego dependan de piezas aun no construidas, mantener la ilacion tecnica del equipo y completar el producto de forma ordenada hasta cubrir el diseno completo.

El detalle tecnico completo de cada bloque se encuentra en `docs/producto/diseno-final-playerwarpsengine.md`. Este archivo no reemplaza ese diseno: lo convierte en una ruta de ejecucion practica.

## Principio de trabajo

La implementacion debe avanzar de base a producto terminado:

1. Primero se construye la base ejecutable del plugin.
2. Luego se asegura persistencia, dominio y cache.
3. Despues se implementa el core jugable.
4. Luego se cierran las operaciones completas de gestion.
5. Finalmente se integra la UI, PlaceholderAPI y el hardening operativo.

No se debe pasar al siguiente bloque hasta que el bloque actual compile, cargue correctamente y tenga sus flujos principales verificados en servidor.

## Orden final de bloques

| Orden | Bloque | Resultado esperado |
| --- | --- | --- |
| 1 | Base ejecutable del plugin | Plugin cargando, configuracion funcionando y comando principal operativo |
| 2 | Persistencia, dominio y cache | Datos confiables, schema, repositorios, warmup e indices en memoria |
| 3 | Core jugable | Crear, eliminar, listar y teletransportarse a warps de forma segura |
| 4 | Gestion completa de warps | Ownership, limites, lock, whitelist, rename, reset, bonuses y purge |
| 5 | UI, integraciones y cierre operativo | Menus zMenu, busqueda, filtros, PlaceholderAPI, reload completo y validacion final |

## Bloque 1: Base ejecutable del plugin

### Objetivo

Dejar el plugin cargando correctamente, con configuracion base, lifecycle estable y comando principal dinamico.

### Alcance

- Configurar Gradle, Shadow, dependencias y relocations.
- Usar `libs/server.jar` como `compileOnly` principal y no agregar `spigot-api` Maven en paralelo.
- Usar `libs/zmenu-1.0.3.7.jar` como `compileOnly`.
- Crear `plugin.yml` con zMenu como dependencia requerida y PlaceholderAPI como softdepend.
- Implementar `PlayerWarpsPlugin` y bootstrap inicial.
- Implementar lifecycle basico de `onEnable` y `onDisable`.
- Cargar `config.yml`, `storage.yml` y `messages.yml` con boosted-yaml.
- Crear `Settings`, `Messages` y utilidades de texto.
- Implementar scheduler/wrapper para operaciones sync y async.
- Registrar dinamicamente el comando principal desde configuracion.
- Implementar `/pwarp help` y `/pwarp reload` inicial.

### Criterio de cierre

- El plugin inicia sin errores en el servidor objetivo.
- Los archivos base se crean y cargan correctamente.
- El comando principal y sus aliases se registran desde configuracion.
- `/pwarp help` responde correctamente.
- `/pwarp reload` recarga configuracion y mensajes sin reiniciar el servidor.

### Motivo de orden

Este bloque es la base estructural. Sin configuracion, lifecycle y comando dinamico, cualquier feature posterior quedaria acoplada a decisiones temporales y seria mas facil generar deuda tecnica.

## Bloque 2: Persistencia, dominio y cache

### Objetivo

Construir el nucleo de datos del plugin antes de implementar flujos jugables o visuales.

### Alcance

- Inicializar HikariCP y conexion MySQL.
- Ejecutar migraciones de schema.
- Crear tablas de warps, whitelist y bonuses.
- Implementar modelos de dominio: warp, ubicacion, nombre normalizado y estados.
- Implementar repositorios MySQL.
- Implementar `WarpCache` e indices principales en memoria.
- Implementar warmup inicial en `onEnable`.
- Implementar carga de bonuses persistentes.
- Implementar calculo de limites efectivos.
- Validar nombres por regex, nombres reservados y mundos permitidos.

### Criterio de cierre

- El plugin crea o valida el schema al iniciar.
- Los warps del `server_id` se cargan desde MySQL al cache local.
- Los indices principales permiten lookup por ID, nombre y owner.
- Los limites se calculan correctamente usando el mayor permiso aplicable mas bonus persistente.
- Las escrituras criticas tienen camino definido: MySQL primero, cache despues.

### Motivo de orden

Crear features antes de tener persistencia y cache estable obliga a reescribir logica. Este bloque deja lista la base que usan comandos, teleport, menus, placeholders y purge.

## Bloque 3: Core jugable

### Objetivo

Hacer que el producto ya sea usable en su flujo principal: crear warps, eliminarlos, listarlos y teletransportarse de forma segura.

### Alcance

- Implementar `/pwarp set <name>`.
- Implementar `/pwarp remove <warp>`.
- Implementar `/pwarp <warp>`.
- Implementar `/pwarp list [page] [player]`.
- Implementar `/pwarp amount [player]`.
- Implementar `SafetyChecker`.
- Implementar `TeleportService` con delay cancelable.
- Implementar listeners de movimiento, dano, muerte y desconexion.
- Implementar `ChunkPreloader` con `World#getChunkAtAsync(...)` desde `libs/server.jar` y fallback sync defensivo.
- Implementar buffer de visitas y flush batch.
- Cubrir fallos de MySQL, ubicaciones inseguras, mundos inexistentes y teleports cancelados por otros plugins.

### Criterio de cierre

- Un jugador puede crear un warp valido en su ubicacion actual.
- Un jugador puede eliminar sus propios warps.
- `/pwarp <warp>` resuelve desde cache, no desde MySQL.
- El teleport final ocurre en main thread con `TeleportCause.PLUGIN`.
- El delay se cancela correctamente si el jugador se mueve, recibe dano, muere o se desconecta.
- Las visitas se acumulan sin escribir MySQL en el camino caliente.

### Motivo de orden

Este es el primer bloque donde el plugin se vuelve funcional para jugadores. Debe hacerse despues de persistencia y cache para no mezclar reglas de negocio con almacenamiento temporal o accesos directos a MySQL.

## Bloque 4: Gestion completa de warps

### Objetivo

Completar todas las operaciones avanzadas de administracion, ownership y control de acceso sobre warps.

### Alcance

- Implementar descripcion de warp: set/remove.
- Implementar lock/unlock.
- Implementar reset de ubicacion.
- Implementar rename.
- Implementar transfer owner.
- Implementar whitelist enable/disable/add/remove/list.
- Implementar remove admin y removeall.
- Implementar `/pwarp addwarps <player> <amount>`.
- Implementar purge unsafe e inactive con controles operativos.
- Validar owner, admin bypass, limites y jugadores offline.
- Garantizar que toda escritura critica confirme MySQL antes de mutar cache.

### Criterio de cierre

- Todas las operaciones de owner funcionan sobre warps propios.
- Los admins pueden ejecutar acciones segun permisos configurados.
- Transfer owner respeta el limite efectivo del nuevo owner salvo bypass permitido.
- Rename no permite colisiones por `server_id` y nombre normalizado.
- Whitelist y lock bloquean o permiten teleports segun reglas de acceso.
- Purge no ejecuta eliminaciones masivas sin control seguro.

### Motivo de orden

Estas funcionalidades dependen directamente del core jugable. Implementarlas antes generaria duplicacion de validaciones y riesgo de inconsistencias en cache, ownership y limites.

## Bloque 5: UI, integraciones y cierre operativo

### Objetivo

Cerrar la experiencia completa del producto con menus, busqueda, PlaceholderAPI, reload completo y validacion final contra los criterios de aceptacion.

### Alcance

- Implementar loader de zMenu.
- Crear menu principal paginado.
- Crear menu de gestion y whitelist.
- Implementar botones custom de warps, mis warps, busqueda, refresh, filtros y gestion.
- Implementar search por chat con timeout.
- Implementar ordenes: newest, oldest, most-visits, alphabetical y owner.
- Integrar PlaceholderAPI sin consultas MySQL.
- Completar reload de comandos, settings, mensajes y menus.
- Manejar zMenu ausente, YAML invalido y paginas fuera de rango.
- Validar que los menus usen materiales compatibles con Minecraft 1.8.
- Ejecutar hardening final de edge cases operativos.

### Criterio de cierre

- El menu principal abre sin consultar MySQL.
- La paginacion renderiza solo la pagina visible.
- La busqueda expira correctamente si el jugador no responde.
- Los placeholders leen cache o devuelven fallback configurable.
- El reload no rompe comandos, menus ni teleports pendientes.
- El producto cumple los criterios de aceptacion definidos en el archivo de diseno.

### Motivo de orden

La UI y las integraciones deben consumir servicios ya estables. Si se implementan antes, se corre el riesgo de convertir menus y placeholders en fuente de reglas de negocio, que es exactamente lo que se debe evitar.

## Reglas de avance entre bloques

- Cada bloque debe terminar con el plugin compilando.
- Cada bloque debe poder cargarse en servidor sin errores criticos.
- Cada bloque debe verificar manualmente sus flujos principales.
- No se deben abrir features del bloque siguiente si el actual tiene fallos estructurales pendientes.
- Las decisiones tecnicas cerradas en el diseno no deben reabrirse salvo que aparezca una incompatibilidad real verificada.

## Resultado esperado

Al completar estos cinco bloques, `PlayerWarpsEngine` debe quedar implementado al 100% del alcance V1: persistente, cacheado, seguro para teleport, configurable, integrado con zMenu y PlaceholderAPI, y preparado para operar en el servidor objetivo sin introducir complejidad innecesaria.
