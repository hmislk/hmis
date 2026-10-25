// Show what a print button really prints, in a recorded demo.
//
// Chrome's print dialog is browser UI, not page content, so Playwright's video never contains it
// (and a headless recorder can't open it at all). Instead:
//   1. hookScript makes print() - and document.execCommand('print'), which PrimeFaces p:printer
//      (jQuery.print) tries first - record the document being printed instead of opening the dialog;
//   2. toPdf() renders that exact HTML with Chrome's own print engine (page.pdf, CSS page size);
//   3. toPng() turns page 1 into an image, and showPrinted() lays it over the page on camera.
// The flow should still narrate the dialog settings (printer, margins, scale) in the caption.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const hookScript = `(() => {
  if (window.__printHooked) return; window.__printHooked = true;
  window.__printed = [];
  const hook = w => {
    const rec = () => { window.__printed.push('<!doctype html>' + w.document.documentElement.outerHTML); };
    w.print = rec;
    const ex = w.Document.prototype.execCommand;
    w.Document.prototype.execCommand = function (cmd, ...a) {
      if (String(cmd).toLowerCase() === 'print') { rec(); return true; }
      return ex.call(this, cmd, ...a);
    };
  };
  hook(window);
  const d = Object.getOwnPropertyDescriptor(HTMLIFrameElement.prototype, 'contentWindow');
  Object.defineProperty(HTMLIFrameElement.prototype, 'contentWindow', {
    configurable: true,
    get() {
      const w = d.get.call(this);
      try { if (w && !w.__hooked) { w.__hooked = true; hook(w); } } catch (e) {}
      return w;
    }
  });
})();`;

// Install on the current page and every later navigation (call once, at the start of flow).
async function install(page) {
  await page.context().addInitScript(hookScript);
  await page.evaluate(hookScript);
}

// Wait for the n-th print() (1-based) and return its HTML.
async function waitForPrint(page, n, timeout = 8000) {
  const t = Date.now();
  while (Date.now() - t < timeout) {
    if (await page.evaluate(() => (window.__printed || []).length) >= n) return page.evaluate(i => window.__printed[i], n - 1);
    await new Promise(r => setTimeout(r, 200));
  }
  throw new Error('print() was not called');
}

// Render captured print HTML to PDF in the same context, so session-bound images (p:graphicImage,
// p:barcode) still load. The recorder's own overlay is injected into every page of a recorded
// context; it is removed first, otherwise its boxes add extra PDF pages.
async function toPdf(context, base, html, outPdf) {
  const p = await context.newPage();
  const url = new URL('__printcap__.html', base).href;
  await p.route(url, r => r.fulfill({ status: 200, contentType: 'text/html', body: html }));
  await p.goto(url, { waitUntil: 'networkidle' });
  await p.evaluate(() => document.querySelectorAll('#__cap, #__cur, #__keep, #__print').forEach(e => e.remove()));
  await p.emulateMedia({ media: 'print' });
  await p.pdf({ path: outPdf, preferCSSPageSize: true, printBackground: true });
  await p.close();
  return path.resolve(outPdf);
}

// PDF page 1 -> PNG (needs `pip install pymupdf`). Returns { png, pages } so a flow can assert the
// page count - a one-card print that comes out as 2 pages is a real defect worth a throw.
function toPng(pdf, png = pdf.replace(/\.pdf$/i, '.png'), dpi = 300) {
  const out = execFileSync('python', ['-c',
    'import pymupdf,sys;d=pymupdf.open(sys.argv[1]);d[0].get_pixmap(dpi=int(sys.argv[3])).save(sys.argv[2]);print(d.page_count)',
    pdf, png, String(dpi)]).toString().trim();
  return { png: path.resolve(png), pages: Number(out) };
}

// Overlay the printed image on the page (dimmed backdrop, label above). hidePrinted() removes it.
async function showPrinted(page, png, label, width = 900) {
  const src = 'data:image/png;base64,' + fs.readFileSync(png).toString('base64');
  await page.evaluate(([src, label, width]) => {
    document.getElementById('__print')?.remove();
    const d = document.createElement('div');
    d.id = '__print';
    d.style.cssText = 'position:fixed;inset:0;z-index:2147483600;background:rgba(30,30,30,.82);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:18px';
    d.innerHTML = `<div style="color:#fff;font:600 30px system-ui,sans-serif">${label}</div>
      <img src="${src}" style="width:${width}px;box-shadow:0 10px 40px rgba(0,0,0,.6)">`;
    document.body.appendChild(d);
  }, [src, label, width]);
}
const hidePrinted = page => page.evaluate(() => document.getElementById('__print')?.remove());

module.exports = { hookScript, install, waitForPrint, toPdf, toPng, showPrinted, hidePrinted };
