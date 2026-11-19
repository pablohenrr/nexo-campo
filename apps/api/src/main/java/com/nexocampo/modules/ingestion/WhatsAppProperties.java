package com.nexocampo.modules.ingestion;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da WhatsApp Cloud API (Meta). Sem esses valores a API sobe normalmente: o webhook
 * recusa as chamadas e nenhuma resposta é enviada.
 *
 * @param graphBaseUrl  URL da Graph API já com a versão (ex.: {@code https://graph.facebook.com/v23.0})
 * @param accessToken   token do app (temporário de 24 h no teste, permanente via usuário do sistema)
 * @param phoneNumberId ID do número que envia as respostas (não é o telefone)
 * @param appSecret     chave secreta do app, usada para validar a assinatura dos webhooks
 * @param verifyToken   valor combinado com a Meta para confirmar a URL do webhook
 */
@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsAppProperties(
        String graphBaseUrl,
        String accessToken,
        String phoneNumberId,
        String appSecret,
        String verifyToken,
        Duration timeout) {
}
