addEventListener('fetch', event => {
  event.respondWith(handle(event.request));
});

const MODEL = 'gemini-flash-latest';

async function handle(request) {
  if (request.method === 'OPTIONS') {
    return new Response(null, {
      status: 204,
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, x-api-key'
      }
    });
  }

  if (request.method !== 'POST') {
    return jsonResponse({ error: 'Method not allowed' }, 405);
  }

  const clientKey = request.headers.get('x-api-key') || new URL(request.url).searchParams.get('api_key');
  const expected = CLIENT_API_KEY;
  if (!expected) return jsonResponse({ error: 'CLIENT_API_KEY not configured on worker' }, 500);
  if (!clientKey || clientKey !== expected) return jsonResponse({ error: 'Invalid or missing client API key' }, 401);

  let payload;
  try {
    payload = await request.json();
  } catch (err) {
    return jsonResponse({ error: 'Invalid JSON body' }, 400);
  }

  const prompt = (payload.prompt || '').toString().trim();
  if (!prompt) return jsonResponse({ error: 'prompt is required' }, 400);

  const temperature = typeof payload.temperature === 'number' ? payload.temperature : 0.2;
  const maxOutputTokens = Number(payload.maxOutputTokens) || 256;

  const googleKey = GOOGLE_API_KEY;
  if (!googleKey) return jsonResponse({ error: 'GOOGLE_API_KEY not configured on worker' }, 500);

  const glUrl = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent?key=${googleKey}`;

  const body = {
    contents: [
      {
        parts: [{ text: prompt }]
      }
    ],
    generationConfig: {
      temperature,
      maxOutputTokens
    }
  };

  try {
    const resp = await fetch(glUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    const text = await resp.text();
    return new Response(text, {
      status: resp.status,
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      }
    });
  } catch (err) {
    return jsonResponse({ error: err.message || 'Upstream request failed' }, 502);
  }
}

function jsonResponse(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*'
    }
  });
    }John API KEY
