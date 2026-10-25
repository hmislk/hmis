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
            // the captured box can be taller than one page; keep the top A4 part
            var crop = document.createElement('canvas');
            crop.width = canvas.width;
            crop.height = Math.round(canvas.width * 297 / 210);
            crop.getContext('2d').drawImage(canvas, 0, 0);
            if (i > 0) {
                pdf.addPage([210, 297], 'portrait');
            }
            pdf.setPage(i + 1);
            pdf.addImage(crop.toDataURL('image/jpeg', 1.0), 'JPEG', 0, 0, 210, 297);
        });
    });
    return chain;
}
