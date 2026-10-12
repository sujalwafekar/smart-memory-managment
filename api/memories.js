const { getMemories, addMemory, deleteMemory } = require('./_store');

module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method === 'GET') {
    return res.status(200).json(getMemories());
  }

  if (req.method === 'POST') {
    const { type, content, importance } = req.body || {};
    if (!content || !content.trim()) {
      return res.status(400).json({ error: 'Memory content cannot be empty' });
    }
    const created = addMemory({ type, content: content.trim(), importance });
    return res.status(201).json(created);
  }

  if (req.method === 'DELETE') {
    const id = req.query.id;
    if (!id) {
      return res.status(400).json({ error: 'Missing memory ID' });
    }
    deleteMemory(id);
    return res.status(204).end();
  }

  return res.status(405).json({ error: 'Method Not Allowed' });
};
