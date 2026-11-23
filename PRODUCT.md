# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Donos de fazenda, donos de sítio e produtores rurais. Agrônomos também podem usar o produto. Os papéis no código são `PRODUTOR` (padrão) e `ADMIN`. Agrônomo entra como público, sem papel próprio. O que `ADMIN` pode fazer a mais ficou em aberto.

O trabalho do dono é registrar fazendas, atividades e ocorrências de forma natural, e acompanhar a operação inteira no sistema. No campo, quem comunica com o sistema é o assistente de WhatsApp: por ele saem foto, áudio e o registro no banco. Dentro do produto também deve existir um jeito de falar com o mesmo assistente. Se isso é uma conversa ou um botão ainda está em aberto.

Papéis separados para dono, quem está no campo e agrônomo estão em aberto.

## Product Purpose

NEXO Campo é a plataforma que centraliza a operação da fazenda. O dono registra o que aconteceu e acompanha a fazenda num só lugar.

A primeira página é a de apresentação do produto, para vender aos proprietários de fazenda. Dela saem os botões de registrar e de entrar. Cada um leva à página correspondente, e dali o dono entra no sistema.

O MVP precisa funcionar de verdade na apresentação do hackathon, sem custo. A interface atual foi recusada: o frontend precisa de uma refatoração completa. Antes de afirmar capacidade nova, validar o que o backend, os mapas e a IA já fazem.

O que a plataforma reúne além de fazenda, talhão, atividade, ocorrência, evidência, mensagem e mapa está em aberto.

## Positioning

A plataforma inteira é o produto. O assistente de WhatsApp é o comunicador com o sistema todo. O que ele registra aparece na operação, e a mesma operação se acompanha no sistema.

Mecanismo já decidido e implementado: a mensagem é interpretada e gravada direto no talhão identificado, sem tela de confirmação. Se o talhão não for identificado com segurança, nada é gravado e o assistente pede o talhão.

## Operating Context

Quatro sócios desenvolvem o produto. Qualquer custo é decidido com eles: perguntar antes de gastar.

O uso acontece no campo, pelo WhatsApp, e na leitura da operação no sistema web. A apresentação é um hackathon. O MVP não tem custo agora, e a forma precisa poder escalar.

A ordem das telas é: apresentação, registro ou entrada, sistema. O mapa é a peça principal da página de dentro.

O login com Google já está na API, conduzido pelo backend: o frontend abre o popup e consome o resultado, sem SDK do Google e sem segredo no cliente. O fluxo fica desligado até existirem credenciais. Documentado em `docs/api/google-oauth.md`. No frontend, o que entra é o botão com a logo oficial do Google.

## Capabilities and Constraints

O que já existe e deve ser preservado:

- Fazenda, talhão (polígono WKT, SRID 4326), atividade, ocorrência, evidência e histórico de mensagem.
- Atividades: plantio, adubação, pulverização, irrigação, capina, colheita, monitoramento, manutenção, ou outro.
- Ocorrências: praga, doença, planta daninha, deficiência nutricional, clima, ou outro; severidade baixa, média ou alta.
- Evidência: foto, vídeo, áudio ou documento. A API aceita JPEG, PNG, WebP, MP4, MP3, WAV, OGG, M4A e PDF.
- Ingestão de texto, áudio e foto pelo app e, na decisão atual, pelo WhatsApp Cloud API oficial. A IA fica no monólito Java. Gemini para texto e foto, Whisper da Groq para áudio, com adaptador stub sem rede para desenvolver sem chave. ADR 0001, status proposto; detalhe em `docs/api/ingestao-ia.md`.
- Mapa atual em MapLibre, com os modos mapa, satélite e maquete (terreno MapTiler), em `apps/web/src/features/maps/`.
- API em Spring Boot (`apps/api`) e web em React 19, TypeScript, Vite e Tailwind (`apps/web`). Idioma da interface: português.

Intenção confirmada e ainda não construída:

- Página de registro no frontend. O cadastro por API existe (`POST /api/autenticacao/cadastro`); a tela não.
- Botão de Google com a logo oficial. O frontend hoje só entra com e-mail e senha.
- Assistente dentro do próprio sistema, além do WhatsApp.
- A tela principal foi pensada com mapas em 2D, 3D, satélite, relevo 3D e estilo maquete, usando three.js e motion. three.js não está no projeto. Validar o mapa que existe antes de trocar o motor.

Amarras do MVP: sem custo e sem infra extra para o hackathon. Plano gratuito do Gemini pode usar o conteúdo enviado para melhorar os produtos do Google; isso não serve para dados reais de produtor. Passar a dados reais, ou gastar qualquer coisa, é decisão dos quatro sócios.

Rotas ainda sem tela própria: atividades, alertas, evidências, mensagens e perfil.

## Brand Commitments

O nome é NEXO Campo. Na interface o wordmark atual é NEXO.

A interface é em português. O botão de Google usa a logo oficial do Google, sem redesenho.

A interface atual foi recusada. A refatoração é completa, e o resultado precisa ser um produto que os sócios apresentem como inovador. A tela principal carrega o mapa nos modos acima. Paleta, tipo e sistema visual ficam para o trabalho de design.

## Evidence on Hand

- API e domínio: `docs/api/README.md`, `docs/api/ingestao-ia.md`, `docs/adr/0001-ia-e-ingestao.md`.
- Google OAuth no backend: `docs/api/google-oauth.md`, `apps/api/src/main/java/com/nexocampo/modules/identity/GoogleLoginResultHandler.java`.
- Web atual: login por senha em `apps/web/src/features/auth/components/login-page.tsx`; painel e mapa em `apps/web/src/features/dashboard/` e `apps/web/src/features/maps/`.
- Conta de demonstração, só com o perfil `dev`: `demo@nexocampo.local` / `password`.

Não há fazenda de cliente, depoimento, caso, preço, licença nem imprensa no repositório. Trabalho futuro não inventa isso.

## Product Principles

1. Uma operação, várias portas. WhatsApp, o sistema e o assistente interno leem e gravam o mesmo registro da fazenda.
2. O dono registra como já fala: mensagem, áudio, foto. O sistema estrutura.
3. A tela principal mostra a operação da fazenda. O mapa é o lugar dela.
4. Só se afirma o que o backend, o mapa e a IA já fazem, até isso ser validado.
5. O MVP não gasta. A forma escala. Dinheiro é decisão dos quatro sócios.

## Accessibility & Inclusion

O uso é no campo, no celular, muitas vezes ao ar livre. A interface precisa ser legível nessa condição: alto contraste, tipografia forte e alvos de toque generosos.
