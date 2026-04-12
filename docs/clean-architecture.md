# Clean Architecture

This project follows the principles of **Clean Architecture** (also known as Hexagonal or Ports and Adapters architecture). The goal is to separate the core business logic from external concerns like databases, web frameworks, and third-party libraries.

## Layer Structure

The project is organized into the following layers within the `com.github.prule.laptimeinsights` package:

### 1. Domain Layer (`application.domain.model`)
Contains the core business entities and logic. This layer is the most stable and has no dependencies on other layers.
- **Entities**: Data classes representing core concepts (e.g., `Lap`, `Session`).
- **Domain Events**: Events that happen within the domain (e.g., `LapCreated`).

### 2. Application Layer (`application`)
Coordinates the flow of data to and from the domain layer.
- **Domain Services (`application.domain.service`)**: Implement the core use cases. They orchestrate domain objects and interact with "ports".
- **Ports (`application.port`)**: Interfaces that define how the application interacts with the outside world.
    - **Inbound Ports (`port.in`)**: Interfaces defining the "Use Cases" the application supports (e.g., `CreateLapUseCase`).
    - **Outbound Ports (`port.out`)**: Interfaces for external dependencies like database persistence or event broadcasting (e.g., `CreateLapPort`, `EventPort`).

### 3. Adapter Layer (`adapter`)
Contains implementation details for connecting the application to external systems.
- **Inbound Adapters (`adapter.in`)**: Handle incoming requests, such as REST controllers or WebSocket handlers. They convert external data into application commands and call inbound ports.
- **Outbound Adapters (`adapter.out`)**: Implement outbound ports. For example, a database adapter using Exposed or a real-time event broadcaster.

## Conventions

### Naming & Patterns
- **Use Cases**: Inbound ports are named `[Action][Entity]UseCase` (e.g., `CreateLapUseCase`).
- **Services**: Implementations of use cases are named `[Action][Entity]Service` (e.g., `CreateLapService`).
- **Commands**: Data transfer objects used as input for use cases are named `[Action][Entity]Command` (e.g., `CreateLapCommand`).
- **Ports**: Outbound interfaces are named `[Action][Entity]Port` (e.g., `CreateLapPort`).
- **Transactions**: Database transactions are typically managed at the Service level to ensure atomicity of business operations.
- **Dependency Injection**: Dependencies are manually wired in the `AppModule.kt` class.

### Data Flow
1. An **Inbound Adapter** (e.g., a Controller) receives a request.
2. It maps the request to a **Command** object.
3. It calls an **Inbound Port** (Use Case) with the command.
4. The **Application Service** implementing the use case:
    - May fetch data via an **Outbound Port**.
    - Interacts with **Domain Models**.
    - Persists changes via an **Outbound Port**.
    - May emit **Domain Events** via an **Event Port**.
5. The result is returned back to the adapter, which maps it to a response format.

## Testing
- **Unit Tests**: Focus on Domain Models and Application Services, mocking the Ports.
- **Integration Tests**: Focus on Adapters (e.g., testing database queries or API endpoints).
- **Library**: JUnit 5 with AssertJ is the preferred testing stack.
