'use strict';
if (localStorage.getItem('memorychat_token')) window.location.replace('/chat/');

const form = document.getElementById('authForm');
const title = document.getElementById('authTitle');
const intro = document.getElementById('intro');
const submitButton = document.getElementById('submitButton');
const error = document.getElementById('authError');
const switchMode = document.getElementById('switchMode');
const switchPrompt = document.getElementById('switchPrompt');
const demoLogin = document.getElementById('demoLogin');
const apiBase = (window.MEMORYCHAT_API_URL || '').replace(/\/$/, '');
let mode = 'login';

switchMode.addEventListener('click', () => {
  mode = mode === 'login' ? 'register' : 'login';
  const isRegister = mode === 'register';
  title.textContent = isRegister ? 'Create your space.' : 'Welcome back.';
  intro.textContent = isRegister ? 'Start a private memory library for your conversations.' : 'Sign in to continue your conversation with context that belongs to you.';
  submitButton.innerHTML = isRegister ? 'Create account <span>↗</span>' : 'Sign in <span>↗</span>';
  switchPrompt.textContent = isRegister ? 'Already have an account?' : 'New to MemoryChat?';
  switchMode.textContent = isRegister ? 'Sign in' : 'Create an account';
  document.getElementById('password').autocomplete = isRegister ? 'new-password' : 'current-password';
  error.textContent = '';
});

form.addEventListener('submit', async event => {
  event.preventDefault();
  error.textContent = '';
  const username = form.username.value.trim();
  const password = form.password.value;
  submitButton.disabled = true;
  try {
    const response = await fetch(`${apiBase}/api/auth/${mode}`, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({username, password})});
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.error || 'Could not sign you in.');
    localStorage.setItem('memorychat_token', data.token);
    localStorage.setItem('memorychat_username', data.username);
    window.location.replace('/chat/');
  } catch (requestError) {
    error.textContent = requestError.message;
  } finally { submitButton.disabled = false; }
});

demoLogin.addEventListener('click', () => {
  if (mode === 'register') switchMode.click();
  form.username.value = 'demo_user';
  form.password.value = 'demo12345';
  form.requestSubmit();
});
