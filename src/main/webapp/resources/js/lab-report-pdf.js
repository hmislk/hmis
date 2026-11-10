/*
 * Finishes the PDF that html2pdf makes from a lab report.
 *
 * One-page report: trimmed exactly as before (html2pdf adds a blank second
 * page for an A4-high box, so page 2 is deleted).
 *
 * Multi-page report (one .labReport box per page): html2pdf cannot paginate
 * absolutely positioned A4 boxes reliably, so each box is rendered on its own
 * and placed on its own PDF page. Returns a promise, so the caller must
 * return it from the html2pdf .then() callback.
 */
function labReportTrimPdf(pdf, element) {
    var boxes = element ? Array.prototype.slice.call(element.querySelectorAll('.labReport, .labReport1')) : [];
    if (boxes.length <= 1) {
        pdf.deletePage(2);
        return;
    }
    var opt = {
        margin: [0, 0, 0, 0],
        image: {type: 'jpeg', quality: 1.0},
        html2canvas: {scale: 2, useCORS: true},
        jsPDF: {unit: 'mm', format: [210, 297], orientation: 'portrait'},
        // each box is already one page; html2pdf's default css mode would see
        // .labReportNext's break-before and push the box down by a blank page
        pagebreak: {mode: []}
    };
    while (pdf.internal.getNumberOfPages() > 1) {
        pdf.deletePage(pdf.internal.getNumberOfPages());
    }
    var chain = Promise.resolve();
    boxes.forEach(function (box, i) {
        chain = chain.then(function () {
            return html2pdf().set(opt).from(box).toCanvas().get('canvas');
        }).then(function (canvas) {
            // The captured box can be taller or shorter than one A4 page (the
            // screen box of some reports is 28cm high). Paint the A4 canvas white
            // first: an unpainted area is transparent, and JPEG turns it into a
            // black band at the bottom of the page. The box's 1px screen border
            // is cut off so it does not print as a line.
            var scale = canvas.width / (box.offsetWidth || canvas.width);
            var border = Math.round((parseFloat(getComputedStyle(box).borderTopWidth) || 0) * scale);
            var srcW = canvas.width - 2 * border;
            var srcH = canvas.height - 2 * border;
            var crop = document.createElement('canvas');
            crop.width = srcW;
            crop.height = Math.round(srcW * 297 / 210);
            var ctx = crop.getContext('2d');
            ctx.fillStyle = '#ffffff';
            ctx.fillRect(0, 0, crop.width, crop.height);
            // Keep only the top A4 part; skip drawing on invalid sizes.
            var drawH = Math.min(srcH, crop.height);
            if (srcW > 0 && drawH > 0) {
                ctx.drawImage(canvas, border, border, srcW, drawH, 0, 0, srcW, drawH);
            }
            if (i > 0) {
                pdf.addPage([210, 297], 'portrait');
            }
            pdf.setPage(i + 1);
            pdf.addImage(crop.toDataURL('image/jpeg', 1.0), 'JPEG', 0, 0, 210, 297);
        });
    });
    return chain;
}
