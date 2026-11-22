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
        if (q && !q.isEmpty()) {
            if (tries++ < 40) {
                setTimeout(clickWhenIdle, 50);
                return;
            }
            // Still busy after the wait: do not settle on a stale balance. Abort and re-arm.
            if (window.console && console.warn) {
                console.warn('Settle skipped: the previous request is still running. Press Enter again.');
            }
            hmisSettleEnterBusy = false;
            return;
        }
        if (btn.disabled || btn.classList.contains('ui-state-disabled')) {
            hmisSettleEnterBusy = false;
            return;
        }
        var leaving = false;
        var onLeave = function () { leaving = true; };
        window.addEventListener('beforeunload', onLeave);
        btn.click();
        // The confirm dialog blocks inside click(). A confirmed settle submits the form and the page
        // unloads, so the flag stays set. If nothing was submitted (confirm cancelled), re-arm.
        setTimeout(function () {
            window.removeEventListener('beforeunload', onLeave);
            if (!leaving) {
                hmisSettleEnterBusy = false;
            }
        }, 1500);
    };
    clickWhenIdle();
    return false;
}
