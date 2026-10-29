// Servidor da IA do Eng base (Cloudflare Workers).
// A chave da IA fica guardada no servidor, nunca dentro do app.
// Variáveis do Worker: ANTHROPIC_API_KEY (segredo), APP_TOKEN (segredo), MODEL (opcional).

const JSON_HEADERS = { "content-type": "application/json; charset=utf-8" };

function reply(obj, status = 200) {
  return new Response(JSON.stringify(obj), { status, headers: JSON_HEADERS });
}

export default {
  async fetch(request, env) {
    if (request.method !== "POST") return reply({ error: "Use POST" }, 405);

    if (!env.APP_TOKEN || request.headers.get("x-app-token") !== env.APP_TOKEN) {
      return reply({ error: "Código de acesso inválido" }, 401);
    }

    let body;
    try {
      body = await request.json();
    } catch (e) {
      return reply({ error: "Pedido inválido" }, 400);
    }

    const system = String(body.system || "").slice(0, 6000);
    let messages = Array.isArray(body.messages) ? body.messages.slice(-20) : [];
    messages = messages
      .filter((m) => (m.role === "user" || m.role === "assistant") && typeof m.content === "string")
      .map((m) => ({ role: m.role, content: m.content.slice(0, 6000) }));
    while (messages.length && messages[0].role !== "user") messages.shift();
    if (!messages.length) return reply({ error: "Sem mensagem" }, 400);

    const r = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-api-key": env.ANTHROPIC_API_KEY,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify({
        model: env.MODEL || "claude-sonnet-5-5",
        max_tokens: 1500,
        system,
        messages,
      }),
    });

    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      return reply({ error: (data.error && data.error.message) || "Erro na IA" }, 502);
    }
    const text = (data.content || [])
      .filter((b) => b.type === "text")
      .map((b) => b.text)
      .join("\n");
    return reply({ text });
  },
};
