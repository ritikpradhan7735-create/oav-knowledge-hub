<<<<<<< HEAD
const $ = s => document.querySelector(s);

let notes = [];
let currentClass = '';
let currentSubject = '';
let admin = null;

$('#year') && ($('#year').textContent = new Date().getFullYear());

async function loadNotes() {
  try {
    const r = await fetch('/api/notes');
    const d = await r.json();
    notes = d.notes || [];
    const c = $('#noteCount');
    if (c) c.textContent = notes.length;
  } catch {
    notes = [];
  }
}

function esc(s = '') {
  return String(s).replace(/[&<>'"]/g, c => ({
    '&':'&amp;', '<':'&lt;', '>':'&gt;', "'":'&#39;', '"':'&quot;'
  }[c]));
}

function getSubjects(cls) {
  return [...new Set(
    notes
      .filter(n => String(n.class).toUpperCase() === cls)
      .map(n => (n.subject || 'General').trim())
      .filter(Boolean)
  )].sort((a,b) => a.localeCompare(b));
}

function renderSubjects(cls) {
  currentClass = cls;
  currentSubject = '';

  const title = $('#notesTitle');
  if (title) title.textContent = `Class ${cls} — choose a subject`;

  const panel = $('#subjectsPanel');
  const list = $('#subjectsList');
  const notesPanel = $('#notesPanel');

  if (!panel || !list) return;

  const subjects = getSubjects(cls);

  panel.hidden = false;
  if (notesPanel) notesPanel.hidden = true;

  list.innerHTML = subjects.length
    ? subjects.map(subject => `
        <button class="subject-card" type="button" data-subject="${esc(subject)}">
          <span class="subject-icon">📘</span>
          <strong>${esc(subject)}</strong>
          <small>${notes.filter(n => String(n.class).toUpperCase() === cls && (n.subject || 'General').trim() === subject).length} note(s)</small>
        </button>
      `).join('')
    : `<div class="empty">No subjects have been added for Class ${esc(cls)} yet.</div>`;

  list.querySelectorAll('[data-subject]').forEach(btn => {
    btn.onclick = () => renderNotesForSubject(btn.dataset.subject);
  });

  document.querySelectorAll('[data-class]').forEach(b => {
    b.classList.toggle('selected', b.dataset.class === cls);
  });
}

function renderNotesForSubject(subject) {
  currentSubject = subject;

  const title = $('#notesTitle');
  if (title) title.textContent = `Class ${currentClass} — ${subject}`;

  const subjectTitle = $('#selectedSubjectTitle');
  if (subjectTitle) subjectTitle.textContent = `${subject} • Class ${currentClass}`;

  const list = $('#notesList');
  const notesPanel = $('#notesPanel');

  if (!list || !notesPanel) return;

  const filtered = notes.filter(n =>
    String(n.class).toUpperCase() === currentClass &&
    (n.subject || 'General').trim() === subject
  );

  notesPanel.hidden = false;

  list.innerHTML = filtered.length
    ? filtered.map(n => `
        <article class="resource">
          <small>${esc(n.subject || 'General')} · Class ${esc(n.class)}</small>
          <h3>${esc(n.title)}</h3>
          <a href="${esc(n.fileUrl || `/api/view-note?id=${encodeURIComponent(n.id)}`)}" target="_blank" rel="noopener">Open PDF →</a>
          ${admin ? `<button type="button" class="delete-note" data-delete="${esc(n.id)}">Delete note</button>` : ''}
        </article>
      `).join('')
    : `<div class="empty">No notes have been uploaded for this subject yet.</div>`;

  list.querySelectorAll('[data-delete]').forEach(btn => {
    btn.onclick = () => deleteNote(btn.dataset.delete);
  });
}

async function deleteNote(id) {
  if (!admin) {
    alert('Please sign in as Admin first.');
    return;
  }

  const note = notes.find(n => n.id === id);
  const title = note?.title || 'this note';

  if (!confirm(`Delete "${title}"? This cannot be undone.`)) return;

  try {
    const r = await fetch(`/api/delete-note?id=${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: admin.username,
        password: admin.password
      })
    });

    const d = await r.json();

    if (!d.success) throw new Error(d.message || 'Delete failed.');

    notes = notes.filter(n => n.id !== id);
    alert('Note deleted successfully.');

    if (currentSubject) renderNotesForSubject(currentSubject);
    else if (currentClass) renderSubjects(currentClass);
  } catch (err) {
    alert(err.message || 'Delete failed.');
  }
}

document.querySelectorAll('[data-class]').forEach(b => {
  b.onclick = () => renderSubjects(b.dataset.class);
});

$('#changeClass') && ($('#changeClass').onclick = () => {
  $('#subjectsPanel').hidden = true;
  $('#notesPanel').hidden = true;
  $('#notesTitle').textContent = 'Choose your class';
});

$('#changeSubject') && ($('#changeSubject').onclick = () => {
  if (currentClass) renderSubjects(currentClass);
});

$('#adminBtn') && ($('#adminBtn').onclick = () => {
  $('#loginModal')?.classList.add('show');
});

document.querySelectorAll('[data-close]').forEach(b => {
  b.onclick = () => $('#' + b.dataset.close)?.classList.remove('show');
});

document.querySelectorAll('.modal').forEach(m => {
  m.addEventListener('click', e => {
    if (e.target === m) m.classList.remove('show');
  });
});

$('#logout') && ($('#logout').onclick = () => {
  admin = null;
  $('#uploadModal')?.classList.remove('show');
  if (currentSubject) renderNotesForSubject(currentSubject);
});

$('#loginForm') && ($('#loginForm').onsubmit = async e => {
  e.preventDefault();

  const username = $('#user').value.trim();
  const password = $('#pass').value;
  const submit = e.submitter || $('#loginForm').querySelector('button[type=submit]');

  if (submit) { submit.disabled = true; submit.textContent = 'Checking...'; }

  try {
    const r = await fetch('/api/admin/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    const d = await r.json();

    if (!r.ok || !d.success) {
      throw new Error(d.message || 'Invalid admin credentials.');
    }

    admin = { username, password };
    $('#loginModal').classList.remove('show');
    $('#uploadModal').classList.add('show');
    $('#loginForm').reset();

    if (currentSubject) renderNotesForSubject(currentSubject);
  } catch (err) {
    alert(err.message || 'Invalid admin credentials.');
  } finally {
    if (submit) { submit.disabled = false; submit.textContent = 'Continue →'; }
  }
});

$('#uploadForm') && ($('#uploadForm').onsubmit = async e => {
  e.preventDefault();

  if (!admin) {
    alert('Please sign in as Admin first.');
    return;
  }

  const file = $('#upFile').files[0];
  if (!file) {
    alert('Please select a PDF.');
    return;
  }

  const f = new FormData();
  f.append('username', admin.username);
  f.append('password', admin.password);
  f.append('classNum', $('#upClass').value);
  f.append('subject', $('#upSubject').value.trim());
  f.append('title', $('#upTitle').value.trim());
  f.append('pdf', file);

  const b = e.submitter || e.target.querySelector('button[type=submit]');
  b.textContent = 'Uploading...';
  b.disabled = true;

  try {
    const r = await fetch('/api/upload-note', { method: 'POST', body: f });
    const d = await r.json();

    if (!d.success) throw Error(d.message);

    notes.unshift(d.note);

    alert('PDF uploaded successfully.');
    e.target.reset();

    if (currentClass === d.note.class) {
      renderSubjects(currentClass);
    }
  } catch (err) {
    alert(err.message || 'Upload failed.');
  } finally {
    b.textContent = 'Upload PDF →';
    b.disabled = false;
  }
});

$('#chatForm') && ($('#chatForm').onsubmit = async e => {
  e.preventDefault();

  const p = $('#prompt');
  const q = p.value.trim();
  if (!q) return;

  const log = $('#chatLog');
  log.insertAdjacentHTML('beforeend', `<div class="bubble user">${esc(q)}</div>`);
  p.value = '';

  const loading = document.createElement('div');
  loading.className = 'bubble ai';
  loading.textContent = 'Thinking…';
  log.appendChild(loading);

  try {
    const r = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt: q })
    });

    const d = await r.json();
    loading.textContent = d.reply || 'Sorry, I could not answer that right now.';
  } catch {
    loading.textContent = 'Connection problem.';
  }

  log.scrollTop = log.scrollHeight;
});

loadNotes().then(() => {
  if ($('#notesList') && document.querySelector('[data-class]')) {
    // Start on the class-selection screen rather than automatically opening Class IX.
    $('#notesTitle').textContent = 'Choose your class';
  }
});
=======
const $=s=>document.querySelector(s);let notes=[],currentClass='',admin=null;
$('#year')&&($('#year').textContent=new Date().getFullYear());
async function loadNotes(){try{const r=await fetch('/api/notes');const d=await r.json();notes=d.notes||[];const c=$('#noteCount');if(c)c.textContent=notes.length}catch{notes=[]}}
function esc(s=''){return String(s).replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]))}
function renderNotes(cls){currentClass=cls; const title=$('#notesTitle');if(title)title.textContent=`Class ${cls} resources`;const list=notes.filter(n=>n.class===cls);$('#notesList').innerHTML=list.length?list.map(n=>`<article class="resource"><small>${esc(n.subject||'General')} · Class ${esc(n.class)}</small><h3>${esc(n.title)}</h3><a href="${esc(n.fileUrl)}" target="_blank" rel="noopener">Open PDF →</a></article>`).join(''):`<div class="empty">No resources have been uploaded for Class ${cls} yet.</div>`}
document.querySelectorAll('[data-class]').forEach(b=>b.onclick=()=>renderNotes(b.dataset.class));
$('#adminBtn')&&($('#adminBtn').onclick=()=>$('#loginModal').classList.add('show'));document.querySelectorAll('[data-close]').forEach(b=>b.onclick=()=>$('#'+b.dataset.close).classList.remove('show')); $('#logout')&&($('#logout').onclick=()=>{admin=null;$('#uploadModal').classList.remove('show')}); document.querySelectorAll('.modal').forEach(m=>m.addEventListener('click',e=>{if(e.target===m)m.classList.remove('show')}));
$('#loginForm')&&($('#loginForm').onsubmit=e=>{e.preventDefault();admin={username:$('#user').value,password:$('#pass').value};$('#loginModal').classList.remove('show');$('#uploadModal').classList.add('show');$('#loginForm').reset()});
$('#uploadForm')&&($('#uploadForm').onsubmit=async e=>{e.preventDefault();const f=new FormData();f.append('username',admin.username);f.append('password',admin.password);f.append('classNum',$('#upClass').value);f.append('subject',$('#upSubject').value);f.append('title',$('#upTitle').value);f.append('pdf',$('#upFile').files[0]);const b=e.submitter || e.target.querySelector('button[type=submit]');b.textContent='Uploading...';b.disabled=true;try{const r=await fetch('/api/upload-note',{method:'POST',body:f});const d=await r.json();if(!d.success)throw Error(d.message);notes.unshift(d.note);alert('PDF uploaded successfully');e.target.reset()}catch(err){alert(err.message||'Upload failed')}finally{b.textContent='Upload PDF →';b.disabled=false}});
$('#chatForm')&&($('#chatForm').onsubmit=async e=>{e.preventDefault();const p=$('#prompt'),q=p.value.trim();if(!q)return;const log=$('#chatLog');log.insertAdjacentHTML('beforeend',`<div class="bubble user">${esc(q)}</div>`);p.value='';const loading=document.createElement('div');loading.className='bubble ai';loading.textContent='Thinking…';log.appendChild(loading);try{const r=await fetch('/api/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({prompt:q})});const d=await r.json();loading.textContent=d.reply||'Sorry, I could not answer that right now.'}catch{loading.textContent='Connection problem.'}log.scrollTop=log.scrollHeight});
loadNotes().then(()=>{if($('#notesList')&&document.querySelector('[data-class]'))renderNotes(document.querySelector('[data-class]').dataset.class)});
>>>>>>> c1cbadf3117cb8cc5b65ae51fb0018c0aebb836f
