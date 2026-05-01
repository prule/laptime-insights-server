# laptimeinsights.com

https://laptimeinsights.com

This repository is the server component for the LapTimeInsights dashboard. It is a work in progress, currently under
development. This software would be installed locally on the users network to communicate with the ACC server.

The server uses [acc-client](https://github.com/prule/acc-client) to listen to telemetry from Assetto Corsa
Competizione (ACC) and records information in a database. The server implements a REST API so the frontend can render
data and provide insights to the user.

The dashboard intends to:

- Show amount of effort put it
    - Record sessions to display how often the user is racing.
    - Record laps to display how many laps the user has completed and distance covered
- Display lap information and let the user compare their lap with other drivers
    - Compare gear changes and speed at certain parts of the track

And much more hopefully!

> Mostly this is an exercise in practicing software development, practicing clean architecture, implementing libraries
> and distributing applications.

## Documentation

- [Clean Architecture](./docs/clean-architecture.md) - Details on the project structure and conventions.
- [Real-time Updates](./docs/real-time-updates.md) - How real-time events and WebSockets are implemented.
