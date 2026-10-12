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

const DEFAULT_NEMOTRON_KEY = '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';
const DEFAULT_KIMI_KEY = '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';

function callNvidiaModel(apiUrl, apiKey, model, messages, extraParams = {}) {
  return new Promise((resolve, reject) => {
    if (!apiKey || !apiKey.trim()) {
      return reject(new Error('No API key provided'));
    }
    const cleanKey = apiKey.trim();
    const url = new URL(apiUrl);
    const postData = JSON.stringify({
      model,
      messages,
      stream: false,
      chat_template_kwargs: { enable_thinking: false },
      ...extraParams
    });

    const req = https.request({
      hostname: url.hostname,
      port: url.port || 443,
      path: url.pathname + url.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${cleanKey}`,
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
            const msg = json.choices?.[0]?.message;
            const content = msg?.content || msg?.reasoning_content;
            if (content && content.trim()) {
              return resolve(content.trim());
            }
            reject(new Error('Empty content in API response'));
          } catch (e) {
            reject(e);
          }
        } else {
          reject(new Error(`HTTP ${res.statusCode}: ${data.slice(0, 150)}`));
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

async function extractMemoriesSmart(apiUrl, apiKey, userMessage) {
  // First run fast 0ms heuristic extractor
  const heuristic = mockExtractMemories(userMessage);
  if (heuristic && heuristic.length > 0) {
    return heuristic;
  }

  // If message doesn't match heuristic patterns, ask AI
  if (!apiKey || !userMessage) return [];
  const systemPrompt = `You are a cognitive memory extraction system.
Analyze the user's message to identify if they shared any personal facts, tech stack choices, preferences, desires, or milestones.
Rules:
1. If the user states a personal preference, technical choice, goal, decision, or fact about themselves/their project (e.g. "I decided to use PostgreSQL", "i want to use supabase now", "I love dark mode", "We use Docker"): classify as type "SEMANTIC", importance between 1.0 and 5.0, with content written in third person starting with "User ...".
2. If the user states a completed achievement or milestone (e.g. "I finished the auth module"): classify as type "EPISODIC", importance between 1.0 and 5.0, with content starting with "User ...".
3. If the user message is just a general question with NO facts about the user: return an empty JSON array [].
4. Output STRICTLY a valid JSON array of objects with keys: "type" ("SEMANTIC" or "EPISODIC"), "content", "importance". Do NOT include explanations.`;

  try {
    const raw = await callNvidiaModel(apiUrl, apiKey, 'nvidia/nemotron-3.5-lightning-30b-a3b', [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userMessage }
    ], { max_tokens: 200, temperature: 0.1 });

    const start = raw.indexOf('[');
    const end = raw.lastIndexOf(']');
    if (start !== -1 && end !== -1 && end > start) {
      const parsed = JSON.parse(raw.substring(start, end + 1));
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed.map(item => ({
          type: (item.type || 'SEMANTIC').toUpperCase(),
          content: item.content,
          importance: Number(item.importance) || 2.0
        }));
      }
    }
  } catch (err) {
    console.warn('AI memory extraction fallback:', err.message);
  }

  return [];
}

async function generateAnswerWithFallback(apiUrl, keyNemotron, keyKimi, prompt, question, selectedMemories) {
  const messages = [
    { role: 'system', content: 'You are SmartChat, an intelligent AI assistant. Use the provided user context to tailor your response.' },
    { role: 'user', content: prompt }
  ];

  const models = [
    { model: 'nvidia/nemotron-3.5-lightning-30b-a3b', key: keyNemotron, params: { temperature: 0.7, max_tokens: 500 } },
    { model: 'nvidia/nemotron-3-ultra-550b-a55b', key: keyNemotron, params: { temperature: 0.7, max_tokens: 500 } },
    { model: 'moonshotai/kimi-k3', key: keyKimi, params: { temperature: 0.7, max_tokens: 500 } }
  ];

  for (const item of models) {
    if (!item.key) continue;
    try {
      const response = await callNvidiaModel(apiUrl, item.key, item.model, messages, item.params);
      if (response && response.trim()) {
        return response.trim();
      }
    } catch (err) {
      console.warn(`Model ${item.model} error: ${err.message}`);
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
  const keyNemotron = (process.env.NVIDIA_API_KEY_NEMOTRON || DEFAULT_NEMOTRON_KEY).trim();
  const keyKimi = (process.env.NVIDIA_API_KEY_KIMI || DEFAULT_KIMI_KEY).trim();
  const primaryKey = keyNemotron || keyKimi;

  // 1. Memory extraction
  const extracted = await extractMemoriesSmart(apiUrl, primaryKey, question);
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

  // 4. Generate Answer with AI
  const answer = await generateAnswerWithFallback(apiUrl, keyNemotron, keyKimi, promptText, question, selected);

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
