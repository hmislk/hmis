/*
 * Collecting Centre self-billing: shrink fixed-size print previews (11cm
 * bills, A4 lab reports) so they fit a phone/tablet screen.
 *
 * Markup:  <div class="cc-fit-screen">          (available width)
 *            <div class="cc-fit-inner">         (gets the zoom)
 *              <h:panelGroup id="...">          (p:printer target)
 *
 * The zoom is set on .cc-fit-inner, which sits OUTSIDE the p:printer
 * target, so the printed copy keeps its real size. A print-media rule in
 * collecting_centre_self_billing_bill.css also resets it for Ctrl+P.
 */
(function () {
    'use strict';

    function fitOne(wrapper) {
        var inner = wrapper.querySelector(':scope > .cc-fit-inner');
        if (!inner) {
            return;
        }
        inner.style.zoom = '';
        var available = wrapper.clientWidth;
        var natural = inner.scrollWidth;
        if (available > 0 && natural > available) {
            inner.style.zoom = (Math.floor((available / natural) * 1000) / 1000).toString();
        }
    }

    function fitAll() {
        var wrappers = document.querySelectorAll('.cc-fit-screen');
        for (var i = 0; i < wrappers.length; i++) {
            fitOne(wrappers[i]);
        }
    }

    function resetAll() {
        var inners = document.querySelectorAll('.cc-fit-screen > .cc-fit-inner');
        for (var i = 0; i < inners.length; i++) {
            inners[i].style.zoom = '';
        }
    }

    var resizeTimer;
    function onResize() {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(fitAll, 150);
    }

    window.ccFitScreen = {fit: fitAll, reset: resetAll};

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', fitAll);
    } else {
        fitAll();
    }
    window.addEventListener('load', fitAll);
    window.addEventListener('resize', onResize);
    window.addEventListener('orientationchange', onResize);
    window.addEventListener('beforeprint', resetAll);
    window.addEventListener('afterprint', fitAll);
    if (window.jQuery) {
        // re-fit after AJAX updates (e.g. "Show Background" re-renders the report)
        window.jQuery(document).on('pfAjaxComplete', function () {
            setTimeout(fitAll, 50);
        });
    }
})();
