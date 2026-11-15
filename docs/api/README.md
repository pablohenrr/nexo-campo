# API do NEXO Campo

## Executar localmente

1. Suba o banco:

   ```bash
   docker compose up -d postgres
   ```

2. Execute a API dentro de `apps/api`:

   ```bash
   mvn spring-boot:run
   ```

3. Acesse a documentação interativa:

   `http://localhost:8080/swagger-ui.html`

As migrations do Flyway criam as tabelas e os dados de demonstração automaticamente.

## Usuário de demonstração

- E-mail: `demo@nexocampo.local`
- Senha: `password`

## Endpoints principais

| Método | Endpoint | Acesso |
| --- | --- | --- |
| GET | `/api/health` | Público |
| POST | `/api/auth/cadastro` | Público |
| POST | `/api/auth/login` | Público |
| POST | `/api/fazendas` | JWT |
| GET | `/api/fazendas` | JWT |
| POST | `/api/fazendas/{farmId}/talhoes` | JWT |
| GET | `/api/fazendas/{farmId}/talhoes` | JWT |
| POST | `/api/talhoes/{plotId}/atividades` | JWT |
| GET | `/api/talhoes/{plotId}/atividades` | JWT |
| POST | `/api/talhoes/{plotId}/evidencias` | JWT |
| GET | `/api/talhoes/{plotId}/evidencias` | JWT |

O campo `boundaryWkt` dos talhões deve ser um `POLYGON` válido no SRID 4326.

Uploads usam `multipart/form-data`, com os campos `type` (`FOTO`, `VIDEO`, `AUDIO` ou
`DOCUMENTO`) e `file`. O limite configurado é de 2 GB por arquivo. O armazenamento
local usa `STORAGE_ROOT`; em produção, essa implementação deve ser substituída pelo
adaptador do Supabase Storage.
