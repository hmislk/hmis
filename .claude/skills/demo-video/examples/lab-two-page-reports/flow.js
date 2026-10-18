// Demo: two-page lab report (enable option, format designer pages, data entry, report view).
// Run from tmp/demo-videos/lab-two-page-reports/ :
//   PT="Demo Alpha" HMIS_BASE=http://localhost:9090/rh/ DEPT="Main Laboratory" node flow.js
// PT = name of the fictional test patient billed in setup (use a new one for every take).
// Local or QA only: setup bills a fictional test patient, which is fine on a test environment and never on production.
const { chromium } = require('playwright');
const { run } = require('../../../.claude/skills/demo-video/scripts/recorder');

const PT = process.env.PT;
if (!PT) { console.error('Set PT to a fresh fictional patient name, e.g. PT="Demo Alpha"'); process.exit(2); }
const FBC = 'FULL BLOOD COUNT (FBC)';
const DEFAULT_PAGE_TEXT = 'Page {page} of {pages}';
const sleep = ms => new Promise(r => setTimeout(r, ms));
const exact = (p, sel, text) => p.locator(sel).filter({ hasText: new RegExp('^\\s*' + text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\s*$') });

async function domClick(page, text) {
  await page.waitForFunction(n => [...document.querySelectorAll('a,button')].some(e => e.innerText.trim() === n), text, { timeout: 60000 });
  await Promise.all([page.waitForLoadState('networkidle'), page.evaluate(n => {
    const a = [...document.querySelectorAll('a,button')].find(e => e.innerText.trim() === n); a.click();
  }, text)]);
  await sleep(1800);
}

// open Administration > Manage Institutions > Preferences > Application Options, filtered to the multi-page key
async function openOptionRow(page, menu) {
  await menu('Administration', 'Manage Institutions');
  const btn = page.locator('button:has-text("Application Options")');
  for (let i = 0; i < 3 && !(await btn.isVisible()); i++) { await page.click('.ui-accordion-header:has-text("Preferences")'); await sleep(1000); }
  await Promise.all([page.waitForLoadState('networkidle'), btn.click()]); await sleep(2500);
  const f = page.locator('#optionsForm th input').first(); await f.click(); await f.pressSequentially('Lab Report - Multi-page', { delay: 30 }); await sleep(2500);
}
async function setOption(page, menu, want) {
  await openOptionRow(page, menu);
  await page.locator('#optionsForm tbody tr button:has(.fa-edit), #optionsForm tbody tr button:has-text("Edit Option")').first().click(); await sleep(1500);
  const sw = page.locator('#editForm .ui-toggleswitch').first();
  const on = (await sw.getAttribute('class')).includes('ui-toggleswitch-checked');
  if (on !== want) { await sw.click(); await sleep(500); }
  await page.locator('#editForm button:has-text("Save")').click(); await sleep(2000);
  console.log('setup: multi-page option =', want);
}
async function openFbcFormat(page, menu) {
  await menu('Administration', 'Manage Lab Services');
  await domClick(page, 'List Investigation');
  const f = page.locator('th input').nth(1); await f.waitFor({ timeout: 60000 }); await f.click(); await f.pressSequentially(FBC, { delay: 30 }); await sleep(3000);
  await page.locator('tbody tr').first().getByText('Manage investigation').first().click(); await page.waitForLoadState('networkidle'); await sleep(1500);
  await domClick(page, 'Manage Format - Single Item'); await sleep(3000);
}
async function normalizeDesigner(page) {
  const pages = page.locator('[id$=btnDesignerPage] .ui-button');
  if (await pages.filter({ hasText: 'Page 3' }).count()) {
    await pages.filter({ hasText: 'Page 3' }).click(); await sleep(1500);
    await page.locator('button:has-text("Remove Page")').click(); await page.waitForLoadState('networkidle'); await sleep(2000);
  }
  for (let i = 0; i < 10; i++) {   // remove the note an earlier take added to page 2 (named 'Page 2 note' once the take has renamed it; only page 2: page 1 has real items named Label)
    await page.locator('.ui-selectonemenu', { has: page.locator('[id$="dtbItem_input"]') }).click(); await sleep(600);
    const opt = page.locator('li.ui-selectonemenu-item:visible', { hasText: /^(Page 2 note|Label) - Label \(page 2\)$/ }).first();
    if (!(await opt.count())) { await page.keyboard.press('Escape'); break; }
    await opt.click(); await page.waitForLoadState('networkidle'); await sleep(1500);
    await page.locator('button:has-text("Remove")').filter({ hasNotText: 'Page' }).first().click(); await page.waitForLoadState('networkidle'); await sleep(2000);
  }
  if (!(await pages.filter({ hasText: 'Page 2' }).count())) {   // a format that has only one page yet
    await page.locator('button:has-text("Add Page")').click(); await page.waitForLoadState('networkidle'); await sleep(2000);
  }
  await pages.filter({ hasText: 'Page 2' }).click(); await sleep(2000);
  const box = page.locator('[id$=pPages] .ui-chkbox-box').first();
  if ((await box.getAttribute('class')).includes('ui-state-active')) { await box.click(); await sleep(1500); }
  await page.locator('[id$=pPages] input[value*="{pages}"]').fill(DEFAULT_PAGE_TEXT); await page.keyboard.press('Tab'); await sleep(800);
  await page.locator('button:has-text("Save Page Settings")').click(); await sleep(2000);
}
async function billPatient(page) {
  page.on('dialog', d => d.accept());
  await domClick(page, 'Lab Billing'); await sleep(1500);
  const pick = async (id, label) => { await page.click(`[id="${id}"]`); await sleep(500); await page.locator(`li.ui-selectonemenu-item:visible:text-is("${label}")`).first().click(); await sleep(500); };
  await page.click('[id="form:btnAddNewPatient"]').catch(() => {}); await sleep(800);
  await pick('form:cmbTitle', 'Mr.');
  await page.fill('[id="form:txtNewPtName"]', PT);
  await pick('form:cmbSex', 'Male');
  await page.fill('[id="form:dpDob_input"]', '01 January 1985'); await page.keyboard.press('Tab'); await sleep(1200);
  await page.fill('[id="form:txtMobile"]', '0710000001'); await page.fill('[id="form:txtPhone"]', '0710000001');
  const f = page.locator('[id="form:acIx_filter"]');
  await f.click(); await f.pressSequentially(FBC, { delay: 40 }); await sleep(2000);
  await page.locator('.ui-selectlistbox-item:visible', { hasText: FBC }).first().click(); await sleep(500);
  await page.click('[id="form:btnAddIx"]'); await sleep(2500);
  const ri = page.locator('[id="form:refIns_input"]'); await ri.click(); await ri.pressSequentially('Galle', { delay: 80 }); await sleep(1800);
  await page.locator('.ui-autocomplete-panel .ui-autocomplete-item').first().click(); await sleep(800);
  await page.fill('[id="form:paid"]', '400'); await page.keyboard.press('Tab'); await sleep(800);
  await page.locator('button', { hasText: /^\s*Settle\s*$/ }).first().click(); await page.waitForLoadState('networkidle'); await sleep(3500);
  if (!/print/.test(page.url())) throw new Error('bill was not settled, now at ' + page.url());
  console.log('setup: billed FBC for', PT);
}

run(chromium, {
  async setup({ page, menu }) {
    await setOption(page, menu, true);          // Pages panel must be visible to reset the designer
    await openFbcFormat(page, menu);
    await normalizeDesigner(page);
    await setOption(page, menu, false);         // the video starts with the feature off
    await billPatient(page);
  },

  async flow(r) {
    const p = r.page;
    p.on('dialog', d => d.accept());
    const side = p.locator('.ui-menubar').first();

    await r.step('intro'); await r.sleep(1500); await r.highlight(side, 1500); await r.done();

    await r.step('adminMenu'); await r.menu('Administration', 'Manage Institutions'); await r.done();
    await r.step('appOptions');
    const optBtn = p.locator('button:has-text("Application Options")');
    for (let i = 0; i < 3 && !(await optBtn.isVisible()); i++) { await r.click(p.locator('.ui-accordion-header:has-text("Preferences")')); await r.sleep(900); }
    await r.click(optBtn, { nav: true }); await r.done();

    await r.step('findOption');
    await r.type(p.locator('#optionsForm th input').first(), 'Lab Report - Multi-page', 90); await r.sleep(2500);
    await r.point(p.locator('#optionsForm tbody tr').first(), 2500); await r.done();

    await r.step('turnOn');
    await r.click(p.locator('#optionsForm tbody tr button:has-text("Edit Option"), #optionsForm tbody tr button:has(.fa-edit)').first());
    await r.sleep(1200);
    const sw = p.locator('#editForm .ui-toggleswitch').first();
    await r.click(sw); await r.sleep(800);
    await r.click(p.locator('#editForm button:has-text("Save")')); await r.sleep(1500); await r.done();

    await r.step('labAdmin'); await r.menu('Administration', 'Manage Lab Services'); await r.done();
    await r.step('listIx');
    const mi = p.locator('.ui-accordion-header:has-text("Manage Investigations")');
    const li = exact(p, 'a:visible, button:visible', 'List Investigation').first();
    for (let i = 0; i < 3 && !(await li.isVisible()); i++) { await r.click(mi); await r.sleep(900); }
    await r.click(li, { nav: true }); await r.done();
    await r.step('findIx'); await r.type(p.locator('th input').nth(1), FBC, 80); await r.sleep(3000); await r.done();
    await r.step('manageIx');
    await r.click(p.locator('tbody tr').first().getByText('Manage investigation').first(), { nav: true });
    await r.sleep(800);
    await r.click(exact(p, 'a:visible', 'Manage').first());
    await r.sleep(800);
    await r.click(exact(p, 'a:visible, button:visible', 'Manage Format - Single Item').first(), { nav: true });
    await r.sleep(2500); await r.done();

    await r.step('pagesPanel');
    if (!(await p.locator('[id$=pPages]').count())) throw new Error('Pages panel not shown although the option is on');
    await r.point(p.locator('[id$=pPages]'), 3000); await r.done();

    await r.step('page2');
    await r.click(p.locator('[id$=btnDesignerPage] .ui-button', { hasText: 'Page 2' })); await r.sleep(2000); await r.done();

    await r.step('commonReport');
    await r.click(p.locator('[id$=pPages] .ui-chkbox-box').first()); await r.sleep(2000); await r.done();

    await r.step('pageNumber');
    const txt = p.locator('[id$=pPages] input[value*="{pages}"]');
    await r.click(txt); await p.keyboard.press('Control+A'); await p.keyboard.type('Sheet {page} of {pages}', { delay: 90 }); await p.keyboard.press('Tab'); await r.sleep(1200);
    await r.click(p.locator('button:has-text("Save Page Settings")')); await r.sleep(2000); await r.done();

    await r.step('background');
    const bg = p.locator('[id$=pPages] .ui-selectonemenu').first();
    await r.click(bg); await r.sleep(4000); await p.keyboard.press('Escape'); await r.done();

    await r.step('itemPage');
    await r.click(exact(p, 'button:visible', 'Label').first(), { nav: true }); await r.sleep(2500);
    const openAcc = async name => {
      const h = p.locator('.ui-accordion-header:has-text("' + name + '")');
      await r.click(h); await r.sleep(800);
      if ((await h.getAttribute('aria-expanded')) !== 'true') { await r.click(h); await r.sleep(800); }
    };
    await openAcc('Item Details');
    await p.locator('[id$="lblIiName"]').fill('Page 2 note');
    await r.click(p.locator('[id$="txtContents"]'));
    await p.keyboard.type('Histograms and scattergrams from the analyzer are shown on this page.', { delay: 45 });
    await p.keyboard.press('Tab'); await r.sleep(800);
    await openAcc('Positioning');
    await p.locator('[id$="txtLeft"]').fill('8'); await p.keyboard.press('Tab');
    await p.locator('[id$="txtTop"]').fill('35'); await p.keyboard.press('Tab'); await r.sleep(600);
    await r.point(p.locator('.ui-selectonemenu', { has: p.locator('[id$=cmbItemPage_input]') }), 3000);
    await r.click(exact(p, 'button:visible', 'Save').first()); await r.sleep(2500); await r.done();

    await r.step('analyzerImage');
    await r.point(exact(p, 'button:visible', 'Analyzer Image').first(), 3500); await r.done();

    await r.step('addPage');
    await r.click(p.locator('button:has-text("Add Page")'), { nav: true }); await r.sleep(2000);
    if (!(await p.locator('[id$=btnDesignerPage] .ui-button', { hasText: 'Page 3' }).count())) throw new Error('Page 3 was not added');
    await r.point(p.locator('[id$=btnDesignerPage]'), 1500);
    await r.click(p.locator('button:has-text("Remove Page")'), { nav: true }); await r.sleep(2000);
    if (await p.locator('[id$=btnDesignerPage] .ui-button', { hasText: 'Page 3' }).count()) throw new Error('Page 3 was not removed');
    await r.done();

    await r.step('dataEntryIntro'); await r.menu('smLab', 'Report Management'); await r.done();
    await r.step('search'); await r.click(p.locator('[id$=btnSearch0]')); await r.sleep(3000); await r.done();
    const row = p.locator('tr', { hasText: PT }).filter({ has: p.locator('[id$=btnSample]') }).first();
    if (!(await row.count())) throw new Error('patient row not found: ' + PT);
    await r.step('sample'); await r.click(row.locator('[id$=btnSample]')); await r.sleep(2500); await r.done();
    await r.step('receive'); await r.click(row.locator('[id$=btnReceive]')); await r.sleep(2500); await r.done();
    await r.step('newReport'); await r.click(row.locator('[id$=btnNewReport]'), { nav: true }); await r.sleep(2500); await r.done();

    await r.step('enter');
    const vals = ['6.5', '60', '30', '6', '3', '1', '13.5', '4.6', '41', '89', '29', '33', '250'];   // WBC ... Platelet Count, in screen order
    const boxes = p.locator('[id*="tblVals:"][id$="txtValDbl"]:visible');
    if ((await boxes.count()) !== vals.length) throw new Error('expected ' + vals.length + ' result boxes, found ' + (await boxes.count()));
    for (let i = 0; i < vals.length; i++) {
      const f = boxes.nth(i);
      if (i < 4) { await r.click(f); await p.keyboard.press('Control+A'); await p.keyboard.type(vals[i], { delay: 140 }); }
      else { await f.fill(vals[i]); }
      await p.keyboard.press('Tab'); await r.sleep(400);
    }
    await r.done();

    await r.step('saveApprove');
    await r.click(exact(p, 'button:visible', 'Calculate').first()); await r.sleep(2500);
    await r.click(exact(p, 'button:visible', 'Save').first()); await r.sleep(2500);
    await r.click(p.locator('[id$=btnApprove]')); await r.sleep(3000); await r.done();

    await r.step('backToList');
    await r.menu('smLab', 'Report Management'); await r.sleep(1500);
    await r.click(p.locator('[id$=btnSearch0]')); await r.sleep(3000);
    const row2 = p.locator('tr', { hasText: PT }).filter({ has: p.locator('[id$=cmdOldReportPrint1]') }).first();
    await r.click(row2.locator('[id$=cmdOldReportPrint1]'), { nav: true }); await r.sleep(3000); await r.done();

    await r.step('twoPages');
    await p.screenshot({ path: 'tp.png', fullPage: true });
    await r.point(p.getByText(/(Page|Sheet) 1 of 2/).first(), 2500);
    await r.point(p.getByText(/(Page|Sheet) 2 of 2/).first(), 2500);
    await r.done();

    await r.step('outro'); await r.point(exact(p, 'button:visible', 'Print').first(), 1500); await r.point(exact(p, 'button:visible', 'Export PDF').first(), 1500); await r.done(1200);
  },
});
