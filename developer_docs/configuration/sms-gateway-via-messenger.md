# SMS through carecode-messenger

HMIS can send SMS through the **carecode-messenger** gateway
([hmislk/carecode-messenger](https://github.com/hmislk/carecode-messenger)),
which forwards to the Mobitel ESMS service. Messenger is a stateless
middleware: HMIS sends the gateway credentials with every request, and each
hospital keeps its own credentials in its own config options.

## Which HMIS mode to use

`SmsManagerEjb.sendSms()` picks one of five modes from these boolean
Application Options (first `true` wins):

| Option | Works with messenger? |
|---|---|
| `SMS Sent Using OAuth 2.0 Supported SMS Gateway` | No. Hutch-specific token flow. |
| `SMS Sent Using Basic Authentication Supported SMS Gateway` | No. Sends GET query parameters. |
| `SMS Sent Using Basic Authentication Supported SMS Gateway with JQON` | No. Messenger rejects its field names. |
| `SMS Sent Using E -SMS Supported SMS Gateway` | No. The Mobitel call is commented out, so it **never sends**. |
| `SMS Sent Using Basic Authentication Supported SMS Gateway with Simple JSON` | **Yes.** Messenger accepts its fields since carecode-messenger#30. |

If all five are `false`, `sendSms()` returns `false` and nothing is sent or
reported. On the *Sent SMS* test page the reply line is simply blank.

## Required options (Simple JSON mode)

All are Application-scope `SHORT_TEXT` except the flag.

| Key | Value |
|---|---|
| `SMS Sent Using Basic Authentication Supported SMS Gateway with Simple JSON` | `true` (BOOLEAN). Leave the other four `false`. |
| `SMS Gateway with Simple JSON - URL` | `https://<hospital-host>/messenger2/api/sms/send` |
| `SMS Gateway with Simple JSON - Username` | The hospital's Mobitel ESMS username |
| `SMS Gateway with Simple JSON - Password` | The hospital's Mobitel ESMS password |
| `SMS Gateway with Simple JSON - User Alias` | The sender alias exactly as registered with Mobitel, including spaces |
| `SMS Gateway with Simple JSON - Additional parameter 1 value (Only Numbers)` | `0` for normal messages. **Must not be empty**: HMIS calls `Integer.parseInt` on it, so an empty value fails every send. |

The five text keys do not exist until HMIS first reads them. Create them with
`setShortText` on the Config API, or with *Application Options → Import*
(CSV with header `Option Key,Option Value,Value Type,Enum Type`, type
`SHORT_TEXT`, **Import Missing Only**). Don't use `setLongText`: it stores
`LONG_TEXT`, which these keys ignore. See [API_CONFIG](../api/using-apis/API_CONFIG.md).

## Testing

Use *Reports (chart icon) → Analytics → SMS tab → Sent SMS*. It sends one SMS
through the configured mode and prints messenger's raw reply under the button:

- `{"status":"SENT","result":200,...}`: delivered to Mobitel.
- `Exception: Server returned HTTP response code: 400`: messenger or Mobitel
  rejected the message. Call messenger directly to see why.
- Blank: no mode is enabled (see above).

To test messenger on its own, bypassing HMIS:

```bash
curl -X POST -H 'Content-Type: application/json' \
  -d '{"username":"<user>","password":"<pw>","senderName":"<alias>","recipientNumbers":["07XXXXXXXX"],"message":"test","isPromo":false}' \
  https://<hospital-host>/messenger2/api/sms/send
```

`result` is the Mobitel code: `200` is success. Since carecode-messenger#30,
any other code returns `"status":"FAILED"` (HTTP 400). Before that fix messenger
said `SENT` for every code; for example, `169` means an invalid alias.
