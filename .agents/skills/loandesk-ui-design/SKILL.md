---
name: loandesk-ui-design
description: Design or revise LoanDesk JavaFX UI and UX when screens, components, forms, dialogs, or interaction states change. Apply the established desktop visual system; do not use for service, persistence, or domain-only work.
---

# LoanDesk UI design

Create clean, modern, moderately compact JavaFX interfaces for frequent desktop
use. The role-selection and authentication screens in `LoanDeskApp` and
`src/main/resources/loandesk.css` are the canonical visual reference while the
rest of the product is progressively brought into this system.

## Use the existing system first

Before introducing styling, inspect the closest existing screen and the CSS
classes already available. Reuse an existing component or visual pattern when
it fits. Add a new component pattern only when the current ones cannot express
the required behaviour; define its normal and interaction states together.

Keep UI work in JavaFX views and CSS. Do not move validation, authorization,
state transitions, or persistence into a screen merely to support a design.

## Visual tokens

Use these exact values unless a state rule below specifies another value.

| Token | Hex | Use |
| --- | --- | --- |
| Ink | `#17191e` | Primary page headings, prominent text, and icon outlines. |
| Accent red | `#d9202b` | Brand emphasis, selected/active emphasis, and icon highlights. |
| Accent gradient | `#d71e2a` → `#e22832` | Primary call-to-action buttons, left to right. |
| Accent hover | `#bb1520` | Hover/pressed primary-button treatment and red text hover. |
| Page white | `#ffffff` | Main page and card surfaces. |
| Page neutral | `#f6f7f9` / `#f7f8fa` | Subtle page or icon-surface background; do not use as body text. |
| Card border | `#e3e5e9` | Cards, panels, and quiet dividers. |
| Input border | `#cbd0d8` | Resting text and password fields. |
| Secondary text | `#5b6370` | Supporting copy, field help, and secondary actions. |
| Muted text | `#525967` | Low-emphasis metadata only. |
| Error | `#b42318` | Error text and error borders. |
| Error surface | `#fff4f2` | Error message background. |
| Selected surface | `#fcebed` | Selected-role or subtle selected-state background. |

Use white cards on a white-to-neutral page surface, `#e3e5e9` borders, rounded
corners, and restrained soft shadows. Red is an action and emphasis colour,
not a large-area page background. Do not introduce a new blue primary-button
pattern for newly designed screens.

## Typography and spacing

Use `"Segoe UI", "Arial", sans-serif`.

| Level | Size | Weight | Use |
| --- | --- | --- | --- |
| Brand wordmark | 70px | bold | Role-selection branding only. |
| Page heading | 31px | bold | Primary authentication and top-level screens. |
| Section/card heading | 25–26px | bold | Card and major workspace sections. |
| Subtitle/body | 15–16px | regular | Explanations and descriptions. |
| Label/metadata | 13px | regular or bold | Form labels, helper text, and status detail. |
| Eyebrow/tracking text | 12px | bold | Compact category or journey text only. |

Use an 4px base spacing scale: `4, 8, 12, 16, 24, 28, 32, 48, 56`px. Prefer
12–16px gaps within controls, 24–32px between related groups, and 48px or more
between page regions. Keep routine workspaces compact; reserve generous
spacing for landing and authentication screens.

## Components

- **Cards/panels:** white surface, `#e3e5e9` border, 14–18px radius, subtle
  shadow, 28–32px inner padding. Role cards may use 18px radius and 32–38px
  padding.
- **Primary button:** red gradient, white bold 15px text, 8–10px radius,
  12–16px vertical padding. It is the sole visually dominant action in a
  group.
- **Secondary/link action:** transparent or quiet neutral surface; use
  `#5b6370` for back actions and red for an alternate action such as
  `Sign up instead`.
- **Inputs:** white, 8px radius, `#cbd0d8` resting border, at least 40px high.
  Use the red accent and a subtle red glow only while focused.
- **Status and feedback:** pair colour with explicit text. Do not convey an
  error, completion, availability, or selection through colour alone.

## Forms

- Put a persistent visible label directly above every field. A placeholder may
  clarify format or give an example, but never replace the label.
- Mark required fields in the label with `*`; provide one concise note near the
  form explaining that `*` means required when any required fields exist.
- Place helper text directly beneath its field label or field. Keep it to one
  short sentence and use secondary text.
- Validate on submit, and revalidate a field after it has failed when its value
  changes or focus leaves it. Keep valid user input after a failed operation;
  clear passwords after authentication attempts.
- Place an error immediately below its associated input. Use a form-level
  error above the submit button only for cross-field or service failures.
- Default to a single-column form in a 360–400px panel. Use two columns only
  for tightly paired desktop inputs (for example, start and due dates) when
  each column remains readable; collapse to one column when space is limited.
- Keep one primary submit button per form. Place an alternate action below it;
  put `Return to role selection` or a comparable back action last.

## Required UI states

Every new or materially changed interactive component or data region must
define and implement these states where applicable:

| State | Requirement |
| --- | --- |
| Loading | Show progress or a skeleton; prevent duplicate submit actions. |
| Empty | Explain what is absent and offer the next useful action, if one exists. |
| Error | Place clear text near the failing area; retain safe input and provide recovery. |
| Success | Confirm the completed action in text and make the next destination clear. |
| Disabled | Explain the unavailable condition when it is not self-evident; never rely on grey alone. |
| Hover | Provide a restrained colour, border, or elevation response for clickable controls. |
| Focus | Use a clear keyboard-visible focus treatment, normally accent-red on inputs. |
| Selected | Use `#fcebed` plus a text, icon, or label change that identifies what is selected. |

For a state that cannot apply (for example, a static label), state why in the
implementation notes or review rather than silently omitting it.

## Dialogs and confirmation

Use a dialog for a short, interruptive decision or a compact confirmation that
does not need sustained context. Use a full page for multi-field entry,
searching, reviewing lists, or any workflow that benefits from navigation and
space.

- Standard informative/confirmation dialogs should be 360–480px wide.
- Put actions at the bottom-right: quiet cancel/back first, primary confirm
  last. Keep at most one primary action.
- Destructive actions (delete, cancel, mark lost, irreversible state changes)
  require explicit confirmation that names the affected record and consequence.
  The confirm action must use an unambiguous verb; focus the safe/cancel action
  by default.
- Do not use a dialog solely to report a normal successful save when inline
  confirmation or the resulting page state is clearer.

## Completion check

Before handing over UI work, verify the screen compiles, inspect the affected
CSS and flow, and test relevant application logic. If JavaFX interaction is
not actually exercised, say so rather than claiming visual verification.
