# Sistema Bancário — Java + SQLite (JDBC)

Sistema bancário via terminal com abertura de contas, depósitos, saques, transferências e extrato. Usa SQLite como banco de dados via JDBC.

## Tecnologias
- **Java 17+** — linguagem principal
- **SQLite** — banco de dados embutido
- **JDBC** — API Java para banco de dados relacional
- **sqlite-jdbc** — driver JDBC para SQLite

## O que você vai aprender com este projeto
- Orientação a Objetos: encapsulamento, herança, enums
- Padrão de camadas: Model → Repository → Service → Main (UI)
- JDBC: conexão com banco, PreparedStatement, ResultSet
- Optional para evitar NullPointerException
- Switch expressions (Java 14+)
- Exceções customizadas com mensagens claras

## Pré-requisitos
- JDK 17 ou superior
- Download do driver JDBC SQLite:
  https://github.com/xerial/sqlite-jdbc/releases → baixe o `.jar`

## Como compilar e rodar

### 1. Baixe o driver JDBC
```bash
# Crie a pasta lib e baixe o driver
mkdir lib
# Baixe manualmente o sqlite-jdbc-3.x.x.jar do link acima e coloque em lib/
```

### 2. Compile
```bash
# Linux/macOS
javac -cp "lib/*" -d out src/banco/model/*.java src/banco/repository/*.java src/banco/service/*.java src/banco/Main.java

# Windows
javac -cp "lib\*" -d out src\banco\model\*.java src\banco\repository\*.java src\banco\service\*.java src\banco\Main.java
```

### 3. Execute
```bash
# Linux/macOS
java -cp "out:lib/*" banco.Main

# Windows
java -cp "out;lib\*" banco.Main
```

## Funcionalidades
- Abrir conta (corrente ou poupança) com depósito inicial
- Depositar valor em uma conta
- Sacar valor com verificação de saldo
- Transferir entre contas com registro duplo
- Ver extrato completo com histórico de transações
- Listar todas as contas cadastradas

## Estrutura do projeto
```
src/
└── banco/
    ├── Main.java                       # Menu CLI e ponto de entrada
    ├── model/
    │   ├── Conta.java                  # Entidade conta com regras de negócio
    │   └── Transacao.java              # Registro imutável de cada operação
    ├── repository/
    │   └── BancoRepository.java        # Acesso ao banco via JDBC
    └── service/
        └── BancoService.java           # Lógica de negócio e orquestração
```

## Arquitetura em camadas
```
Main (UI) → BancoService (regras) → BancoRepository (SQL) → SQLite
```
Cada camada tem uma responsabilidade única — isso facilita testes e manutenção.
