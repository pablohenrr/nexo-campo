# ADR 0001 — IA e ingestão de mensagens de campo

- Status: proposto
- Data: 2026-11-16
- Área: IA e ingestão

## Contexto

O produtor vai mandar tudo pelo WhatsApp (texto, áudio e foto). A IA precisa transformar essas
mensagens em atividades e ocorrências no talhão certo, e a resposta tem que voltar ao produtor em
segundos. O projeto é um MVP de hackathon: precisa funcionar de verdade, mas sem custo e sem infra
extra.

## Decisões

1. **A IA fica dentro do monólito Java, nos módulos `ingestion` e `investigation`.** Não vamos
   treinar modelo, só chamar APIs. Um serviço Python separado traria mais um deploy, outra
   autenticação e outra conexão com o banco sem ganho para o MVP. Python fica em `ai/`, só para
   experimentos.

2. **Cada uso de IA é uma porta (interface) no módulo, com dois adaptadores:** o real e um *stub*
   sem rede. O stub é o padrão: o time desenvolve e testa sem chave, e os testes não dependem de
   internet. Exemplos: `FieldMessageInterpreter` e `AudioTranscriber`.

3. **Um cliente HTTP próprio e enxuto para o formato da OpenAI (`OpenAiCompatibleClient`).** Esse
   formato é aceito por Gemini, Groq, OpenAI, OpenRouter e Ollama, então trocar de provedor é só
   mudar URL, chave e modelo no `.env`. Ele usa o `RestClient` do Spring, sem dependência nova.

4. **Gemini para texto e foto, Whisper da Groq para áudio**, ambos com plano gratuito. O Gemini não
   oferece o endpoint de transcrição no formato da OpenAI, por isso a transcrição tem configuração
   própria. No chat usamos a versão fixa `gemini-3.5-flash-lite`: nos testes respondeu em ~2 s com o
   mesmo acerto do Flash comum, que levou de 17 a 80 s. Os números estão em
   `docs/api/ingestao-ia.md`.

5. **Resposta estruturada por JSON Schema.** O contrato fica em um arquivo versionado
   (`field-message-interpretation.schema.json`), espelhado no record `Interpretation`. Valores
   desconhecidos viram `OUTRO` em vez de derrubar a mensagem.

6. **A IA grava direto, sem tela de confirmação.** O produtor recebe um resumo do que foi registrado
   e corrige pelo app se precisar. Se o talhão não for identificado com segurança, nada é gravado e o
   bot pede o talhão.

## Alternativas consideradas

- **Spring AI:** é o padrão do ecossistema Spring e traria abstração de provedores e saída
  estruturada. Ficou de fora por agora: a autoconfiguração exige a chave já na inicialização (o que
  quebra o modo sem chave do time), e precisaríamos validar a compatibilidade de versão com o Spring
  Boot do projeto. Como tudo passa pelas portas, dá para trocar o adaptador depois sem mexer no resto.
- **SDK oficial da OpenAI:** prende o código a um único provedor.
- **Whisper local:** grátis e sem limite, mas lento sem GPU e pesado para o deploy. Fica como plano B.
- **Bibliotecas não oficiais de WhatsApp (Evolution API, Baileys):** violam os termos do WhatsApp e o
  número pode ser banido. Vamos usar a WhatsApp Cloud API oficial.

## Consequências

- Planos gratuitos mudam limites sem aviso. Para a apresentação, ter uma segunda chave configurada
  ou algum crédito pago.
- No plano gratuito do Gemini, o Google pode usar o conteúdo enviado para melhorar os produtos dele.
  Aceitável para o MVP; para dados reais de produtores, usar plano pago.
- A chamada à IA leva segundos. O webhook do WhatsApp precisa responder na hora e processar em
  segundo plano.
