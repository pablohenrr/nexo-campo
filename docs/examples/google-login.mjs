/**
 * Integração de referência para o frontend de Pablo.
 * Crie uma instância por tela e chame login() diretamente no clique (sem await antes).
 * Não usa storage, URL ou console para transportar o JWT.
 * A página hospedeira deve preservar opener; se usar COOP, use same-origin-allow-popups.
 */
export function createGoogleLogin(apiOrigin, { browser = globalThis.window, timeoutMs = 180_000 } = {}) {
  const url = new URL(apiOrigin);
  const local = ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname);
  if (url.origin !== apiOrigin || url.username || url.password ||
      !(url.protocol === 'https:' || (url.protocol === 'http:' && local))) {
    throw new Error('Informe somente a origem HTTPS da API; HTTP é permitido apenas no localhost.');
  }
  if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) throw new Error('Timeout inválido.');
  let active = null;

  function login() {
    if (active) return Promise.reject(new Error('Já existe um login com Google em andamento.'));
    // Abrir sincronamente mantém o gesto do usuário e permite detectar bloqueadores.
    const popup = browser.open('about:blank', '_blank', 'popup,width=520,height=720');
    if (!popup) return Promise.reject(new Error('O navegador bloqueou a janela. Permita pop-ups para entrar.'));
    return new Promise((resolve, reject) => {
      let interval;
      let timeout;
      let finished = false;
      function finish(error, data) {
        if (finished) return;
        finished = true;
        browser.removeEventListener('message', onMessage);
        browser.clearInterval(interval);
        browser.clearTimeout(timeout);
        active = null;
        try { popup.close(); } catch { /* Janela já fechada ou isolada pelo navegador. */ }
        if (error) reject(error); else resolve(data);
      }
      function onMessage(event) {
        if (event.origin !== apiOrigin || event.source !== popup) return;
        const message = event.data;
        if (!message || message.type !== 'nexo:google-auth') return;
        if (message.ok === false) {
          const descriptions = {
            ACCOUNT_EMAIL_CONFLICT: 'Já existe uma conta com este e-mail. Entre com sua senha; as contas não são vinculadas automaticamente.',
            GOOGLE_EMAIL_UNVERIFIED: 'O Google não confirmou seu e-mail. Use uma conta com e-mail verificado.',
            GOOGLE_LOGIN_FAILED: 'O login com Google não foi concluído. Tente novamente.'
          };
          finish(new Error(Object.hasOwn(descriptions, message.error) ? descriptions[message.error] : descriptions.GOOGLE_LOGIN_FAILED));
          return;
        }
        if (message.ok !== true || !validSession(message.data)) {
          finish(new Error('Resposta de autenticação inválida. Tente novamente.'));
          return;
        }
        const { token, usuarioId, nome, email, perfil } = message.data;
        finish(null, { token, usuarioId, nome, email, perfil });
      }
      active = { cancel: () => finish(new Error('Login com Google cancelado.')) };
      browser.addEventListener('message', onMessage);
      interval = browser.setInterval(() => {
        if (popup.closed) finish(new Error('A janela de login foi fechada.'));
      }, 500);
      timeout = browser.setTimeout(() => finish(new Error('O login expirou. Abra novamente a janela do Google.')), timeoutMs);
      try { popup.location.replace(`${apiOrigin}/oauth2/authorization/google`); }
      catch { finish(new Error('Não foi possível abrir o login com Google.')); }
    });
  }

  return { login, cancel: () => active?.cancel(), get pending() { return active !== null; } };
}

function validSession(data) {
  if (!data || typeof data !== 'object') return false;
  return typeof data.token === 'string' && data.token.length > 0 && data.token.length <= 16_384 &&
    typeof data.usuarioId === 'string' && /^[\da-f]{8}-(?:[\da-f]{4}-){3}[\da-f]{12}$/i.test(data.usuarioId) &&
    ['nome', 'email', 'perfil'].every(key => typeof data[key] === 'string' && data[key].trim().length > 0);
}
