import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { pathToFileURL } from 'node:url';

/** Servidor apenas do exemplo OAuth; não serve o repositório nem arquivos de configuração. */
export function createDemoServer() {
  const routes = new Map([
    ['/', ['google-login.html', 'text/html; charset=utf-8']],
    ['/google-login.html', ['google-login.html', 'text/html; charset=utf-8']],
    ['/google-login.mjs', ['google-login.mjs', 'text/javascript; charset=utf-8']]
  ]);
  return createServer(async (request, response) => {
    response.setHeader('Cache-Control', 'no-store');
    response.setHeader('X-Content-Type-Options', 'nosniff');
    response.setHeader('Referrer-Policy', 'no-referrer');
    response.setHeader('Cross-Origin-Opener-Policy', 'same-origin-allow-popups');
    if (request.method !== 'GET') {
      response.writeHead(405, { Allow:'GET' });response.end('Método não permitido.');return;
    }
    const route = routes.get((request.url || '/').split('?')[0]);
    if (!route) { response.writeHead(404);response.end('Não encontrado.');return; }
    try {
      const content = await readFile(new URL(route[0], import.meta.url));
      const inline = route[0].endsWith('.html') ? content.toString('utf8').match(/<script type="module">([\s\S]*?)<\/script>/)?.[1] : null;
      const scriptHash = inline ? ` 'sha256-${createHash('sha256').update(inline).digest('base64')}'` : '';
      response.setHeader('Content-Security-Policy', `default-src 'none'; script-src 'self'${scriptHash}; style-src 'unsafe-inline'; connect-src http://localhost:8080; base-uri 'none'; object-src 'none'; frame-ancestors 'none'; form-action 'none'`);
      response.writeHead(200, { 'Content-Type':route[1] });response.end(content);
    } catch { response.writeHead(500);response.end('Falha ao ler o exemplo.'); }
  });
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  const port = Number(process.env.PORT || 5173);
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('PORT inválida.');
  const server = createDemoServer();
  server.on('error', error => { process.stderr.write(`Não foi possível iniciar o exemplo: ${error.code || 'erro'}\n`);process.exitCode=1; });
  server.listen(port, 'localhost', () => {
    process.stdout.write(`Exemplo OAuth: http://localhost:${port}/google-login.html\n`);
    process.stdout.write('Configure GOOGLE_FRONTEND_ORIGIN na API com essa origem exata. Ctrl+C encerra.\n');
  });
}
