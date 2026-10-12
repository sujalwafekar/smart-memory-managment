const https = require('https');
const {
  getMemories,
  addMemory,
  setTrace,
  extractKeywords,
  scoreMemories,
  mockAiGenerate,
  mockExtractMemories
} = require('./_store');

function callNvidiaModel(apiUrl, apiKey, model, messages, extraParams = {}) {
  return new Promise((resolve, reject) => {
    if (!apiKey) {
      return reject(new Error('No API key provided'));
    }
    const url = new URL(apiUrl);
    const postData = JSON.stringify({
      model,
      messages,
      stream: false,
      ...extraParams
    });

    const req = https.request({
      hostname: url.hostname,
      port: url.port || 443,
      path: url.pathname + url.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiKey}`,
        'Content-Length': Buffer.byteLength(postData)
      },
      timeout: 15000
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) {
          try {
            const json = JSON.parse(data);
            const content = json.choices?.[0]?.message?.content;
            if (content) return resolve(content);
            reject(new Error('Empty choices in API response'));
          } catch (e) {
            reject(e);
          }
        } else {
          reject(new Error(`HTTP ${res.statusCode}: ${data}`));
        }
      });
    });

    req.on('timeout', () => {
      req.destroy();
      reject(new Error('Request timed out'));
    });
    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

async function generateAnswerWithFallback(apiUrl, keyKimi, keyNemotron, prompt, question, selectedMemories) {
  const messages = [
    { role: 'system', content: 'You are SmartChat, an intelligent AI assistant. Use the provided user context to tailor your response.' },
    { role: 'user', content: prompt }
  ];

  const models = [
    { model: 'moonshotai/kimi-k3', key: keyKimi, params: { temperature: 1.0, max_tokens: 4096 } },
    { model: 'nvidia/nemotron-3.5-lightning-30b-a3b', key: keyNemotron, params: { temperature: 1.0, max_tokens: 4096 } },
    { model: 'nvidia/nemotron-3-ultra-550b-a55b', key: keyNemotron, params: { temperature: 1.0, max_tokens: 4096 } }
  ];

  for (const item of models) {
    if (!item.key) continue;
    try {
      const response = await callNvidiaModel(apiUrl, item.key, item.model, messages, item.params);
      if (response && response.trim()) {
        return response.trim();
      }
    } catch (err) {
      console.warn(`Model ${item.model} failed: ${err.message}`);
    }
  }

  return mockAiGenerate(question, selectedMemories);
}

module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method Not Allowed' });
  }

  const { message } = req.body || {};
  if (!message || !message.trim()) {
    return res.status(400).json({ error: 'Message cannot be empty' });
  }
  const question = message.trim();

  const apiUrl = process.env.NVIDIA_API_URL || 'https://integrate.api.nvidia.com/v1/chat/completions';
  const keyKimi = process.env.NVIDIA_API_KEY_KIMI;
  const keyNemotron = process.env.NVIDIA_API_KEY_NEMOTRON;

  // 1. Memory extraction
  const extracted = mockExtractMemories(question);
  const existingMemories = getMemories();
  const learnedMemories = [];

  for (const item of extracted) {
    if (!item.content || !item.content.trim()) continue;
    const exists = existingMemories.some(m => m.content.trim().toLowerCase() === item.content.trim().toLowerCase());
    if (!exists) {
      const saved = addMemory(item);
      learnedMemories.push(`[${saved.type}] ${saved.content}`);
    }
  }

  // 2. Score memories
  const currentMemories = getMemories();
  const keywords = extractKeywords(question);
  const scored = scoreMemories(question, currentMemories, 5);
  const selected = scored.filter(m => m.selected);

  // 3. Build Prompt
  let promptText = 'You are SmartChat, a helpful assistant.\n\nRelevant user memory:\n';
  if (selected.length === 0) {
    promptText += '- No strongly relevant memory found.\n';
  } else {
    for (const mem of selected) {
      promptText += `- [${mem.type}] ${mem.content}\n`;
    }
  }
  promptText += `\nCurrent question:\n${question}`;

  // 4. Generate Answer
  const answer = await generateAnswerWithFallback(apiUrl, keyKimi, keyNemotron, promptText, question, selected);

  // 5. Update trace for live explainer
  setTrace({
    timestamp: new Date().toISOString(),
    question,
    keywords,
    scoredMemories: scored,
    selectedMemories: selected,
    prompt: promptText,
    answer,
    learnedMemories
  });

  return res.status(200).json({
    answer,
    keywords,
    relevantMemories: selected,
    learnedMemories
  });
};

