// Narrated-demo recorder for HMIS. A video's flow.js calls run(chromium, { setup, flow }).
// Playwright is passed in (not required here) so it resolves from the work dir's node_modules.
//
//   env: HMIS_BASE  app root, e.g. http://localhost:8080/rh/   (login page only - never inner URLs)
//        DEPT       login department label, exactly as in the department picker
//        VIEW       viewport WxH (default 1920x1080: the zoom level where the whole top menu fits on one line)
//        HMIS_USER / HMIS_PASS  else read from C:/Credentials/hmis_web_login.txt
const fs = require('fs');
const path = require('path');

const BASE = process.env.HMIS_BASE || 'http://localhost:8080/rh/';
const DEPT = process.env.DEPT;
const [W, H] = (process.env.VIEW || '1920x1080').split('x').map(Number);
const sleep = ms => new Promise(r => setTimeout(r, ms));

function creds() {
  if (process.env.HMIS_USER) return { user: process.env.HMIS_USER, pass: process.env.HMIS_PASS };
  const t = fs.readFileSync('C:/Credentials/hmis_web_login.txt', 'utf8');
  return { user: /Username\s*:\s*(\S+)/.exec(t)[1], pass: /Password\s*:\s*(\S+)/.exec(t)[1] };
}

async function login(page, dept = DEPT) {
  await page.goto(BASE, { waitUntil: 'networkidle' });
  const c = creds();
  await page.fill('[id$=txtUserName]', c.user);
  await page.fill('[id$=pwd]', c.pass);
  await Promise.all([page.waitForLoadState('networkidle'), page.click('button:has-text("Login")')]);
  // department picker: filterable selectOneMenu whose items are table rows
  await page.click('[id$=formDept] .ui-selectonemenu');
  await page.locator('.ui-selectonemenu-filter:visible').pressSequentially(dept, { delay: 40 });
  await sleep(400);
  await page.locator(`tr.ui-selectonemenu-row:visible:has(td:text-is("${dept}")), tr.ui-selectonemenu-item:visible:has(td:text-is("${dept}"))`).first().click();
  await Promise.all([page.waitForLoadState('networkidle'), page.click('[id$="formDept:btnSelect"]')]);
  await sleep(800);
}

// Top-level menubar entry: 'Administration' is labelled; the rest are icon-only <li id$=smXxx>
// (smPharmacy, smInpatient, smOpd, smLab, smStore, smReports, smSettings ...).
function topMenu(page, top) {
  return top === 'Administration'
    ? page.locator('.ui-menubar > .ui-menu-list > li.ui-menu-parent:has(> a .ui-menuitem-text:text-is("Administration"))').first()
    : page.locator(`li[id$=${top}]`);
}

// Unrecorded navigation, for setup (resetting a config to its demo start state).
async function menuQuiet(page, top, item) {
  const li = topMenu(page, top);
  await li.hover(); await sleep(500);
  await Promise.all([page.waitForLoadState('networkidle'), li.locator(`a:has-text("${item}")`).first().click()]);
}

async function run(chromium, { setup, flow, workDir = process.cwd() }) {
  if (!DEPT) { console.error('Set DEPT to the login department label, e.g. DEPT="Main Pharmacy"'); process.exit(2); }
  const script = JSON.parse(fs.readFileSync(path.join(workDir, 'script.json'), 'utf8'));
  const dur = JSON.parse(fs.readFileSync(path.join(workDir, 'audio', 'durations.json'), 'utf8'));
  const say = Object.fromEntries(script.map(s => [s.id, s.say]));
  const overlay = fs.readFileSync(path.join(__dirname, 'overlay.js'), 'utf8');
  const browser = await chromium.launch({ channel: 'chrome', headless: true });

  if (setup) {   // separate, unrecorded context
    const sctx = await browser.newContext({ viewport: { width: W, height: H } });
    const sp = await sctx.newPage();
    await login(sp);
    await setup({ page: sp, sleep, menu: (t, i) => menuQuiet(sp, t, i) });
    await sctx.close();
  }

  const ctx = await browser.newContext({ viewport: { width: W, height: H }, recordVideo: { dir: path.join(workDir, 'raw'), size: { width: W, height: H } } });
  await ctx.addInitScript(overlay);
  const p = await ctx.newPage();
  const tVideo = Date.now();
  await login(p);
  await sleep(800);
  const t0 = Date.now();
  const marks = [];
  let stepId, stepStart;

  const r = {
    page: p, sleep, dept: DEPT,
    // caption + narration clip start here; clears a pinned growl unless { keep: true }
    async step(id, { keep = false } = {}) {
      if (!(id in say)) throw new Error(`step "${id}" not in script.json`);
      if (!keep) await r.unpin().catch(() => {});
      stepId = id; stepStart = Date.now();
      marks.push({ id, t: (stepStart - t0) / 1000 });
      await p.evaluate(t => window.__setCap && window.__setCap(t), say[id]).catch(() => {});
    },
    async done(extra = 600) {   // hold until the step's narration has finished
      const left = dur[stepId] * 1000 + extra - (Date.now() - stepStart);
      if (left > 0) await sleep(left);
    },
    async moveTo(loc) {
      await loc.scrollIntoViewIfNeeded();
      const b = await loc.boundingBox();
      await p.mouse.move(b.x + b.width / 2, b.y + b.height / 2, { steps: 28 });
    },
    async point(loc, ms = 700) {    // move + red highlight, no click
      await r.moveTo(loc);
      await loc.evaluate(e => e.classList.add('__hl'));
      await sleep(ms);
      await loc.evaluate(e => e.classList.remove('__hl')).catch(() => {});
    },
    async highlight(loc, ms = 1500) {   // red outline only, cursor stays put (use for menus/bars: hovering opens dropdowns)
      await loc.evaluate(e => e.classList.add('__hl'));
      await sleep(ms);
      await loc.evaluate(e => e.classList.remove('__hl')).catch(() => {});
    },
    async click(loc, { nav = false, hold = 700 } = {}) {   // nav: true for ajax="false" buttons/menu items
      await r.point(loc, hold);
      if (nav) await Promise.all([p.waitForLoadState('networkidle'), loc.click()]); else await loc.click();
      await sleep(nav ? 900 : 400);
    },
    async type(loc, text, delay = 120) { await r.click(loc); await loc.pressSequentially(text, { delay }); },
    async menu(top, item) {
      const li = topMenu(p, top);
      await r.moveTo(li.locator('> a')); await li.hover(); await sleep(900);
      // travel straight down the submenu column: a diagonal path crosses other items and pops their flyouts
      const target = li.locator(`a:has-text("${item}")`).first();
      const b = await target.boundingBox(), a = await li.locator('> a').boundingBox();
      const x = Math.max(b.x + 12, Math.min(b.x + b.width / 3, a.x + a.width / 2));
      await p.mouse.move(x, a.y + a.height - 2, { steps: 8 });
      await p.mouse.move(x, b.y + b.height / 2, { steps: 24 });
      await r.click(target, { nav: true, hold: 900 });
    },
    async autocomplete(text, pick) {   // first visible PrimeFaces autocomplete
      await r.type(p.locator('.ui-autocomplete-input:visible').first(), text, 140);
      await sleep(1300);
      await r.click(p.locator('.ui-autocomplete-panel:visible .ui-autocomplete-item', { hasText: pick }).first());
    },
    // PrimeFaces growls fade after a few seconds - pin a styled copy (#__keep) that stays until
    // unpin() or the next full page load. Returns the message text; calling it again returns the
    // already-pinned text.
    async pinGrowl() {
      const kept = await p.evaluate(() => document.getElementById('__keep')?.innerText.trim() || null);
      if (kept) return kept;
      await p.locator('.ui-growl-item-container').first().waitFor({ timeout: 5000 }).catch(() => {});
      return p.evaluate(() => {
        const g = document.querySelector('.ui-growl-item-container'); if (!g) return null;
        const b = g.getBoundingClientRect(); const c = g.cloneNode(true);
        const err = /error/.test(g.className + g.innerHTML);
        const [bg, fg] = err ? ['#ffcdd2', '#73000c'] : ['#c8e6c9', '#1b5e20'];
        c.id = '__keep';
        c.style.cssText += `;position:fixed;left:${b.left}px;top:${b.top}px;width:${b.width}px;z-index:2147483640;opacity:1;display:block;background:${bg};color:${fg};border-radius:6px;box-shadow:0 2px 8px rgba(0,0,0,.25)`;
        c.querySelectorAll('*').forEach(e => { e.style.color = fg; e.style.background = 'transparent'; });
        document.querySelectorAll('.ui-growl').forEach(e => e.style.visibility = 'hidden');
        document.body.appendChild(c);
        return g.innerText.trim();
      });
    },
    // drop the pinned copy and dismiss the original growl (as a user closing it would) -
    // error growls can be sticky, so just un-hiding them would bring the old message back
    async unpin() {
      await p.evaluate(() => {
        document.getElementById('__keep')?.remove();
        document.querySelectorAll('.ui-growl-item-container').forEach(e => e.remove());
        document.querySelectorAll('.ui-growl').forEach(e => e.style.visibility = '');
      });
    },
  };

  let failed = null;
  try { await flow(r); } catch (e) { failed = e; }
  await p.evaluate(() => window.__setCap('')).catch(() => {}); await sleep(1200);
  const total = (Date.now() - t0) / 1000;
  const video = await p.video().path();
  await ctx.close(); await browser.close();
  fs.writeFileSync(path.join(workDir, 'timeline.json'), JSON.stringify({ video, trim: (t0 - tVideo) / 1000, total, marks, W, H }, null, 1));
  if (failed) { console.error('FLOW FAILED at step', stepId, '-', failed.message); process.exit(1); }
  console.log(`recorded ${marks.length} steps, ${total.toFixed(1)}s, ${W}x${H}`);
}

module.exports = { run, login, topMenu, sleep };
