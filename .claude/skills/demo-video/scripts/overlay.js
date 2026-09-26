// Injected into every page of the recording context (context.addInitScript):
// fake cursor (headless Chrome draws none), caption bar, click ripple, and hides
// developer-only banners that end users never see.
(() => {
  const css = `
  #__cur{position:fixed;z-index:2147483647;width:22px;height:22px;margin:-3px 0 0 -3px;pointer-events:none;
    background:url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='22' height='22'><path d='M3 2 L3 18 L7.5 14 L10.5 20.5 L13 19.4 L10 13 L16 13 Z' fill='black' stroke='white' stroke-width='1.4'/></svg>") no-repeat}
  #__cap{position:fixed;z-index:2147483646;left:50%;bottom:28px;transform:translateX(-50%);max-width:82%;
    background:rgba(20,24,33,.86);color:#fff;font:600 20px/1.4 'Segoe UI',Arial,sans-serif;padding:10px 20px;border-radius:10px;text-align:center;pointer-events:none}
  #__cap:empty{display:none}
  .__hl{outline:4px solid #ff3b30 !important;outline-offset:3px !important;box-shadow:0 0 0 9px rgba(255,59,48,.22) !important}
  .__ripple{position:fixed;z-index:2147483645;width:16px;height:16px;border-radius:50%;margin:-8px 0 0 -8px;background:rgba(255,59,48,.55);pointer-events:none;animation:__rp .6s ease-out forwards}
  @keyframes __rp{to{transform:scale(3.2);opacity:0}}`;
  function init() {
    if (document.getElementById('__cur')) return;
    const st = document.createElement('style'); st.textContent = css; document.head.appendChild(st);
    const cur = document.createElement('div'); cur.id = '__cur'; document.body.appendChild(cur);
    const cap = document.createElement('div'); cap.id = '__cap'; document.body.appendChild(cap);
    // cursor position and caption survive full-page (ajax="false") navigations
    let pos = {};
    try { pos = JSON.parse(sessionStorage.getItem('__pos') || '{}'); cap.textContent = sessionStorage.getItem('__cap') || ''; } catch (e) {}
    cur.style.left = (pos.x || innerWidth / 2) + 'px'; cur.style.top = (pos.y || innerHeight / 2) + 'px';
    document.addEventListener('mousemove', e => {
      cur.style.left = e.clientX + 'px'; cur.style.top = e.clientY + 'px';
      try { sessionStorage.setItem('__pos', JSON.stringify({ x: e.clientX, y: e.clientY })); } catch (x) {}
    }, true);
    document.addEventListener('mousedown', e => {
      const r = document.createElement('div'); r.className = '__ripple';
      r.style.left = e.clientX + 'px'; r.style.top = e.clientY + 'px';
      document.body.appendChild(r); setTimeout(() => r.remove(), 700);
    }, true);
    const hideDev = () => document.querySelectorAll('body *').forEach(el => {
      if (el.children.length < 4 && /Database Migration Pending/.test(el.textContent || '')
          && getComputedStyle(el).position === 'fixed') el.style.display = 'none';
    });
    hideDev(); setInterval(hideDev, 500);
  }
  window.__setCap = t => {
    const c = document.getElementById('__cap'); if (c) c.textContent = t;
    try { sessionStorage.setItem('__cap', t); } catch (e) {}
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
