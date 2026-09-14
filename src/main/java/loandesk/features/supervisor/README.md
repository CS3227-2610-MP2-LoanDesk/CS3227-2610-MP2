# Supervisor Feature Area

Place supervisor-owned UI under `ui/` and supervisor-specific application use
cases under `application/`. Use shared `loandesk.domain`,
`loandesk.application`, and `loandesk.persistence` services instead of
duplicating shared rules or storage.