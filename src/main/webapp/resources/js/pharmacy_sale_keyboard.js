/*
 * Keyboard helpers shared by the pharmacy sale pages (#25815).
 *
 * hmisTenderedEnter(event, input): used as onkeydown on the Tendered input (id "paid").
 * Enter clicks the page's Settle button (id ending "btnSettle"), so the existing confirm,
 * checks and privileges run exactly as for a mouse click. Nothing is duplicated.
 *  - waits for the pending PrimeFaces AJAX queue (the keyup balance calculation) to finish first
 *  - does nothing if the Settle button is missing, hidden or disabled
 *  - ignores key auto-repeat and a second press while one settle is pending
 *  - stops the event so the form-level p:defaultCommand (Add) does not fire
 */
var hmisSettleEnterBusy = false;

function hmisTenderedEnter(e, input) {
    if (!e || e.key !== 'Enter') {
        return true;
    }
    e.preventDefault();
    e.stopPropagation();
    if (e.repeat || hmisSettleEnterBusy) {
        return false;
    }
    var scope = input && input.form ? input.form : document;
    var btn = scope.querySelector("[id$='btnSettle']");
    if (!btn || btn.disabled || btn.classList.contains('ui-state-disabled') || btn.offsetParent === null) {
        return false;
    }
    hmisSettleEnterBusy = true;
    var tries = 0;
    var clickWhenIdle = function () {
        var q = window.PrimeFaces && PrimeFaces.ajax && PrimeFaces.ajax.Queue;
        if (q && !q.isEmpty() && tries++ < 40) {
            setTimeout(clickWhenIdle, 50);
            return;
        }
        if (!btn.disabled && !btn.classList.contains('ui-state-disabled')) {
            btn.click();
        }
        // The confirm dialog blocks inside click(); if it was cancelled the page stays, so re-arm.
        // A confirmed settle submits the form and reloads the page, which resets this flag too.
        setTimeout(function () { hmisSettleEnterBusy = false; }, 1500);
    };
    clickWhenIdle();
    return false;
}
