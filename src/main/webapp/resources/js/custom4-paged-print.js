/*
    Print helper for the Inward Final Bill letterhead paper ("Custom Bill 3").

    Why this exists instead of the usual <p:printer target="..."/>: PrimeFaces'
    printer.js clones the target into a fresh iframe/window and calls print()
    on the iframe's "load" event or after a fixed timeout (750ms by default) -
    whichever comes first. Paged.js needs to finish asynchronously repaginating
    the content before print() is called, which can easily take longer than
    that timeout on a long bill, so a fixed-timeout race is not reliable here.
    This instead prints only after Paged.js's own pagination promise resolves.

    Also note: printer.js's markup-copying step only copies <style>/<link>/
    <meta>/<title> tags into the print context, never <script> tags, so even
    passing a custom PrimeFaces p:printer "configuration" JSON with an
    appended <script> would not reliably execute either - a fully separate
    print window is the straightforward way to guarantee the polyfill runs.

    Stylesheets are FETCHED and inlined as <style> text (not copied as <link
    href="...">). Copying <link> tags turned out unreliable across a few
    rounds of iteration: relative-URL resolution in a blank popup, JSF resource
    request timing relative to the window "load" event, and Paged.js's own
    handling of external stylesheets were all suspects, and the CSS ended up
    not visibly loading in the popup at all. Fetching the actual CSS text from
    the parent page (where URL resolution is unambiguous) and writing it
    directly into the popup as inline <style> removes all of that uncertainty -
    if a fetch fails, it's now a visible console.error instead of a silent gap.
*/
function printCustom4Bill(targetId, pagedJsUrl) {
    var target = document.getElementById(targetId);
    if (!target) {
        console.error('printCustom4Bill: target not found: ' + targetId);
        return;
    }

    var win = window.open('', '_blank');
    if (!win) {
        alert('Please allow pop-ups for this site to print this bill.');
        return;
    }

    fetchInlineStyles().then(function (styleMarkup) {
        var absolutePagedJsUrl = new URL(pagedJsUrl, document.baseURI).href;

        // outerHTML (not innerHTML) so the printed markup keeps the
        // panelGroup's own id - the CSS for this paper is scoped to that id
        // via [id$="..."] attribute selectors so it never bleeds onto other
        // bill previews rendered on the same source page.
        var bodyMarkup = target.outerHTML;

        win.document.open();
        win.document.write(
            '<!doctype html><html><head><meta charset="UTF-8">' +
            styleMarkup +
            '<script>window.PagedConfig = { auto: false };</script>' +
            '</head><body>' +
            bodyMarkup +
            '<script src="' + absolutePagedJsUrl + '"><\/script>' +
            '</body></html>'
        );
        win.document.close();

        win.addEventListener('load', function () {
            if (!win.Paged) {
                console.error('printCustom4Bill: Paged.js failed to load, printing unpaginated content.');
                win.focus();
                win.print();
                return;
            }
            new win.Paged.Previewer().preview().then(function () {
                console.log('printCustom4Bill: pagination complete, ' +
                    win.document.querySelectorAll('.pagedjs_page').length + ' page(s) generated.');
                win.focus();
                win.print();
            });
        });

        win.onafterprint = function () {
            win.close();
        };
    });
}

/*
    Fetches the current page's <link rel="stylesheet"> files as text and
    returns them (plus any already-inline <style> tags, copied as-is) as one
    HTML string of <style> blocks ready to drop into the print popup's <head>.
    A cache-busting query param forces a fresh copy of each stylesheet on
    every print, so a CSS edit is guaranteed to show up without needing to
    clear the browser cache.
*/
function fetchInlineStyles() {
    var inlineFromStyleTags = Array.prototype.map.call(
        document.querySelectorAll('style'),
        function (el) {
            return '<style>' + el.textContent + '</style>';
        }
    ).join('');

    var fetches = Array.prototype.map.call(
        document.querySelectorAll('link[rel="stylesheet"]'),
        function (el) {
            var absoluteHref = new URL(el.getAttribute('href'), document.baseURI).href;
            var bustedHref = absoluteHref + (absoluteHref.indexOf('?') === -1 ? '?' : '&') + '_cb=' + Date.now();
            return fetch(bustedHref, { cache: 'no-store' })
                .then(function (res) {
                    if (!res.ok) {
                        throw new Error('HTTP ' + res.status + ' fetching ' + bustedHref);
                    }
                    return res.text();
                })
                .then(function (cssText) {
                    return '<style data-source="' + absoluteHref + '">' + cssText + '</style>';
                })
                .catch(function (err) {
                    console.error('printCustom4Bill: failed to load stylesheet ' + absoluteHref, err);
                    return '<!-- printCustom4Bill: failed to load ' + absoluteHref + ' -->';
                });
        }
    );

    return Promise.all(fetches).then(function (styleBlocks) {
        return inlineFromStyleTags + styleBlocks.join('');
    });
}
