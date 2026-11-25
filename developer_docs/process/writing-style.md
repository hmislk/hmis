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
8. **Business-rule docs are not narrative.** A business rule states the current practice only. Decisions, history, reasons for change and customer communication go in the issue, the PR or `tmp/`, never in the rule doc.

## Simplified Technical English (ASD-STE100)

Hub messages, asks to the user and Telegram replies follow ASD-STE100
(team request, 24 Nov 2026). Target: 80% of sentences obey all rules.

1. An instruction has 20 words or fewer. A description has 25 words or fewer.
2. Write one instruction or one fact in one sentence.
3. Use the active voice ("The cashier saved the bill", not "The bill was saved").
4. Use simple tenses only: present, simple past, future. No -ing verb forms.
5. Write the subject first and the result before the details.
6. Use one word for one thing. Use the words on the screen.
7. No method names, class names, commit codes or code in text for the user.
8. A paragraph has 6 sentences or fewer. Use a numbered list for steps.
9. Do not use jargon. Use only the agreed short words below; write every other short form in full.
10. Write full sentences with "the" and "a". Do not write in note form.
11. When you give a PR or issue number, say what it is: "PR 26067 (inpatient due reports)".
12. Put gate words, commit codes and code names on the last line, after the label "For sessions:".

Fixed gate words such as "REV OK <sha>" on a PR stay exactly as written.

Measure: once a week one session scores 10 random hub messages (sentences
that obey all rules / all sentences) and reports the score to the user. If
the user replies "unclear", the sender rewrites the message.

### Agreed short words

The user agreed this list on 24 Nov 2026. The team adds a word when it needs one: first add the word and its meaning to this list and to the team playbook list, then use it.

| Short word | Meaning |
|---|---|
| e2e | a test in the app, from start to end |
| REV | a code review by another member |
| REV OK | the review passed |
| REV CHG | the review asks for changes |
| PR | a pull request (a proposed code change) |
| CI | the automatic build and tests on GitHub |
| CR | CodeRabbit, the review robot |
| merge | add a change to the main code |
| deploy | put new code on a server |
| arm | set a merge for a fixed time |
| hotfix | an urgent fix for a live hospital |
| prod | production, the live hospital system |
| QA1 to QA4 | our test servers |
| DB | database |
| BHT | the inpatient admission number |
| SL time | Sri Lanka time |
| Needs-you | a hub question for the user |

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
| persisted (to the database) | saved |
| navigates to `/lab/x.xhtml` | opens the X page |
| rendered=false | hidden |
| regression | it used to work and now doesn't |
| slice, delta, head sha | part, the new change, the latest commit |

Keep fixed gate words exactly as written where a rule or tool reads them,
for example the "REV OK <sha>" comment on a PR.

## Example (#25564)

Before: "Buttons whose ajax process leaves out the button itself never run
their action. A `p:commandButton` that is not in its own `process` list is not
decoded, so its `action` is skipped silently."

After: "The red 'Sent to Lab' button does nothing when clicked. It was copied
from the Revert button and never finished. Should we remove it or make it work?"


## From CLAUDE.md (moved detail)

applies to replies, hub messages, issues, PRs and docs. Lead with the point. Use the words on screen (menu, page, button), not code names; put code names in a "For developers" section. Ask one question at a time, with options and a recommendation.
