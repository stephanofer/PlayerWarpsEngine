# PlayerWarpsEngine - Diseno final del producto

Este documento define el diseno formal de `PlayerWarpsEngine`, el plugin de Player Warps para HERA Network. Su objetivo es dejar cerrado el alcance, la arquitectura, las decisiones tecnicas, los flujos principales, las reglas de rendimiento y los edge cases que deben guiar la implementacion inicial.

El diseno esta basado en las decisiones conversadas y en la verificacion directa de las dependencias locales: `boosted-yaml`, `caffeine`, `cloud-minecraft`, `zMenu 1.0.3.7`, `zAuctionHouse`, PlaceholderAPI, Spigot API `1.8.8-R0.1-SNAPSHOT` como referencia historica y el `server.jar` real usado por el equipo como API principal de compilacion.

## Estado del diseno

| Tema | Decision final |
| --- | --- |
| Plataforma objetivo | Java 8, Minecraft 1.8.8/1.8.9, Bukkit/Spigot/Paper-like forks |
| Motor real verificado | `libs/server.jar` basado en WineSpigot/TacoSpigot `1.8.8-R0.2-SNAPSHOT`, con APIs PaperLib/async chunks disponibles |
| API de compilacion | `libs/server.jar` como `compileOnly` principal; no usar `spigot-api` Maven en paralelo |
| UI | zMenu `1.0.3.7` como dependencia requerida |
| Configuracion | boosted-yaml, con defaults y `config-version` |
| Cache | Caffeine `2.9.3`, version correcta para Java 8 |
| Persistencia | MySQL con HikariCP |
| Comandos | Router propio dinamico; no usar `cloud-minecraft 2.x` |
| Teleport | Precarga de chunk optimizada cuando sea posible, teleport final sync con `TeleportCause.PLUGIN` |
| PlaceholderAPI | Integracion opcional con expansion interna persistente |
| Multi-server realtime | Fuera de V1 |

## Objetivo del producto

`PlayerWarpsEngine` permite que los jugadores creen warps publicos o restringidos hacia ubicaciones propias. El plugin debe mejorar la experiencia de navegacion, discovery y comunidad sin comprometer TPS, mantenibilidad o compatibilidad con Java 8/Minecraft 1.8.

El producto debe ser:

- Liviano y facil de depurar.
- Altamente configurable en mensajes, comandos, limites y menus.
- Seguro contra ubicaciones peligrosas.
- Performante con muchos warps y muchos jugadores abriendo menus.
- Preparado para monetizacion mediante limites por permisos/rangos.
- Ordenado desde el inicio para evitar deuda arquitectonica.

## Alcance V1

### Incluido

| Feature | Decision |
| --- | --- |
| Crear warps | Incluido con `/pwarp set <name>` |
| Multiple warps por jugador | Incluido con limites por permisos |
| Listar todos los warps | Incluido por GUI y comando texto |
| Remover warps | Incluido para owner y admins |
| Menu principal | Incluido via zMenu |
| Paginacion | Incluida via zMenu y boton custom paginado |
| Busqueda/filtros basicos | Incluido en menu y comando dedicado si hace falta |
| Purga | Incluida para warps inseguros o inactivos |
| Unsafe detection | Incluida en set/reset/teleport/purge |
| Delay de teleport | Incluido y cancelable |
| Lock de warp | Incluido |
| Reset de ubicacion | Incluido |
| Rename | Incluido |
| Transfer owner | Incluido |
| Whitelist por warp | Incluido |
| PlaceholderAPI | Incluido como softdepend |
| Comando principal configurable | Incluido con registro dinamico |
| Bonus de warps por admin | Incluido con persistencia |

### Fuera de V1

| Feature | Motivo |
| --- | --- |
| Redis / sincronizacion multi-server realtime | Aumenta complejidad y no es indispensable para el primer release |
| Ratings, reviews, favorites | Buenas features, pero no son core del sistema |
| Cobro por teleport | Depende de economia/Vault y no fue requisito base |
| Estadisticas avanzadas/leaderboards globales | Puede nacer despues sobre datos confiables |
| Folia | El objetivo real es Java 8/1.8.9 |

## Decisiones tecnicas validadas

### boosted-yaml

Se usara `boosted-yaml` para configuracion porque soporta:

- Creacion automatica de archivos desde resources.
- Defaults.
- Reload.
- Auto-update mediante `config-version`.
- Lectura de secciones y rutas de forma limpia.

Debe ir sombreado y relocado para evitar conflictos de classloader:

```kotlin
relocate("dev.dejvokep.boostedyaml", "com.hera.playerwarps.libs.boostedyaml")
```

### Caffeine

Se usara Caffeine `2.9.3`, porque Caffeine `3.x` requiere Java 11+ y el proyecto debe soportar Java 8.

La cache no debe usarse como excusa para ocultar errores de persistencia. Las lecturas calientes salen de cache, pero las escrituras criticas se confirman primero en MySQL.

### cloud-minecraft

No se usara `cloud-minecraft 2.x`.

Motivos verificados:

- El checkout local esta en `org.incendo/cloud-minecraft 2.0.0-SNAPSHOT`.
- El modulo Bukkit moderno compila contra `spigot-api 1.13.2-R0.1-SNAPSHOT`.
- Nuestro target es Java 8 y Minecraft 1.8.8/1.8.9.
- El set de comandos del plugin es acotado y no justifica una dependencia moderna con riesgo de compatibilidad.
- El comando principal debe ser configurable y conviene controlar el registro dinamico directamente.

La decision final es implementar un command router propio, simple y auditable.

### zMenu

Se usara `zMenu 1.0.3.7` como motor de UI.

Verificaciones relevantes:

- El jar existe en el workspace como `zmenu-1.0.3.7.jar`.
- El bytecode usa major version `52`, compatible con Java 8.
- Contiene `ButtonManager`, `InventoryManager`, `ButtonLoader`, `PaginateButton`, `ZButton`, `PatternManager` y `InventoryDefault`.
- La API antigua no usa `InventoryEngine`; ese tipo pertenece a ejemplos modernos de zMenu/zAuctionHouse y no debe usarse en este proyecto.
- `zAuctionHouse` sirve como referencia conceptual de menus, slots, patrones, busqueda y refresh, pero no como codigo copiable porque es Java 21, Paper moderno y zMenu API mas nueva.

Riesgo detectado:

- `plugin.yml` de zMenu declara `api-version: 1.13`.
- Nuestros menus default deben usar materiales compatibles con 1.8, por ejemplo `STAINED_GLASS_PANE` con `data`, no `LIGHT_BLUE_STAINED_GLASS_PANE`.

### PlaceholderAPI

Se integrara como `softdepend`.

La expansion sera interna y persistente:

```java
public boolean persist() {
    return true;
}
```

Regla critica: los placeholders no deben consultar MySQL. Deben leer cache o devolver fallback.

### server.jar real

El `server.jar` del proyecto no es Spigot puro y debe ubicarse en `libs/server.jar`. Este jar sera la API principal de compilacion porque el plugin siempre correra sobre este motor controlado por HERA. Contiene:

- WineSpigot/TacoSpigot `1.8.8-R0.2-SNAPSHOT`.
- `io.papermc.lib.PaperLib`.
- `io.papermc.lib.features.asyncchunks`.
- `io.papermc.lib.features.asyncteleport`.
- `org.bukkit.World#getChunkAtAsync(...)`.
- `org.bukkit.World$ChunkLoadCallback`.

Esto permite optimizar la preparacion del chunk destino antes de teletransportar.

Decision final:

- Compilar contra `libs/server.jar` como `compileOnly` principal.
- No agregar `org.spigotmc:spigot-api` en paralelo para evitar clases duplicadas y diferencias por orden de classpath.
- Usar principalmente APIs publicas `org.bukkit.*` y extensiones publicas del fork, como `World#getChunkAtAsync(...)`.
- Optimizar solo la carga/preparacion de chunk destino cuando el fork lo permita.
- Mantener el teleport final controlado en main thread.
- No ejecutar safety checks de bloques async.
- No usar `net.minecraft.server.v1_8_R3.*` ni `org.bukkit.craftbukkit.v1_8_R3.*` salvo necesidad tecnica fuerte y documentada.

## Dependencias Gradle propuestas

```kotlin
plugins {
    java
    id("com.gradleup.shadow") version "8.3.11"
}

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/releases/")
    }
}

dependencies {
    compileOnly(files("libs/server.jar"))
    compileOnly(files("libs/zmenu-1.0.3.7.jar"))
    compileOnly("me.clip:placeholderapi:2.12.2")

    implementation("com.stephanofer.boostedyaml:boosted-yaml:1.3.7")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("com.mysql:mysql-connector-j:8.0.33")
}
```

Relocations:

```kotlin
tasks.shadowJar {
    relocate("dev.dejvokep.boostedyaml", "com.hera.playerwarps.libs.boostedyaml")
    relocate("com.github.benmanes.caffeine", "com.hera.playerwarps.libs.caffeine")
    relocate("com.zaxxer.hikari", "com.hera.playerwarps.libs.hikari")
    mergeServiceFiles()
}
```

No se deben relocar Bukkit, Spigot, zMenu ni PlaceholderAPI.

`libs/server.jar` y `libs/zmenu-1.0.3.7.jar` son dependencias `compileOnly`: sirven para compilar y autocompletar contra el entorno real, pero no deben incluirse dentro del jar final del plugin.

El conector MySQL debe tratarse con cuidado. Si se sombrea dentro del jar, `mergeServiceFiles()` es importante para conservar metadata de `META-INF/services`. No relocar `com.mysql` por defecto salvo que se pruebe explicitamente el `driverClassName` resultante y el registro JDBC, porque una relocacion mal hecha del driver puede romper la conexion.

## plugin.yml

El plugin debe declarar zMenu como dependencia requerida y PlaceholderAPI como opcional:

```yaml
name: PlayerWarpsEngine
main: com.hera.playerwarps.PlayerWarpsPlugin
version: ${version}
author: HERA Network
depend:
  - zMenu
softdepend:
  - PlaceholderAPI
```

El comando principal se registrara dinamicamente desde config. Aun asi, puede declararse un comando fallback si se decide mantener compatibilidad basica con Bukkit, pero el comportamiento principal debe vivir en `CommandRegistrar`.

## Reglas de producto

### Identidad de warp

- Cada warp tiene un nombre visible y un nombre normalizado.
- Los nombres son unicos por `server_id`.
- La comparacion de nombre debe ser case-insensitive mediante `name_normalized`.
- No se permiten nombres fuera de regex configurable.
- No se permiten nombres reservados que choquen con subcomandos, por ejemplo `set`, `remove`, `reload`, `help`, `list`, `open`, `lock`, `rename`, `reset`, `whitelist`.

### Ubicacion

Cada warp guarda:

- `world`
- `x`
- `y`
- `z`
- `yaw`
- `pitch`

La ubicacion debe capturarse exactamente desde donde esta parado el jugador al crear o resetear el warp.

### Lock

- Un warp locked no permite teleport a jugadores normales.
- El owner puede seguir administrando su warp.
- Admins con bypass pueden entrar.

### Whitelist

- Si `whitelist_enabled = false`, el warp es publico salvo que este locked.
- Si `whitelist_enabled = true`, solo pueden entrar el owner, jugadores whitelist y admins bypass.
- La whitelist se guarda por UUID, con nombre solo como dato informativo.

### Transfer owner

Transferir owner debe validar:

- Que el warp exista.
- Que quien ejecuta sea owner o admin.
- Que el nuevo owner pueda recibir el warp segun limite efectivo, salvo bypass admin configurable.
- Que la actualizacion de owner y caches ocurra solo despues de confirmar MySQL.

### Exceso de limite por cambio de rango

Si un jugador pierde permisos y queda sobre su limite actual:

- No se borran warps automaticamente.
- No puede crear mas warps hasta quedar debajo del limite.
- Los admins pueden remover o transferir warps manualmente.

## Sistema de limites

El limite efectivo se calcula asi:

```text
effectiveLimit = max(defaultLimit, configuredPermissionLimits, numericPermissionLimits) + persistentBonus
```

Reglas:

- Los permisos de limite no se suman.
- Siempre gana el limite mas alto.
- El bonus persistente de `/pwarp addwarps` si se suma.
- Si el jugador tiene `pwarp.limit.vip` y `pwarp.limit.50`, gana `50`.

Ejemplo:

```yaml
limits:
  default: 10
  numeric-permission:
    enabled: true
    prefix: "pwarp.limit."
  groups:
    vip:
      permission: "pwarp.limit.vip"
      amount: 20
    premium:
      permission: "pwarp.limit.premium"
      amount: 35
    staff:
      permission: "pwarp.limit.staff"
      amount: 100
```

Permisos relacionados:

| Permiso | Uso |
| --- | --- |
| `pwarp.set` | Crear warps |
| `pwarp.limit.<amount>` | Limite numerico directo |
| `pwarp.limit.vip` | Limite configurado por grupo |
| `pwarp.admin.limit.bypass` | Bypass de limite si se decide habilitar |

## Persistencia

La persistencia principal sera MySQL con HikariCP.

No se usara YAML por jugador porque el sistema necesita busquedas globales, listados paginados, ownership, purgas y estabilidad operativa.

### Tablas

#### player_warps

```sql
CREATE TABLE player_warps (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  server_id VARCHAR(64) NOT NULL,
  name VARCHAR(32) NOT NULL,
  name_normalized VARCHAR(32) NOT NULL,
  owner_uuid CHAR(36) NOT NULL,
  owner_name VARCHAR(16) NOT NULL,
  world VARCHAR(64) NOT NULL,
  x DOUBLE NOT NULL,
  y DOUBLE NOT NULL,
  z DOUBLE NOT NULL,
  yaw FLOAT NOT NULL,
  pitch FLOAT NOT NULL,
  icon_material VARCHAR(64) NULL,
  icon_data SMALLINT NOT NULL DEFAULT 0,
  locked BOOLEAN NOT NULL DEFAULT FALSE,
  whitelist_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  visits BIGINT NOT NULL DEFAULT 0,
  safe_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  last_visited_at BIGINT NULL,
  UNIQUE KEY uq_warp_name (server_id, name_normalized),
  KEY idx_owner (server_id, owner_uuid),
  KEY idx_visits (server_id, visits),
  KEY idx_updated (server_id, updated_at)
);
```

#### player_warp_whitelist

```sql
CREATE TABLE player_warp_whitelist (
  warp_id BIGINT NOT NULL,
  player_uuid CHAR(36) NOT NULL,
  player_name VARCHAR(16) NOT NULL,
  created_at BIGINT NOT NULL,
  PRIMARY KEY (warp_id, player_uuid)
);
```

#### player_warp_bonuses

```sql
CREATE TABLE player_warp_bonuses (
  server_id VARCHAR(64) NOT NULL,
  player_uuid CHAR(36) NOT NULL,
  extra_limit INT NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (server_id, player_uuid)
);
```

## Estrategia cache/MySQL

### Principio central

```text
Lecturas calientes -> cache local
Escrituras criticas -> MySQL primero -> cache despues
Stats no criticas -> buffer local -> batch async
```

No se usara write-behind para datos criticos como crear, borrar, renombrar, transferir owner, lock o whitelist. Esos cambios deben confirmarse en MySQL antes de modificar cache.

### Carga inicial

Durante `onEnable`:

```text
1. Conectar MySQL.
2. Ejecutar migraciones de schema.
3. Cargar todos los warps del server_id.
4. Cargar bonuses.
5. Inicializar indices en memoria.
6. Habilitar comandos, menus, listeners y placeholders.
```

### Caches principales

| Cache / indice | Uso |
| --- | --- |
| `warpsById` | Lookup por ID |
| `warpIdByName` | Lookup O(1) para `/pwarp <warp>` |
| `warpIdsByOwner` | Listado por owner |
| `allWarpSnapshot` | Menu global paginado y ordenable |
| `playerLimitCache` | Limite efectivo por jugador |
| `whitelistCache` | Whitelist por warp |
| `pendingTeleports` | Teleports con delay activos |
| `visitBuffer` | Visitas pendientes de flush a MySQL |

### Operaciones

| Operacion | Lee de | Escribe en |
| --- | --- | --- |
| `/pwarp <warp>` | Cache | Stats buffer |
| Abrir GUI | Cache | Nada |
| Buscar warps | Cache | Nada |
| Crear warp | Cache para validar | MySQL primero, cache despues |
| Borrar warp | Cache para validar | MySQL primero, cache despues |
| Rename | Cache + constraint SQL | MySQL primero, cache despues |
| Reset location | Cache para validar | MySQL primero, cache despues |
| Transfer owner | Cache para validar | MySQL primero, cache despues |
| Lock/unlock | Cache para validar | MySQL primero, cache despues |
| Whitelist | Cache para validar | MySQL primero, cache despues |
| Visits | Cache/buffer | Batch async |
| PlaceholderAPI | Cache | Nada |

### Ejemplo crear warp

```text
/pwarp set base
1. Validar sender jugador.
2. Validar regex y nombre reservado.
3. Validar limite efectivo.
4. Validar mundo permitido.
5. Validar ubicacion segura en main thread.
6. Insertar async en MySQL.
7. Si MySQL confirma, actualizar indices cache.
8. Si MySQL falla, no crear y enviar mensaje de error.
```

### Ejemplo teleport

```text
/pwarp base
1. Buscar warp en cache.
2. Validar lock/whitelist/permisos.
3. Iniciar delay si aplica.
4. Preparar chunk destino.
5. Volver a main thread.
6. Validar seguridad final.
7. Teleport sync con TeleportCause.PLUGIN.
8. Incrementar visits en buffer.
```

## Teleport y rendimiento

### Decision final

El teleport final debe ejecutarse sync:

```java
player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
```

Motivo:

- `Entity#teleport(...)` toca entidad, mundo, chunk y eventos Bukkit.
- Bukkit/Spigot 1.8 no es thread-safe para world/player/block APIs.
- `PlayerTeleportEvent` sigue siendo el evento correcto.

### Optimizacion permitida

El unico punto que vale la pena optimizar es la carga del chunk destino.

El `server.jar` real expone APIs de async chunk loading. Como compilamos contra `libs/server.jar`, el diseno debe aprovechar `World#getChunkAtAsync(...)` directamente desde un adaptador propio, manteniendo fallback sync solo como proteccion ante cambios futuros del motor.

Flujo final:

```text
Cache -> delay -> async chunk preload si disponible -> main thread safety check -> sync teleport
```

### Capa de precarga de chunk

```java
interface ChunkPreloader {
    CompletableFuture<Boolean> preload(World world, int chunkX, int chunkZ, boolean generate);
}
```

Implementaciones:

| Implementacion | Uso |
| --- | --- |
| `AsyncChunkPreloader` | Usa directamente `World#getChunkAtAsync(...)` desde la API de `libs/server.jar` |
| `SyncChunkPreloader` | Fallback con `world.loadChunk(x, z, false)` en main thread |

No se debe depender de `io.papermc.lib.PaperLib` para el teleport. Aunque existe en el motor, `World#getChunkAtAsync(...)` nos da control explicito sobre la carga de chunks y evita usar `PaperLib.teleportAsync(...)`, que permite generar chunks en el flujo interno verificado.

### Por que no usar `PaperLib.teleportAsync` directamente

El bytecode verificado muestra que `PaperLib.teleportAsync(...)`:

```text
1. Calcula el chunk destino.
2. Llama `PaperLib.getChunkAtAsyncUrgently(world, chunkX, chunkZ, true)`.
3. Luego llama `Entity.teleport(location, cause)`.
```

El punto delicado es el `true`, porque permite/genera chunk. Para player warps, generar chunks durante teleports puede provocar picos de TPS. Por eso preferimos controlar explicitamente `generate=false` por defecto.

### Config recomendada

```yaml
teleport:
  delay-seconds: 3
  cooldown-seconds: 2
  cancel-on-move: true
  cancel-on-damage: true
  preload-chunk: true
  generate-missing-chunks: false
  max-teleports-per-tick: 3
  unsafe-policy: "BLOCK"
  bypass-delay-permission: "pwarp.delay.bypass"
```

### Eventos relacionados

| Evento | Uso |
| --- | --- |
| `PlayerTeleportEvent` | Observar/cancelar teleports, detectar `TeleportCause.PLUGIN` |
| `PlayerMoveEvent` | Cancelar delay si cambia de bloque |
| `PlayerQuitEvent` | Cancelar teleport pendiente |
| `EntityDamageEvent` o `PlayerDamage` equivalente | Cancelar si recibe dano durante delay |
| `PlayerDeathEvent` | Cancelar si muere durante delay |

Para `PlayerMoveEvent`, comparar bloque y mundo, no yaw/pitch:

```java
from.getBlockX() != to.getBlockX()
|| from.getBlockY() != to.getBlockY()
|| from.getBlockZ() != to.getBlockZ()
|| !from.getWorld().equals(to.getWorld())
```

## Unsafe detection

La validacion de seguridad debe ejecutarse en main thread.

Reglas base:

- El mundo debe existir.
- La Y debe estar dentro del rango valido.
- El bloque de pies debe ser seguro/transitable.
- El bloque de cabeza debe ser seguro/transitable.
- El bloque inferior debe ser solido y no peligroso.
- No se debe permitir lava, fuego, cactus, portal u otros bloques peligrosos configurables.
- Si el chunk no esta cargado y no se permite precarga/generacion, bloquear teleport.

Politicas configurables:

| Politica | Comportamiento |
| --- | --- |
| `BLOCK` | Bloquea teleport si la ubicacion es insegura |
| `WARN` | Permite teleport y avisa; no recomendado como default |
| `SEARCH_NEARBY` | Busca ubicacion cercana segura; fuera de V1 salvo radio pequeno |

Default recomendado: `BLOCK`.

## Comandos

### Comando principal configurable

```yaml
commands:
  primary: "pwarp"
  aliases:
    - "pw"
    - "playerwarp"
```

El registro se hara con `CommandMap` para poder cambiar labels/aliases desde config.

En reload:

```text
1. Desregistrar labels anteriores.
2. Leer nuevo primary/aliases.
3. Registrar nuevos comandos.
4. Actualizar tab-complete.
```

### Lista de comandos V1

| Comando | Permiso | Uso |
| --- | --- | --- |
| `/pwarp` | `pwarp.open` | Abre menu principal |
| `/pwarp help` | `pwarp.help` | Muestra ayuda |
| `/pwarp <warp>` | `pwarp.warp` | Teletransporta al warp |
| `/pwarp set <name>` | `pwarp.set` | Crea warp en la ubicacion actual |
| `/pwarp remove <warp>` | `pwarp.remove` | Elimina warp propio |
| `/pwarp list [page] [player]` | `pwarp.list` | Lista warps por texto |
| `/pwarp open [menu] [option]` | `pwarp.open` | Abre menu especifico |
| `/pwarp amount [player]` | `pwarp.amount` | Ver cantidad y limite |
| `/pwarp lock <warp> [true/false]` | `pwarp.lock` | Bloquea/desbloquea warp |
| `/pwarp reset <warp>` | `pwarp.reset` | Actualiza ubicacion del warp |
| `/pwarp rename <warp> <name>` | `pwarp.rename` | Renombra warp |
| `/pwarp setowner <warp> <player>` | `pwarp.setowner` | Transfiere owner |
| `/pwarp whitelist enable <warp>` | `pwarp.whitelist` | Activa whitelist |
| `/pwarp whitelist disable <warp>` | `pwarp.whitelist` | Desactiva whitelist |
| `/pwarp whitelist add <warp> <player>` | `pwarp.whitelist` | Agrega jugador |
| `/pwarp whitelist remove <warp> <player>` | `pwarp.whitelist` | Quita jugador |
| `/pwarp whitelist list <warp>` | `pwarp.whitelist` | Lista whitelist |
| `/pwarp removeall <player>` | `pwarp.admin.removeall` | Elimina todos los warps de un jugador |
| `/pwarp remove <warp> <player>` | `pwarp.admin.remove` | Elimina warp como admin |
| `/pwarp reload` | `pwarp.admin.reload` | Recarga config/menus/mensajes |
| `/pwarp addwarps <player> <amount>` | `pwarp.admin.addwarps` | Suma bonus persistente |
| `/pwarp purge unsafe` | `pwarp.admin.purge` | Purga o lista warps inseguros |
| `/pwarp purge inactive <days>` | `pwarp.admin.purge` | Purga warps inactivos |

Alias de UX aceptables:

- `/pwarp del` como alias de `remove`.
- `/pwarp tp <warp>` opcional, pero `/pwarp <warp>` debe seguir siendo la ruta rapida.

## GUI con zMenu

### Principio

zMenu debe encargarse del inventario, patrones, botones base y paginacion. Nuestro plugin solo registra botones custom para renderizar warps y manejar acciones.

### Archivos

```text
menus/
  inventories/
    main.yml
    manage.yml
    whitelist.yml
  patterns/
    decoration.yml
    pagination.yml
```

### Botones custom

| Boton | Responsabilidad |
| --- | --- |
| `PWE_WARP_LIST` | Renderizar warps globales paginados |
| `PWE_MY_WARPS` | Renderizar warps propios |
| `PWE_SEARCH` | Iniciar busqueda por chat o limpiar busqueda |
| `PWE_REFRESH` | Refrescar cache visual/menu |
| `PWE_FILTER_SORT` | Cambiar orden/filtro actual |
| `PWE_MANAGE_WARP` | Acciones de owner/admin sobre un warp |

### API correcta de zMenu 1.0.3.7

Usar:

- `fr.maxlego08.menu.button.ZButton`
- `fr.maxlego08.menu.api.button.PaginateButton`
- `fr.maxlego08.menu.api.loader.ButtonLoader`
- `fr.maxlego08.menu.inventory.inventories.InventoryDefault`
- `fr.maxlego08.menu.api.utils.Placeholders`

No usar:

- `InventoryEngine`, porque no existe en zMenu `1.0.3.7`.

### Slots y materiales 1.8

Ejemplo compatible con 1.8:

```yaml
items:
  warps:
    type: PWE_WARP_LIST
    empty-slot: 22
    slots:
      - "10-16"
      - "19-25"
      - "28-34"
      - "37-43"
    item:
      material: BARRIER
      name: "&cNo warps found"
```

Para decoracion:

```yaml
material: STAINED_GLASS_PANE
data: 3
```

No usar nombres modernos como `LIGHT_BLUE_STAINED_GLASS_PANE`.

### Renderizado performante

El boton paginado debe:

- Resolver solo los warps de la pagina actual.
- No construir `ItemStack` para todos los warps existentes.
- Leer de snapshots/cache local.
- Aplicar placeholders por warp solo al renderizar la pagina visible.

### Busqueda

V1 soporta busqueda simple por:

- Nombre del warp.
- Owner.

Flujo:

```text
1. Click en boton search.
2. Cerrar menu o dejar listener temporal.
3. Capturar siguiente mensaje de chat.
4. Guardar query en cache del jugador.
5. Reabrir menu filtrado.
6. Click derecho limpia busqueda.
```

Debe existir timeout para busqueda pendiente, por ejemplo 30 segundos.

### Ordenes V1

- `newest`
- `oldest`
- `most-visits`
- `alphabetical`
- `owner`

## PlaceholderAPI

Identifier recomendado:

```text
playerwarps
```

Placeholders V1:

| Placeholder | Resultado |
| --- | --- |
| `%playerwarps_amount%` | Warps del jugador |
| `%playerwarps_limit%` | Limite efectivo |
| `%playerwarps_remaining%` | Slots disponibles |
| `%playerwarps_total%` | Total de warps del server_id |
| `%playerwarps_visits_<warp>%` | Visitas del warp |
| `%playerwarps_owner_<warp>%` | Owner del warp |
| `%playerwarps_locked_<warp>%` | Estado locked |

Reglas:

- No consultar MySQL desde placeholders.
- Usar cache local.
- Devolver fallback configurable si el dato no existe.
- Registrar solo si PlaceholderAPI esta habilitado.

## Configuracion

### config.yml

```yaml
config-version: 1

server-id: "survival-1"

commands:
  primary: "pwarp"
  aliases:
    - "pw"
    - "playerwarp"

warps:
  name-regex: "^[a-zA-Z0-9_-]{3,32}$"
  reserved-names:
    - "help"
    - "set"
    - "remove"
    - "reload"
    - "list"
    - "open"
    - "lock"
    - "rename"
    - "reset"
    - "whitelist"
  allowed-worlds:
    mode: "BLACKLIST"
    worlds:
      - "disabled_world"
  default-locked: false
  default-whitelist-enabled: false

limits:
  default: 10
  numeric-permission:
    enabled: true
    prefix: "pwarp.limit."
  groups:
    vip:
      permission: "pwarp.limit.vip"
      amount: 20

teleport:
  delay-seconds: 3
  cooldown-seconds: 2
  cancel-on-move: true
  cancel-on-damage: true
  preload-chunk: true
  generate-missing-chunks: false
  max-teleports-per-tick: 3
  unsafe-policy: "BLOCK"
  bypass-delay-permission: "pwarp.delay.bypass"

purge:
  unsafe:
    enabled: true
  inactive:
    enabled: false
    days: 90
```

### storage.yml

```yaml
config-version: 1

mysql:
  host: "localhost"
  port: 3306
  database: "hera_playerwarps"
  username: "root"
  password: ""
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout-ms: 5000
    max-lifetime-ms: 1800000
```

### messages.yml

Debe contener todos los mensajes configurables. No debe haber textos hardcodeados salvo logs tecnicos internos.

Ejemplos de keys:

```yaml
messages:
  prefix: "&8[&bPlayerWarps&8] "
  warp-created: "&aWarp &f%warp% &acreated."
  warp-removed: "&aWarp &f%warp% &aremoved."
  warp-not-found: "&cWarp &f%warp% &cdoes not exist."
  teleport-starting: "&eTeleporting in &f%seconds% &eseconds. Do not move."
  teleport-cancelled: "&cTeleport cancelled."
  teleport-success: "&aTeleported to &f%warp%&a."
  unsafe-location: "&cThis warp location is unsafe."
```

## Arquitectura de paquetes

Paquete base:

```text
com.hera.playerwarps
```

Estructura:

```text
src/main/java/com/hera/playerwarps/
  PlayerWarpsPlugin.java

  bootstrap/
    PluginBootstrap.java
    Services.java
    Scheduler.java

  config/
    ConfigManager.java
    Configs.java
    Settings.java
    Messages.java

  command/
    CommandRegistrar.java
    PlayerWarpCommand.java
    CommandContext.java
    subcommands/

  warp/
    Warp.java
    WarpId.java
    WarpName.java
    WarpService.java
    WarpRepository.java
    WarpCache.java
    WarpSearchService.java
    WarpLimitService.java
    WarpWhitelistService.java
    WarpPurgeService.java

  teleport/
    TeleportService.java
    PendingTeleport.java
    SafetyChecker.java
    SafetyResult.java
    ChunkPreloader.java
    AsyncChunkPreloader.java
    SyncChunkPreloader.java

  menu/
    MenuLoader.java
    button/
      WarpListButton.java
      MyWarpsButton.java
      SearchButton.java
      RefreshButton.java
      ManageWarpButton.java
    loader/
      WarpListButtonLoader.java
      SearchButtonLoader.java

  storage/
    Database.java
    SchemaMigrator.java
    MysqlWarpRepository.java
    MysqlWhitelistRepository.java
    MysqlLimitBonusRepository.java

  placeholder/
    PlayerWarpsExpansion.java

  listener/
    PlayerQuitListener.java
    PlayerMoveListener.java
    PlayerDamageListener.java
    PlayerDeathListener.java
    ChatSearchListener.java

  permission/
    PermissionService.java

  util/
    Texts.java
    Names.java
    TimeFormatter.java
```

Esta estructura evita sobreingenieria, pero separa responsabilidades reales: configuracion, comandos, dominio de warps, teleport, menu, storage, placeholders y listeners.

## Lifecycle

### onEnable

```text
1. Crear data folder si no existe.
2. Cargar config.yml, storage.yml, messages.yml con boosted-yaml.
3. Inicializar HikariCP.
4. Ejecutar migraciones de schema.
5. Cargar warps y bonuses del server_id.
6. Construir caches e indices.
7. Detectar capacidades del servidor para async chunk preload.
8. Registrar listeners.
9. Registrar comandos dinamicos.
10. Registrar botones zMenu.
11. Cargar patrones e inventarios zMenu.
12. Registrar PlaceholderAPI si esta presente.
```

### onDisable

```text
1. Cancelar teleports pendientes.
2. Flush final de visitBuffer.
3. Desregistrar comandos dinamicos.
4. Desregistrar botones/listeners de zMenu si aplica.
5. Cerrar HikariCP.
6. Limpiar caches.
```

### reload

```text
1. Recargar configs y mensajes.
2. Recalcular Settings.
3. Re-registrar comandos si cambio primary/aliases.
4. Recargar menus y patrones.
5. Reconfigurar limites, teleport y purge.
6. No reiniciar Hikari salvo que cambie storage.yml.
```

Si cambia `storage.yml`, el reload debe reconectar de forma controlada o pedir restart segun implementacion final. No se debe cerrar/reabrir pool mientras hay operaciones pendientes sin control.

## Edge cases obligatorios

### Creacion y edicion

- Nombre duplicado por `server_id`.
- Nombre invalido por regex.
- Nombre reservado que choque con subcomando.
- Crear warp en mundo bloqueado.
- Crear warp en ubicacion insegura.
- Crear warp si MySQL esta caido.
- Crear warp mientras el jugador supera el limite.
- Rename hacia nombre existente.
- Reset hacia mundo bloqueado.
- Reset hacia ubicacion insegura.

### Ownership y limites

- Jugador pierde rango y queda sobre limite.
- Transfer owner a jugador sin espacio disponible.
- Transfer owner a jugador que nunca entro.
- Owner offline.
- Admin bypass si esta configurado.

### Teleport

- World eliminado o renombrado.
- Chunk no cargado.
- Chunk no generado y `generate-missing-chunks=false`.
- Ubicacion insegura al momento final.
- Warp locked.
- Whitelist enabled sin miembros.
- Jugador se mueve durante delay.
- Jugador recibe dano durante delay.
- Jugador muere durante delay.
- Jugador se desconecta durante delay.
- Jugador inicia otro teleport durante delay.
- Otro plugin cancela `PlayerTeleportEvent`.
- Demasiados teleports en el mismo tick.

### Menu y cache

- Menu abierto mientras un warp se elimina.
- Menu abierto mientras un warp se renombra.
- Pagina queda fuera de rango despues de cambios.
- Busqueda pendiente expira.
- Placeholder consultado antes de terminar warmup.
- zMenu no esta presente.
- zMenu presente pero menu YAML invalido.

### Operacion

- Reload durante teleports pendientes.
- Reload cambia comando principal.
- MySQL lento o caido.
- Hikari no puede iniciar.
- Flush de visitas falla.
- Purge masivo sin confirmacion.

## Criterios de aceptacion del diseno

- El plugin no consulta MySQL para abrir el menu principal.
- El plugin no consulta MySQL para resolver `/pwarp <warp>`.
- Las escrituras criticas no modifican cache antes de confirmar MySQL.
- El teleport final se ejecuta sync con `TeleportCause.PLUGIN`.
- La precarga async de chunk es opcional y tiene fallback.
- `generate-missing-chunks` es `false` por defecto.
- Los menus default usan materiales compatibles con Minecraft 1.8.
- zMenu API usada corresponde a `1.0.3.7`, no a versiones modernas.
- PlaceholderAPI no ejecuta queries.
- Los limites usan el mayor permiso y no suman permisos.
- Los bonus persistentes si se suman.
- El comando principal y aliases son configurables.

## Orden recomendado de implementacion

1. Gradle, shadow, resources base y `plugin.yml`.
2. Config loader con boosted-yaml.
3. Database/Hikari y migraciones.
4. Modelos `Warp`, `WarpLocation`, `WarpCache`.
5. Repositorios MySQL.
6. Warmup cache en `onEnable`.
7. Command router dinamico con `/pwarp help` y `/pwarp reload`.
8. `/pwarp set`, `/pwarp remove`, `/pwarp <warp>` basico.
9. `SafetyChecker` y `TeleportService` con chunk preload.
10. Limites por permisos y bonuses.
11. zMenu loader y menu principal paginado.
12. Search/filter/sort.
13. Lock, whitelist, reset, rename, transfer owner.
14. PlaceholderAPI.
15. Purge y hardening operativo.

## Decisiones que no deben reabrirse sin motivo fuerte

- No usar `cloud-minecraft 2.x` para este proyecto.
- No hacer teleports finales async manuales.
- No consultar MySQL desde PlaceholderAPI.
- No renderizar todos los warps para construir una sola pagina del menu.
- No usar materiales modernos en menus default de 1.8.
- No aplicar write-behind a datos criticos.
- No introducir Redis o multi-server realtime en V1.

## Resumen final

`PlayerWarpsEngine` debe nacer como un plugin simple pero serio: MySQL para persistencia, cache local para lecturas calientes, zMenu para UI configurable, PlaceholderAPI opcional y un sistema de teleport seguro que aprovecha async chunk loading del fork real sin romper las reglas de Bukkit.

La decision mas importante de rendimiento es no convertir MySQL ni la generacion de chunks en parte del camino caliente. El camino caliente de `/pwarp <warp>` debe ser cache, validacion, precarga controlada de chunk, safety check sync y teleport sync.

Este documento es la referencia formal para la implementacion inicial del proyecto.
