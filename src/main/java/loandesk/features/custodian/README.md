# Custodian Feature Area

Place custodian-owned UI under `ui/` and custodian-specific application use
cases under `application/`. Use shared `loandesk.domain`,
`loandesk.application`, and `loandesk.persistence` services instead of
duplicating shared rules or storage.