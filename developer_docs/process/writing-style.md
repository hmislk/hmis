# Writing Style: Short, Plain, To the Point

Applies to everything a person reads: chat replies, hub messages and asks,
issues, PR bodies, comments and docs.

## Rules

1. **Lead with the point.** The first line says the answer, the problem or the
   question. Background comes after, if at all.
2. **Short.** One idea per sentence and per bullet. Cut anything the reader
   does not need to act or decide.
3. **Screen words, not code words.** Name what the user sees: menu names,
   page titles, button labels. Put class, field and method names only in a
   "For developers" section at the end.
4. **Plain, standard terms.** Use the everyday word (see the table). Never
   invent a term or use an abbreviation without spelling it out once.
5. **One question at a time.** Give lettered options and mark one
   "(recommended)". Make the question make sense without the rest of the message.
6. **No story.** Say what is true now and what happens next, not how you
   found it.
7. **Numbers and facts over adjectives.** "3 of 4 buttons" beats "several".

## Length guide

| Where | Aim |
|---|---|
| Hub message or ask | 3 lines or less |
| Question to the user | Up to 3 short lines of context, then one question with options |
| Issue body | Problem, expected, fix: 10 lines or less. Then "For developers" |
| PR summary | 5 lines or less above the Changes section |

## Plain words

| Instead of | Write |
|---|---|
| the action is not decoded / not in `process` | the click is ignored |
| `PatientSample`, `PatientInvestigation` | sample, test |
| persisted, merged | saved |
| navigates to `/lab/x.xhtml` | opens the X page |
| rendered=false | hidden |
| regression | it used to work and now doesn't |
| slice, delta, head sha | part, the new change, the latest commit |
| REV OK, e2e | review passed, tested end to end |

## Example (#25564)

Before: "Buttons whose ajax process leaves out the button itself never run
their action. A `p:commandButton` that is not in its own `process` list is not
decoded, so its `action` is skipped silently."

After: "The red 'Sent to Lab' button does nothing when clicked. It was copied
from the Revert button and never finished. Should we remove it or make it work?"
