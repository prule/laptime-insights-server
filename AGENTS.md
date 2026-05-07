# Laptime Insights - a web dashboard for ACC performance.

Be concise.

Folders in this project:

- `docs`: information about architecture, design and specs.
- `app`: a Kotlin/Ktor/Exposed REST/HATEOAS API build with clean architecture
- `frontend`: a React app that consumes the REST/HATEOAS API

When making changes ensure:

- ask for more information when required to ensure the right solution
- documentation is kept up to date
- tests are updated
- sample code is updated
- use clean architecture
- if you see something that needs fixing or cleaning up add to docs/technical-debt.md
- if something in technical-debt.md is cleaned up, mark it as DONE in docs/technical-debt.md
- favor keeping logic in the backend so the frontend just presents data when possible/reasonable.

## Startup routine

Read all files in context/ - this is your foundation
Read MEMORY.md - this is what you've learned over time
Use both to shape every task

Memory system
When I correct you or you learn something new, update the relevant section in MEMORY.md

Keep MEMORY.md current. When something changes update it in place - replace outdated info, don't just append it. The
file should always reflect the latest state.