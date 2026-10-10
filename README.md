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
