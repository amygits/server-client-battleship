package client;

import java.net.*;
import java.io.*;

import org.json.*;

import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

import java.util.*;
import java.util.stream.Collectors;

class SockBaseClient {

    public static void main(String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int i1 = 0, i2 = 0;
        int port = 9099; // default port

        String host = "localhost";

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }

        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // Ask user for username
        System.out.println("Please provide your name for the server.");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String strToSend;
        String name = stdin.readLine();
        System.out.println("string to send:" + name);

        // Build the first request object just including the name
        Request op = Request.newBuilder().setOperationType(Request.OperationType.NAME).setName(name).build();
        Response response;

        try {
            // connect to the server
            System.out.println("attempting connection....");
            serverSock = new Socket(host, port);
            System.out.println("host: " + host);
            System.out.println("port: " + port);
            System.out.println("Successfully connected to server " + host + " @ " + port);

            // write to the server
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            Request toServ = null;
            op.writeDelimitedTo(out);
            System.out.println("waiting for response from server..");
            response = Response.parseDelimitedFrom(in);
            System.out.println("received response");

            do {
                // if response is greeting
                if (response.getResponseType() == Response.ResponseType.GREETING) {
                    System.out.println("response type: GREETING ");
                    System.out.println(response.getMessage());
                    int choice;
                    System.out.println(
                            "* \nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game");
                    strToSend = stdin.readLine();
                    if (strToSend.equals("exit")) {
                        System.out.println("Exiting client..");
                        System.exit(0);
                    }
                    choice = Integer.parseInt(strToSend);
                    System.out.println("choice to send: " + strToSend);
                    switch (choice) {
                    case 1:
                        System.out.println("entering case 1");
                        toServ = Request.newBuilder().setOperationType(Request.OperationType.LEADER).build();
                        System.out.println("successfully built request");
                        break;
                    case 2:
                        System.out.println("Entering case 2");
                        toServ = Request.newBuilder().setOperationType(Request.OperationType.NEW).build();
                        System.out.println("successfully built request");
                        break;
                    case 3:
                        System.out.println("entering case 3");
                        toServ = Request.newBuilder().setOperationType(Request.OperationType.QUIT).build();
                        System.out.println("successfully built request");
                        break;
                    default:
                        System.out.println("Invalid choice, try again");
                    }
                }

                // If response from server is task
                else if (response.getResponseType() == Response.ResponseType.TASK) {
                    System.out.println("response type: TASK ");
                    if (response.getHit() == true) {
                        System.out.println("You found one! +1 was added to your score");
                    }
                    final String regex = "^[\\d\\s]+$";
                    boolean valid;
                    do {
                        valid = true;
                        System.out.println(response.getImage());
                        System.out.println(response.getTask());

                        int row, col;
                        try {
                            strToSend = stdin.readLine();
                            if (strToSend.equals("exit")) {
                                System.out.println("Exiting client..");
                                System.exit(0);
                            }
                            if (strToSend.length() != 3) {
                                throw new Exception("bad input");
                            }
                            if (!strToSend.matches(regex)) {
                                throw new Exception("bad input");
                            }
                            row = Character.getNumericValue(strToSend.charAt(0));
                            col = Character.getNumericValue(strToSend.charAt(2));
                            if (row > 6 || col > 6 || row < 0 || col < 0) {
                                throw new Exception("bad input");
                            }
                            System.out.println("row: " + row + " col: " + col);
                            toServ = Request.newBuilder().setOperationType(Request.OperationType.ROWCOL).setRow(row)
                                    .setColumn(col).build();
                        } catch (Exception e) {
                            valid = false;
                            System.out.println("Bad Input! Try again (Row and column values must be between 0-6)");
                        }
                    } while (!valid);
                }

                // if response is leader
                else if (response.getResponseType() == Response.ResponseType.LEADER) {
                    System.out.println("response type: LEADER ");
                    System.out.println("leaders: " + response.getLeaderCount());
                    for (Entry lead : response.getLeaderList()) {
                        System.out.println("Name: " + lead.getName() + " Points: " + lead.getWins());
                    }
                    response = Response.newBuilder().setResponseType(Response.ResponseType.GREETING).build();
                    toServ = null;
                }

                // if response is error
                else if (response.getResponseType() == Response.ResponseType.ERROR) {
                    System.out.println("response type: ERROR");
                    System.out.println("Message from server: " + response.getMessage());
                    response = Response.newBuilder().setResponseType(Response.ResponseType.TASK).build();
                    toServ = null;
                }

                // if response is WON
                else if (response.getResponseType() == Response.ResponseType.WON) {
                    System.out.println("response type: WON");
                    System.out.println("Message from server: " + response.getMessage());
                    response = Response.newBuilder().setResponseType(Response.ResponseType.GREETING).build();
                    toServ = null;
                }

                // if response is BYE
                else if (response.getResponseType() == Response.ResponseType.BYE) {
                    System.out.println("response type: BYE");
                    System.out.println("Message from server: " + response.getMessage());
                    System.exit(0);
                }

                // send and read from the server
                if (toServ != null) {
                    toServ.writeDelimitedTo(out);
                    System.out.println("sent request to server");
                    System.out.println("waiting for response from server..");
                    response = Response.parseDelimitedFrom(in);
                    System.out.println("received response");
                }

            } while (true);

        } catch (ConnectException c) {
            System.out.println("Server is inactive");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (serverSock != null)
                serverSock.close();
        }
    }
}
