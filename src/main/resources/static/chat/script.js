'use strict';
const $ = (selector) => document.querySelector(selector);
const memoryList = $('#memoryList');
const thread = $('#thread');
const welcome = $('#welcome');
const chatInput = $('#chatInput');
const chatForm = $('#chatForm');
const sendButton = $('#sendButton');
const apiStatus = $('#apiStatus');
const memoryDialog = $('#memoryDialog');
const apiBase = (window.MEMORYCHAT_API_URL || '').replace(/\/$/, '');
let memories = [];
let currentFilter = 'ALL';
let memorySearchTerm = '';
let editingMemoryId = null;
let sending = false;
const authToken = localStorage.getItem('memorychat_token');
if (!authToken) window.location.replace('/auth/');

function showStatus(message) {
  apiStatus.textContent = message;
  apiStatus.hidden = !message;
}
async function apiFetch(path, options = {}) {
  const response = await fetch(`${apiBase}${path}`, {cache: 'no-store', headers: {'Content-Type':'application/json', 'Authorization': `Bearer ${localStorage.getItem('memorychat_token') || ''}`}, ...options});
  if (response.status === 401 || response.status === 403) {
    localStorage.removeItem('memorychat_token');
    localStorage.removeItem('memorychat_username');
    window.location.replace('/auth/');
    throw new Error('Your session has expired. Please sign in again.');
  }
  if (!response.ok) {
    let message = `Server error (${response.status})`;
    try {const data = await response.json(); message = data.error || message;} catch (_) {}
    throw new Error(message);
  }
  return response.status === 204 ? null : response.json();
}
function make(tag, cls, text) {
  const element = document.createElement(tag);
  if (cls) element.className = cls;
  if (text !== undefined) element.textContent = text;
  return element;
}
function renderMemories() {
  memoryList.replaceChildren();
  $('#memoryCount').textContent = memories.length;
  const shown = memories.filter(memory => {
    const matchesType = currentFilter === 'ALL' || memory.type === currentFilter;
    const searchableText = `${memory.type} ${memory.content}`.toLowerCase();
    return matchesType && searchableText.includes(memorySearchTerm);
  });
  if (!shown.length) {
    memoryList.append(make('p','empty-sidebar',memories.length ? 'No memories in this category.' : 'No memories yet. Add one to get started.'));
    return;
  }
  shown.forEach(memory => {
    const card = make('article','memory-card');
    const head = make('div','memory-top');
    head.append(make('span',`memory-type ${memory.type}`,memory.type.charAt(0) + memory.type.slice(1).toLowerCase()));
    const actions = make('div', 'memory-actions');
    const edit = make('button','edit-memory','Edit');
    edit.type = 'button';
    edit.setAttribute('aria-label',`Edit memory ${memory.id}`);
    edit.addEventListener('click',() => openMemoryDialog(memory));
    const remove = make('button','delete-memory','Delete');
    remove.type = 'button';
    remove.setAttribute('aria-label',`Delete memory ${memory.id}`);
    remove.addEventListener('click',() => deleteMemory(memory.id));
    actions.append(edit, remove);
    head.append(actions);
    card.append(head,make('p','',memory.content));
    const foot = make('div','memory-bottom');
    foot.append(make('span','',`Importance ${Number(memory.importance ?? 1).toFixed(1)}/5`));
    if (memory.createdAt) foot.append(make('span','',new Date(memory.createdAt).toLocaleDateString()));
    card.append(foot);
    memoryList.append(card);
  });
}
async function loadMemories() {
  try {
    const fresh = await apiFetch('/api/memories?t=' + Date.now());
    if (Array.isArray(fresh)) {
      const freshJson = JSON.stringify(fresh);
      if (freshJson !== JSON.stringify(memories)) {
        memories = fresh;
        renderMemories();
      }
    }
  } catch (error) {
    if (!memories.length) {
      memoryList.replaceChildren(make('p','empty-sidebar','Memories are unavailable. Start Spring Boot and check MySQL.'));
      showStatus(`Unable to load memories: ${error.message}`);
    }
  }
}
async function deleteMemory(id) {
  if (!window.confirm('Delete this stored memory?')) return;
  try {
    await apiFetch(`/api/memories/${id}`,{method:'DELETE'});
    memories = memories.filter(m=>m.id !== id);
    renderMemories();
    showStatus('');
  } catch(error) {showStatus(error.message);}
}
function closeSidebar() {
  $('#sidebar').classList.remove('open');
  $('#sidebarBackdrop').hidden = true;
}
$('#openSidebar').addEventListener('click',()=> {$('#sidebar').classList.add('open');$('#sidebarBackdrop').hidden=false;});
$('#closeSidebar').addEventListener('click',closeSidebar);
$('#sidebarBackdrop').addEventListener('click',closeSidebar);
document.querySelectorAll('[data-filter]').forEach(button => button.addEventListener('click',()=> {
  currentFilter = button.dataset.filter;
  document.querySelectorAll('[data-filter]').forEach(b => b.classList.toggle('active', b===button));
  renderMemories();
}));
$('#memorySearch').addEventListener('input', event => {
  memorySearchTerm = event.target.value.trim().toLowerCase();
  renderMemories();
});
function openMemoryDialog(memory) {
  editingMemoryId = memory?.id ?? null;
  $('#memoryForm').reset();
  $('#memoryError').textContent = '';
  $('#dialogTitle').textContent = editingMemoryId ? 'Edit memory.' : 'Something to remember.';
  $('#saveMemory').textContent = editingMemoryId ? 'Save changes ↗' : 'Save memory ↗';
  if (memory) {
    $('#memoryType').value = memory.type;
    $('#memoryText').value = memory.content;
    $('#memoryImportance').value = memory.importance ?? 1;
  }
  $('#importanceValue').textContent = Number($('#memoryImportance').value).toFixed(1);
  memoryDialog.showModal();
  $('#memoryText').focus();
  closeSidebar();
}
$('#openMemory').addEventListener('click',()=> openMemoryDialog());
const dismissDialog = () => { editingMemoryId = null; memoryDialog.close(); };
$('#closeMemory').addEventListener('click',dismissDialog);
$('#cancelMemory').addEventListener('click',dismissDialog);
$('#memoryImportance').addEventListener('input',()=> {$('#importanceValue').textContent = Number($('#memoryImportance').value).toFixed(1);});
$('#memoryForm').addEventListener('submit',async event => {
  event.preventDefault();
  $('#memoryError').textContent='';
  const content = $('#memoryText').value.trim();
  if (!content) return;
  $('#saveMemory').disabled=true;
  try {
    const payload = JSON.stringify({type:$('#memoryType').value,content,importance:Number($('#memoryImportance').value)});
    const path = editingMemoryId ? `/api/memories/${editingMemoryId}` : '/api/memories';
    await apiFetch(path, {method:editingMemoryId ? 'PUT' : 'POST', body:payload});
    dismissDialog();
    await loadMemories();
    showStatus('');
  } catch(error) {$('#memoryError').textContent=error.message;}
  finally {$('#saveMemory').disabled=false;}
});
function scrollDown() {thread.scrollTop = thread.scrollHeight;}
function cleanAssistantText(text) {
  return String(text)
    .replace(/^#{1,6}\s*/gm, '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/```[^\n]*\n?/g, '')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/^\s*\/\/\s?/gm, '');
}
function appendMessage(role,text,relevant = []) {
  welcome?.remove();
  const row = make('div',`message-row ${role}`);
  if (role==='assistant') row.append(make('div','message-avatar','✳'));
  const bubble=make('div','message-bubble');
  bubble.append(make('p','',role === 'assistant' ? cleanAssistantText(text) : text));
  if (role==='assistant') {
    if (relevant && relevant.length) {
      const reveal = make('button', 'memory-reveal', `Show memories used (${relevant.length})`);
      reveal.type = 'button';
      reveal.setAttribute('aria-expanded', 'false');
      const contextBox = make('section', 'selected-memories-box');
      contextBox.hidden = true;
      contextBox.append(make('strong', '', 'Memories used for this answer'));
      contextBox.append(make('p', 'selected-memories-copy', 'MemoryChat used only these saved memories to prepare this answer.'));
      const list = make('ul', 'selected-memories-list');
      relevant.forEach(memory => {
        const item = make('li', 'selected-memory-item');
        item.append(make('span', `selected-memory-type ${memory.type}`, memory.type));
        item.append(make('span', 'selected-memory-content', memory.content));
        list.append(item);
      });
      contextBox.append(list);
      reveal.addEventListener('click', () => {
        const isHidden = contextBox.hidden;
        contextBox.hidden = !isHidden;
        reveal.setAttribute('aria-expanded', String(isHidden));
        reveal.textContent = isHidden ? 'Hide memories used' : `Show memories used (${relevant.length})`;
      });
      bubble.append(reveal, contextBox);
    }
  }
  row.append(bubble);
  thread.append(row);
  scrollDown();
  return row;
}
function adjustComposer(){chatInput.style.height='auto';chatInput.style.height=Math.min(chatInput.scrollHeight,120)+'px';}
chatInput.addEventListener('input',adjustComposer);
chatInput.addEventListener('keydown',event=> {
  if (event.key==='Enter' && !event.shiftKey && !event.isComposing){event.preventDefault();chatForm.requestSubmit();}
});
chatForm.addEventListener('submit',async event=>{
  event.preventDefault();
  const message=chatInput.value.trim();
  if (!message || sending) return;
  sending=true;
  showStatus('');
  sendButton.disabled=true;
  chatInput.value='';adjustComposer();
  appendMessage('user',message);
  const pending=appendMessage('assistant','Thinking through your saved memories…',[]);
  try {
    const response=await apiFetch('/api/chat',{method:'POST',body:JSON.stringify({message})});
    pending.remove();
    appendMessage('assistant', response.answer ?? 'No response received.', response.relevantMemories ?? []);
    await loadMemories();
  } catch(error) {
    pending.remove();
    appendMessage('assistant',`I couldn't reach the chat backend: ${error.message}. Please start Spring Boot and MySQL, then try again.`);
    showStatus('The chat API could not be reached.');
  } finally {sending=false;sendButton.disabled=false;chatInput.focus();}
});
$('#newChat').addEventListener('click',()=>{
  thread.replaceChildren();
  thread.append(welcome);
  // Removed welcome is still referenced; reusing it restores the original empty state.
  closeSidebar();
  showStatus('');
  chatInput.focus();
});
$('#signOut').addEventListener('click', () => {
  localStorage.removeItem('memorychat_token');
  localStorage.removeItem('memorychat_username');
  window.location.replace('/auth/');
});
document.querySelectorAll('[data-question]').forEach(button=>button.addEventListener('click',()=>{
  chatInput.value=button.dataset.question;
  adjustComposer();
  chatForm.requestSubmit();
}));
loadMemories();
setInterval(loadMemories, 3000);
window.addEventListener('focus', loadMemories);
