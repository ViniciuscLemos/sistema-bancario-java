# Sistema Bancário

![Testes](https://github.com/ViniciuscLemos/sistema-bancario-java/actions/workflows/testes.yml/badge.svg)

Um banco simples que roda no terminal, feito em Java com SQLite (JDBC).

Dá pra abrir conta, depositar, sacar, transferir entre contas, ver o extrato e encerrar uma conta. Os dados ficam salvos num arquivo `banco.db`, então continuam lá quando você abre o programa de novo.

## Rodando

Precisa do JDK 17 e do Maven.

```bash
mvn package
java -jar target/sistema-bancario.jar
```

Na primeira vez ele cria duas contas de exemplo pra você testar.

Pra rodar só os testes:

```bash
mvn test
```

## Como fica

Uma transferência e o extrato de quem recebeu:

```
--- TRANSFERÊNCIA ---
ID da conta de origem: 6C10938A
  Maria Silva
ID da conta de destino: 2CE77D9C
  João Santos
Valor: R$ 250
Transferir R$ 250,00 de Maria Silva para João Santos? (s/N): s

Transferência realizada com sucesso!

--- EXTRATO ---
ID da conta: 2CE77D9C
  João Santos

Extrato da conta 2CE77D9C (João Santos)
------------------------------------------------------------------------------------------
08/10/2026 09:55:56  +     R$ 250,00  Saldo:    R$ 1.350,00  | Transferência de 6C10938A (Maria Silva)
08/10/2026 09:55:01  +     R$ 300,00  Saldo:    R$ 1.100,00  | Transferência de 6C10938A (Maria Silva)
08/10/2026 09:55:01  +     R$ 800,00  Saldo:      R$ 800,00  | Depósito inicial na abertura da conta
------------------------------------------------------------------------------------------
Saldo atual: R$ 1.350,00
```

Depois de digitar o ID, o programa já mostra de quem é a conta. Se errar o ID, você descobre na hora, e não depois de digitar o valor. E a transferência só acontece depois de confirmar vendo o nome de quem vai receber.

No Windows, se os acentos aparecerem estranhos no terminal, roda `chcp 65001` antes.

## Algumas decisões

- Usei `BigDecimal` pros valores em vez de `double`, porque com `double` aparecem erros de arredondamento (tipo `0.1 + 0.2` dar `0.30000000000000004`).
- A transferência roda numa transação do banco: ou as duas contas são atualizadas, ou nenhuma é.
- O CPF é validado pelos dígitos verificadores, e não dá pra abrir duas contas no mesmo CPF.
- Na hora de digitar valores, dá pra usar `1500`, `1500,50` ou `1.500,50`.

## Estrutura

Separei em camadas:

```
Main (menu) → BancoService (regras) → BancoRepository (SQL) → SQLite
```

```
src/main/java/banco/
  Main.java
  model/        Conta, Transacao, Moeda
  repository/   BancoRepository
  service/      BancoService, Cpf
src/test/java/banco/
```
