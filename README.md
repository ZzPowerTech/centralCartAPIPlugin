# 🏆 CentralCart Top Plugin

Plugin para Minecraft (Paper/Spigot) que exibe os top 3 doadores do mês anterior através da API da CentralCart.

## 📋 Descrição

Este plugin se conecta à API da CentralCart e busca automaticamente os top 3 doadores do mês anterior, exibindo suas informações de forma elegante no chat do jogo.

## ✨ Funcionalidades

- ✅ Busca automática dos top doadores do mês anterior
- ✅ Exibição formatada com medalhas (🥇🥈🥉)
- ✅ Requisições assíncronas (não trava o servidor)
- ✅ Integração completa com a API CentralCart
- ✅ Suporte a aliases para o comando
- ✅ Sistema de configuração personalizável
- ✅ Mensagens customizáveis
- ✅ Timeout configurável para API
- ✅ Opção de mostrar/ocultar valores totais
- ✅ Símbolo de moeda configurável
- ✅ **Sistema de recompensas automáticas**
- ✅ **Recompensas pendentes para jogadores offline**
- ✅ **Atualização automática mensal (dia 1º)**
- ✅ **NPCs dos top doadores (requer Citizens)**

## 🎮 Comandos

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/topdonadores` | `/topdoadores`, `/topdonors` | Mostra os top 3 doadores do mês anterior | Nenhuma |
| `/spawntopnpcs` | - | Cria/atualiza NPCs dos top doadores | `centralcart.admin` |
| `/removetopnpcs` | - | Remove todos os NPCs dos top doadores | `centralcart.admin` |
| `/centralcartreload` | `/ccreload`, `/centralreload` | Recarrega as configurações do plugin | `centralcart.admin` |
| `/testschedule` | `/testaratualizacao`, `/testupdate` | Testa a atualização automática mensal | `centralcart.admin` |
| `/scheduleinfo` | `/infoatualizacao`, `/schedulestat` | Mostra informações da próxima atualização | `centralcart.admin` |

## 📦 Instalação

1. Baixe o arquivo `.jar` da seção [Releases](../../releases)
2. Coloque o arquivo na pasta `plugins` do seu servidor
3. **(Opcional)** Instale o plugin [Citizens](https://www.spigotmc.org/resources/citizens.13811/) para NPCs
4. Inicie o servidor (arquivos `config.yml` e `rewards.yml` serão criados)
5. Pare o servidor e edite `plugins/centralCartTopPlugin/config.yml`:
   - Configure seu **token de API** (obrigatório)
   - Configure as **localizações dos NPCs** se desejar usar Citizens
6. Edite `plugins/centralCartTopPlugin/rewards.yml` para configurar as recompensas
7. Reinicie o servidor
8. Use `/topdonadores` para testar

## 🚀 Uso Rápido

### Para Jogadores
```
/topdonadores - Ver os top 3 doadores do mês passado
```

### Para Administradores
```
/spawntopnpcs - Criar/atualizar NPCs dos top doadores
/removetopnpcs - Remover todos os NPCs
/centralcartreload - Recarregar configurações
/testschedule - Testar atualização automática
/scheduleinfo - Ver próxima atualização automática
```

### Atualização Automática

O plugin atualiza automaticamente **todo dia 1º de cada mês às 00:00h**:
- ✅ Busca os top 3 doadores do mês anterior
- ✅ Atualiza os NPCs com os novos dados
- ✅ Distribui recompensas automaticamente
- ✅ Envia broadcast no servidor
- ✅ Notifica administradores online

## ⚙️ Configuração

O plugin cria um arquivo `config.yml` que permite personalizar diversos aspectos:

```yaml
# URL da API CentralCart
api:
  url: "https://api.centralcart.com.br/v1/app/widget/top_customers"
  timeout: 5000 # Timeout em milissegundos
  token: "SEU_TOKEN_AQUI"  # ⚠️ OBRIGATÓRIO - Token de autenticação

# Mensagens personalizáveis
messages:
  loading: "§e§l[CentralCart] §aBuscando top doadores do mês anterior..."
  error: "§c§l[CentralCart] §cNão foi possível buscar os dados. Verifique os logs."
  header: "§6§l========================================"
  title: "§e§l      TOP 3 DOADORES DO MÊS ANTERIOR"
  footer: "§6§l========================================"

# Formato de exibição
display:
  show-total: true # Exibir valor total doado
  currency-symbol: "R$" # Símbolo da moeda

# Medalhas por posição
medals:
  first: "§6🥇"
  second: "§7🥈"
  third: "§c🥉"
```

### Códigos de Cor do Minecraft

Você pode usar os seguintes códigos nas mensagens:
- `§0` - Preto
- `§1` - Azul escuro
- `§2` - Verde escuro
- `§3` - Ciano escuro
- `§4` - Vermelho escuro
- `§5` - Roxo
- `§6` - Dourado
- `§7` - Cinza
- `§8` - Cinza escuro
- `§9` - Azul
- `§a` - Verde
- `§b` - Ciano
- `§c` - Vermelho
- `§d` - Rosa
- `§e` - Amarelo
- `§f` - Branco
- `§l` - Negrito
- `§o` - Itálico
- `§r` - Reset

## 🎁 Sistema de Recompensas

O plugin possui um sistema automático de recompensas para os top 3 doadores do mês.

### Como Funciona

1. **Dia 1º do mês**: Automaticamente às 00:00h, o sistema:
   - Busca os top 3 doadores do mês anterior
   - Atualiza os NPCs com os novos doadores
   - Distribui as recompensas configuradas
   - Envia broadcast para o servidor

2. **Jogadores Online**: Recebem as recompensas imediatamente
3. **Jogadores Offline**: As recompensas são salvas e entregues quando logarem

### Configuração de Recompensas

Edite o arquivo `rewards.yml` para configurar as recompensas:

```yaml
enabled: true  # Ativar/desativar sistema

rewards:
  first:  # 1º lugar
    commands:  # Comandos executados pelo console
      - "give {player} minecraft:diamond 64"
      - "eco give {player} 100000"
      - "lp user {player} permission set vip.diamond true"
    
    items:  # Itens entregues no inventário
      - material: DIAMOND
        amount: 64
        name: "§6§l🥇 Prêmio 1º Lugar"
        lore:
          - "§7Top Doador de {month}"
          - "§e§lParabéns!"
        enchantments:
          - "UNBREAKING:3"
  
  second:  # 2º lugar
    commands:
      - "give {player} minecraft:diamond 32"
      - "eco give {player} 50000"
    items:
      - material: DIAMOND
        amount: 32
        name: "§7§l🥈 Prêmio 2º Lugar"
  
  third:  # 3º lugar
    commands:
      - "give {player} minecraft:diamond 16"
      - "eco give {player} 25000"
    items:
      - material: DIAMOND
        amount: 16
        name: "§c§l🥉 Prêmio 3º Lugar"

# Mensagens personalizadas
messages:
  broadcast:  # Anúncio público quando distribuir
    - "§6§l========================================"
    - "§e§l    🎉 RECOMPENSAS DO TOP DOADORES 🎉"
    - "§a§lParabéns aos top 3 de {month}!"
    - "§6🥇 1º: {first_player}"
    - "§7🥈 2º: {second_player}"
    - "§c🥉 3º: {third_player}"
    - "§6§l========================================"
  
  player_received:  # Mensagem para quem recebeu online
    - "§e§l🎉 VOCÊ ESTÁ NO TOP {position}! 🎉"
    - "§fParabéns! Você ficou em §e{position}º lugar!"
  
  pending_rewards:  # Mensagem ao logar (estava offline)
    - "§e§l🎁 VOCÊ TEM RECOMPENSAS PENDENTES!"
    - "§fVocê ficou no top {position} de {month}!"
```

### Variáveis Disponíveis

- `{player}` - Nome do jogador
- `{month}` - Nome do mês em português (ex: "Outubro")
- `{position}` - Posição no ranking (1º, 2º, 3º)
- `{first_player}`, `{second_player}`, `{third_player}` - Nomes dos top 3

### Materiais Disponíveis

Consulte [esta lista](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html) para nomes de materiais válidos.

## 🔧 Requisitos

- **Servidor**: Paper/Spigot 1.21+
- **Java**: 21+
- **Token de API**: Token de autenticação da CentralCart (obrigatório)
- **Citizens** (opcional): Para criar NPCs dos top doadores

## 🔐 Autenticação

⚠️ **IMPORTANTE**: Este plugin requer um token de autenticação para acessar a API da CentralCart.

### Configuração do Token

Após a instalação, edite `plugins/centralCartTopPlugin/config.yml` e configure seu token:

```yaml
api:
  token: "7cf783d2-6142-4705-b207-e50b722735a8"  # ← Substitua pelo seu token
```

### Resolvendo Erro 401

Se você receber o erro `HTTP error code: 401`:

1. Edite `plugins/centralCartTopPlugin/config.yml`
2. Substitua `COLOQUE_SEU_TOKEN_AQUI` pelo seu token real
3. Salve o arquivo
4. Reinicie o servidor (não use apenas `/reload`)

**⚠️ NUNCA compartilhe seu token publicamente!**

## 🛠️ Desenvolvimento

### Estrutura do Projeto

```
src/main/java/plugin/centralCartTopPlugin/
├── CentralCartTopPlugin.java          # Classe principal do plugin
├── command/
│   └── TopDonadoresCommand.java       # Comando /topdonadores
├── model/
│   └── TopCustomer.java               # Modelo de dados do doador
└── service/
    └── CentralCartApiService.java     # Serviço de integração com API
```

### Compilar o Projeto

```bash
./gradlew clean build
```

O arquivo JAR será gerado em `build/libs/centralCartTopPlugin-1.0.jar`

### API Utilizada

O plugin consome a seguinte API:
```
GET https://api.centralcart.com.br/v1/app/widget/top_customers?from=YYYY-MM-DD&to=YYYY-MM-DD
```

**Resposta esperada:**
```json
{
  "data": [
    {
      "name": "Nome do Doador",
      "total": 1234.56
    }
  ]
}
```

## 📸 Preview

### Comando no Chat
Exemplo de saída do comando `/topdonadores`:

```
========================================
      TOP 3 DOADORES DO MÊS ANTERIOR
========================================

🥇 #1 - ZzPowerTechzZ (R$ 1139,99)
🥈 #2 - fjZariel_ (R$ 110,00)
🥉 #3 - herick_gamer (R$ 1,01)

========================================
```

### NPCs no Servidor
Com o Citizens instalado, você pode criar NPCs dos top doadores:

1. **Configure as localizações** no `config.yml`
2. **Execute** `/spawntopnpcs` para criar os NPCs
3. Os NPCs serão criados com:
   - Nome do jogador (skin do Minecraft)
   - Nome exibido personalizado
   - Posição no ranking

**Atualização Automática Mensal:**
- ✅ **Todo dia 1º do mês às 00:00h** os NPCs são atualizados automaticamente!
- ✅ Os administradores online recebem notificação
- ✅ Os logs registram a atualização
- ✅ Para desativar: `npcs.auto_update_enabled: false` no config.yml

**Atualização Manual:**
Execute `/spawntopnpcs` a qualquer momento para atualizar os NPCs manualmente.

## 🚀 CI/CD

O projeto possui integração contínua configurada com GitHub Actions:

- ✅ Build automático em cada push/PR
- ✅ Validação do Gradle Wrapper
- ✅ Cache do Gradle para builds mais rápidos
- ✅ Criação automática de releases
- ✅ Upload do JAR como artefato

---

Desenvolvido com ❤️ para a comunidade CentralCart

