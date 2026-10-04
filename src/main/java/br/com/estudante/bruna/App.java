package br.com.estudante.bruna;

import br.com.estudante.bruna.console.Menu;
import br.com.estudante.bruna.repositorio.ContaRepositorioEmMemoria;
import br.com.estudante.bruna.servico.BancoServico;

import java.time.Clock;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        var banco = new BancoServico(new ContaRepositorioEmMemoria(), Clock.systemUTC());
        // System.console() existe quando rodando em terminal real: permite digitar a senha sem eco.
        new Menu(banco, new Scanner(System.in), System.out, System.console()).executar();
    }
}
