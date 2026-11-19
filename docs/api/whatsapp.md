# WhatsApp

O produtor manda texto, áudio ou foto para o número do NEXO Campo no WhatsApp. A mensagem passa pelo
mesmo fluxo da [ingestão com IA](ingestao-ia.md) e o bot responde no mesmo chat, citando a mensagem
original, com o resumo do que foi registrado.

```
Meta ──webhook──► /api/webhooks/whatsapp ──► valida assinatura ──► 200 na hora
                                                   │
                                                   └─► fila (4 threads) ──► telefone → usuário
                                                                            ├─ baixa foto/áudio (Graph API)
                                                                            ├─ FieldMessageService (IA + gravação)
                                                                            └─ responde no WhatsApp (Graph API)
```

Usamos a **WhatsApp Cloud API oficial** da Meta. Bibliotecas não oficiais (Baileys, Evolution API)
violam os termos do WhatsApp e o número pode ser banido.

## Endpoints

| Método | Endpoint | Acesso | O que faz |
| --- | --- | --- | --- |
| GET | `/api/webhooks/whatsapp` | Público | Confirmação da URL pela Meta (`hub.challenge`) |
| POST | `/api/webhooks/whatsapp` | Assinatura da Meta | Recebe as mensagens |
| PUT | `/api/ingestao/whatsapp/vinculo` | JWT | Cadastra ou troca o WhatsApp do usuário logado |
| GET | `/api/ingestao/whatsapp/vinculo` | JWT | Número cadastrado (`204` se não houver) |
| DELETE | `/api/ingestao/whatsapp/vinculo` | JWT | Remove o número |

### Vincular o número (tela do app)

Só mensagens de números vinculados viram registros. Os outros recebem um pedido para cadastrar o
número no app.

```http
PUT /api/ingestao/whatsapp/vinculo
Authorization: Bearer TOKEN
Content-Type: application/json

{ "telefone": "(11) 91234-5678" }
```

Resposta `200`:

```json
{ "telefone": "5511912345678", "criadoEm": "2026-11-18T18:40:00Z" }
```

O número aceita qualquer formatação, com DDD; sem código do país, assume Brasil. Um número só pode
estar ligado a uma conta (`400` se já estiver em outra). O WhatsApp identifica alguns celulares
brasileiros sem o nono dígito; a busca considera as duas formas.

## Como o webhook se comporta

- Valida o cabeçalho `X-Hub-Signature-256` (HMAC-SHA256 do corpo com `WHATSAPP_APP_SECRET`). Sem
  assinatura válida, responde `401` e não processa nada.
- Responde `200` imediatamente e processa em segundo plano. A Meta reenvia webhooks que demoram.
- Reenvios da mesma mensagem (mesmo `wamid`) são ignorados, sem responder de novo.
- Texto, foto (com ou sem legenda) e áudio são processados. Outros tipos (figurinha, vídeo,
  documento, localização) recebem uma resposta explicando os formatos aceitos.
- Marca a mensagem como lida enquanto a IA processa.
- A Meta aceita o envio da resposta e só avisa depois, pelo próprio webhook, se não conseguiu
  entregar. Essas falhas vão para o log da API com o código da Meta, por exemplo
  `A Meta não entregou a mensagem wamid... do WhatsApp: 131031 Business Account locked`.

## Configuração

No `.env` (veja o `.env.example`):

| Variável | Onde pegar |
| --- | --- |
| `WHATSAPP_ACCESS_TOKEN` | WhatsApp → Configuração da API → token de acesso. O temporário vence em 24 h; veja abaixo como gerar um permanente |
| `WHATSAPP_PHONE_NUMBER_ID` | WhatsApp → Configuração da API → "Identificação do número de telefone" (ID numérico, não o telefone) |
| `WHATSAPP_APP_SECRET` | Configurações do app → Básico → Chave secreta do app |
| `WHATSAPP_VERIFY_TOKEN` | Texto aleatório escolhido por nós; o mesmo valor vai no painel da Meta |
| `WHATSAPP_GRAPH_BASE_URL` | Opcional. Padrão `https://graph.facebook.com/v23.0` |

Sem essas variáveis a API sobe normalmente: o webhook recusa as chamadas e nada é enviado.

## Passo a passo na Meta

1. Em [developers.facebook.com](https://developers.facebook.com), **Meus apps → Criar app**, caso de
   uso do WhatsApp. Se pedir, crie um portfólio empresarial.
2. **WhatsApp → Configuração da API**: a Meta fornece um número de teste. Em **Para**, adicione até 5
   números que podem conversar com o bot (cada um confirma um código no WhatsApp).
3. Copie o token, o ID do número e a chave secreta do app para o `.env`.
4. Suba a API com uma URL pública (deploy ou túnel, abaixo).
5. **WhatsApp → Configuração → Webhook**: URL de retorno `https://SEU-ENDERECO/api/webhooks/whatsapp`
   e o `WHATSAPP_VERIFY_TOKEN` no campo "Verificar token". Depois, em **Campos do webhook**, assine
   `messages`.
6. Vincule seu número pelo app (ou pelo `PUT /api/ingestao/whatsapp/vinculo`) e mande uma mensagem
   para o número de teste.

### Token permanente (para a apresentação)

O token da tela de configuração vence em 24 h. Para um que não vence: em
[business.facebook.com](https://business.facebook.com), **Configurações do negócio → Usuários →
Usuários do sistema**, crie um usuário do sistema (administrador), atribua a ele o app e a conta do
WhatsApp e clique em **Gerar token** com as permissões `whatsapp_business_messaging` e
`whatsapp_business_management`, validade "Nunca".

### Testar no computador (túnel)

A Meta precisa de uma URL pública com HTTPS. Para testar com a API rodando local, um túnel temporário
do Cloudflare funciona sem conta:

```bash
cloudflared tunnel --url http://localhost:8080
```

Use a URL `https://....trycloudflare.com` que aparecer no passo 5. Ela muda a cada execução.

## Se o bot não responder

1. Procure `A Meta não entregou` no log da API. O código explica o motivo
   ([lista de códigos](https://developers.facebook.com/docs/whatsapp/cloud-api/support/error-codes/)).
2. Consulte a saúde da conta (troque os IDs e use o token do `.env`):

   ```bash
   curl -H "Authorization: Bearer $WHATSAPP_ACCESS_TOKEN" \
     "https://graph.facebook.com/v23.0/ID_DA_CONTA_WHATSAPP?fields=health_status"
   ```

   `can_send_message: BLOCKED` vem com a causa e a solução em `errors[].possible_solution`.
3. Sem o webhook configurado, o painel da Meta mostra os mesmos avisos em **Casos de uso → WhatsApp →
   Etapa 1. Experimente → Verifique webhooks de teste**.

Causas que já encontramos:

| Código | Significado | O que fazer |
| --- | --- | --- |
| `131031` Business Account locked | A Meta travou a conta (evento `BUSINESS_VERIFICATION_NEEDED`) | Ver o `health_status` |
| `131000` Business profile incomplete | Portfólio empresarial sem nome legal, país ou site | Preencher em Configurações do negócio → Informações da empresa |
| `141010` | Portfólio sem verificação da empresa | Etapa 3. Verificação da empresa (pede documentos) |
| `141006` | Sem forma de pagamento válida | Bloqueia só mensagens iniciadas pela empresa (modelos como o Hello World); respostas a quem escreveu primeiro não usam |
| `131030` | Destinatário fora da lista do número de teste | Adicionar o número no campo **Para** |

## Limitações conhecidas do MVP

- Com o número de teste, só os até 5 números cadastrados no painel conseguem conversar com o bot.
- Foto sem talhão identificado não fica guardada: o produtor reenvia citando o talhão.
- A fila de processamento fica em memória. Mensagens na fila se perdem se a API reiniciar no meio.
