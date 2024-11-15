import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerConnection implements Runnable {
    private Socket client;
    private Client clientFrame;  // Reference to the client GUI
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;
    private Server mainServer;  // Reference to the server to remove player upon disconnection

    public ServerConnection(Socket s, Client frame) throws IOException {
        this.client = s;
        this.clientFrame = frame;
        clientName= start.playername;
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true); // Auto-flush enabled
    }

    // Sets the client name (from the client)
//    public static void setClientName(String name) {
//        clientName = name;
//    }

    // Gets the client's name
    public String getClientName() {
        return clientName;
    }

    // Sends a message to the client
    public void sendMessage(String message) {
        out.println(message);
    }

    // Main run method to listen for messages from the server
    @Override
    public void run() {
        String serverResponse;
        try {
            while ((serverResponse = in.readLine()) != null) { // Read response from server
                System.out.println("Server says: " + serverResponse);

                // Handling connected users list update
                if (serverResponse.startsWith("Connected users: ")) {
                    String connectedPlayers = serverResponse.substring(17);
                    clientFrame.updateConnectedPlayers(connectedPlayers);
                }

                // Handling currently playing clients update
                if (serverResponse.startsWith("PLAYING:")) {
                    String playingClients = serverResponse.substring(8);
                    clientFrame.updatePlayingClients(playingClients);
                }

                // Handling game start
                if (serverResponse.startsWith("GAME_START:")) {
                    String players = serverResponse.substring(11);
                    clientFrame.startGame(players);
                }

                // Handling room full message
                if (serverResponse.equals("ROOM_FULL")) {
                    clientFrame.displayRoomFullMessage();
                }
                
                if (serverResponse.startsWith("SCORES:")) {
                    String scores = serverResponse.substring(7);
                    clientFrame.updateScores(scores);
                }
                if (serverResponse.startsWith("QUESTION:")) {
                    String question = serverResponse.substring(9);
                    clientFrame.displayQuestion(question);
                }
            }
        }catch (IOException e){
            System.out.println("IO exception in new client class");
            System.out.println(e.getStackTrace());
        }finally{
            
        }
        
    }
}