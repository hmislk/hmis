// Demo: print the PVC patient card, front then back (#24297 / #24305). Published: https://youtu.be/xvFcELFmhzM
// Shows the print-button pattern: the native print dialog can't be recorded, so the exact printed
// output is captured (printcap.js), rendered by Chrome to PDF, and shown on screen.
// Needs a test patient with a PHN; this take used "Nimal Demo Perera", phone 0711234599.
// Run from tmp/demo-videos/<slug>/ :  HMIS_BASE=http://localhost:9090/rh/ DEPT=OPD PHONE=0711234599 node flow.js
const { chromium } = require('playwright');
const { run } = require('../../../.claude/skills/demo-video/scripts/recorder');
const pc = require('../../../.claude/skills/demo-video/scripts/printcap');

const PHONE = process.env.PHONE || '0711234599';

run(chromium, {
  async flow(r) {
    const p = r.page;
    await pc.install(p);
    let prints = 0;
    const printSide = async side => {
      await r.click(p.locator(`button:has-text("Print ${side}")`), { hold: 900 });
      const html = await pc.waitForPrint(p, ++prints);
      const pdf = await pc.toPdf(p.context(), process.env.HMIS_BASE, html, `printed_${side}.pdf`);
      const { png, pages } = pc.toPng(pdf);
      if (pages !== 1) throw new Error(`Print ${side} produced ${pages} pages, expected 1`);
      return png;
    };

    await r.step('intro'); await r.sleep(1200); await r.highlight(p.locator('.ui-menubar').first(), 1500); await r.done();
    await r.step('opdMenu'); await r.menu('smOpd', 'Patient Lookup'); await r.done();
    await r.step('search');
    await r.type(p.locator('[id$=txtPhoneSearch]'), PHONE);          // a unique phone opens the profile directly,
    await r.click(p.locator('[id$=btnSearchbyPhoneNo]'), { nav: true }); // so no other patient is ever on screen
    await r.done();
    await r.step('patient');
    await r.point(p.locator('button:has-text("Print Front")'), 1500);
    await r.point(p.locator('button:has-text("Print Back")'), 1500); await r.done();

    await r.step('printFront'); const front = await printSide('Front'); await r.done();
    await r.step('dialog'); await r.done();   // caption narrates the dialog settings
    await r.step('frontResult'); await pc.showPrinted(p, front, 'Printed front (85.6 × 54 mm)'); await r.done(1500);
    await r.step('flip'); await pc.hidePrinted(p); await r.point(p.locator('button:has-text("Print Back")'), 1500); await r.done();
    await r.step('printBack'); const back = await printSide('Back'); await r.done();
    await r.step('backResult'); await pc.showPrinted(p, back, 'Printed back (85.6 × 54 mm)'); await r.done(1500);
    await r.step('outro'); await pc.hidePrinted(p); await r.highlight(p.locator('.ui-menubar').first(), 1500); await r.done(1500);
  },
});
