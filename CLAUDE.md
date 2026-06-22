# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## O que é

Plugin Paper/Spigot (Java 21) que consome a API da **CentralCart** para: exibir os top 3 doadores do mês anterior, criar/atualizar NPCs (via Citizens) desses doadores, distribuir recompensas e fazer broadcast de novos posts do blog da loja. Nome do artefato: `centralCartTopPlugin`. Pacote raiz: `plugin.centralCartTopPlugin`.

## Build, deploy e execução

> ⚠️ **Gotcha de JDK (lê isto antes de buildar).** O `~/.gradle/gradle.properties` global desta máquina força `org.gradle.java.home=jdk-25.0.3` (para projetos Fabric). O **Gradle 8.8 não roda sobre Java 25** e falha com `Unsupported class file major version 69`. Há um **JDK 21** em `C:\Program Files\Java\jdk-21` (alvo do projeto). Sobrescreva por build, **sem** alterar o gradle.properties global:

```bash
# Compilar
./gradlew compileJava --no-daemon -Dorg.gradle.java.home="C:\Program Files\Java\jdk-21"

# Gerar o jar (sai em build/libs/centralCartTopPlugin-<version>.jar; inclui o gson shaded)
./gradlew jar --no-daemon -Dorg.gradle.java.home="C:\Program Files\Java\jdk-21"

# Copiar o jar para o servidor local (D:/AUSTV/localhost/plugins)
./gradlew copyJar --no-daemon -Dorg.gradle.java.home="C:\Program Files\Java\jdk-21"

# Subir um servidor Paper de teste (plugin run-paper, MC 1.21)
./gradlew runServer --no-daemon -Dorg.gradle.java.home="C:\Program Files\Java\jdk-21"
```

Não há suíte de testes automatizados — a validação é via build + teste no servidor (comandos `/test*`).

A versão vive em `build.gradle` (`version = '...'`) e é injetada no `plugin.yml` via `processResources` (`expand`, o `plugin.yml` usa `${version}`). **Convenção do dono do repo: sempre incrementar a versão em `build.gradle` a cada modificação.**

## Arquitetura

Camadas sob `src/main/java/plugin/centralCartTopPlugin/`:

- **`CentralCartTopPlugin`** — entrypoint. No `onEnable`: `saveDefaultConfig()` → `mergeConfigDefaults()` → inicializa serviços → registra comandos/listeners → carrega NPCs após `STARTUP_DELAY_TICKS` → inicia as tasks. `reloadServices()` recria serviços e re-registra comandos (usado pelo `/centralcartreload`).
- **`service/`** — `CentralCartApiService` (top doadores, com `TopDonatorsCache`), `BlogPostService` (posts do blog), `RewardsManager`. Os serviços de rede expõem `CompletableFuture` e fazem I/O fora da main thread.
- **`task/`** — `MonthlyNpcUpdateTask` (timer horário; atualiza NPCs no dia 1º), `BlogPostCheckTask` (a cada 5 min; detecta posts novos).
- **`service/TopNpcManager`** — toda a integração com Citizens (criação/atualização/skin/posição dos NPCs).
- **`manager/MessagesManager`** + **`util/MessageFormatter`** — mensagens externalizadas em `messages.yml`; formatter aceita **códigos legados `&`/`§` E tags MiniMessage** na mesma string.
- **`util/`** — `Constants` (defaults, URLs, intervalos), `PluginUtils` (mapeamento posição→key, normalização de domínio, leitura de corpo de erro HTTP), `DateTimeUtil` (parsing de datas da API), `BlogNotifier` (montagem de placeholders + broadcast do blog, compartilhado entre task e comando de teste).

### Invariantes que não são óbvias

- **A API CentralCart é multi-tenant.** TODA chamada (`top_customers` e `webstore/post`) precisa do header **`x-store-domain`** com o domínio da loja (`api.store_domain`, ex.: `loja.austv.net`). Sem ele a API responde **`404 "Store not found"`** — foi a causa de NPCs e broadcast de blog pararem de funcionar. O token vai em `Authorization: Bearer`. Mantenha os dois sempre que adicionar novos endpoints.
- **Config self-healing.** `mergeConfigDefaults()` (`copyDefaults(true)` + `saveConfig()`) mescla chaves novas em `config.yml` já existentes — `saveDefaultConfig()` sozinho **não** atualiza arquivos existentes. Ao adicionar uma chave de config, garanta que ela exista no `config.yml` embutido, senão servidores antigos nunca a recebem.
- **Thread safety.** Serviços fazem HTTP em async (`CompletableFuture`/`runTaskAsynchronously`). Qualquer interação com Bukkit/Citizens (spawn de NPC, `broadcast`, `saveConfig`) DEVE voltar à main thread via `Bukkit.getScheduler().runTask(...)`. As tasks e comandos já seguem esse padrão — preserve-o.
- **NPCs são reutilizados, não recriados.** `TopNpcManager` persiste `npcs.saved_ids` (posição→id do NPC) no config; o Citizens recarrega os NPCs entre reinícios. Não destrua NPCs no `onDisable`.
- **Citizens é `softdepend`.** Sempre cheque `npcManager.isCitizensEnabled()` antes de mexer com NPCs.
- **Detecção de post novo.** `BlogPostCheckTask` faz *seeding* na primeira execução (marca o post mais recente como visto sem anunciar) e depois anuncia todos os posts com `id` maior que `blog.last_seen_post_id` (comparação numérica). A API entrega os posts em ordem decrescente de `id`.

## Configuração (`src/main/resources/`)

`config.yml` (API/token/store_domain, NPCs, blog), `messages.yml` (textos), `rewards.yml` (recompensas), `plugin.yml` (comandos/permissões). Permissão de admin: `centralcart.admin`. Detalhes de comandos no `README.md`.
