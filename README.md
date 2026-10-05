# Sistema Bancário — Java + SQLite (JDBC)

![Testes](https://github.com/ViniciuscLemos/sistema-bancario-java/actions/workflows/testes.yml/badge.svg)

Sistema bancário via terminal com abertura de contas, depósitos, saques, transferências, extrato e encerramento de contas. Usa SQLite como banco de dados via JDBC.

## Tecnologias
- **Java 17+** — linguagem principal
- **SQLite** — banco de dados embutido
- **JDBC** — API Java para banco de dados relacional
- **Maven** — build e dependências (baixa o driver `sqlite-jdbc` sozinho)
- **JUnit 5** — testes automatizados

## O que você vai aprender com este projeto
- Orientação a Objetos: encapsulamento, enums, objetos imutáveis
- Padrão de camadas: Model → Repository → Service → Main (UI)
- JDBC: conexão com banco, PreparedStatement, ResultSet
- **Transações de banco** (commit/rollback): uma transferência grava tudo ou nada
- **BigDecimal para dinheiro** — `double` tem erros de arredondamento (`0.1 + 0.2 = 0.30000000000000004`)
- Validação de CPF pelos dígitos verificadores
- Optional para evitar NullPointerException
- Switch com `->` (Java 14+) e text blocks (Java 15+)
- Testes de integração com um banco SQLite real

## Pré-requisitos
- JDK 17 ou superior
- Maven 3.8+ (`mvn -v` para verificar)

## Como compilar e rodar

```bash
# Compila, roda os testes e gera um .jar executável com o driver incluído
mvn package

# Executa
java -jar target/sistema-bancario.jar

# Opcional: usar outro arquivo de banco
java -jar target/sistema-bancario.jar meu-banco.db
```

Na primeira execução, duas contas de exemplo são criadas (com uma transferência entre elas).

## Testes
```bash
mvn test
```
Os testes usam um arquivo SQLite temporário e cobrem as regras de negócio, a validação de CPF, a formatação de valores e a persistência entre execuções. Rodam automaticamente no GitHub Actions a cada push.

## Funcionalidades
- Abrir conta (corrente ou poupança) com depósito inicial — CPF validado e único
- Depositar valor em uma conta
- Sacar valor com verificação de saldo
- Transferir entre contas (atômico: ou tudo é gravado, ou nada)
- Ver extrato com histórico de transações
- Listar todas as contas e o saldo total
- Encerrar conta (exige saldo zerado)
- Valores aceitos como `1500`, `1500,50`, `1.500,50` ou `1500.50`; exibidos como `R$ 1.500,50`

## Correções da versão 1.1
- **Saldo não era salvo após reiniciar:** ao ler uma conta do banco, o repositório criava um objeto com ID novo, e os `UPDATE` não encontravam a conta. Agora o ID e a data originais são preservados.
- **Segunda execução quebrava:** IDs de transação vinham de um contador que voltava a `TXN-001` a cada execução, colidindo com a chave primária. Agora são UUIDs.
- **Transferências duplicadas no extrato:** cada transferência aparecia duas vezes. Agora cada transação pertence a uma conta só.
- **Transferência sem transação de banco:** uma falha no meio podia deixar dinheiro "sumido". Agora usa commit/rollback.

## Estrutura do projeto
```
pom.xml                                  # Dependências e build (Maven)
src/main/java/banco/
├── Main.java                            # Menu CLI e ponto de entrada
├── model/
│   ├── Conta.java                       # Entidade conta com regras de negócio
│   ├── Transacao.java                   # Registro imutável de cada operação
│   └── Moeda.java                       # Formatação e leitura de valores em reais
├── repository/
│   └── BancoRepository.java             # Acesso ao banco via JDBC + transações
└── service/
    ├── BancoService.java                # Lógica de negócio e orquestração
    └── Cpf.java                         # Validação de CPF
src/test/java/banco/
├── BancoServiceTest.java                # Testes de integração (SQLite real)
└── ModelTest.java                       # Testes unitários
```

## Arquitetura em camadas
```
Main (UI) → BancoService (regras) → BancoRepository (SQL) → SQLite
```
Cada camada tem uma responsabilidade única — isso facilita testes e manutenção.
