package projeto1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class ClienteHandler {
    private static final AtomicInteger contadorId = new AtomicInteger(1);

    private final int id;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private boolean ativo = true;

    public ClienteHandler(Socket socket) throws IOException {
        this.id = contadorId.getAndIncrement();
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public int getId() {
        return id;
    }

    public boolean isAtivo() {
        return ativo && !socket.isClosed();
    }

    public void enviarMensagem(String mensagem) {
        out.println(mensagem);
    }

    public String lerJogadaComTimeout(int timeoutMs) {
        try {
            socket.setSoTimeout(timeoutMs);
            String linha = in.readLine();

            if (linha == null) {
                ativo = false;
                return "";
            }

            linha = linha.trim();

            if (linha.startsWith("GUESS ")) {
                return linha.substring(6).trim();
            }

            if (linha.equals("GUESS")) {
                return "";
            }

            return "";
        } catch (SocketTimeoutException e) {
            return "";
        } catch (IOException e) {
            ativo = false;
            return "";
        }
    }

    public void fechar() {
        ativo = false;

        try {
            in.close();
        } catch (IOException ignored) {
        }

        try {
            out.close();
        } catch (Exception ignored) {
        }

        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}