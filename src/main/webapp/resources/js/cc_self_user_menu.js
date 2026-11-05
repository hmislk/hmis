/*
 * Collecting Centre self-service dashboard: the user chip in the hero opens
 * the User Settings menu (Change Password, About) below it. Closes on an
 * outside click or Escape.
 */
(function ($) {
    'use strict';

    function close(menu) {
        menu.removeClass('open');
        menu.find('.cc-user-menu-toggle').attr('aria-expanded', 'false');
    }

    $(document).on('click', '.cc-user-menu-toggle', function (e) {
        e.preventDefault();
        var menu = $(this).closest('.cc-user-menu');
        var opening = !menu.hasClass('open');
        $('.cc-user-menu.open').each(function () {
            close($(this));
        });
        if (opening) {
            menu.addClass('open');
            $(this).attr('aria-expanded', 'true');
        }
    });

    $(document).on('click', function (e) {
        if ($(e.target).closest('.cc-user-menu').length === 0) {
            $('.cc-user-menu.open').each(function () {
                close($(this));
            });
        }
    });

    $(document).on('keydown', function (e) {
        if (e.key === 'Escape') {
            $('.cc-user-menu.open').each(function () {
                close($(this));
                $(this).find('.cc-user-menu-toggle').trigger('focus');
            });
        }
    });
})(jQuery);
