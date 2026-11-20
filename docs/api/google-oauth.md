# Login com Google (OAuth 2.0 / OpenID Connect)

O backend conduz o authorization-code flow, valida a identidade Google e emite o mesmo
JWT local e `AuthResponse` do login por senha. O frontend de Pablo só precisa iniciar
o popup e consumir o resultado; não recebe Client Secret nem access/refresh token Google.
O fluxo fica desativado por padrão. Criar as credenciais abaixo é necessário para teste real.

## Configuração no Google Cloud

1. Crie/selecione o projeto no [Google Cloud Console](https://console.cloud.google.com/).
2. Abra **Google Auth Platform** (em algumas interfaces, APIs e serviços → Credenciais/tela de consentimento).
   Configure nome do aplicativo, e-mail de suporte e contato do desenvolvedor.
3. Em **Audience/Público**, use o público adequado. Para testes com contas pessoais,
   escolha External/Externo e cadastre os e-mails de teste enquanto o app estiver em Testing.
4. Em **Clients/Clientes**, crie um cliente OAuth do tipo **Web application**.
5. Cadastre exatamente esta **Authorized redirect URI** para desenvolvimento:
   `http://localhost:8080/login/oauth2/code/google`.
   É o callback do backend, não a página do frontend. Não acrescente barra no final.
6. Guarde Client ID e Client Secret no ambiente do backend. Nunca coloque o segredo em
   `VITE_*`, JavaScript, repositório, screenshot ou mensagens de chat.

Este fluxo usa somente `openid`, `profile` e `email`, para login; não solicita acesso a
Drive/Gmail/Calendar nem acesso offline. A configuração de origens JavaScript do Google
não substitui o redirect URI. Aqui o fluxo é iniciado pelo backend, sem SDK Google no frontend.
Referência: [OpenID Connect do Google](https://developers.google.com/identity/openid-connect/openid-connect).

## Ambiente local

| Variável | Valor local |
| --- | --- |
| `GOOGLE_OAUTH_ENABLED` | `true` após obter credenciais |
| `GOOGLE_CLIENT_ID` | ID do cliente Web criado |
| `GOOGLE_CLIENT_SECRET` | Segredo desse mesmo cliente, somente backend |
| `GOOGLE_REDIRECT_URI` | `http://localhost:8080/login/oauth2/code/google` |
| `GOOGLE_FRONTEND_ORIGIN` | `http://localhost:5173` (origem exata, sem caminho/barra final) |
| `SESSION_COOKIE_SECURE` | `false` só no HTTP local; `true` com HTTPS |
| `JWT_SECRET` | Segredo local existente, de pelo menos 32 bytes |

Não misture `localhost` e `127.0.0.1` entre as URLs. A origem frontend é fixa no servidor:
não é recebida como parâmetro livre, para evitar enviar tokens a outro site.
O `.env.example` é uma referência: Spring Boot iniciado com Maven/Java **não importa
automaticamente** o `.env` da raiz. Configure variáveis na execução da IDE ou no processo.

Exemplo PowerShell na raiz, sem digitar o segredo no histórico:

```powershell
$env:GOOGLE_OAUTH_ENABLED = 'true'
$env:GOOGLE_CLIENT_ID = 'SEU_CLIENT_ID.apps.googleusercontent.com'
$env:GOOGLE_CLIENT_SECRET = [System.Net.NetworkCredential]::new('', (Read-Host 'Client Secret Google' -AsSecureString)).Password
$env:GOOGLE_REDIRECT_URI = 'http://localhost:8080/login/oauth2/code/google'
$env:GOOGLE_FRONTEND_ORIGIN = 'http://localhost:5173'
$env:SESSION_COOKIE_SECURE = 'false'
# Defina também JWT_SECRET e DB_URL/DB_USERNAME/DB_PASSWORD conforme docs/api/README.md.
mvn -f apps/api/pom.xml spring-boot:run
```

O Flyway aplica a V10, que adiciona a identidade Google à tabela `usuario`. Não apague
o banco nem altere migrations antigas. Login por senha e dados existentes são preservados.

## Contrato para o frontend

| Endpoint | Finalidade |
| --- | --- |
| `GET /api/autenticacao/google/configuracao` | Público; informa `{enabled, authorizationPath}` sem segredos |
| `GET /oauth2/authorization/google` | Inicia o login no popup |
| `GET /login/oauth2/code/google` | Callback exclusivo para resposta do Google; não chamar manualmente |

Abra o popup **no clique do usuário**. A janela de callback envia para a origem configurada:

```javascript
// Sucesso: o mesmo contrato retornado por /api/autenticacao/login.
{ type: 'nexo:google-auth', ok: true,
  data: { token, usuarioId, nome, email, perfil } }
// Falha: sem tokens, mensagens internas ou credenciais.
{ type: 'nexo:google-auth', ok: false, error: 'GOOGLE_LOGIN_FAILED' }
```

Antes de aceitar o resultado, exija `event.origin === origemDaApi`,
`event.source === janelaQueFoiAberta`, tipo correto e estrutura válida.
Use o JWT local em `Authorization: Bearer ...` nas APIs existentes. Nunca coloque o token
na URL ou nos logs. O helper [google-login.mjs](../examples/google-login.mjs) implementa
esses controles, timeout e cancelamento; adapte sua integração à gestão de sessão do frontend.
Ele não escolhe persistência do JWT pelo aplicativo.

A API não aceita a sessão temporária de OAuth como autenticação. A sessão do handshake
é invalidada ao concluir/falhar. O callback usa `postMessage` com origem exata, não `*`.
O frontend deve manter a referência da janela: não use `noopener` nesse popup. Se aplicar
COOP à aplicação, use `Cross-Origin-Opener-Policy: same-origin-allow-popups` nessa integração;
políticas que eliminem `window.opener` podem impedir a entrega e devem ser testadas no navegador.
Com proxies, preserve o callback público configurado; não derive URLs de cabeçalhos não confiáveis.

## Identidade e contas existentes

- A chave é o `sub` Google validado, não o e-mail ou nome recebido pelo cliente.
- É exigido `email_verified=true`. A biblioteca valida assinatura, emissor, público,
  validade e o contexto OIDC; não se aceita ID token arbitrário enviado pela web.
- Uma conta nova recebe o perfil local padrão de produtor. Claims não escolhem permissões.
- Não há vínculo automático de conta por coincidência de e-mail. Se já houver uma conta
  local com aquele e-mail, o retorno é `ACCOUNT_EMAIL_CONFLICT`: usar o login existente.
  Vinculação autenticada de contas é um fluxo separado, fora desta implementação.
- O erro `GOOGLE_EMAIL_UNVERIFIED` indica que a conta não trouxe e-mail verificado.
  Os demais erros de consentimento/callback usam `GOOGLE_LOGIN_FAILED`.

## Problemas comuns

- **`invalid_id_token` com `The ID Token contains invalid claims: {iat=...}` no log da API:** o relógio da máquina que roda a API está atrasado em relação ao Google, e o token parece emitido "no futuro". A tolerância é de 60 s e não deve ser aumentada. No Docker Desktop do Windows isso acontece depois de suspender ou hibernar o PC. Confira com `docker run --rm alpine date -u` e acerte pela internet com `docker run --rm --privileged alpine ntpd -q -n -p pool.ntp.org` (validado; o `hwclock -s` pode não bastar), ou rode `wsl --shutdown` no PowerShell e reabra o Docker Desktop. Se o atraso voltar, o relógio do próprio Windows está errado (o Docker Desktop copia a hora dele): confira em https://time.is e use Configurações → Hora e idioma → Data e hora → Sincronizar agora (ou `w32tm /resync` como administrador).
- **"O login com Google não foi concluído" na tela:** é a mensagem genérica de propósito. O motivo real fica no log da API, na linha `Login Google falhou no callback: <código> - <descrição>`.
- **`redirect_uri_mismatch` na janela do Google:** a URI cadastrada no cliente OAuth tem de ser idêntica a `GOOGLE_REDIRECT_URI` (protocolo, host, porta e caminho).
- **`access_denied` com o app em modo de teste:** a conta não está em Público-alvo → Usuários de teste.
- **A janela fecha, mas a tela não recebe o resultado:** `GOOGLE_FRONTEND_ORIGIN` não é exatamente a origem do frontend (`localhost` ≠ `127.0.0.1`, e a porta conta).

## Verificação manual real

Sem alterar as telas do projeto, em outro terminal na raiz:

```powershell
node docs/examples/serve-google-login.mjs
```

Abra `http://localhost:5173`. A página de diagnóstico usa o helper, exibe o usuário
autenticado sem mostrar o token e permite conferir uma chamada protegida à API.
Ela serve apenas para validação; não é uma nova tela de produto.

1. Com OAuth desativado, configuração deve indicar `enabled=false` e login local continuar funcional.
2. Com credenciais configuradas, clique no botão, escolha uma conta Google de teste e conclua.
3. Confira criação do usuário no banco e chamada protegida com JWT; segundo login deve usar o mesmo UUID.
4. Cancele o consentimento/feche popup; confira erro compreensível e possibilidade de nova tentativa.
5. Use e-mail já cadastrado localmente: deve rejeitar vínculo automático sem alterar a conta.
6. Sem Bearer token, a API deve rejeitar acesso mesmo que o navegador tenha cookie do handshake.
7. Repita em navegador/ambiente de produção antes de liberar para usuários.

Para produção, configure domínio HTTPS da API e frontend, redirect URI exato no cliente
Google correspondente, cookie Secure e audiência/consentimento adequados. Não reutilize
URLs localhost em produção. Não houve deploy nem criação automática do cliente Google.

Testes automatizados usam identidades/provedor simulados para validar a implementação;
não comprovam a configuração de um cliente Google real. Veja também a
[documentação do fluxo de servidor Google](https://developers.google.com/identity/protocols/oauth2/web-server)
e a [configuração OAuth2 Login no Spring](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html).
