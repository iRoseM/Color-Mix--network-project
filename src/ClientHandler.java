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
        // Continuously read messages from the client
        while ((clientMessage = in.readLine()) != null) {
            System.out.println("Received from " + username + ": " + clientMessage);
            
            // Check if the message starts with "PLAY:"
            if (clientMessage.startsWith("PLAY:")) {
                String playerName = clientMessage.substring(5);
                synchronized (Server.playingClients) {
                    // Check if the waiting list is full (maximum 3 players)
                    if (Server.playingClients.size() < 3) {
                        if (!Server.playingClients.contains(playerName)) {
                            Server.playingClients.add(playerName);
                        }
                        // Broadcast the updated list of playing clients
                        Server.broadcastPlayingPlayers();
                    } else {
                        // Send a message back to the client that the room is full
                        sendMessage("ROOM_FULL");
                    }
                }

                // Check if the game can start (minimum 2 players)
                synchronized (Server.playingClients) {
                    if (Server.playingClients.size() >= 2) {
                        Server.startGame();
                    }
                }
            }
            
            // Check if the message is a DISCONNECT command
            if (clientMessage.equalsIgnoreCase("DISCONNECT")) {
                disconnectClient();
                break;
            }

            // Check if the message is an ANSWER
            if (clientMessage.startsWith("ANSWER:")) {
                String playerAnswer = clientMessage.substring(7);
                
                synchronized (Server.class) { // Ensure thread safety when accessing shared resources
                    if (playerAnswer.equalsIgnoreCase(Server.answers[Server.currentQuestionIndex])) {
                        int playerIndex = Server.playingClients.indexOf(username);
                        
                        // Check if player is valid before updating score
                        if (playerIndex != -1 && playerIndex < Server.playerScores.length) {
                            Server.playerScores[playerIndex] += 10; // Increment score
                            Server.broadcastScores(); // Update scores for all clients

                            Server.currentQuestionIndex++;

                            // Check if there are more questions
                            if (Server.currentQuestionIndex < Server.questions.length) {
                                Server.broadcastQuestion(); // Send the next question
                            } else {
                                Server.endGame(); // No more questions, end the game
                            }
                        }
                    } else {
                        sendMessage("WRONG_ANSWER");
                    }
                }
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        try {
            out.close();
            in.close();
            client.close();
            // Remove the client handler from the list of clients
            synchronized (clients) {
                clients.remove(this);
                broadcast(username + " has left the game.");
                playerList();
            }
        } catch (IOException e) {
            e.printStackTrace();
        
    }finally {
        try {
            out.close();
            in.close();
            client.close();
            // Remove the client handler from the list of clients
            synchronized (clients) {
                clients.remove(this);
                broadcast(username + " has left the game.");
                playerList();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}}

   private String getPlayingClientsString() {
    StringBuilder playingList = new StringBuilder("PLAYING:");

    // Synchronize access to playingClients to avoid concurrent modification issues
    synchronized (Server.playingClients) {
        for (String player : Server.playingClients) {
            playingList.append(player).append(",");
        }
    }

    // Remove the trailing comma if there are players
    if (playingList.length() > 8) {  // "PLAYING:" has length 8
        playingList.setLength(playingList.length() - 1);
    }

    // Return the string representation of the playing clients
    return playingList.toString();
    }
   
   private void disconnectClient() {
    try {
        client.close();
        in.close();
        out.close();
       
    } catch (IOException e) {
        System.out.println("Error while disconnecting client: " + e.getMessage());
    }
   }
   
   
    
    public void sendMessage(String message) {
        out.println(message);
    }

    public String getUsername() {
        return username;  // Return the client's username
    }
    
    
    private void broadcast(String Name) {
        
        for(ClientHandler client:clients)
    {
        client.out.println(Name);
        
    }
    
    }
    
    
    private  void playerList() {
        
  StringBuilder sb = new StringBuilder("");
  
  for (ClientHandler client : clients) {
    sb.append(client.username).append(","); // Get name from ServerThread
  }
  
  if(sb.length()>2)
  {
      sb.setLength(sb.length()-1);
  }
  broadcast(sb.toString());


  }
}
