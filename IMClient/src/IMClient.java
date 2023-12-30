/**
 * Stephen Schmith
 * IMClient.
 * <p>
 * IMClient connects to and communicates with the IMServer.
 * Most user input is retrieved through the MessageThread class.
 * <p>
 * Last Modified: 3/17/2013
 * Created in Eclipse 4.2.2
 */

import java.io.*;
import java.util.*;
import java.net.*;

public class IMClient {
    private static String username;                        // Username associated with this client.
    private static boolean isMsgThreadRunning = false;    // True if the user has logged in.
    private static String prompt;                        // Sets the text for the command prompt.
    private static boolean firstRun = true;

    public static void main(String[] args) throws IOException {
        // Read the configuration file for IP Address and Port information.
        File file = new File("IMClient/src/config.txt");
        Scanner configReader;
        if (file.exists() && file.canRead()) {
            configReader = new Scanner(file);
        } else {
            System.out.println("Unable to locate config.txt. Exiting.");
            System.exit(1);
            return;
        }
        // Print the menu and get user selection
        int userChoice = getUserMenuSelection();
        String msgContent = "";
        String msgPrefix = "";
        String[] config = new String[2];
        int count = 0;

        while (configReader.hasNext()) {
            config[count] = configReader.nextLine();
            count++;
        }

        // If 1, get log in information. If 2, quit.
        switch (userChoice) {
            case 1:
                msgPrefix = "LOGIN REQUEST=";
                msgContent = getLogin();
                break;
            default:
                System.exit(0);
                return;
        }

        // Assemble the message to the server, then attempt to connect.
        String toServer = msgPrefix + msgContent;

        Socket mySocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            mySocket = new Socket(config[0].trim(), Integer.parseInt(config[1].trim()));
            out = new PrintWriter(mySocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Could not find host" + config[0]);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("I/O exception with " + config[0]);
            System.exit(1);
        }

        String fromServer;

        // Send message to the server
        out.println(toServer);

        try {
            // This loop listens for messages from the server and interprets them appropriately.
            while ((fromServer = in.readLine()) != null) {
                // If a login request failed, create another login request.
                if (fromServer.equals("LOGIN REQUEST=RETRY")) {
                    System.out.println("\nInvalid login. Try again.");
                    msgPrefix = "LOGIN REQUEST=";
                    msgContent = getLogin();
                    toServer = msgPrefix + msgContent;
                    out.println(toServer);
                } else if (fromServer.startsWith("LOGIN REQUEST=SUCCESS.")) {
                    // If a login request succeeded, ask the server for this user's friends list.
                    // Check for logged messages.
                    System.out.println("You are logged in.\n");
                    String[] s = fromServer.split("[.]");

                    // If s.length > 1, there are messages waiting to be displayed.
                    if (s.length > 1) {
                        System.out.println("You received " + ((s.length - 1) / 2) + " messages while offline. Showing them now...\n");
                        System.out.println("----------------------------------");

                        for (int i = 1; i < s.length; i++) {
                            // If odd, this String is a source (the index is offset by the message prefix in fromServer)
                            if (i % 2 == 1) {
                                System.out.print(s[i] + ": ");
                            } else {
                                System.out.println(s[i]);
                                System.out.println("----------------------------------");
                            }
                        }
                    }

                    toServer = "FRIENDS LIST REQUEST=" + username;
                    out.println(toServer);
                } else if (fromServer.startsWith("SHOW FRIENDS LIST=")) {
                    // Show the user's friends list. The first time this code runs, it will also start
                    // a new thread which handles user input.
                    int index = fromServer.indexOf("=");
                    String friends = fromServer.substring(index + 1);
                    String[] f = friends.split("[.]");

                    showFriends(f);

                    // Start the input thread.
                    if (!isMsgThreadRunning) {
                        MessageThread newThread = new MessageThread(username, out);
                        newThread.start();
                        isMsgThreadRunning = true;
                    }

                } else if (fromServer.startsWith("SEND MESSAGE REQUEST FAILED=")) {
                    // If this user sent a message to an invalid destination, it's handled here.
                    String[] msg = fromServer.split("=", 2);
                    System.out.println("User " + msg[1] + " does not exist.");
                } else if (fromServer.equals("REMOVE FRIEND REQUEST=FAILED")) {
                    // Receive REMOVE FRIEND REQUESTS in the next two blocks.
                    System.out.println("That user isn't one of your friends.");
                } else if (fromServer.equals("REMOVE FRIEND REQUEST=SUCCESSFUL")) {
                    System.out.println("Friend removed.");
                } else if (fromServer.startsWith("SEND UMESSAGE FAILED=")) {
                    // If the destination user of a message is offline, tell this user.
                    String[] msg = fromServer.split("=", 2);
                    System.out.println("\n" + msg[1] + " is offline. Your message will be delivered when they sign in.");
                } else if (fromServer.startsWith("SEND UMESSAGE DISCONNECT=")) {
                    // If the target user disconnected during message delivery, tell the client.
                    String[] msg = fromServer.split("=", 2);
                    System.out.println("\nUser " + msg[1] + " disconnected during message delivery. Try again.");
                } else if (fromServer.startsWith("INCOMING UMESSAGE=")) {
                    // Incoming messages are parsed in this code block.
                    String[] msg = fromServer.split("[=.]", 3);
                    // Display the message source and content.
                    System.out.println("\n" + msg[1] + ": " + msg[2]);
                } else if (fromServer.startsWith("ADD FRIEND REQUEST=FAILED")) {
                    // Listen for Add Friend Requests on the next two blocks.
                    String[] s = fromServer.split("[.]", 2);
                    System.out.println("User " + s[1] + " does not exist.");
                } else if (fromServer.startsWith("ADD FRIEND REQUEST=SUCCESS")) {
                    System.out.println("Friend added.");
                } else {
                    // Handle erroneous messages here.
                    System.out.println("Received invalid command from server.");
                }

                if (!firstRun) {
                    System.out.print(prompt);
                }

                firstRun = false;
            }
        } catch (SocketException e) {
            // If for some reason the connection to the server is lost, the client will close.
            System.out.println("Connection lost.");
            System.exit(1);
        }
        out.close();
        in.close();
        mySocket.close();

    }

    // Set the console prompt.
    public static void setPrompt(String s) {
        prompt = s;
    }

    // Print the friends list.
    public static void showFriends(String[] f) {
        // If f[0] is null, this user hasn't added any friends.
        if (f[0].equals("null")) {
            System.out.println("\nYou haven't added any friends yet.");
        } else {
            // If f[0] is not null, the friends list is displayed.
            // Online users are listed at the top. Offline users are listed at the bottom.
            ArrayList<String> online = new ArrayList<>();
            ArrayList<String> offline = new ArrayList<>();
            for (int i = 0; i < f.length; i += 2) {
                if (f[i + 1].equals("True")) {
                    online.add(f[i]);
                } else {
                    offline.add(f[i]);
                }
            }
            // Print the online users.
            System.out.println("\n\nFRIENDS LIST\n----------------------------------");
            System.out.println("ONLINE:");
            for (String string : online) {
                System.out.println(string);
            }
            System.out.println("----------------------------------");
            // Print the offline users.
            System.out.println("OFFLINE:");
            for (String s : offline) {
                System.out.println(s);
            }
            System.out.println("----------------------------------\n");
        }
        System.out.println("\nEnter \"/help\" (without quotes) for a list of commands.\n");
    }

    // Print the menu and return a valid user selection.
    private static int getUserMenuSelection() {
        Scanner scan = new Scanner(System.in);
        printMenu();

        int choice = scan.nextInt();

        while (choice <= 0 || choice >= 3) {
            printMenu();
            choice = scan.nextInt();
        }

        return choice;
    }

    // Print the client menu.
    private static void printMenu() {
        System.out.println("Welcome to IM Client.\n");
        System.out.println("\tMENU");
        System.out.println("==========================");
        System.out.println("1. Log In");
        System.out.println("2. Quit");
        System.out.println("==========================");
        System.out.println("Enter 1 or 2.");
    }

    private static String getLogin() {
        Scanner scan = new Scanner(System.in);
        System.out.println("\nEnter username:");
        String u = scan.nextLine();
        username = u;
        prompt = u + ": ";
        System.out.println("\nEnter password:");
        String p = scan.nextLine();
        return u + "." + p;
    }
}
