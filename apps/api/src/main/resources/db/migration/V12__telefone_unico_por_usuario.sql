-- O telefone identifica o produtor quando ele escreve no WhatsApp, então não pode se repetir entre contas.
CREATE UNIQUE INDEX uq_usuario_telefone ON usuario (telefone) WHERE telefone IS NOT NULL;
