// Demo: how to log in to HMIS and select a department.
// Run from tmp/demo-videos/login-department-select/ :  DEPT="Main Pharmacy" node flow.js
const { chromium } = require('playwright');
const { run, creds } = require('../../../.claude/skills/demo-video/scripts/recorder');

run(chromium, {
  manualLogin: true,

  async flow(r) {
    const p = r.page;
    const c = creds();

    await r.step('intro'); await r.sleep(1000); await r.done();

    await r.step('username');
    await r.type(p.locator('[id$=txtUserName]'), c.user);
    await r.done();

    await r.step('password');
    await r.type(p.locator('[id$=pwd]'), c.pass);
    await r.done();

    await r.step('loginClick');
    await r.click(p.locator('button:has-text("Login")'), { nav: true });
    await r.done();

    await r.step('deptOpen');
    await r.click(p.locator('[id$=formDept] .ui-selectonemenu'));
    await r.done();

    await r.step('deptType');
    await r.type(p.locator('.ui-selectonemenu-filter:visible'), r.dept.slice(0, 9), 100);
    await r.sleep(500);
    await r.done();

    const row = p.locator(`tr.ui-selectonemenu-row:visible:has(td:text-is("${r.dept}")), tr.ui-selectonemenu-item:visible:has(td:text-is("${r.dept}"))`).first();
    await r.step('deptPick');
    await r.click(row);
    await r.done();

    await r.step('deptSelect');
    await r.click(p.locator('[id$="formDept:btnSelect"]'), { nav: true });
    await r.done();

    await r.step('done');
    if (!(await p.locator('.ui-menubar').first().count())) throw new Error('menu bar did not appear after department select');
    await r.point(p.locator('.ui-menubar').first(), 1800);
    await r.done(1000);
  },
});
