# Sistema Bancário BBSC

[![CI](https://github.com/Brunacoelhob/estudo-java-sistema-bancario/actions/workflows/ci.yml/badge.svg)](https://github.com/Brunacoelhob/estudo-java-sistema-bancario/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/java-21-orange)
![License](https://img.shields.io/badge/license-MIT-blue)

Simulador bancário de console em Java para praticar Programação Orientada a Objetos, com regras de domínio tratadas como em um sistema real: dinheiro exato, senhas protegidas, transferências atômicas e testes automatizados.

## Funcionalidades

- Abrir conta (nome, CPF validado e senha) com número de conta único
- Entrar com CPF e senha (bloqueio temporário após 5 erros)
- Saldo, **extrato**, saque, depósito e transferência por CPF

## Arquitetura

```
console/Menu ──► servico/BancoServico ──► repositorio/ContaRepositorio ──► (em memória)
 (entrada/saída)   (abrir conta, login,        │
                    transferir)                ▼
                                       dominio/ Conta · Cpf · SenhaHash · Transacao
```

| Camada | Responsabilidade |
|---|---|
| `dominio` | Regras que valem sempre: valor positivo com até 2 casas, saldo nunca negativo, CPF válido, hash de senha |
| `servico` | Casos de uso e segurança de acesso (bloqueio por tentativas) |
| `repositorio` | Armazenamento (interface + implementação em memória, trocável) |
| `console` | Só conversa com o usuário; entrada/saída injetadas, por isso é testável |

## O que foi melhorado em relação à primeira versão

| Antes | Agora |
|---|---|
| Saldo em `double` (erro de arredondamento) | `BigDecimal` com 2 casas |
| Depósito/saque negativo aceito | Valor deve ser > 0, até 2 casas |
| Senha em texto puro, comparada com `equals` | PBKDF2-SHA256 com sal; comparação em tempo constante |
| CPF como `long` (perdia o zero à esquerda), sem validação | `Cpf` validado (dígitos verificadores), exibido mascarado |
| Número da conta com `Math.random()` (podia repetir) | Sequência única |
| Listas estáticas globais | Repositório injetado |
| Transferência sem proteção de concorrência | Atômica, com ordem de travas (sem *deadlock*) |
| Login revelava se o CPF existia; sem limite de tentativas | Mensagem única, mesmo tempo de resposta e bloqueio de 15 min após 5 erros |
| `InputMismatchException` derrubava o programa | Entrada inválida vira mensagem |
| Sem testes | 28 testes (incluindo concorrência) e CI |

## Como executar

Requer Java 21 e Maven.

```bash
mvn package
java -jar target/sistema-bancario-2.0.0.jar
```

Em terminal real a senha é digitada **sem eco**. Os dados ficam só em memória: ao fechar o programa, as contas se perdem (é um simulador).

## Testes

```bash
mvn verify
```

## Licença

[MIT](LICENSE)
