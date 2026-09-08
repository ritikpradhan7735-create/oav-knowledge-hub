const path = location.pathname.split('/').pop() || 'index.html';
document.querySelectorAll('nav a[data-page]').forEach(a => {
  if (a.getAttribute('href') === path) a.classList.add('active');
});
const menu = document.querySelector('#menu');
const nav = document.querySelector('#nav');
if (menu && nav) menu.onclick = () => nav.classList.toggle('open');

const theme = document.querySelector('#theme');
if (localStorage.oavTheme === 'dark') document.body.classList.add('dark');
if (theme) theme.onclick = () => {
  document.body.classList.toggle('dark');
  localStorage.oavTheme = document.body.classList.contains('dark') ? 'dark' : 'light';
};

if (!document.querySelector('#loginModal')) {
  document.body.insertAdjacentHTML('beforeend', `
    <div class="modal" id="loginModal" aria-hidden="true">
      <div class="modal-box" role="dialog" aria-modal="true" aria-label="Admin sign in">
        <button type="button" class="close" data-close="loginModal" aria-label="Close">×</button>
        <span class="eyebrow">ADMIN ACCESS</span>
        <h2>Admin Sign In</h2>
        <form id="loginForm">
          <input id="user" placeholder="Username" autocomplete="username" required>
          <input id="pass" type="password" placeholder="Password" autocomplete="current-password" required>
          <button type="submit" class="btn primary">Continue →</button>
        </form>
        <p class="hint">Use your administrator credentials to continue.</p>
      </div>
    </div>
    <div class="modal" id="uploadModal" aria-hidden="true">
      <div class="modal-box" role="dialog" aria-modal="true" aria-label="Upload study material">
        <button type="button" class="close" data-close="uploadModal" aria-label="Close">×</button>
        <span class="eyebrow">ADMIN PORTAL</span>
        <h2>Upload Study Material</h2>
        <form id="uploadForm">
          <select id="upClass" required><option value="IX">Class IX</option><option value="X">Class X</option><option value="XI">Class XI</option><option value="XII">Class XII</option></select>
          <input id="upSubject" placeholder="Subject" required>
          <input id="upTitle" placeholder="Title / Chapter Name" required>
          <input id="upFile" type="file" accept="application/pdf" required>
          <button type="submit" class="btn primary">Upload PDF →</button>
        </form>
        <button type="button" class="logout" id="logout">Sign Out</button>
      </div>
    </div>
  `);
}
