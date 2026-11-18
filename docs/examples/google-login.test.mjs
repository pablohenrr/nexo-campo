import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createGoogleLogin } from './google-login.mjs';
import { createDemoServer } from './serve-google-login.mjs';

const origin = 'http://localhost:8080';
const session = { token:'test-only-token', usuarioId:'00000000-0000-0000-0000-000000000001', nome:'Pessoa', email:'pessoa@example.test', perfil:'PRODUTOR' };
function fixture() {
  const listeners = new Set();
  const intervals = new Map();
  const timeouts = new Map();
  let nextId = 0;
  const popup = { closed:false, close() { this.closed=true; }, location:{ replace(url) { popup.url=url; } } };
  const browser = {
    open() { return popup; },
    addEventListener(_, fn) { listeners.add(fn); }, removeEventListener(_, fn) { listeners.delete(fn); },
    setInterval(fn) { const id=++nextId; intervals.set(id,fn); return id; }, clearInterval(id) { intervals.delete(id); },
    setTimeout(fn) { const id=++nextId; timeouts.set(id,fn); return id; }, clearTimeout(id) { timeouts.delete(id); }
  };
  const google = createGoogleLogin(origin, {browser});
  return { browser,popup,google,listeners,intervals,timeouts,
    send(data, overrides={}) { for (const fn of [...listeners]) fn({ origin,source:popup,data,...overrides }); }
  };
}
const success = {type:'nexo:google-auth',ok:true,data:session};
function cleaned(f) { assert.equal(f.listeners.size,0);assert.equal(f.intervals.size,0);assert.equal(f.timeouts.size,0);assert.equal(f.google.pending,false); }

test('abre no gesto e aceita somente origem e janela esperadas', async () => {
  const f=fixture();const promise=f.google.login();
  assert.equal(f.popup.url, `${origin}/oauth2/authorization/google`);
  f.send(success,{origin:'https://evil.example'});assert.equal(f.google.pending,true);
  f.send(success,{source:{}});assert.equal(f.google.pending,true);
  f.send(success);assert.deepEqual(await promise,session);cleaned(f);
});
test('payload inválido não cria sessão', async () => {
  const f=fixture();const promise=f.google.login();f.send({...success,data:{...session,usuarioId:'invalid'}});
  await assert.rejects(promise,/inválida/);cleaned(f);
});
test('mensagem de outro tipo é ignorada', async () => {
  const f=fixture();const promise=f.google.login();f.send({...success,type:'other'});assert.equal(f.google.pending,true);
  f.send(success);await promise;cleaned(f);
});
test('colisão de e-mail exige login local', async () => {
  const f=fixture();const promise=f.google.login();f.send({type:'nexo:google-auth',ok:false,error:'ACCOUNT_EMAIL_CONFLICT'});
  await assert.rejects(promise,/senha/);cleaned(f);
});
test('cancelamento limpa listeners e timers', async () => {
  const f=fixture();const promise=f.google.login();f.google.cancel();await assert.rejects(promise,/cancelado/);cleaned(f);
});
test('fechar janela cancela', async () => {
  const f=fixture();const promise=f.google.login();f.popup.closed=true;for(const fn of [...f.intervals.values()])fn();
  await assert.rejects(promise,/fechada/);cleaned(f);
});
test('timeout cancela e permite nova tentativa', async () => {
  const f=fixture();const promise=f.google.login();for(const fn of [...f.timeouts.values()])fn();
  await assert.rejects(promise,/expirou/);cleaned(f);
  f.popup.closed=false;const second=f.google.login();f.send(success);await second;cleaned(f);
});
test('popup bloqueado falha sem pendência', async () => {
  const f=fixture();f.browser.open=()=>null;await assert.rejects(f.google.login(),/bloqueou/);cleaned(f);
});
test('tentativa concorrente não altera a primeira', async () => {
  const f=fixture();const first=f.google.login();await assert.rejects(f.google.login(),/andamento/);
  f.send(success);await first;cleaned(f);
});
test('origens com caminho, wildcard ou HTTP remoto são rejeitadas', () => {
  for(const bad of ['http://localhost:8080/path','https://example.test/','http://example.test','*','https://user:pass@example.test']) {
    assert.throws(()=>createGoogleLogin(bad));
  }
});

test('servidor local expõe somente o exemplo com headers de segurança', async () => {
  const server = createDemoServer();
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    const page=await fetch(`${base}/`);
    assert.equal(page.status,200);
    assert.equal(page.headers.get('cache-control'),'no-store');
    assert.equal(page.headers.get('cross-origin-opener-policy'),'same-origin-allow-popups');
    assert.match(page.headers.get('content-security-policy'),/script-src 'self' 'sha256-/);
    assert.match(await page.text(),/createGoogleLogin/);
    const module=await fetch(`${base}/google-login.mjs`);assert.equal(module.status,200);
    assert.equal((await fetch(`${base}/.env`)).status,404);
    assert.equal((await fetch(`${base}/google-login.test.mjs`)).status,404);
    assert.equal((await fetch(`${base}/`,{method:'POST'})).status,405);
  } finally { await new Promise(resolve=>server.close(resolve)); }
});
