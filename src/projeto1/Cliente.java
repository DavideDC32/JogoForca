package projeto1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    private static final String HOST = "127.0.0.1";
    private static final int PORTA = 5432;
    private static final int MAX_TENTATIVAS = 10;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private boolean running = true;
    private int meuId;

    public Cliente() {
        try {
            socket = new Socket(HOST, PORTA);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            scanner = new Scanner(System.in);

            System.out.println("Ligado ao servidor em " + HOST + ":" + PORTA);

            String mensagem;
            while (running && (mensagem = in.readLine()) != null) {
                tratarMensagemServidor(mensagem);
            }

            System.out.println("Ligação terminada.");

        } catch (IOException e) {
            System.out.println("Erro no cliente: " + e.getMessage());
        } finally {
            fecharRecursos();
        }
    }

    private void tratarMensagemServidor(String mensagem) {
        String[] partes = mensagem.split(" ");

        if (partes.length == 0) {
            return;
        }

        switch (partes[0]) {
            case "WELCOME":
                tratarWelcome(partes);
                break;

            case "START":
                tratarStart(partes);
                break;

            case "ROUND":
                tratarRound(partes);
                break;

            case "STATE":
                tratarState(partes);
                break;

            case "END":
                tratarEnd(partes);
                running = false;
                break;

            case "FULL":
                System.out.println("Servidor cheio ou jogo já iniciado. Ligação recusada.");
                running = false;
                break;

            default:
                System.out.println("Mensagem desconhecida do servidor: " + mensagem);
                break;
        }
    }

    private void tratarWelcome(String[] partes) {
        if (partes.length >= 3) {
            meuId = Integer.parseInt(partes[1]);
            String total = partes[2];

            System.out.println("Bem-vindo, jogador " + meuId + ".");
            System.out.println("Jogadores ligados neste momento: " + total);
        } else {
            System.out.println("Mensagem WELCOME inválida.");
        }
    }

    private void tratarStart(String[] partes) {
        if (partes.length >= 4) {
            String mask = partes[1];
            int attempts = Integer.parseInt(partes[2]);
            String timeout = partes[3];

            System.out.println("\n=== JOGO INICIADO ===");
            System.out.println("Palavra: " + mask);
            System.out.println("Forca:");
            System.out.println(obterDesenhoForca(attempts));
            System.out.println("Timeout por ronda: " + timeout + " ms");
        } else {
            System.out.println("Mensagem START inválida.");
        }
    }

    private void tratarRound(String[] partes) {
        if (partes.length >= 5) {
            String ronda = partes[1];
            String mask = partes[2];
            int attempts = Integer.parseInt(partes[3]);
            String usedLetters = partes[4];

            System.out.println("\n--- RONDA " + ronda + " ---");
            System.out.println("Palavra: " + mask);
            System.out.println("Forca:");
            System.out.println(obterDesenhoForca(attempts));
            System.out.println("Letras usadas: " + ("-".equals(usedLetters) ? "(nenhuma)" : usedLetters));

            jogarRodada();
        } else {
            System.out.println("Mensagem ROUND inválida.");
        }
    }

    private void tratarState(String[] partes) {
        if (partes.length >= 4) {
            String mask = partes[1];
            int attempts = Integer.parseInt(partes[2]);
            String usedLetters = partes[3];

            System.out.println("\nEstado atualizado:");
            System.out.println("Palavra: " + mask);
            System.out.println("Forca:");
            System.out.println(obterDesenhoForca(attempts));
            System.out.println("Letras usadas: " + ("-".equals(usedLetters) ? "(nenhuma)" : usedLetters));
        } else {
            System.out.println("Mensagem STATE inválida.");
        }
    }

    private void tratarEnd(String[] partes) {
        if (partes.length >= 3) {
            if ("WIN".equals(partes[1]) && partes.length >= 4) {
                String vencedores = partes[2];
                String palavra = partes[3];

                boolean souVencedor = false;
                String[] listaVencedores = vencedores.split(",");

                for (String id : listaVencedores) {
                    if (Integer.parseInt(id.trim()) == meuId) {
                        souVencedor = true;
                        break;
                    }
                }

                System.out.println("\n=== FIM DO JOGO ===");
                System.out.println("Resultado: " + (souVencedor ? "WIN" : "LOSE"));
                System.out.println("Vencedores: " + vencedores);
                System.out.println("Palavra correta: " + palavra);
                System.out.println("Forca final:");
                System.out.println(obterDesenhoForca(souVencedor ? MAX_TENTATIVAS : 0));

            } else if ("LOSE".equals(partes[1]) && partes.length >= 3) {
                String palavra = partes[2];

                System.out.println("\n=== FIM DO JOGO ===");
                System.out.println("Resultado: LOSE");
                System.out.println("Palavra correta: " + palavra);
                System.out.println("Forca final:");
                System.out.println(obterDesenhoForca(0));

            } else {
                System.out.println("Mensagem END inválida.");
            }
        }
    }

    private void jogarRodada() {
        System.out.print("Introduza uma letra ou a palavra completa: ");
        String tentativa = scanner.nextLine().trim();
        out.println("GUESS " + tentativa);
    }

    private String obterDesenhoForca(int tentativasRestantes) {
        int erros = MAX_TENTATIVAS - tentativasRestantes;

        char[][] g = {
            "       ".toCharArray(),
            "       ".toCharArray(),
            "       ".toCharArray(),
            "       ".toCharArray(),
            "       ".toCharArray(),
            "       ".toCharArray(),
            "       ".toCharArray()
        };

        if (erros >= 1) {
            g[6][0] = '=';
            g[6][1] = '=';
            g[6][2] = '=';
            g[6][3] = '=';
            g[6][4] = '=';
        }

        if (erros >= 2) {
            g[1][0] = '|';
            g[2][0] = '|';
            g[3][0] = '|';
            g[4][0] = '|';
            g[5][0] = '|';
        }

        if (erros >= 3) {
            g[0][0] = '+';
            g[0][1] = '-';
            g[0][2] = '-';
            g[0][3] = '-';
            g[0][4] = '+';
        }

        if (erros >= 4) {
            g[1][4] = '|';
        }

        if (erros >= 5) {
            g[2][4] = 'O';
        }

        if (erros >= 6) {
            g[3][4] = '|';
        }

        if (erros >= 7) {
            g[3][3] = '/';
        }

        if (erros >= 8) {
            g[3][5] = '\\';
        }

        if (erros >= 9) {
            g[4][3] = '/';
        }

        if (erros >= 10) {
            g[4][5] = '\\';
        }

        StringBuilder sb = new StringBuilder();
        for (char[] linha : g) {
            sb.append(new String(linha)).append("\n");
        }

        return sb.toString();
    }

    private void fecharRecursos() {
        try {
            if (scanner != null) {
                scanner.close();
            }
        } catch (Exception ignored) {
        }

        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception ignored) {
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public static void main(String[] args) {
        new Cliente();
    }
}