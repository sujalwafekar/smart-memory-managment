module.exports = (request, response) => {
  const apiUrl = (process.env.MEMORYCHAT_API_URL || '').replace(/\/$/, '');
  response.setHeader('Content-Type', 'application/javascript; charset=utf-8');
  response.setHeader('Cache-Control', 'no-store');
  response.status(200).send(`window.MEMORYCHAT_API_URL = ${JSON.stringify(apiUrl)};`);
};
