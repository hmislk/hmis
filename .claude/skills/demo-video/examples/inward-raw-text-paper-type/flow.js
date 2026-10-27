// Demo: Raw Text paper type for inpatient payment/deposit receipts (#24459 / PR #24460). Published: https://youtu.be/Hjr3UIW0_00
// Films only a test admission (set BHT and PATIENT); each take adds one Rs 1 cash payment to it.
// setup() turns Raw Text OFF for the login department; afterwards run restore.js (ON, width 36).
// Run from tmp/demo-videos/<slug>/ :  HMIS_BASE=http://localhost:8080/rh/ DEPT=Inward BHT=<test BHT> PATIENT="<test patient name>" node flow.js
const fs = require('fs');
const { chromium } = require('playwright');
const { run } = require('../../../.claude/skills/demo-video/scripts/recorder');
const { setRawText, BHT } = require('./cfg');

const WIDTH = 48;
const PATIENT = process.env.PATIENT;   // test patient's name, as typed in the admission search

async function showFile(p, name, text) {
  await p.evaluate(([name, text]) => {
    document.getElementById('__print')?.remove();
    const d = document.createElement('div');
    d.id = '__print';
    d.style.cssText = 'position:fixed;inset:0;z-index:2147483600;background:rgba(30,30,30,.82);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:18px';
    const t = document.createElement('div');
    t.style.cssText = 'color:#fff;font:600 30px system-ui,sans-serif';
    t.textContent = 'Downloaded: ' + name;
    const pre = document.createElement('pre');
    pre.style.cssText = 'background:#fff;color:#000;font:15px/1.3 "Courier New",monospace;padding:16px 22px;margin:0;box-shadow:0 10px 40px rgba(0,0,0,.6);max-height:80vh;overflow:hidden';
    pre.textContent = text;
    d.append(t, pre);
    document.body.appendChild(d);
  }, [name, text]);
}
const hideFile = p => p.evaluate(() => document.getElementById('__print')?.remove());

run(chromium, {
  async setup({ page }) { await setRawText(page, false, 36); },

  async flow(r) {
    const p = r.page;
    p.on('dialog', d => d.accept());   // Pay asks "Are you sure?"
    const accordion = async () => {
      const hdr = p.locator('.ui-accordion-header:has-text("Inward Deposit")').first();
      for (let i = 0; i < 3 && (await hdr.getAttribute('aria-expanded')) !== 'true'; i++) { await r.click(hdr); await r.sleep(600); }
      if ((await hdr.getAttribute('aria-expanded')) !== 'true') throw new Error('Inward Deposit accordion did not open');
    };

    await r.step('intro'); await r.sleep(1200); await r.highlight(p.locator('.ui-menubar').first(), 1500); await r.done();

    await r.step('menu'); await r.menu('smPayment', 'Payment Management'); await r.done();
    await r.step('accordion');
    await accordion();
    await r.click(p.locator('.ui-accordion-content:visible button:has-text("Payment")').first(), { nav: true });
    await r.done();

    await r.step('patient');
    await r.type(p.locator('.ui-autocomplete-input:visible').first(), PATIENT, 140);
    const rows = p.locator('.ui-autocomplete-panel:visible tr.ui-autocomplete-item');
    await rows.first().waitFor({ timeout: 15000 }); await r.sleep(700);
    if ((await rows.count()) !== 1) throw new Error('expected exactly the one test admission in the list');
    await r.click(rows.first());
    await r.click(p.locator('[id$=btnSelect]'), { nav: true });
    if (!(await p.locator(`text=${PATIENT}`).first().isVisible())) throw new Error('test admission not selected');
    await r.done();

    await r.step('pay');
    await r.click(p.locator('[id$=cmbPs]'));
    await r.click(p.locator('.ui-selectonemenu-panel:visible li:text-is("Cash")'));
    await r.sleep(1200);
    const amt = p.locator('[id$=paymentDetails] input:visible').first();
    await r.click(amt); await amt.fill(''); await amt.pressSequentially('1', { delay: 150 });
    await r.click(p.locator('[id$=btnPay]'), { nav: true });
    await r.done();

    await r.step('before');
    if (await p.locator('button:has-text("(Raw Text)")').count()) throw new Error('Raw Text is still on');
    await r.point(p.locator('[id$=gpBillPreviewDmx]'), 1800);
    await r.point(p.locator('button:has-text("Print (Dot-Matrix)")'), 1200);
    await r.done();

    await r.step('settings');
    await r.click(p.locator('button:has-text("Settings")').first());
    await p.locator('[id$=rawTextPaper]:visible').waitFor();
    await r.done();

    await r.step('tick');
    const cb = p.locator('[id$=rawTextPaper]:visible');
    await r.point(p.locator('.card-header:has-text("Raw Text (Dot-Matrix)"):visible'), 1200);
    await r.click(cb);
    if (!(await cb.isChecked())) throw new Error('Raw Text checkbox not ticked');
    await r.done();

    await r.step('layout');
    await r.point(p.locator('[id$=rawTextTopMargin_input]:visible'), 900);
    const w = p.locator('[id$=rawTextLineWidth_input]:visible');
    await r.click(w); await w.fill(''); await w.pressSequentially(String(WIDTH), { delay: 180 });
    await r.point(p.locator('label:has-text("Pre-printed stationery"):visible'), 900);
    await r.point(p.locator('label:has-text("Show patient phone"):visible'), 700);
    await r.click(p.locator('button:has-text("Apply & Close"):visible'), { nav: true });
    await r.done(0);

    await r.step('after');
    const pre = p.locator('pre').first();
    const ruler = (await pre.innerText()).split('\n').find(l => /^=+$/.test(l.trim()));
    if (!ruler || ruler.trim().length !== WIDTH) throw new Error(`preview is not ${WIDTH} wide: "${ruler}"`);
    const printBtn = p.locator('button:has-text("Print (Raw Text)")');
    await r.point(pre, 2200);
    await r.point(printBtn, 1500);
    await r.done();

    await r.step('print');
    const [dl] = await Promise.all([p.waitForEvent('download'), r.click(printBtn)]);
    const name = dl.suggestedFilename();
    if (!name.endsWith('.prn')) throw new Error('download is not a .prn: ' + name);
    const text = fs.readFileSync(await dl.path(), 'latin1')
      .replace(/\x1b[@P]|\x1bx./g, '').replace(/\f/g, '').replace(/^\n+/, '').replace(/\n+$/, '');
    await r.done();

    await r.step('download'); await showFile(p, name, text); await r.done(1200);

    await r.step('setup');
    await hideFile(p);
    const panel = p.locator('.ui-panel:has(.ui-panel-title:text-is("Raw text printer setup (one-time per PC)"))');
    await r.click(panel.locator('.ui-panel-titlebar-icon').first());
    await r.sleep(800);
    const agent = panel.locator('button:has-text("Download print agent (.zip)")');
    if (!(await agent.isVisible())) throw new Error('setup panel did not expand');
    await r.point(panel.locator('ol'), 1800);
    await r.point(agent, 1200);
    await r.point(panel.locator('a:has-text("Full setup guide")'), 1000);
    await r.done();

    await r.step('reprintNav');
    await r.menu('smPayment', 'Payment Management');
    await accordion();
    await r.click(p.locator('.ui-accordion-content:visible button:has-text("Payment Search")').first(), { nav: true });
    await r.done();

    await r.step('search');
    await r.type(p.locator('label:text-is("BHT No") ~ input').first(), BHT);
    await r.click(p.locator('button:has-text("Search")').first(), { nav: true });
    await r.sleep(800);
    await r.click(p.locator('a:has-text("Inward/DIS/BHT")').first(), { nav: true });
    await r.done();

    await r.step('reprint');
    const orig = p.locator('button:has-text("Print Original (Raw Text)")'), dup = p.locator('button:has-text("Print Duplicate (Raw Text)")');
    if (!(await dup.first().isVisible()) || (await p.locator('pre').count()) < 2) throw new Error('reprint is not in Raw Text mode');
    await r.point(p.locator('pre').nth(0), 1500);
    await r.point(p.locator('pre').nth(1), 1500);
    await r.point(orig.last(), 900);
    await r.point(dup.last(), 900);
    await r.done();

    await r.step('outro');
    await r.point(p.locator('button:has-text("Settings")').first(), 1500);
    await r.highlight(p.locator('.ui-menubar').first(), 1500);
    await r.done(1500);
  },
});
