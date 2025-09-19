package main.network;

import java.io.*;
import java.net.*;

public class NetworkClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;

    // --- THIS IS YOUR INTERFACE ---
    public interface MessageListener {
        void onMessage(String msg);
    }
    private MessageListener listener;

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port, String username, MessageListener listener) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        this.listener = listener;

        out.println("LOGIN:" + username);

        listenerThread = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (this.listener != null) {
                        this.listener.onMessage(msg);
                    }
                }
            } catch (IOException ignored) {}
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void send(String msg) {
        if (out != null) out.println(msg);
    }

    public void disconnect() {
        try {
            if (out != null) out.println("LOGOUT");
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        if (listenerThread != null && listenerThread.isAlive()) {
            listenerThread.interrupt();
        }
        socket = null;
        in = null;
        out = null;
        listenerThread = null;
    }
}