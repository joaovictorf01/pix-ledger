# pix-ledger

Double-entry ledger for Pix payments, built for the Cumbuca technical challenge.

## Running

Requires Docker. In the Codespace, Docker is already set up.

```bash
./mvnw spring-boot:run
```

Spring starts the Postgres service from `compose.yaml` automatically.

## Tests

```bash
./mvnw test
```

Tests use Testcontainers with a real Postgres instance.

## API documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Ready-to-run requests in the `http/` folder, for the VS Code REST Client extension or the IntelliJ HTTP client.
