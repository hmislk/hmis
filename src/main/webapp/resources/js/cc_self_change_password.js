/*
 * Live feedback for the Collecting Centre self-service Change Password page:
 * a strength meter and a rule checklist. Guidance only - the server
 * (SessionController.changePassword) still does the real validation.
 */
(function ($) {
    'use strict';

    var STRENGTH_LABELS = ['', 'Weak', 'Fair', 'Good', 'Strong'];

    function score(pw) {
        if (!pw) {
            return 0;
        }
        var s = 0;
        if (pw.length >= 8) {
            s++;
        }
        if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) {
            s++;
        }
        if (/\d/.test(pw)) {
            s++;
        }
        if (/[^A-Za-z0-9]/.test(pw)) {
            s++;
        }
        if (pw.length >= 12 && s < 4) {
            s++;
        }
        return Math.max(1, Math.min(4, s));
    }

    function mark(rules, rule, ok) {
        rules.find('[data-rule="' + rule + '"]').toggleClass('ok', ok);
    }

    function refresh() {
        var root = $('.cc-pw-form');
        var pw = root.find('.cc-pw-new').val() || '';
        var username = root.data('username') || '';
        var rules = root.find('.cc-pw-rules');

        var s = score(pw);
        root.find('.cc-pw-meter').attr('data-score', s);
        root.find('.cc-pw-meter-label').text(pw ? 'Strength: ' + STRENGTH_LABELS[s] : '');

        mark(rules, 'length', pw.length >= 8);
        mark(rules, 'upper', /[A-Z]/.test(pw));
        mark(rules, 'lower', /[a-z]/.test(pw));
        mark(rules, 'number', /\d/.test(pw));
        mark(rules, 'special', /[@$!%*?&]/.test(pw));
        mark(rules, 'username', pw.length > 0 && pw !== String(username));
    }

    $(document).on('input keyup', '.cc-pw-new', refresh);
    $(refresh);
})(jQuery);
