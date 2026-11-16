# API do NEXO Campo

## Executar localmente

1. Suba o banco:

   ```bash
   Copy-Item .env.example .env
   docker compose up -d postgres
   ```

2. Execute a API dentro de `apps/api`:

   ```bash
   $env:OPENAPI_ENABLED = "true"
   $env:SPRING_PROFILES_ACTIVE = "dev"
   mvn spring-boot:run
   ```

Antes de iniciar a API, configure `JWT_SECRET` com um valor aleatório de pelo
menos 32 bytes. A aplicação não inicia sem essa variável.
Substitua também os valores de banco no arquivo `.env`.

3. Acesse a documentação interativa:

   `http://localhost:8080/swagger-ui.html` (quando `OPENAPI_ENABLED=true`)

As migrations do Flyway criam as tabelas. Os dados de demonstração só são criados
quando o perfil `dev` está ativo.

## Usuário de demonstração (apenas perfil `dev`)

- E-mail: `demo@nexocampo.local`
- Senha: `password`

## Endpoints principais

| Método | Endpoint | Acesso |
| --- | --- | --- |
| GET | `/api/saude` | Público |
| POST | `/api/autenticacao/cadastro` | Público |
| POST | `/api/autenticacao/login` | Público |
| POST | `/api/fazendas` | JWT |
| GET | `/api/fazendas` | JWT |
| POST | `/api/fazendas/{fazendaId}/talhoes` | JWT |
| GET | `/api/fazendas/{fazendaId}/talhoes` | JWT |
| POST | `/api/talhoes/{talhaoId}/atividades` | JWT |
| GET | `/api/talhoes/{talhaoId}/atividades` | JWT |
| POST | `/api/talhoes/{talhaoId}/evidencias` | JWT |
| GET | `/api/talhoes/{talhaoId}/evidencias` | JWT |
| POST | `/api/webhooks` | Público com assinatura |

O campo `limiteWkt` dos talhões deve ser um `POLYGON` válido no SRID 4326.

Uploads usam `multipart/form-data`, com os campos `tipo` (`FOTO`, `VIDEO`, `AUDIO` ou
`DOCUMENTO`) e `arquivo`. O limite configurado é de 2 GB por arquivo e a API valida a
assinatura do conteúdo. São aceitos JPEG, PNG, WebP, MP4, MP3, WAV, OGG, M4A e PDF.
O armazenamento local usa `STORAGE_ROOT`; em produção, essa implementação deve ser
substituída pelo adaptador do Supabase Storage.

## Webhook

O endpoint `POST /api/webhooks` recebe o payload bruto e exige o header:

```http
X-Hub-Signature-256: sha256=HASH_HEXADECIMAL
```

A assinatura é um HMAC-SHA256 do corpo bruto usando `WEBHOOK_SECRET`. Eventos
válidos são armazenados para processamento posterior.
