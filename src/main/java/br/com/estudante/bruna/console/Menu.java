package br.com.estudante.bruna.console;

import br.com.estudante.bruna.dominio.Conta;
import br.com.estudante.bruna.dominio.OperacaoInvalidaException;
import br.com.estudante.bruna.dominio.Transacao;
import br.com.estudante.bruna.servico.BancoServico;

import java.io.Console;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/** Interface de console. Entrada/saída injetadas (testável); a senha não aparece na tela quando há terminal. */
public class Menu {

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final BancoServico banco;
    private final Scanner entrada;
    private final PrintStream saida;
    private final Console terminal;

    public Menu(BancoServico banco, Scanner entrada, PrintStream saida, Console terminal) {
        this.banco = banco;
        this.entrada = entrada;
        this.saida = saida;
        this.terminal = terminal;
    }

    public void executar() {
        while (true) {
            saida.println("""

                    ==========================
                          Banco BBSC
                    ==========================
                    1 - Entrar
                    2 - Abrir conta
                    3 - Sair""");
            String opcao = perguntar("Escolha uma opção: ");
            if (opcao == null) return;
            try {
                switch (opcao.trim()) {
                    case "1" -> entrar();
                    case "2" -> abrirConta();
                    case "3" -> {
                        saida.println("Encerrando. Obrigado!");
                        return;
                    }
                    default -> saida.println("Opção inválida.");
                }
            } catch (OperacaoInvalidaException e) {
                saida.println("Não foi possível concluir: " + e.getMessage());
            }
        }
    }

    private void abrirConta() {
        String nome = perguntar("Nome completo: ");
        String cpf = perguntar("CPF: ");
        char[] senha = lerSenha("Senha (mínimo 6 caracteres): ");
        char[] confirmacao = lerSenha("Confirme a senha: ");
        if (nome == null || cpf == null || senha == null || confirmacao == null) return;
        if (!java.util.Arrays.equals(senha, confirmacao)) {
            saida.println("As senhas não coincidem.");
            return;
        }
        Conta conta = banco.abrirConta(nome, cpf, senha);
        saida.println("Conta aberta! Agência " + Conta.AGENCIA + ", conta " + conta.numero() + ".");
    }

    private void entrar() {
        String cpf = perguntar("CPF: ");
        char[] senha = lerSenha("Senha: ");
        if (cpf == null || senha == null) return;
        Conta conta = banco.entrar(cpf, senha);
        saida.println("Olá, " + conta.nomeTitular() + "! Agência " + Conta.AGENCIA + ", conta " + conta.numero());
        menuDaConta(conta);
    }

    private void menuDaConta(Conta conta) {
        while (true) {
            saida.println("""

                    === Sua conta ===
                    1 - Saldo
                    2 - Extrato
                    3 - Sacar
                    4 - Depositar
                    5 - Transferir
                    6 - Sair da conta""");
            String opcao = perguntar("Escolha uma opção: ");
            if (opcao == null) return;
            try {
                switch (opcao.trim()) {
                    case "1" -> saida.println("Saldo: R$ " + conta.saldo());
                    case "2" -> extrato(conta);
                    case "3" -> {
                        conta.sacar(lerValor("Valor do saque: "));
                        saida.println("Saque realizado. Saldo: R$ " + conta.saldo());
                    }
                    case "4" -> {
                        conta.depositar(lerValor("Valor do depósito: "));
                        saida.println("Depósito realizado. Saldo: R$ " + conta.saldo());
                    }
                    case "5" -> {
                        String cpfDestino = perguntar("CPF do destinatário: ");
                        BigDecimal valor = lerValor("Valor: ");
                        banco.transferir(conta, cpfDestino, valor);
                        saida.println("Transferência realizada. Saldo: R$ " + conta.saldo());
                    }
                    case "6" -> {
                        return;
                    }
                    default -> saida.println("Opção inválida.");
                }
            } catch (OperacaoInvalidaException e) {
                saida.println("Não foi possível concluir: " + e.getMessage());
            }
        }
    }

    private void extrato(Conta conta) {
        if (conta.extrato().isEmpty()) {
            saida.println("Nenhuma movimentação.");
            return;
        }
        for (Transacao t : conta.extrato()) {
            saida.printf("%s  %-22s R$ %10s  saldo R$ %10s%n", DATA.format(t.data()), t.tipo(), t.valor(), t.saldoApos());
        }
    }

    /** Aceita "10,50" ou "10.50". Texto inválido vira erro de operação (e não derruba o programa). */
    private BigDecimal lerValor(String pergunta) {
        String texto = perguntar(pergunta);
        if (texto == null) throw new OperacaoInvalidaException("Entrada encerrada.");
        try {
            return new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new OperacaoInvalidaException("Valor inválido.");
        }
    }

    private String perguntar(String texto) {
        saida.print(texto);
        return entrada.hasNextLine() ? entrada.nextLine() : null;
    }

    private char[] lerSenha(String texto) {
        if (terminal != null) {
            return terminal.readPassword(texto);
        }
        String linha = perguntar(texto);
        return linha == null ? null : linha.toCharArray();
    }
}
