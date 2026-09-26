// Demo: department preference "Allow Disposal Issue to Same Department".
// Run from tmp/demo-videos/<slug>/ :  DEPT="Main Pharmacy" node flow.js
const { chromium } = require('playwright');
const { run } = require('../../../.claude/skills/demo-video/scripts/recorder');

const toggle = page => page.locator('[id$=allowIssueToSameDept]');
async function openDeptSettings(page, sleep) {
  const b = page.locator('button:has-text("Department Settings")');
  if (!(await b.isVisible())) { await page.click('.ui-accordion-header:has-text("Preferences")'); await sleep(800); }
  return b;
}

run(chromium, {
  // start from the default state, so the "blocked" half is real
  async setup({ page, sleep, menu }) {
    await menu('Administration', 'Manage Institutions');
    await Promise.all([page.waitForLoadState('networkidle'), (await openDeptSettings(page, sleep)).click()]);
    if (!(await toggle(page).innerText()).includes('Not Allowed')) {
      await toggle(page).click(); await page.click('button:has-text("Save")'); await sleep(1500);
    }
    console.log('setup: toggle =', (await toggle(page).innerText()).trim());
  },

  async flow(r) {
    const p = r.page;
    const pickOwn = async () => {
      await r.autocomplete(r.dept.slice(0, 9), r.dept);
      await r.sleep(500);
      await r.click(p.locator('button:has-text("Select")'), { nav: true });
    };

    await r.step('intro'); await r.sleep(1500); await r.point(p.locator('.ui-menubar').first(), 800); await r.done();
    await r.step('pharmMenu'); await r.menu('smPharmacy', 'Consumption'); await r.done();
    await r.step('newIssue'); await r.click(p.locator('button:has-text("New Consumption Issue")'), { nav: true }); await r.done();
    await r.step('pickOwn1'); await pickOwn(); await r.pinGrowl(); await r.done(300);

    await r.step('blocked', { keep: true });
    const msg = await r.pinGrowl();          // returns the copy pinned during the previous step
    if (!/does not allow/.test(msg || '')) throw new Error('expected the same-department block, got: ' + msg);
    await r.point(p.locator('#__keep'), 2500); await r.done(900);

    await r.step('adminMenu'); await r.menu('Administration', 'Manage Institutions'); await r.done();
    await r.step('deptSettings'); await r.click(await openDeptSettings(p, r.sleep), { nav: true }); await r.done();

    await r.step('toggle');
    await r.point(p.locator('label:has-text("Allow Disposal Issue to Same Department")'), 1800);
    await r.sleep(4000);                      // let the explanation land before the click
    await r.click(toggle(p)); await r.done();

    await r.step('save');
    await r.click(p.locator('button:has-text("Save")').first()); await r.sleep(700);
    await r.point(p.locator('.ui-messages-info').first(), 1200); await r.done();

    await r.step('back');
    await r.menu('smPharmacy', 'Consumption');
    await r.click(p.locator('button:has-text("New Consumption Issue")'), { nav: true }); await r.done(200);
    await r.step('pickOwn2'); await pickOwn(); await r.done(300);

    await r.step('allowed');
    const h = p.locator('text=Department Consumption — Issue to').first();
    if (!(await h.count())) throw new Error('issue screen did not open after enabling the preference');
    await r.point(h, 2500); await r.done(1500);
  },
});
