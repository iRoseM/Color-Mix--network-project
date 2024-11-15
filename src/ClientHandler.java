import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private ArrayList<ClientHandler> clients;

    private String username;

    public ClientHandler(Socket s, ArrayList<ClientHandler> clients) throws IOException {
        this.client = s;
        this.clients = clients;
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);

        // Receive the username as the first message from the client
        this.username = in.readLine();  // Read the username from the client
        System.out.println("Username received: " + username);  // Print for debugging
    }

    @Override
 public void run() {
    String clientMessage;
        try {
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("Received from " + username + ": " + clientMessage);

                // Handle playing client updates
                if (clientMessage.startsWith("PLAY:")) {
                    handlePlayRequest(clientMessage.substring(5));
                }

                // Handle client disconnection
                if (clientMessage.equalsIgnoreCase("DISCONNECT")) {
                    disconnectClient();
                    break;
                }

                // Handle answers
                if (clientMessage.startsWith("ANSWER:")) {
                    handleAnswer(clientMessage.substring(7));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanupResources();
        }
    }

    private void handlePlayRequest(String playerName) {
        synchronized (Server.playingClients) {
            if (Server.playingClients.size() < 3) {
                if (!Server.playingClients.contains(playerName)) {
                    Server.playingClients.add(playerName);
                }
                Server.broadcastPlayingPlayers();
            } else {
                sendMessage("ROOM_FULL");
            }

            if (Server.playingClients.size() >= 2) {
                Server.startGame();
            }
        }
    }

    private void handleAnswer(String playerAnswer) {
        synchronized (Server.class) {
            if (playerAnswer.equalsIgnoreCase(Server.answers[Server.currentQuestionIndex])) {
                int playerIndex = Server.playingClients.indexOf(username);

                if (playerIndex != -1 && playerIndex < Server.playerScores.length) {
                    Server.playerScores[playerIndex] += 10;
                    Server.broadcastScores();

                    Server.currentQuestionIndex++;
                    if (Server.currentQuestionIndex < Server.questions.length) {
                        Server.broadcastQuestion();
                    } else {
                        Server.endGame();
                    }
                }
            } else {
                sendMessage("WRONG_ANSWER");
            }
        }
    }

    private void disconnectClient() {
        try {
            // Remove from clients list and notify others
            synchronized (clients) {
                clients.remove(this);
                broadcast(username + " has left the game.");
                playerList();
            }

            // Remove from playing clients list
            synchronized (Server.playingClients) {
                if (Server.playingClients.contains(username)) {
                    Server.playingClients.remove(username);
                    Server.broadcastPlayingPlayers();
                }
            }

            System.out.println(username + " disconnected.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cleanupResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (client != null) client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private void playerList() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler client : clients) {
            sb.append(client.username).append(",");
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // Remove trailing comma
        }

        broadcast("Connected users: " + sb.toString());
    }

    public String getUsername() {
        return username;
    }
}