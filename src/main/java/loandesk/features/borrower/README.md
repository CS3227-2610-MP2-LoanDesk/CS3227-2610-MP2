# Borrower Feature Area

Place borrower-owned UI under `ui/` and borrower-specific application use cases
under `application/`. Use shared `loandesk.domain`, `loandesk.application`, and
`loandesk.persistence` services instead of duplicating shared rules or storage.