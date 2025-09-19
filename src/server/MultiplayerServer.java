package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MultiplayerServer {
    private static final int PORT = 44444;
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static final Map<String, ClientHandler> usernameMap = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> invites = new ConcurrentHashMap<>();
    private static volatile String gameHost = null;
    private static volatile boolean gameStarted = false;

    // Thread pool for client handlers (max 100 concurrent for safety)
    private static final ExecutorService pool = Executors.newFixedThreadPool(100);

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Multiplayer Server (Lobby+Invite+Start) running on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);
            pool.submit(handler);
        }
    }

    // Broadcast to ALL clients
    private static void broadcast(String msg) {
        for (ClientHandler c : clients) c.send(msg);
    }

    private static void broadcastPlayerList() {
        StringBuilder sb = new StringBuilder("PLAYERS:");
        List<String> names = new ArrayList<>();
        for (ClientHandler c : clients) {
            if (c.username != null && !c.username.isEmpty()) names.add(c.username);
        }
        sb.append(String.join(",", names));
        broadcast(sb.toString());
    }

    private static void broadcastStartGame(String host) {
        broadcast("START_GAME:" + host);
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username = "";

        ClientHandler(Socket socket) { this.socket = socket; }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String line;
                while ((line = in.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) break;

                    if (line.startsWith("LOGIN:")) {
                        username = line.substring(6).trim();
                        usernameMap.put(username, this);
                        broadcastPlayerList();
                    }
                    else if (line.startsWith("INVITE:")) {
                        String[] parts = line.split(":");
                        if (parts.length == 3) {
                            String from = parts[1];
                            String to = parts[2];
                            invites.computeIfAbsent(from, k -> new HashSet<>()).add(to);
                            ClientHandler invitee = usernameMap.get(to);
                            if (invitee != null) invitee.send("INVITED_BY:" + from);
                        }
                    }
                    else if (line.startsWith("INVITE_ACCEPT:")) {
                        String[] parts = line.split(":");
                        if (parts.length == 3) {
                            String inviter = parts[1], invitee = parts[2];
                            ClientHandler inviterHandler = usernameMap.get(inviter);
                            if (inviterHandler != null) inviterHandler.send("INVITE_ACCEPTED:" + invitee);
                        }
                    }
                    else if (line.startsWith("START_GAME:")) {
                        String host = line.substring(11).trim();
                        gameHost = host;
                        gameStarted = true;
                        broadcastStartGame(host);
                    }
                    else if (line.startsWith("POS:")) {
                        broadcast(line);
                    }
                    else if (line.startsWith("LOGOUT")) {
                        break;
                    }
                }
            } catch (IOException ignored) {}
            finally {
                cleanup();
            }
        }

        void send(String msg) {
            if (out != null) out.println(msg);
        }

        private void cleanup() {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { socket.close(); } catch (IOException ignored) {}
            clients.remove(this);
            if (!username.isEmpty()) usernameMap.remove(username);
            broadcastPlayerList();
        }
    }
}