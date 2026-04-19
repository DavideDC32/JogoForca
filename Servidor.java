package projeto1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Servidor {
    private static final int PORTA = 5432;
    private static final int MAX_JOGADORES = 4;
    private static final int MIN_JOGADORES = 2;

    private static final int LOBBY_TIMEOUT_MS = 20000;
    private static final int ROUND_TIMEOUT_MS = 25000;
    private static final int MAX_TENTATIVAS = 10;

    private static final String FICHEIRO_PALAVRAS = "palavras";

    private final List<ClienteHandler> jogadores;
    private final List<String> palavras;

    private ServerSocket servidorSocket;
    private String palavraEscolhida;
    private String mascara;
    private Set<Character> letrasUsadas;
    private int tentativas;
    private boolean jogoIniciado;
    private int numeroRonda;

    public Servidor() {
        jogadores = new ArrayList<>();
        letrasUsadas = new HashSet<>();
        tentativas = MAX_TENTATIVAS;
        jogoIniciado = false;
        numeroRonda = 1;

        palavras = carregarPalavrasDoFicheiro(FICHEIRO_PALAVRAS);

        if (palavras.isEmpty()) {
            System.out.println("Erro: não foi possível carregar palavras do ficheiro '" + FICHEIRO_PALAVRAS + "'.");
            return;
        }

        try {
            servidorSocket = new ServerSocket(PORTA);
            System.out.println("Servidor iniciado na porta " + PORTA);
            System.out.println("À espera de jogadores...");

            aceitarJogadores();
            iniciarJogo();
            jogar();

        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        } finally {
            fecharConexoes();
        }
    }

    private List<String> carregarPalavrasDoFicheiro(String nomeFicheiro) {
        List<String> lista = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(nomeFicheiro))) {
            String linha;

            while ((linha = br.readLine()) != null) {
                linha = linha.trim().toUpperCase();

                if (!linha.isEmpty()) {
                    lista.add(linha);
                }
            }

            System.out.println("Foram carregadas " + lista.size() + " palavra(s) do ficheiro '" + nomeFicheiro + "'.");

        } catch (IOException e) {
            System.out.println("Erro ao ler o ficheiro de palavras: " + e.getMessage());
        }

        return lista;
    }

    private void aceitarJogadores() throws IOException {
        long inicioLobby = -1;

        while (!jogoIniciado) {
            if (jogadores.isEmpty()) {
                Socket socket = servidorSocket.accept();
                ClienteHandler jogador = new ClienteHandler(socket);
                jogadores.add(jogador);
                jogador.enviarMensagem("WELCOME " + jogador.getId() + " " + jogadores.size());

                inicioLobby = System.currentTimeMillis();
                System.out.println("Jogador " + jogador.getId() + " entrou no lobby.");
                continue;
            }

            long tempoPassado = System.currentTimeMillis() - inicioLobby;
            long tempoRestante = LOBBY_TIMEOUT_MS - tempoPassado;

            if (jogadores.size() >= MIN_JOGADORES && tempoRestante <= 0) {
                jogoIniciado = true;
                break;
            }

            if (jogadores.size() == MAX_JOGADORES) {
                jogoIniciado = true;
                break;
            }

            int timeoutAccept = (int) Math.max(1, tempoRestante);
            servidorSocket.setSoTimeout(timeoutAccept);

            try {
                Socket socket = servidorSocket.accept();

                if (jogadores.size() >= MAX_JOGADORES || jogoIniciado) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("FULL");
                    socket.close();
                    continue;
                }

                ClienteHandler jogador = new ClienteHandler(socket);
                jogadores.add(jogador);
                jogador.enviarMensagem("WELCOME " + jogador.getId() + " " + jogadores.size());
                System.out.println("Jogador " + jogador.getId() + " entrou no lobby.");

            } catch (SocketTimeoutException e) {
                if (jogadores.size() >= MIN_JOGADORES) {
                    jogoIniciado = true;
                } else {
                    System.out.println("Timeout do lobby, mas ainda não há jogadores suficientes.");
                    System.out.println("Continua à espera até existirem pelo menos 2 jogadores...");

                    servidorSocket.setSoTimeout(0);
                    while (jogadores.size() < MIN_JOGADORES) {
                        Socket socket = servidorSocket.accept();
                        ClienteHandler jogador = new ClienteHandler(socket);
                        jogadores.add(jogador);
                        jogador.enviarMensagem("WELCOME " + jogador.getId() + " " + jogadores.size());
                        System.out.println("Jogador " + jogador.getId() + " entrou no lobby.");
                    }
                    jogoIniciado = true;
                }
            }
        }

        servidorSocket.setSoTimeout(0);
        System.out.println("Lobby terminado com " + jogadores.size() + " jogador(es).");
    }

    private void iniciarJogo() {
        palavraEscolhida = escolherPalavraAleatoria();
        mascara = "_".repeat(palavraEscolhida.length());

        for (ClienteHandler jogador : jogadores) {
            jogador.enviarMensagem("START " + mascara + " " + tentativas + " " + ROUND_TIMEOUT_MS);
        }

        System.out.println("Jogo iniciado.");
        System.out.println("Palavra escolhida: " + palavraEscolhida);
    }

    private void jogar() {
        while (tentativas > 0) {
            enviarRound();

            Map<ClienteHandler, String> jogadas = recolherJogadas();

            List<Integer> vencedores = processarRonda(jogadas);

            if (!vencedores.isEmpty()) {
                terminarJogoComVitoria(vencedores);
                return;
            }

            if (tentativas <= 0) {
                terminarJogoComDerrota();
                return;
            }

            enviarState();
            numeroRonda++;
        }

        terminarJogoComDerrota();
    }

    private void enviarRound() {
        String letrasUsadasStr = obterLetrasUsadasString();

        for (ClienteHandler jogador : jogadores) {
            jogador.enviarMensagem(
                    "ROUND " + numeroRonda + " " + mascara + " " + tentativas + " " + letrasUsadasStr
            );
        }
    }

    private Map<ClienteHandler, String> recolherJogadas() {
        Map<ClienteHandler, String> jogadas = new LinkedHashMap<>();

        for (ClienteHandler jogador : jogadores) {
            String jogada = jogador.lerJogadaComTimeout(ROUND_TIMEOUT_MS);
            jogadas.put(jogador, jogada);
            System.out.println("Jogador " + jogador.getId() + " enviou: [" + jogada + "]");
        }

        return jogadas;
    }

    private List<Integer> processarRonda(Map<ClienteHandler, String> jogadas) {
        List<Integer> vencedores = new ArrayList<>();

        for (Map.Entry<ClienteHandler, String> entry : jogadas.entrySet()) {
            ClienteHandler jogador = entry.getKey();
            String tentativa = entry.getValue();

            if (tentativa == null || tentativa.isBlank()) {
                tentativas--;
                continue;
            }

            tentativa = tentativa.trim().toUpperCase();

            if (tentativa.equals(palavraEscolhida)) {
                vencedores.add(jogador.getId());
                continue;
            }

            if (tentativa.length() == 1) {
                char letra = tentativa.charAt(0);

                if (!Character.isLetter(letra)) {
                    tentativas--;
                    continue;
                }

                if (letrasUsadas.contains(letra)) {
                    tentativas--;
                    continue;
                }

                letrasUsadas.add(letra);

                if (palavraEscolhida.indexOf(letra) >= 0) {
                    atualizarMascara(letra);
                } else {
                    tentativas--;
                }

            } else {
                boolean houveProgresso = revelarLetrasDaPalavraTentada(tentativa);

                if (!houveProgresso) {
                    tentativas--;
                }
            }
        }

        return vencedores;
    }

    private void atualizarMascara(char letra) {
        StringBuilder novaMascara = new StringBuilder(mascara);

        for (int i = 0; i < palavraEscolhida.length(); i++) {
            if (palavraEscolhida.charAt(i) == letra) {
                novaMascara.setCharAt(i, letra);
            }
        }

        mascara = novaMascara.toString();
    }

    private boolean revelarLetrasDaPalavraTentada(String tentativa) {
        StringBuilder novaMascara = new StringBuilder(mascara);
        boolean houveProgresso = false;

        int limite = Math.min(tentativa.length(), palavraEscolhida.length());

        for (int i = 0; i < limite; i++) {
            char letraTentativa = tentativa.charAt(i);
            char letraCorreta = palavraEscolhida.charAt(i);

            if (letraTentativa == letraCorreta && novaMascara.charAt(i) == '_') {
                novaMascara.setCharAt(i, letraCorreta);
                houveProgresso = true;
            }
        }

        if (houveProgresso) {
            mascara = novaMascara.toString();
        }

        return houveProgresso;
    }

    private void enviarState() {
        String letrasUsadasStr = obterLetrasUsadasString();

        for (ClienteHandler jogador : jogadores) {
            jogador.enviarMensagem("STATE " + mascara + " " + tentativas + " " + letrasUsadasStr);
        }
    }

    private void terminarJogoComVitoria(List<Integer> vencedores) {
        String idsVencedores = juntarIds(vencedores);

        for (ClienteHandler jogador : jogadores) {
            jogador.enviarMensagem("END WIN " + idsVencedores + " " + palavraEscolhida);
        }

        System.out.println("Jogo terminado com vitória.");
        System.out.println("Vencedores: " + idsVencedores);
    }

    private void terminarJogoComDerrota() {
        for (ClienteHandler jogador : jogadores) {
            jogador.enviarMensagem("END LOSE " + palavraEscolhida);
        }

        System.out.println("Jogo terminado com derrota.");
    }

    private String escolherPalavraAleatoria() {
        Random random = new Random();
        return palavras.get(random.nextInt(palavras.size()));
    }

    private String obterLetrasUsadasString() {
        if (letrasUsadas.isEmpty()) {
            return "-";
        }

        List<Character> lista = new ArrayList<>(letrasUsadas);
        Collections.sort(lista);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            sb.append(lista.get(i));
            if (i < lista.size() - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    private String juntarIds(List<Integer> ids) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    private void fecharConexoes() {
        for (ClienteHandler jogador : jogadores) {
            jogador.fechar();
        }

        try {
            if (servidorSocket != null && !servidorSocket.isClosed()) {
                servidorSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public static void main(String[] args) {
        new Servidor();
    }
}