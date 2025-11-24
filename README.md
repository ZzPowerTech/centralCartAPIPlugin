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

## 🎮 Comandos

| Comando | Aliases | Descrição | Permissão |
|---------|---------|-----------|-----------|
| `/topdonadores` | `/topdoadores`, `/topdonors` | Mostra os top 3 doadores do mês anterior | Nenhuma (todos podem usar) |

## 📦 Instalação

1. Baixe o arquivo `.jar` da seção [Releases](../../releases)
2. Coloque o arquivo na pasta `plugins` do seu servidor
3. Inicie o servidor (um arquivo `config.yml` será criado)
4. Pare o servidor e edite `plugins/centralCartTopPlugin/config.yml`
5. Configure seu token de API (veja seção Autenticação acima)
6. Reinicie o servidor
7. Use `/topdonadores` no jogo

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

## 🔧 Requisitos

- **Servidor**: Paper/Spigot 1.21+
- **Java**: 21+
- **Token de API**: Token de autenticação da CentralCart (obrigatório)

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

## 🚀 CI/CD

O projeto possui integração contínua configurada com GitHub Actions:

- ✅ Build automático em cada push/PR
- ✅ Validação do Gradle Wrapper
- ✅ Cache do Gradle para builds mais rápidos
- ✅ Criação automática de releases
- ✅ Upload do JAR como artefato

## 📝 Licença

Este projeto é proprietário da CentralCart.

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/MinhaFeature`)
3. Commit suas mudanças (`git commit -m 'Adiciona MinhaFeature'`)
4. Push para a branch (`git push origin feature/MinhaFeature`)
5. Abra um Pull Request

## 📧 Suporte

Para suporte, entre em contato através do site da [CentralCart](https://centralcart.com.br)

---

Desenvolvido com ❤️ para a comunidade CentralCart

