package server;

import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;
import java.lang.*;

import buffers.RequestProtos.Request;
import buffers.RequestProtos.Logs;
import buffers.RequestProtos.Message;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

class Helper {
    static String logFilename = "logs.txt";

    ServerSocket serv = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9000; // default port
    Game game;
    Map<String, Integer> guesses;
    Map<String, Integer> playerIndex;
    Response.Builder res;

    public Helper(Socket sock, Game game, Map<String, Integer> guesses, Response.Builder res,
            Map<String, Integer> playerIndex) {
        this.clientSocket = sock;
        this.game = game;
        this.res = res;
        this.guesses = guesses;
        this.playerIndex = playerIndex;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e) {
            System.out.println("Error in constructor: " + e);
        }
    }

    // Handles the communication right now it just accepts one input and then is
    // done you should make sure the server stays open
    // can handle multiple requests and does not crash when the server crashes
    // you can use this server as based or start a new one if you prefer.
    public void start() throws IOException {
        String name = "";
        boolean valid;
        // Creating Entry and Leader response
        Entry leader = null;
        Response response = null;
        String result = null;

        try {
            do {
                // System.out.println("waiting on request from client");
                Request op = Request.parseDelimitedFrom(in);
                // System.out.println("received request");

                if (op != null) {
                    try {

                        // if the operation is NAME (so the beginning then say there is a commention and
                        // greet the client)
                        if (op.getOperationType() == Request.OperationType.NAME) {
                            // get name from proto object
                            name = op.getName();
                            if (!playerIndex.containsKey(name)) {
                                // writing a connect message to the log with name and CONNENCT
                                writeToLog(name, Message.CONNECT);
                                System.out.println("Got a connection and a name: " + name);
                                response = Response.newBuilder().setResponseType(Response.ResponseType.GREETING)
                                        .setMessage("Hello " + name
                                                + " and welcome. Welcome to a simple game of battleship. ")
                                        .build();
                                leader = Entry.newBuilder().setName(name).setWins(0).setLogins(1).build();
                                System.out.println("entry: " + leader.getName() + " wins: " + leader.getWins());
                                res.addLeader(leader);
                                System.out.println("Added leader to RES BUILDER");
                                playerIndex.put(name, playerIndex.size());
                                System.out.println(
                                        "Added leader to PLAYER INDEX (CURRENT SIZE: " + playerIndex.size() + ")");
                            } else {
                                writeToLog(name, Message.CONNECT);
                                System.out.println("Got a connection and a name: " + name);
                                response = Response.newBuilder().setResponseType(Response.ResponseType.GREETING)
                                        .setMessage("Hello " + name
                                                + " and welcome. Looks like you've played before!  Please choose an option. ")
                                        .build();
                            }
                        }
                        // JOIN GAME
                        else if (op.getOperationType() == Request.OperationType.NEW) {
                            System.out.println("received request to enter game");
                            // game.newGame(); // starting a new game
                            writeToLog(name, Message.START);
                            System.out.println("game joined");
                            System.out.println("answer:");
                            System.out.println(game.getAnswer());
                            // IMPLEMENTATION HERE
                            response = Response.newBuilder().setResponseType(Response.ResponseType.TASK)
                                    .setImage(game.getImage())
                                    .setTask("Select a row and column (FORMAT: ROW<int> COL<int> ex. '3 4')").build();
                        }

                        // LEADER BOARDS
                        else if (op.getOperationType() == Request.OperationType.LEADER) {
                            System.out.println("receive request for leaders board");
                            // implementation here
                            response = res.build();
                        }

                        // ROW COL
                        else if (op.getOperationType() == Request.OperationType.ROWCOL) {
                            System.out.println("received request for TASK");
                            System.out.println("answer:");
                            System.out.println(game.getAnswer());
                            int row = op.getRow();
                            int col = op.getColumn();
                            System.out.println("row: " + row + " col: " + col);
                            String guess = row + "," + col;
                            // implementation here
                            // if row and col haven't been guessed yet
                            if (!guesses.containsKey(guess)) {
                                guesses.put(guess, 1);
                                int tempScore = game.getIdx();
                                String newImg = game.replaceOneCharacter(op.getRow(), op.getColumn());

                                if (tempScore < game.getIdx()) {
                                    response = Response.newBuilder().setResponseType(Response.ResponseType.TASK)
                                            .setHit(true).setImage(newImg)
                                            .setTask("Select a row and column (FORMAT: ROW<int> COL<int> ex. '1 2')")
                                            .build();
                                    // leader.setWins(1 + leader.getWins());
                                    Entry temp = res.getLeader(playerIndex.get(name));
                                    temp = Entry.newBuilder().setName(name).setWins(1 + temp.getWins()).setLogins(1)
                                            .build();
                                    res.removeLeader(playerIndex.get(name));

                                    // ADD POINTS
                                    System.out.println("new score for " + temp.getName() + ": " + temp.getWins());
                                    res.addLeader(temp);
                                    if (game.getIdx() == 12) {
                                        writeToLog(name, Message.WIN);
                                        System.out.println("winning move");
                                        game.setWon();
                                        response = Response.newBuilder().setResponseType(Response.ResponseType.WON)
                                                .setMessage("YOU'VE WON THE GAME, CONGRATS!").build();
                                    }
                                } else {
                                    response = Response.newBuilder().setResponseType(Response.ResponseType.TASK)
                                            .setHit(false).setImage(newImg)
                                            .setTask("Select a row and column (FORMAT: ROW<int> COL<int> ex. '1 2')")
                                            .build();
                                }
                            } else {
                                System.out.println("duplicate answer");
                                response = Response.newBuilder().setResponseType(Response.ResponseType.ERROR)
                                        .setMessage("This answer has been used already, try another.").build();
                            }
                        }
                        // QUIT
                        else if (op.getOperationType() == Request.OperationType.QUIT) {
                            System.out.println("receive request for to quit");
                            // implementation here
                            response = Response.newBuilder().setResponseType(Response.ResponseType.BYE)
                                    .setMessage("Good game, " + name).build();
                        }

                        // sends out response
                        response.writeDelimitedTo(out);
                        System.out.println("sent out request");

                    } catch (NullPointerException e) {
                        System.out.println("null pointer exception");
                        // valid = false;
                        // op =
                        // Request.newBuilder().setOperationType(Request.OperationType.ROWCOL).build();
                    }
                }
            } while (true);

            // Example how to start a new game and how to build a response with the image
            // which you could then send to the server
            // LINE 67-108 are just an example for Protobuf and how to work with the
            // differnt types. They DO NOT
            // belong into this code.
            /*
             * game.newGame(); // starting a new game
             * 
             * // adding the String of the game to
             * 
             * 
             * // On the client side you would receive a Response object which is the same
             * as the one in line 70, so now you could read the fields
             * System.out.println("Task: " + response2.getResponseType());
             * System.out.println("Image: \n" + response2.getImage());
             * System.out.println("Task: \n" + response2.getTask());
             * 
             * // Creating Entry and Leader response Response.Builder res =
             * Response.newBuilder() .setResponseType(Response.ResponseType.LEADER);
             * 
             * // building an Entry for the leaderboard Entry leader = Entry.newBuilder()
             * .setName("name") .setWins(0) .setLogins(0) .build();
             * 
             * // building another Entry for the leaderboard Entry leader2 =
             * Entry.newBuilder() .setName("name2") .setWins(1) .setLogins(1) .build();
             * 
             * // adding entries to the leaderboard res.addLeader(leader);
             * res.addLeader(leader2);
             * 
             * // building the response Response response3 = res.build();
             * 
             * // iterating through the current leaderboard and showing the entries for
             * (Entry lead: response3.getLeaderList()){ System.out.println(lead.getName() +
             * ": " + lead.getWins()); }
             */

        } catch (Exception ex) {
            ex.printStackTrace();
        } /*
           * finally { System.out.println("Server shutting off."); if (out != null)
           * out.close(); if (in != null) in.close(); if (clientSocket != null)
           * clientSocket.close(); }
           */
    }

    /**
     * Writing a new entry to our log
     * 
     * @param name    - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be
     *                written in the log (e.g. Connect)
     * @return String of the new hidden image
     */

    public static void writeToLog(String name, Message message) {
        try {
            // read old log file
            Logs.Builder logs = readLogFile();

            // get current time and data
            Date date = java.util.Calendar.getInstance().getTime();
            System.out.println(date);

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date.toString() + ": " + name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // This is only to show how you can iterate through a Logs object which is a
            // protobuf object
            // which has a repeated field "log"

            for (String log : logsObj.getLogList()) {
                System.out.println(log);
            }

            // write to log file
            logsObj.writeTo(output);
        } catch (Exception e) {
            System.out.println("Issue while trying to save");
        }
    }

    /**
     * Reading the current log file
     * 
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public static Logs.Builder readLogFile() throws Exception {
        Logs.Builder logs = Logs.newBuilder();

        try {
            // just read the file and put what is in it into the logs object
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found.  Creating a new file.");
            return logs;
        }
    }
}

class SockBaseServer {

    public static void main(String args[]) throws Exception {
        Game game = new Game();
        Map<String, Integer> guesses = new Hashtable<>();
        Response.Builder res = Response.newBuilder().setResponseType(Response.ResponseType.LEADER);
        Map<String, Integer> playerIndex = new Hashtable<>();

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int)>");
            System.exit(1);
        }
        int port = 9000; // default port
        int sleepDelay = 10000; // default delay
        Socket clientSocket = null;
        ServerSocket serv = null;

        try {
            port = Integer.parseInt(args[0]);
            sleepDelay = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }
        try {
            serv = new ServerSocket(port);
            System.out.println("Server started..");
            game.newGame();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        do {
            System.out.println("Accepting requests..");
            clientThread t = new clientThread(serv, game, guesses, res, playerIndex);
            t.start();
        } while (true);

    }
}

class clientThread extends Thread {
    Socket sock;
    Game game;
    Map<String, Integer> guesses;
    Response.Builder res;
    Map<String, Integer> playerIndex;

    public clientThread(ServerSocket serv, Game game, Map<String, Integer> guesses, Response.Builder res,
            Map<String, Integer> playerIndex) {
        try {
            this.sock = serv.accept();
            this.game = game;
            this.guesses = guesses;
            this.res = res;
            this.playerIndex = playerIndex;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        System.out.println("Connected to client " + sock.getInetAddress() + " @ port " + sock.getPort());
        try {
            Helper server = new Helper(sock, game, guesses, res, playerIndex);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
