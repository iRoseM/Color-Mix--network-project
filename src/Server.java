import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;




public class Server {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static ArrayList<ClientHandler> clients=new ArrayList<>();
    private static ArrayList<String> clientNames = new ArrayList<>();
    static ArrayList<String> playingClients = new ArrayList<>();
    private static PrintWriter out;
    private static BufferedReader in;
    private Socket server;
    static Client clientframe;
    static int port = start.Server_port;
        static int[] playerScores = new int[3]; // Adjust size based on max players

 static String[] questions = {
        "Red + Yellow = ?",
        "Blue + Yellow = ?",
        "Red + Blue = ?",
        "Red + White = ?",
        "Yellow + Green = ?",
        "Blue + White = ?",
        "Green + Red = ?",
        "Orange + Blue = ?",
        "Yellow + Blue + Red = ?",
        "Green + Yellow = ?"
    };

    static String[] answers = {
        "Orange", "Green", "Purple", "Pink", "Lime",
        "Light Blue", "Brown", "Gray", "Brown", "Chartreuse"
    };

    static int currentQuestionIndex = 0;

         
    public static void main(String argv[]) throws Exception {
        
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server is running and listening on port "+ port);          
        try{
        while (true) {
            
            System.out.println("Waiting for client connection...");
            Socket client = serverSocket.accept();
            
//            clientframe= new Client();
//            clientframe.updateConnectedPlayers("" +getConnectedUsernames());
            System.out.println("Client connected");
            // Create a new thread for each client that connects
            ClientHandler clientHandler = new ClientHandler(client, clients);
            clients.add(clientHandler);
            
            new Thread(clientHandler).start();
            
             broadcastConnectedPlayers();

             
             
        }} catch (IOException e) {
            e.printStackTrace();
        } finally {
            // This block ensures that the server socket is closed, releasing the port
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("Server socket closed.");
                } catch (IOException e) {
                    e.printStackTrace();
            }}
        }
    }
        
    public static synchronized void broadcastConnectedPlayers() {
        StringBuilder connectedPlayers = new StringBuilder("Connected users: ");
        for (ClientHandler client : clients) {
            connectedPlayers.append(client.getUsername()).append(",");
        }

        if (connectedPlayers.length() > 0) {
            connectedPlayers.setLength(connectedPlayers.length() - 1);  // Remove trailing comma
        }

        // Broadcast to all clients
        for (ClientHandler client : clients) {
            client.sendMessage(connectedPlayers.toString());
        }
        
    }
    public static synchronized void broadcastPlayingPlayers() {
    StringBuilder playingList = new StringBuilder("PLAYING:");
    for (String player : playingClients) {
        playingList.append(player).append(",");
    }

    // Remove the trailing comma if there are players
    if (playingList.length() > 8) {  // "PLAYING:" has length 8
        playingList.setLength(playingList.length() - 1);
    }

    // Broadcast the message to all connected clients
    for (ClientHandler client : clients) {
        client.sendMessage(playingList.toString());
    }
    }   
    
    public void removeClient(ClientHandler clientHandler) {
    clients.remove(clientHandler);
    clientNames.remove(clientHandler.getUsername());  // Assuming `getClientName` returns the client's name
    updateAllClients();  // Method to broadcast updated client list
}
    
    public void updateAllClients() {
    // Prepare the list of client names as a comma-separated string
    StringBuilder names = new StringBuilder();
    for (String name : clientNames) {
        names.append(name).append(",");
    }
    // Remove the last comma
    if (names.length() > 0) {
        names.setLength(names.length() - 1);
    }
    String namesList = names.toString();

    // Send the updated list to each client
    for (ClientHandler client : clients) {
        client.sendMessage("Connected users: " + namesList);  // Assuming sendMessage is a method to send messages to a client
    }
}
    
public static synchronized void startGame() {
    if (playingClients.size() >= 2 && playingClients.size() <= 3) {
        if (playingClients.size() == 2)
            startTimer();
        
        StringBuilder gameStartMessage = new StringBuilder("GAME_START:");
        for (String player : playingClients) {
            gameStartMessage.append(player).append(",");
        }
        if (gameStartMessage.length() > 11) {  // "GAME_START:" has length 11
            gameStartMessage.setLength(gameStartMessage.length() - 1);
        }

        // Notify all clients in the `playingClients` list
        for (ClientHandler client : clients) {
            if (playingClients.contains(client.getUsername())) {
                client.sendMessage(gameStartMessage.toString());
            }
        }

        // Immediately send the first question after starting the game
    }
}

public static void startTimer() {
    final int[] timeRemaining = {30};
    Timer timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
            timeRemaining[0]--;
            System.out.println("Time remaining: " + timeRemaining[0] + " seconds");

            // Check if time is up
            if (timeRemaining[0] <= 0) {
                timer.cancel();
                System.out.println("Waiting Time's up! Starting the game.");
                broadcastQuestion();
            }
            
            // Check if players have reached 3, then stop the timer
            if (playingClients.size() >= 3) {
                timer.cancel();
                System.out.println("3 players joined. Starting the game immediately.");
                broadcastQuestion();
            }
        }
    }, 0, 1000);
}

public static void stopTimer() {
    System.out.println("Game stopped.");
}


public static synchronized void broadcastScores() {
    StringBuilder scoresMessage = new StringBuilder("SCORES:");
    for (int i = 0; i < playingClients.size(); i++) {
        scoresMessage.append(playingClients.get(i)).append("=").append(playerScores[i]).append(",");
    }

    if (scoresMessage.length() > 7) {  // "SCORES:" has length 7
        scoresMessage.setLength(scoresMessage.length() - 1);  // Remove the last comma
    }

    for (ClientHandler client : clients) {
        client.sendMessage(scoresMessage.toString());
    }
}
public static synchronized void broadcastQuestion() {
    if (currentQuestionIndex < questions.length) {
        String questionMessage = "QUESTION:" + questions[currentQuestionIndex] + ";OPTIONS:";
        
        // Define options for the current question, including the correct answer
        List<String> options = new ArrayList<>();
        options.add(answers[currentQuestionIndex]); // Add correct answer
        
        // Add some other color names as distractor options
        String[] distractors = {"Blue", "Yellow", "Red", "White", "Black", "Purple", "Green", "Pink"};
        while (options.size() < 4) { // Ensure we have 4 options in total
            String option = distractors[(int) (Math.random() * distractors.length)];
            if (!options.contains(option)) { // Avoid duplicates
                options.add(option);
            }
        }
        
        // Shuffle the options to randomize their order
        Collections.shuffle(options);
        
        // Append options to the message
        for (String option : options) {
            questionMessage += option + ",";
        }
        
        // Remove trailing comma
        questionMessage = questionMessage.substring(0, questionMessage.length() - 1);
        
        // Broadcast question and options to all clients in the game
        for (ClientHandler client : clients) {
            if (playingClients.contains(client.getUsername())) {
                client.sendMessage(questionMessage);
            }
        }
    }}
public static synchronized void endGame() {
    String winnerMessage = "GAME_OVER: The game has ended! ";
    int maxScore = -1;
    String winner = "";

    for (int i = 0; i < playingClients.size(); i++) {
        if (playerScores[i] > maxScore) {
            maxScore = playerScores[i];
            winner = playingClients.get(i);
        }
    }
    winnerMessage += "Winner: " + winner + " with score: " + maxScore;

    for (ClientHandler client : clients) {
        client.sendMessage(winnerMessage);
    }
}
    
}