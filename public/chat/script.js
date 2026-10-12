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
let memories = [];
let currentFilter = 'ALL';
let sending = false;

function showStatus(message) {
  apiStatus.textContent = message;
  apiStatus.hidden = !message;
}
async function apiFetch(path, options = {}) {
  const response = await fetch(path, {cache: 'no-store', headers: {'Content-Type':'application/json'}, ...options});
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
  const shown = memories.filter(m => currentFilter === 'ALL' || m.type === currentFilter);
  if (!shown.length) {
    memoryList.append(make('p','empty-sidebar',memories.length ? 'No memories in this category.' : 'No memories yet. Add one to get started.'));
    return;
  }
  shown.forEach(memory => {
    const card = make('article','memory-card');
    const head = make('div','memory-top');
    head.append(make('span',`memory-type ${memory.type}`,memory.type.charAt(0) + memory.type.slice(1).toLowerCase()));
    const remove = make('button','delete-memory','Delete');
    remove.type = 'button';
    remove.setAttribute('aria-label',`Delete memory ${memory.id}`);
    remove.addEventListener('click',() => deleteMemory(memory.id));
    head.append(remove);
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
$('#openMemory').addEventListener('click',()=> {memoryDialog.showModal();$('#memoryText').focus();closeSidebar();});
const dismissDialog = () => memoryDialog.close();
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
    await apiFetch('/api/memories', {method:'POST',body:JSON.stringify({type:$('#memoryType').value,content,importance:Number($('#memoryImportance').value)})});
    dismissDialog();
    $('#memoryForm').reset();
    $('#importanceValue').textContent='2.0';
    await loadMemories();
    showStatus('');
  } catch(error) {$('#memoryError').textContent=error.message;}
  finally {$('#saveMemory').disabled=false;}
});
function scrollDown() {thread.scrollTop = thread.scrollHeight;}
function appendMessage(role,text,relevant = [], learned = []) {
  welcome?.remove();
  const row = make('div',`message-row ${role}`);
  if (role==='assistant') row.append(make('div','message-avatar','✳'));
  const bubble=make('div','message-bubble');
  bubble.append(make('p','',text));
  if (role==='assistant') {
    if (learned && learned.length) {
      const learnedBox = make('div', 'learned-memories-box');
      learnedBox.append(make('strong', '', '🧠 Auto-extracted Memory: '));
      learned.forEach(lm => {
        learnedBox.append(make('span', 'learned-pill', lm));
      });
      bubble.append(learnedBox);
    }
    const note=make('div','message-meta');
    note.append(document.createTextNode(`${relevant.length} relevant ${relevant.length===1?'memory':'memories'} selected · AI · `));
    const link=make('a','','View reasoning trace ↗');
    link.href='/backend-explainer/';
    note.append(link);
    bubble.append(note);
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
    appendMessage('assistant', response.answer ?? 'No response received.', response.relevantMemories ?? [], response.learnedMemories ?? []);
    if (response.learnedMemories && response.learnedMemories.length) {
      for (const item of response.learnedMemories) {
        const match = item.match(/^\[(.*?)\]\s*(.*)$/);
        if (match) {
          const type = match[1];
          const content = match[2];
          if (!memories.some(m => m.content.toLowerCase() === content.toLowerCase())) {
            try {
              await apiFetch('/api/memories', {
                method: 'POST',
                body: JSON.stringify({ type, content, importance: 2.5 })
              });
            } catch (_) {}
          }
        }
      }
    }
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
document.querySelectorAll('[data-question]').forEach(button=>button.addEventListener('click',()=>{
  chatInput.value=button.dataset.question;
  adjustComposer();
  chatForm.requestSubmit();
}));
loadMemories();
setInterval(loadMemories, 3000);
window.addEventListener('focus', loadMemories);
