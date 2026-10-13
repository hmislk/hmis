// Unrecorded helper: set Raw Text on/off and the line width for the logged department,
// through the Settings dialog on the test admission's payment reprint (menu path only).
const { topMenu, sleep } = require('../../../.claude/skills/demo-video/scripts/recorder');
const BHT = process.env.BHT;   // BHT number of a TEST admission (required)
if (!BHT) { console.error('Set BHT to a test admission number'); process.exit(2); }

async function openPaymentIndex(p) {
  const li = topMenu(p, 'smPayment');
  await li.hover(); await sleep(600);
  await Promise.all([p.waitForLoadState('networkidle'), li.locator('a:has-text("Payment Management")').first().click()]);
  const hdr = p.locator('.ui-accordion-header:has-text("Inward Deposit")').first();
  for (let i = 0; i < 3 && (await hdr.getAttribute('aria-expanded')) !== 'true'; i++) { await hdr.click(); await sleep(800); }
}

async function setRawText(p, on, width) {
  await openPaymentIndex(p);
  await Promise.all([p.waitForLoadState('networkidle'), p.locator('.ui-accordion-content:visible button:has-text("Payment Search")').first().click()]);
  await p.locator('label:text-is("BHT No") ~ input').first().fill(BHT);
  await Promise.all([p.waitForLoadState('networkidle'), p.click('button:has-text("Search")')]); await sleep(1000);
  await Promise.all([p.waitForLoadState('networkidle'), p.locator('a:has-text("Inward/DIS/BHT")').first().click()]); await sleep(1000);
  await p.locator('button:has-text("Settings")').first().click(); await sleep(1500);
  const cb = p.locator('[id$=rawTextPaper]:visible');
  if ((await cb.isChecked()) !== on) await cb.click();
  await p.locator('[id$=rawTextLineWidth_input]:visible').fill(String(width));
  await Promise.all([p.waitForLoadState('networkidle'), p.locator('button:has-text("Apply & Close"):visible').click()]); await sleep(1000);
  const raw = await p.locator('button:has-text("(Raw Text)")').count();
  if ((raw > 0) !== on) throw new Error(`Raw Text should be ${on ? 'on' : 'off'} but ${raw} raw-text buttons are shown`);
}

module.exports = { setRawText, openPaymentIndex, BHT };
