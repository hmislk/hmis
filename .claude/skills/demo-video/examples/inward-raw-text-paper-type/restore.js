// Put Inward back as found before recording: Raw Text ON, line width 36.
// Run:  DEPT=Inward BHT=<test BHT> node restore.js
const { chromium } = require('playwright');
const { login } = require('../../../.claude/skills/demo-video/scripts/recorder');
const { setRawText } = require('./cfg');

(async () => {
  const b = await chromium.launch({ channel: 'chrome', headless: true });
  const p = await (await b.newContext({ viewport: { width: 1920, height: 1080 } })).newPage();
  await login(p);
  await setRawText(p, true, 36);
  console.log('restored: Raw Text ON, width 36');
  await b.close();
})().catch(e => { console.error(e); process.exit(1); });
