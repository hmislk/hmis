/*
 * Lab report preview - fit the A4 page to its box.
 *
 * Same behaviour as micFitPreview() in lab/microbiology_patient_report.xhtml.
 * Used by lab/patient_report.xhtml and lab/patient_report_print.xhtml.
 *
 * Markup:
 *   .lab-pr-preview             the box (its width is the space available)
 *     .lab-pr-preview-outer     gets the scaled width and height
 *       .lab-pr-preview-inner   gets transform: scale(...)
 *         divPrint / #pdf       the report (not changed)
 *
 * When the report is wider than the box, it is scaled down so the whole page
 * shows. Print (p:printer copies divPrint) and Export PDF (html2pdf clones
 * #pdf) do not get the scale, because it is set on the wrappers only; the
 * @media print rules in lab-patient-report-mobile.css also reset it.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
(function () {

    function fitPreview(box) {
        var outer = box.querySelector('.lab-pr-preview-outer');
        var inner = box.querySelector('.lab-pr-preview-inner');
        if (!outer || !inner) {
            return;
        }
        inner.style.transform = 'none';
        outer.style.width = '';
        outer.style.height = '';
        var w = inner.scrollWidth;
        var h = inner.scrollHeight;
        var cs = window.getComputedStyle(box);
        var avail = Math.floor(box.clientWidth - (parseFloat(cs.paddingLeft) || 0) - (parseFloat(cs.paddingRight) || 0)) - 1;
        if (w > 0 && avail > 0 && w > avail) {
            var s = avail / w;
            inner.style.transform = 'scale(' + s + ')';
            outer.style.width = Math.ceil(w * s) + 'px';
            outer.style.height = Math.ceil(h * s) + 'px';
        }
    }

    function fitAll() {
        var boxes = document.querySelectorAll('.lab-pr-preview');
        for (var i = 0; i < boxes.length; i++) {
            fitPreview(boxes[i]);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', fitAll);
    } else {
        fitAll();
    }
    window.addEventListener('load', fitAll);
    window.addEventListener('resize', fitAll);
    window.addEventListener('orientationchange', fitAll);

    // Re-fit after AJAX updates of the preview (Report Format, Show Background)
    if (window.jQuery) {
        jQuery(document).on('pfAjaxComplete', fitAll);
    }
})();
