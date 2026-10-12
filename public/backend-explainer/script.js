'use strict';
const el = id => document.getElementById(id);
const previous = { value: '' };
function node(tag, className, value) {
  const item=document.createElement(tag);
  if (className) item.className=className;
  item.textContent=value == null ? '' : String(value);
  return item;
}
function renderTrace(trace) {
  el('emptyState').hidden=true;
  el('traceContent').hidden=false;
  el('question').textContent=trace.question || '—';
  el('timestamp').textContent=trace.timestamp ? `Processed ${new Date(trace.timestamp).toLocaleString()}` : '—';
  const scored=Array.isArray(trace.scoredMemories)?trace.scoredMemories:[];
  const selected=Array.isArray(trace.selectedMemories)?trace.selectedMemories:[];
  const words=Array.isArray(trace.keywords)?trace.keywords:[];
  el('scannedCount').textContent=scored.length;
  el('selectedCount').textContent=selected.length;
  el('keywordsCount').textContent=words.length;
  el('keywords').replaceChildren(...(words.length ? words.map(w=>node('span','',w)) : [node('span','','No keywords detected')]));
  const learned=Array.isArray(trace.learnedMemories)?trace.learnedMemories:[];
  if(el('learnedArea')){
    el('learnedArea').hidden=learned.length===0;
    if(learned.length){
      el('learnedList').replaceChildren(...learned.map(l=>node('span','pill',l)));
    }
  }
  const rows=scored.map(m=>{
    const tr=document.createElement('tr');
    tr.append(node('td','',m.content));
    const t=node('td');t.append(node('span','pill',m.type));tr.append(t);
    tr.append(node('td','matches',Array.isArray(m.matchedKeywords)&&m.matchedKeywords.length?m.matchedKeywords.join(', '):'—'));
    tr.append(node('td','score-num',Number(m.score).toFixed(2)));
    tr.append(node('td',m.selected?'selected-yes':'selected-no',m.selected?'● Selected':'○ Ignored'));
    return tr;
  });
  el('scores').replaceChildren(...rows);
  el('prompt').textContent=trace.prompt || '—';
  el('answer').textContent=trace.answer || '—';
}
async function refresh(){
  try {
    const response=await fetch('/api/trace/latest',{cache:'no-store'});
    if(response.status===204){if(!previous.value){el('emptyState').hidden=false;el('traceContent').hidden=true;}return;}
    if(!response.ok)throw new Error('HTTP '+response.status);
    const data=await response.json();
    const signature=JSON.stringify(data);
    if(signature!==previous.value){previous.value=signature;renderTrace(data);}
  } catch(_) {
    if(!previous.value){el('emptyState').querySelector('p').textContent='Could not connect to the backend. Run Spring Boot with MySQL, then send a message in the chat.';}
  }
}
refresh();
setInterval(refresh,1500);
