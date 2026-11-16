# Ingestão com IA

O produtor manda texto, áudio ou foto (pelo WhatsApp ou pelo app). A IA interpreta a mensagem e o
backend registra o resultado direto no talhão citado: uma **atividade** (o que foi feito) ou uma
**ocorrência** (um problema observado). A resposta traz o texto de confirmação que o bot devolve ao
produtor.

```
mensagem ──► transcrição (se áudio) ──► IA interpreta ──► identifica o talhão ──► grava
                                                                                   ├─ evidência (arquivo)
                                                                                   ├─ atividade ou ocorrência
                                                                                   └─ histórico da mensagem
```

O WhatsApp e o app usam o mesmo serviço (`FieldMessageService`), então tudo o que funciona por um
canal funciona pelo outro.

## Endpoints

Todos exigem `Authorization: Bearer TOKEN`.

| Método | Endpoint | O que faz |
| --- | --- | --- |
| POST | `/api/ingestao/mensagens` | Envia uma mensagem (`multipart/form-data`: `text` e/ou `file`) |
| GET | `/api/ingestao/mensagens` | Últimas 50 mensagens do usuário, com o resultado de cada uma |
| POST | `/api/talhoes/{plotId}/ocorrencias` | Registra uma ocorrência manualmente |
| GET | `/api/talhoes/{plotId}/ocorrencias` | Ocorrências do talhão, das mais recentes para as mais antigas |

### `POST /api/ingestao/mensagens`

- `text` (opcional): texto da mensagem ou legenda da foto.
- `file` (opcional): áudio (`audio/*`) ou foto (`image/*`), até 25 MB.
- Pelo menos um dos dois é obrigatório. Outros tipos de arquivo voltam `400`.

Resposta `201`:

```json
{
  "id": "6f1c...",
  "channel": "APP",
  "status": "REGISTRADA",
  "kind": "ATIVIDADE",
  "plotId": "a3e9...",
  "plotName": "Talhão 04 - Milho Safrinha",
  "activityId": "01d2...",
  "occurrenceId": null,
  "evidenceId": "7af0...",
  "text": null,
  "transcript": "Hoje apliquei 2 litros de glifosato no talhão 4, gastei 800 reais.",
  "reply": "✅ Atividade registrada no Talhão 04 - Milho Safrinha: Pulverização de glifosato",
  "interpretation": { "...": "ver contrato abaixo" },
  "createdAt": "2026-11-16T18:09:40Z"
}
```

| `status` | Significado | O que aparece em `reply` |
| --- | --- | --- |
| `REGISTRADA` | Virou atividade ou ocorrência | Confirmação do que foi registrado e, se faltou algo, o que completar |
| `SEM_TALHAO` | Entendida, mas sem talhão identificado. Nada foi gravado além do histórico | Lista dos talhões para o produtor reenviar citando um |
| `IGNORADA` | Saudação, pergunta ou mensagem sem informação de campo | Exemplo de mensagem válida |
| `FALHOU` | O provedor de IA falhou. Nada foi gravado além do histórico | Pedido para tentar de novo |

Regras para identificar o talhão: vale o nome escolhido pela IA; se ela não escolher, o backend
procura o nome ou o número ("talhão 4") no texto; se o produtor tem um único talhão e nenhum foi
citado, usa esse. Se a IA citar um talhão que não existe, nada é registrado.

## Contrato da IA

A IA devolve o JSON abaixo, definido em
[`field-message-interpretation.schema.json`](../../apps/api/src/main/resources/ai/field-message-interpretation.schema.json)
e espelhado no record `Interpretation`. **Mude os dois juntos.** O prompt fica em
[`field-message-interpreter.prompt.md`](../../apps/api/src/main/resources/ai/field-message-interpreter.prompt.md).

```json
{
  "kind": "ATIVIDADE",
  "plotName": "Talhão 04 - Milho Safrinha",
  "summary": "Pulverização de glifosato no Talhão 04",
  "activity": {
    "type": "PULVERIZACAO",
    "date": "2026-11-16",
    "description": "Aplicação de glifosato",
    "inputs": "Glifosato 2 L",
    "cost": 800
  },
  "occurrence": null,
  "missingFields": [],
  "confidence": 0.9
}
```

| Campo | Valores |
| --- | --- |
| `kind` | `ATIVIDADE`, `OCORRENCIA`, `OUTRO` |
| `activity.type` | `PLANTIO`, `ADUBACAO`, `PULVERIZACAO`, `IRRIGACAO`, `CAPINA`, `COLHEITA`, `MONITORAMENTO`, `MANUTENCAO`, `OUTRO` |
| `occurrence.category` | `PRAGA`, `DOENCA`, `PLANTA_DANINHA`, `DEFICIENCIA_NUTRICIONAL`, `CLIMA`, `OUTRO` |
| `occurrence.severity` | `BAIXA`, `MEDIA`, `ALTA` |
| `missingFields` | `plotName`, `date`, `inputs`, `cost`, `description` |

Valores fora dessas listas viram `OUTRO` (ou ficam nulos) em vez de derrubar a mensagem. O tipo da
atividade é gravado como texto em `activity.type`, que continua aceitando texto livre nos cadastros
manuais.

## Configuração

Por padrão `AI_PROVIDER=stub`: a IA é simulada por palavras-chave, sem rede e sem chave, e todo
resumo vem marcado com `[modo de teste]`. Serve para o time desenvolver e testar o fluxo.

Para usar IA de verdade, no `.env`:

```bash
AI_PROVIDER=openai-compatible
AI_CHAT_API_KEY=...            # texto e foto (padrão: Gemini)
AI_TRANSCRIPTION_API_KEY=...   # áudio (padrão: Whisper na Groq)
```

A API conversa no formato da OpenAI (`/chat/completions` e `/audio/transcriptions`), então dá para
trocar de provedor só pelo ambiente:

| Provedor | `*_BASE_URL` | Observação |
| --- | --- | --- |
| Gemini | `https://generativelanguage.googleapis.com/v1beta/openai` | Padrão do chat. Não oferece `/audio/transcriptions` |
| Groq | `https://api.groq.com/openai/v1` | Padrão da transcrição. No chat, alguns modelos não aceitam `json_schema`: use `AI_CHAT_JSON_SCHEMA=false` |
| OpenAI | `https://api.openai.com/v1` | Faz chat e transcrição com a mesma chave |
| Ollama (local) | `http://localhost:11434/v1` | Sem transcrição; qualquer valor serve como chave |

Os nomes de modelo (`AI_CHAT_MODEL`, `AI_TRANSCRIPTION_MODEL`) mudam com frequência; confira a lista
atual no site do provedor.

### Modelos escolhidos (testados em 16/11/2026)

Comparamos modelos do Gemini com o mesmo prompt e schema da API, em texto e foto:

| Modelo | Tempo por resposta | Observação |
| --- | --- | --- |
| `gemini-3.5-flash-lite` | ~2 s | **Padrão.** Acertou texto informal, datas relativas, custo e foto |
| `gemini-flash-lite-latest` | ~2 s | Mesmo resultado, mas é um apelido que muda de versão sem aviso |
| `gemini-3.1-flash-lite` | 8–10 s | Acerta, mas mais lento |
| `gemini-flash-latest` | 17–80 s | Acerta, mas lento demais para responder no WhatsApp |

Na transcrição, `whisper-large-v3-turbo` (Groq) transcreveu corretamente um áudio OGG/Opus, o mesmo
formato do WhatsApp. No fluxo completo, uma mensagem leva de 1 a 3 s.

Os planos gratuitos devolvem `503` em picos de uso. O cliente repete a chamada até 3 vezes em erros
5xx e de rede (`AI_MAX_ATTEMPTS`, `AI_RETRY_BACKOFF`); erros 4xx, inclusive `429` de cota, não são
repetidos. Os limites diários do plano gratuito aparecem no AI Studio, na página de limites do
projeto.

## Limitações conhecidas do MVP

- A chamada à IA é síncrona (alguns segundos). O webhook do WhatsApp vai responder na hora e
  processar em segundo plano.
- Mensagens `SEM_TALHAO` não guardam o arquivo; o produtor precisa reenviar citando o talhão.
- Se a gravação falhar depois de salvar o arquivo da evidência, o arquivo fica órfão no storage.
