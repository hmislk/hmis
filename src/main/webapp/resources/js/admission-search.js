var HmisAdmissionSearch = (function () {
    var pending = {};

    function normalize(text) {
        return (text || '').replace(/\s+/g, '').toUpperCase();
    }

    function onQueryComplete(widgetVar, continueClientId) {
        var widget = PF(widgetVar);
        if (!widget || !widget.items || widget.items.length !== 1) {
            return;
        }
        var query = normalize(widget.input.val());
        if (!query) {
            return;
        }
        var row = widget.items.eq(0);
        var phn = normalize(row.find('.hmis-adm-phn').text());
        var bht = normalize(row.find('.hmis-adm-bht').text());
        var mrn = normalize(row.find('.hmis-adm-mrn').text());
        if (query === phn || query === bht || (mrn && query === mrn)) {
            pending[widgetVar] = continueClientId;
            row.trigger('click');
        }
    }

    function onItemSelectComplete(continueClientId) {
        var advanced = false;
        Object.keys(pending).forEach(function (widgetVar) {
            if (pending[widgetVar] === continueClientId) {
                delete pending[widgetVar];
                advanced = true;
            }
        });
        if (advanced) {
            var btn = document.getElementById(continueClientId);
            if (btn) {
                btn.click();
            }
        }
    }

    return {
        onQueryComplete: onQueryComplete,
        onItemSelectComplete: onItemSelectComplete
    };
})();
