// In-memory cognitive store for SmartChat Vercel deployment
const initialMemories = [
  { id: 1, type: "SEMANTIC", content: "User is building a React + Firebase mess management system.", importance: 2.0, createdAt: new Date("2026-10-08T03:44:00Z") },
  { id: 2, type: "SEMANTIC", content: "User knows JavaScript.", importance: 1.5, createdAt: new Date("2026-10-08T03:44:00Z") },
  { id: 3, type: "SEMANTIC", content: "User prefers a simple UI.", importance: 1.0, createdAt: new Date("2026-10-08T03:44:00Z") },
  { id: 4, type: "EPISODIC", content: "User participated in a hackathon.", importance: 1.0, createdAt: new Date("2026-10-08T03:44:00Z") },
  { id: 5, type: "EPISODIC", content: "User completed the login module.", importance: 2.0, createdAt: new Date("2026-10-08T03:44:00Z") },
  { id: 6, type: "WORKING", content: "User previously asked how authentication should be implemented.", importance: 2.5, createdAt: new Date("2026-10-08T03:44:00Z") }
];

let memories = [...initialMemories];
let nextId = 7;
let latestTrace = null;

const STOP_WORDS = new Set([
  "the","a","an","is","are","am","i","me","my","we","our","you","your","to","of","in","on","for",
  "and","or","but","how","what","when","where","why","can","could","should","would","do","does","did",
  "this","that","it","with","from","into","about","please","add","make","implement"
]);

const SYNONYMS = {
  authentication: new Set(["auth", "login", "signin", "firebase"]),
  login: new Set(["authentication", "auth", "signin", "firebase"]),
  database: new Set(["mysql", "sql", "storage", "db"]),
  frontend: new Set(["html", "css", "javascript", "ui"]),
  backend: new Set(["spring", "springboot", "api", "server", "java"]),
  memory: new Set(["remember", "context", "semantic", "episodic", "working"]),
  react: new Set(["frontend", "javascript"]),
  firebase: new Set(["authentication", "auth", "login"])
};

function extractKeywords(question) {
  if (!question) return [];
  const tokens = question.toLowerCase().split(/[^a-z0-9+#.-]+/);
  const seen = new Set();
  const result = [];
  for (const token of tokens) {
    const w = token.trim();
    if (w.length > 2 && !STOP_WORDS.has(w) && !seen.has(w)) {
      seen.add(w);
      result.push(w);
    }
  }
  return result;
}

function typeWeight(type) {
  switch (type) {
    case "WORKING": return 3.0;
    case "SEMANTIC": return 2.0;
    case "EPISODIC": return 1.0;
    case "ARCHIVE": return 0.5;
    default: return 1.0;
  }
}

function scoreMemories(question, currentMemories, limit = 5) {
  const keywords = extractKeywords(question);
  const scored = [];

  for (const memory of currentMemories) {
    const content = (memory.content || "").toLowerCase();
    let score = typeWeight(memory.type) + (Number(memory.importance) || 1.0);
    const matched = [];

    for (const kw of keywords) {
      if (content.includes(kw)) {
        score += 3.0;
        if (!matched.includes(kw)) matched.push(kw);
      }
      const synonyms = SYNONYMS[kw] || new Set();
      for (const syn of synonyms) {
        if (content.includes(syn)) {
          score += 1.25;
          const matchLabel = kw + "~" + syn;
          if (!matched.includes(matchLabel)) matched.push(matchLabel);
          break;
        }
      }
    }

    if (matched.length > 0) score += 1.0;
    scored.push({
      id: memory.id,
      type: memory.type,
      content: memory.content,
      score: Math.round(score * 100) / 100,
      matchedKeywords: matched
    });
  }

  scored.sort((a, b) => b.score - a.score);
  const threshold = keywords.length === 0 ? 4.0 : 5.0;

  const selectedIds = new Set(
    scored
      .filter(item => item.matchedKeywords.length > 0 && item.score >= threshold)
      .slice(0, limit)
      .map(item => item.id)
  );

  return scored.map(item => ({
    ...item,
    selected: selectedIds.has(item.id)
  }));
}

function mockAiGenerate(question, selectedMemories) {
  const q = question.toLowerCase();
  const context = selectedMemories.map(m => m.content).join(" ").toLowerCase();

  if (q.includes("auth") || q.includes("login")) {
    if (context.includes("firebase")) {
      return "Because your stored context says the project uses Firebase and you already worked on the login module, the next logical step is Firebase Authentication: enable your required sign-in provider, connect the existing login form, keep the authenticated user state, and protect private pages. SmartChat selected only the memories related to authentication instead of sending the complete history.";
    }
    return "For authentication, first choose a provider, connect the login form to the backend, create a secure session/token flow, and protect private routes. Add your project stack as a semantic memory so SmartChat can tailor the answer.";
  }

  if (q.includes("memory") || q.includes("relevant")) {
    return "SmartChat extracts keywords from the current question, scores stored memories using memory-type weight plus keyword/synonym matches, selects the top relevant items, and sends only those memories as context to the AI layer.";
  }

  if (selectedMemories.length > 0) {
    return `I found ${selectedMemories.length} relevant stored ${selectedMemories.length === 1 ? 'memory' : 'memories'} and used them as context. The backend explainer shows exactly which memories were selected and why.`;
  }

  return "I could not find a strongly relevant stored memory for that question. I can still answer, but this demo intentionally avoids injecting unrelated context.";
}

function mockExtractMemories(text) {
  if (!text) return [];
  const lower = text.toLowerCase();
  const list = [];
  const sanitize = (t) => {
    const trimmed = t.trim();
    if (trimmed.toLowerCase().startsWith("i ")) {
      return "User " + trimmed.slice(2);
    }
    return trimmed;
  };

  if (lower.includes("finished") || lower.includes("completed") || lower.includes("deployed") || lower.includes("participated in") || lower.includes("attended")) {
    list.push({ type: "EPISODIC", content: sanitize(text), importance: 2.0 });
  } else if (lower.includes("decided to") || lower.includes("switched to") || lower.includes("prefer") || lower.includes("using") || lower.includes("stack is") || lower.includes("i know ") || lower.includes("we use")) {
    list.push({ type: "SEMANTIC", content: sanitize(text), importance: 2.5 });
  }
  return list;
}

module.exports = {
  getMemories: () => memories,
  addMemory: (item) => {
    const newMem = {
      id: nextId++,
      type: (item.type || "SEMANTIC").toUpperCase(),
      content: item.content,
      importance: Number(item.importance) || 1.0,
      createdAt: new Date().toISOString()
    };
    memories.push(newMem);
    return newMem;
  },
  deleteMemory: (id) => {
    const numericId = Number(id);
    const prevLen = memories.length;
    memories = memories.filter(m => m.id !== numericId);
    return memories.length < prevLen;
  },
  getTrace: () => latestTrace,
  setTrace: (trace) => { latestTrace = trace; },
  extractKeywords,
  scoreMemories,
  mockAiGenerate,
  mockExtractMemories
};

