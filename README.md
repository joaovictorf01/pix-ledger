# pix-ledger

Ledger de Pix em partidas dobradas, feito para o mini desafio técnico da Cumbuca.

## Como rodar

Requer Docker. No Codespace, o Docker já vem configurado.

```bash
./mvnw spring-boot:run
```

O Spring sobe o Postgres do `compose.yaml` sozinho.

## Testes

```bash
./mvnw test
```

Os testes usam Testcontainers, com um Postgres real.

## Documentação da API

- Swagger: `http://localhost:8080/swagger-ui.html`
- Requisições prontas na pasta `http/`, para a extensão REST Client do VS Code (ambiente `local`) ou para o cliente HTTP do IntelliJ.
