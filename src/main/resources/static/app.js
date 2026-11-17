'use strict';
const $ = id => document.getElementById(id);
let session, current=null, page=0, busy=false;
function notify(message) { $('notice').textContent=message; }
async function api(path, options={}) {
  const headers={'Content-Type':'application/json', ...options.headers};
  if (session && options.method && options.method!=='GET') headers[session.headerName]=session.token;
  const response=await fetch('/api'+path,{...options,headers});
  if (response.status===401) { location.href='/login'; throw new Error('Please sign in again.'); }
  if (!response.ok) { const data=await response.json().catch(()=>({})); throw new Error(data.detail || `Request failed (${response.status}). Refresh and try again.`); }
  return response.json();
}
function renderDetail(ticket) {
  current=ticket;
  $('empty-detail').hidden=true; $('ticket-detail').hidden=false;
  $('detail-title').textContent=ticket.title;
  $('category').textContent=ticket.category;
  $('meta').textContent=`#${ticket.id} · ${ticket.environment} · ${new Date(ticket.createdAt).toLocaleString()}`;
  $('issue-text').textContent=ticket.description; $('status').value=ticket.status;
  $('source').textContent=ticket.analysisSource || 'NOT ANALYZED';
  $('analysis').textContent=ticket.analysis || 'Ready when you are. Select Analyze issue to generate suggested next steps.';
}
async function refresh() {
  const data=await api('/tickets?page='+page);
  $('count').textContent=`${data.totalElements} ticket${data.totalElements===1?'':'s'} · newest first`;
  $('tickets').replaceChildren();
  if (!data.content.length) $('tickets').textContent='No tickets yet. Create your first issue to get started.';
  for (const ticket of data.content) {
    const button=document.createElement('button'); button.className='ticket'+(current?.id===ticket.id?' active':'');
    const top=document.createElement('span'); top.className='ticket-top'; top.textContent=`#${ticket.id} / ${ticket.status.replaceAll('_',' ')}`;
    const title=document.createElement('strong'); title.textContent=ticket.title;
    const meta=document.createElement('small'); meta.textContent=ticket.category+' · '+new Date(ticket.createdAt).toLocaleDateString();
    button.append(top,title,meta); button.disabled=busy;
    button.onclick=async()=>{try{renderDetail(await api('/tickets/'+ticket.id));await refresh();}catch(e){notify(e.message);}};
    $('tickets').append(button);
  }
  $('page').textContent=`${page+1} / ${Math.max(1,data.totalPages)}`;
  $('prev').disabled=page===0 || busy; $('next').disabled=data.last || busy;
}
function lock(value) { busy=value; document.querySelectorAll('button,select').forEach(b=>b.disabled=value); }
$('ticket-form').onsubmit=async event=>{
  event.preventDefault(); lock(true); notify('Saving ticket…');
  try {
    const form=new FormData(event.target);
    const ticket=await api('/tickets',{method:'POST',body:JSON.stringify(Object.fromEntries(form))});
    page=0; renderDetail(ticket); event.target.reset(); notify('Ticket created. Select Analyze issue for troubleshooting suggestions.');
  }catch(e){notify(e.message);}finally{lock(false);await refresh().catch(e=>notify(e.message));}
};
$('analyze').onclick=async()=>{
  if(!current || busy)return;
  lock(true); notify(session.llmEnabled?'Requesting AI suggestions…':'Preparing offline checklist…');
  try{renderDetail(await api(`/tickets/${current.id}/analyze`,{method:'POST'}));notify(current.analysisSource==='OFFLINE_FALLBACK'?'AI provider unavailable. An offline checklist is shown.':'Troubleshooting suggestions saved.');}
  catch(e){notify(e.message);}finally{lock(false);await refresh().catch(e=>notify(e.message));}
};
$('status').onchange=async()=>{
  const previous=current.status;lock(true);
  try{renderDetail(await api(`/tickets/${current.id}/status`,{method:'PATCH',body:JSON.stringify({status:$('status').value})}));notify('Status updated.');}
  catch(e){$('status').value=previous;notify(e.message);}finally{lock(false);await refresh().catch(e=>notify(e.message));}
};
$('refresh').onclick=()=>refresh().catch(e=>notify(e.message));
$('prev').onclick=()=>{page--;refresh().catch(e=>notify(e.message));};
$('next').onclick=()=>{page++;refresh().catch(e=>notify(e.message));};
$('logout').onclick=async()=>{
  try{const r=await fetch('/logout',{method:'POST',headers:{[session.headerName]:session.token}});if(!r.ok)throw new Error('Could not sign out.');location.href='/login?logout';}catch(e){notify(e.message);}
};
(async()=>{try{session=await api('/session');$('mode').textContent=session.llmEnabled?'LLM ENABLED':'OFFLINE MODE';await refresh();}catch(e){notify(e.message);}})();
