# API do NEXO Campo

Login social opcional: [configurar Google OAuth/OIDC e integrar ao frontend](google-oauth.md).

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
| GET | `/api/usuarios/me/contato` | JWT |
| PUT | `/api/usuarios/me/contato` | JWT |
| POST | `/api/fazendas` | JWT |
| GET | `/api/fazendas` | JWT |
| PUT | `/api/fazendas/{fazendaId}` | JWT |
| POST | `/api/fazendas/{fazendaId}/talhoes` | JWT |
| GET | `/api/fazendas/{fazendaId}/talhoes` | JWT |
| POST | `/api/talhoes/{talhaoId}/atividades` | JWT |
| GET | `/api/talhoes/{talhaoId}/atividades` | JWT |
| POST | `/api/talhoes/{talhaoId}/evidencias` | JWT |
| GET | `/api/talhoes/{talhaoId}/evidencias` | JWT |
| POST | `/api/talhoes/{talhaoId}/ocorrencias` | JWT |
| GET | `/api/talhoes/{talhaoId}/ocorrencias` | JWT |
| POST | `/api/ingestao/mensagens` | JWT |
| GET | `/api/ingestao/mensagens` | JWT |
| POST | `/api/webhooks/**` | Público; implementação no módulo de ingestão |

A ingestão com IA (mensagens de texto, áudio e foto que viram atividades e ocorrências) está
descrita em [ingestao-ia.md](ingestao-ia.md).

### Cadastro e edição da fazenda

`POST /api/fazendas` e `PUT /api/fazendas/{fazendaId}` recebem o contorno da propriedade
inteira, marcado com pinos no mapa:

```json
{
  "nome": "Fazenda Boa Vista",
  "descricao": null,
  "municipio": "Londrina",
  "estado": "PR",
  "contorno": [
    { "latitude": -23.300, "longitude": -51.170 },
    { "latitude": -23.300, "longitude": -51.160 },
    { "latitude": -23.310, "longitude": -51.160 },
    { "latitude": -23.310, "longitude": -51.170 }
  ]
}
```

- `contorno` é obrigatório, com 3 a 500 pinos na ordem em que formam o contorno. Não repita o
  primeiro pino no fim; o backend fecha o polígono. Cliques repetidos no mesmo ponto são ignorados.
- Linhas que se cruzam, pinos todos alinhados ou coordenadas fora dos limites resultam em 400,
  sem gravar nada.
- `latitude`, `longitude` e `areaTotalHectares` não são enviados: o backend calcula. O centro
  (centroide do contorno) é gravado em `fazenda.latitude`/`fazenda.longitude`, que é o ponto
  usado pelos alertas climáticos. O endereço pesquisado no mapa não é enviado nem usado como centro.
- A resposta traz os campos da fazenda, o centro calculado, a área e o `contorno` na mesma ordem.
  Fazendas antigas, sem contorno, vêm com `contorno: null` e mantêm latitude/longitude.
- `PUT` de fazenda inexistente ou de outra conta responde 400 `Fazenda não encontrada`, como no
  cadastro de talhões.

O campo `limiteWkt` dos talhões deve ser um `POLYGON` válido no SRID 4326.

O cadastro aceita `telefone` opcional e `notificacoesSmsAtivas` (padrão `false`).
Números válidos são normalizados para E.164 com região padrão Brasil (`+55`).
Para ativar SMS, informe um número móvel válido. O usuário pode consultar e atualizar
seu telefone e a preferência usando `GET` e `PUT /api/usuarios/me/contato`.

Uploads usam `multipart/form-data`, com os campos `tipo` (`FOTO`, `VIDEO`, `AUDIO` ou
`DOCUMENTO`) e `arquivo`. O limite configurado é de 2 GB por arquivo e a API valida a
assinatura do conteúdo. São aceitos JPEG, PNG, WebP, MP4, MP3, WAV, OGG, M4A e PDF.
O armazenamento local usa `STORAGE_ROOT`; em produção, essa implementação deve ser
substituída pelo adaptador do Supabase Storage.

O módulo de ingestão implementará o webhook oficial do WhatsApp Cloud API em
`GET` e `POST /api/webhooks/whatsapp`. A rota já está liberada no Spring Security
para que a assinatura HMAC-SHA256 seja validada sobre o corpo cru no controller.
