# Configuration Documentation

Centralized references for runtime configuration. Start here before introducing new config keys or controllers.

- [Application Options](application-options.md) – catalogue of feature toggles and usage guidance.
- [Bill Number Configuration](bill-number-config-options.md) – instructions for enabling the prefix/year counting strategies.
- [Printer Configuration System](printer-configuration-system.md) – setup for printing profiles, templates, and troubleshooting.
- [SMS through carecode-messenger](sms-gateway-via-messenger.md) – the only SMS mode that works with messenger, its required keys, and how to test a send.
- [Institution-Specific Behavior](institution-specific-behavior.md) – why `applicationInstitution eq 'HospitalName'` checks are highly discouraged, and the `ConfigOption` pattern to use instead.
