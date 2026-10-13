# Desafio Cumbuca: ledger de Pix

Além de escrever a proposta, fiz uma versão em código para testar se a ideia funciona: https://github.com/joaovictorf01/pix-ledger. O README explica como rodar.

## Como eu entendi o problema

São dois jeitos de olhar o mesmo dinheiro. O livro de clientes diz de quem é o dinheiro: quanto cada cliente tem. O livro interno diz onde ele está: em Recursos de Clientes, na conta de Pagamentos Instantâneos ou, no caso do dinheiro da própria instituição, na Reserva Financeira.

O enunciado fala em três livros e descreve dois. Eu tratei o terceiro como o lado de fora: as outras instituições. Quando um cliente manda um Pix, o dinheiro sai do cliente e vai para fora. Sem uma conta representando esse "fora", a saída não teria para onde ir e a partida dobrada não fecharia. Por isso cada livro tem uma conta `WORLD`. Não tenho certeza de que é isso que o enunciado quis dizer com o terceiro livro; foi a leitura que fez as contas fecharem.

A regra principal é: o total dos saldos dos clientes tem que ser igual a Recursos de Clientes mais Pagamentos Instantâneos. Se não for, dinheiro foi criado ou sumiu.

```mermaid
flowchart LR
    subgraph clientes[Livro de clientes]
        C[Saldos dos clientes]
        WC[WORLD]
    end
    subgraph interno[Livro interno]
        IP[Pagamentos Instantâneos]
        CF[Recursos de Clientes]
        R[Reserva Financeira]
        WI[WORLD]
    end
    C -- Pix enviado --> WC
    WC -- Pix recebido --> C
    IP -- Pix enviado --> WI
    WI -- Pix recebido --> IP
    CF -- reabastecimento, 8h às 18h --> IP
    IP -- sobra e fechamento das 18h --> CF
    R -- empréstimo, 18h às 8h --> IP
    IP -- devolução do empréstimo --> R
    CF -- devolução às 8h --> R
```

## Partida dobrada

Toda operação tem pelo menos duas linhas, uma tirando e outra colocando, e as linhas somam zero. Uma operação nunca mistura os dois livros. Se a soma não der zero, a operação nem é criada.

Nenhuma linha é alterada depois de gravada. Se algo estiver errado, entra uma linha nova corrigindo.

O saldo de uma conta não fica guardado em lugar nenhum: é a soma das linhas dela.

## Enviando um Pix

1. Travo a conta do cliente no banco. Se chegar outro Pix da mesma conta ao mesmo tempo, ele espera. Pix de outras contas não esperam.
2. Vejo se esse Pix já foi gravado, pelo EndToEndId. Se já foi com os mesmos dados, é uma repetição e devolvo o que já existe. Se o mesmo EndToEndId vier com outro valor ou outra conta, recuso.
3. Confiro o saldo e o limite do dia. Isso tem que acontecer depois da trava: se dois Pix lessem o saldo ao mesmo tempo, os dois veriam o mesmo valor e os dois passariam.
4. Gravo no livro de clientes: cliente menos o valor, `WORLD` mais o valor.
5. Depois que isso é confirmado, atualizo o livro interno: Pagamentos Instantâneos menos o valor, `WORLD` mais o valor.

O passo 5 é a consistência eventual: o saldo do cliente muda na hora, e o livro interno acompanha logo depois.

Receber um Pix é o mesmo caminho ao contrário, sem conferir saldo. No enunciado esses Pix chegam por uma fila; no código, um endpoint faz esse papel.

```mermaid
sequenceDiagram
    participant Cliente
    participant API as POST /pix/transfers
    participant Banco as PostgreSQL
    participant Interno as Livro interno
    Cliente->>API: conta, valor e EndToEndId
    API->>Banco: trava a conta do cliente
    API->>Banco: EndToEndId já gravado?
    API->>Banco: confere saldo e limite do dia
    API->>Banco: grava cliente menos valor, WORLD mais valor
    Banco-->>API: confirmado
    API->>Interno: atualiza depois da confirmação
    Interno->>Banco: grava Pagamentos Instantâneos menos valor, WORLD mais valor
    API-->>Cliente: 201
```

## Limite de Pix

Para saber quanto um cliente pode "legitimamente" mandar, usei o limite diário. Segui a regra do Banco Central: diminuir o limite vale na hora; aumentar só vale depois de 24 horas, para proteger o cliente num golpe. Se o cliente pede um aumento e depois uma redução, a redução vale e o aumento é cancelado.

## Quanto deixar em Pagamentos Instantâneos

Para cada cliente, vejo o máximo que ele ainda pode mandar hoje: o menor valor entre o saldo e o que sobra do limite. Somando todos os clientes, tenho o alvo. Se a conta tiver pelo menos isso, nenhum Pix falha, mesmo que todo mundo mande tudo ao mesmo tempo.

Exemplo: Ana tem 70 e limite 50, pode mandar 50. Bia tem 20 e limite 50, pode mandar 20. Caio tem 90, limite 50 e já mandou 40 hoje, pode mandar 10. O alvo é 80.

Um job roda a cada 5 minutos. Se a conta estiver abaixo do alvo, por pouco que seja, completa com dinheiro de Recursos de Clientes. Se estiver acima, devolve a sobra, mas só quando ela passa de R$ 100, para não ficar mexendo o tempo todo. Ou seja: falta nunca é tolerada; sobra pequena é.

## O fechamento das 18h

Aqui o enunciado tem um conflito. A regra principal diz que o total dos clientes é igual a Recursos de Clientes mais Pagamentos Instantâneos. O requisito 2 diz que, às 18h, Recursos de Clientes sozinho é igual a esse total. Juntando as duas, às 18h Pagamentos Instantâneos tem que estar zerada. Mas o requisito 1 diz que essa conta nunca pode ficar sem dinheiro para um Pix legítimo, e o Pix funciona 24 horas.

Resolvi assim: às 18h, Pagamentos Instantâneos é zerada, a igualdade do requisito 2 é conferida e gravada, e logo em seguida a Reserva Financeira coloca dinheiro de volta na conta. Esse dinheiro é da instituição, não dos clientes, então a partir daí a regra principal passa a ser: total dos clientes = Recursos de Clientes + Pagamentos Instantâneos - o que a Reserva emprestou. Isso muda a regra do enunciado. É uma decisão minha, e pode ser discutida. Não guardo o empréstimo à parte: ele é o capital da Reserva menos o saldo atual dela.

Também assumi que, às 18h, o dinheiro de Recursos de Clientes vai para custódia (aplicado, por exemplo) e só volta às 8h. Sem isso, o requisito 2 seria só uma fotografia das 18h: logo depois, o job poderia puxar dinheiro de Recursos de Clientes de novo, e a Reserva nem seria necessária, o que deixaria tudo mais simples. Escolhi a versão com custódia porque ela dá um motivo para a regra das 18h e para a Reserva existir, mas as duas leituras são possíveis. Os dois horários são configuráveis.

Às 8h, o job primeiro devolve o empréstimo para a Reserva e só depois completa Pagamentos Instantâneos. A ordem importa: se completasse primeiro, poderia não sobrar dinheiro para devolver, e a Reserva não estaria cheia na noite seguinte.

Cada fechamento fica gravado numa tabela, um registro por dia, com o total dos clientes, o valor de Recursos de Clientes e se os dois bateram. O fechamento só roda a partir das 18h: se alguém chamar antes, ele recusa, para não gastar o registro do dia antes da hora.

## Operações internas

São dois jobs, o de reabastecimento e o de fechamento, que também podem ser chamados à mão. Para acompanhar, existe o `GET /internal/invariants`. Ele mostra os saldos das contas internas, o alvo, o empréstimo da Reserva e confere cinco coisas: cada livro soma zero, a regra principal, Pagamentos Instantâneos cobre o alvo e o empréstimo não é negativo. Cada item vem com uma frase explicando, e o resultado geral é OK ou ATTENTION.

## Endpoints

- `POST /customers`: abre uma conta.
- `GET /customers/{id}/balance`: saldo.
- `GET` e `PUT /customers/{id}/pix-limit`: consulta e altera o limite.
- `POST /pix/transfers`: envia um Pix.
- `POST /pix/incoming`: recebe um Pix (simula a fila).
- `GET /internal/invariants`, `POST /internal/rebalance`, `POST /internal/close-of-day`: operações internas.

Os valores são em centavos. Os erros vêm em formato padrão (RFC 9457): saldo insuficiente ou limite estourado dão 422; EndToEndId repetido com outros dados e fechamento antes da hora dão 409.

## Tecnologias

- Java 25 e Spring Boot 4, que é a stack com que trabalho.
- PostgreSQL, porque o ledger precisa gravar todas as linhas de uma operação juntas, recusar chave repetida e travar a conta enquanto confere o saldo. Isso um banco relacional faz bem. As regras mais importantes também estão no banco (nenhuma linha com valor zero, chave única, chaves estrangeiras).
- Flyway para criar as tabelas.
- Testcontainers para testar com um Postgres de verdade, principalmente a parte de concorrência.

## Testes

Há testes das regras sem banco e testes com banco real. Os principais: dois Pix ao mesmo tempo com saldo para um só (um passa e o outro é recusado; sem a trava, o teste falha), o mesmo Pix enviado duas vezes (e o mesmo EndToEndId com outro valor, que é recusado), o limite com aumento pendente e um dia inteiro simulado, do meio-dia até a manhã seguinte, conferindo a regra principal em cada etapa.

## O que ficou de fora

- Hoje a atualização do livro interno acontece dentro da aplicação, logo depois do lado do cliente. Se a aplicação cair entre um e outro, o livro interno fica faltando, e nada refaz sozinho: se o cliente reenviar o mesmo Pix, a resposta é a transação já gravada, mas o livro interno continua faltando. O `/internal/invariants` mostra a diferença. A correção seria gravar um aviso numa tabela do próprio Postgres, na mesma transação do cliente, e ter um job que processa os avisos pendentes até dar certo (padrão outbox).
- O Pix não confere se Pagamentos Instantâneos tem dinheiro na hora; a cobertura depende do job. Entre duas rodadas pode faltar, por exemplo à meia-noite, quando os limites diários renovam e o alvo sobe de uma vez, ou se a Reserva não tiver capital suficiente. Nesses casos a conta pode ficar negativa no livro, e o painel mostra ATTENTION. Para garantir de verdade, cada Pix teria que reservar o valor nessa conta antes de ser autorizado, o que faz todo Pix passar pela mesma trava (ver o item do caixa único, abaixo).
- Um Pix feito exatamente durante o fechamento pode fazer a comparação das 18h dar diferente sem erro nenhum, porque o lado do cliente já mudou e o lado interno ainda não. A correção seria um corte: o fechamento espera os Pix em andamento terminarem, aplica o que estiver pendente no livro interno e só então compara.
- Limite noturno do Pix (20h às 6h), que diminuiria o quanto a Reserva precisa emprestar.
- Todo Pix, de qualquer cliente, passa pela conta de Pagamentos Instantâneos, e ela é travada a cada atualização do livro interno. É como uma loja com um caixa só: com muito volume, forma fila. A saída seria dividir essa conta em várias (por exemplo, dez), cada Pix usando uma delas, com o saldo total sendo a soma de todas.
- Fins de semana e feriados. Assumi que o dinheiro de Recursos de Clientes volta todo dia às 8h, mas em fim de semana e feriado ele provavelmente não volta, e a Reserva teria que cobrir de sexta às 18h até segunda às 8h.
- Os endpoints internos estão abertos. Em produção, só o time interno poderia chamar, por exemplo, o fechamento do dia.
- O `/internal/invariants` mostra quando algo não bate, mas só para quem olha. Em produção, ele avisaria a equipe sozinho.

## Sobre o uso de IA

Usei IA (Claude) como apoio para escrever parte do código e para revisar. As regras de negócio e as decisões de design foram minhas, e testei cada uma delas.
