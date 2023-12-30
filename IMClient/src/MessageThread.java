/**
 * Stephen Schmith
 * IMClient.
 * <p>
 * MessageThread reads user input after the user has logged into their account.
 * It runs in a separate thread from IMClient, which allows the user to create input
 * while the client is still reading from the server.
 * <p>
 * Last Modified: 3/10/2013
 * Created in Eclipse 4.2.2
 */

import java.io.PrintWriter;
import java.util.NoSuchElementException;
import java.util.Scanner;


public class MessageThread extends Thread {
    private final String username;        // Username passed from IMClient
    private final PrintWriter out;        // PrintWriter passed from IMClient
    private final Scanner scan;

    public MessageThread(String un, PrintWriter o) {
        this.username = un;        // Username from this IMClient
        this.out = o;            // PrintWriter from this IMClient
        this.scan = new Scanner(System.in);
    }

    public void run() {
        this.inputLoop();
    }

    private void inputLoop()
    // Main loop for the IM client. Interprets user commands and messages. Returns messages to the server.
    {
        String userInput = scan.nextLine().trim();

        while (true) {
            // Code for parsing the /msg command.
            // If a user messages another user, a conversation with that user is started.
            // While in a conversation, the user does not need to repeat the /msg command.
            // The conversation can be broken with any other command.
            //
            // Command syntax: /msg user message
            if (userInput.startsWith("/msg") && userInput.matches("^/msg [A-Za-z]+ .+$")) {
                boolean inConversation = false;
                String trimmedMessage = "";
                String[] input;
                String friendName = "";

                // TODO: refactor to use a different loop structure
                do {
                    /* Send Message Request Format:
                     *
                     * [PREFIX]=destination.source.[message content]
                     */
                    if (!inConversation) {
                        // If this is the first message to this client, parse the full /msg command.
                        input = userInput.split(" ", 3);
                        friendName = input[1];
                        trimmedMessage = input[2].trim();
                        // Set the message prompt to "To [username]: "
                        IMClient.setPrompt("To " + friendName + ": ");
                        inConversation = true;
                    }
                    // Build the message to the server
                    String toServer = "SEND MESSAGE REQUEST=";
                    toServer += friendName + "." + username + "." + trimmedMessage;
                    out.println(toServer);
                    // Get next input
                    System.out.print("To " + friendName + ": ");
                    try {
                        trimmedMessage = scan.nextLine().trim();
                    } catch (NoSuchElementException e) {
                        // Replace with something like log4j
                        System.out.println("Received an empty message: " + e.getMessage());
                    } catch (IllegalStateException e) {
                        System.out.println("Unable to read message contents: " + e.getMessage());
                    }
                    while (trimmedMessage.isEmpty()) {
                        // Print the prompt and get input from the user
                        System.out.print("To " + friendName + ": ");
                        try {
                            trimmedMessage = scan.nextLine().trim();
                        } catch (NoSuchElementException e) {
                            // Replace with something like log4j
                            System.out.println("Received an empty message: " + e.getMessage());
                        } catch (IllegalStateException e) {
                            System.out.println("Unable to read message contents: " + e.getMessage());
                        }
                    }

                    // Continue running the loop until the user's message starts with a /
                } while (trimmedMessage.charAt(0) != '/');
                // Reset the command prompt
                IMClient.setPrompt(username + ": ");
                // Treat the last user message as a new command
                userInput = trimmedMessage;
            } else if (userInput.startsWith("/help")) {
                // Print the help menu
                printHelpMenu();
                System.out.print(username + ": ");
                userInput = scan.nextLine().trim();
            } else if (userInput.startsWith("/add")) {
                // Add a friend to this user's friends list.
                // If userInput.length() is 4, then no friend is being added.
                if (userInput.length() == 4) {
                    System.out.println("Error: Invalid command.");
                    // Reprint the prompt.
                } else {
                    // Send an ADD FRIEND REQUEST to the server.
                    String toServer = "ADD FRIEND REQUEST=" + username;
                    String[] s = userInput.split(" ", 2);
                    toServer += "." + s[1];
                    out.println(toServer);
                    // Server will reply when it has checked the Add the Friend Request.
                }
                System.out.print(username + ": ");
                userInput = scan.nextLine().trim();
            } else if (userInput.startsWith("/friends")) {
                // Request this user's friends list from the server.
                String toServer = "FRIENDS LIST REQUEST=" + username;
                out.println(toServer);
                userInput = scan.nextLine().trim();
            } else if (userInput.startsWith("/remove")) {
                // Remove a friend from this user's friends list.
                String toServer = "REMOVE FRIEND REQUEST=" + username;
                // Get the friend to be removed from userInput and add it to the server message.
                String[] s = userInput.split(" ", 2);
                toServer += "." + s[1];
                out.println(toServer);
                userInput = scan.nextLine().trim();
            } else if (userInput.startsWith("/exit")) {
                // Exit the program
                // Tell the server this client is exiting.
                String toServer = "EXIT REQUEST=" + username;
                out.println(toServer);
                System.out.println("\nGoodbye!");
                System.exit(0);
            } else {
                // Handle invalid commands here.
                System.out.println("\nError: Invalid command\n");
                System.out.print(username + ": ");
                userInput = scan.nextLine().trim();
            }
        }


    }

    private static void printHelpMenu()
    // Prints the help menu.
    {
        System.out.println();
        String leftCol, rightCol, format, separator;
        leftCol = "COMMAND";
        rightCol = "DESCRIPTION";
        format = "%1$-24s%2$-60s%n";    // The format here is two columns. The left one is 24 characters long, the next is 60, and they're followed by a new line.
        separator = "------------------------------------------------------------------------------";
        System.out.format(format, leftCol, rightCol);
        System.out.println(separator);

        for (int i = 1; i <= 5; i++) {
            switch (i) {
                case 1:
                    leftCol = "/exit";
                    rightCol = "Exits the program.";
                    System.out.format(format, leftCol, rightCol);
                    System.out.println(separator);
                    break;
                case 2:
                    leftCol = "/msg [user] [message]";
                    rightCol = "Sends a message to a user. Put the user's username where [user] is, and your message at [message].";
                    System.out.format(format, leftCol, rightCol);

                    leftCol = "";
                    rightCol = "You only need to enter this command once, unless you use another command.";
                    System.out.format(format, leftCol, rightCol);

                    rightCol = "The messenger will read any new text you enter as another message to the user you specified.";
                    System.out.format(format, leftCol, rightCol);

                    rightCol = "EXAMPLE:";
                    System.out.format(format, leftCol, rightCol);

                    rightCol = "You: /msg YourFriend Hey!";
                    System.out.format(format, leftCol, rightCol);

                    rightCol = "You: What are you up to?";
                    System.out.format(format, leftCol, rightCol);

                    System.out.println(separator);
                    break;
                case 3:
                    leftCol = "/friends";
                    rightCol = "Displays your friends list.";
                    System.out.format(format, leftCol, rightCol);
                    System.out.println(separator);

                    break;
                case 4:
                    leftCol = "/add [user]";
                    rightCol = "Add a user to your friends list.";
                    System.out.format(format, leftCol, rightCol);
                    System.out.println(separator);

                    break;
                case 5:
                    leftCol = "/remove [user]";
                    rightCol = "Remove a user from your friends list.";
                    System.out.format(format, leftCol, rightCol);
                    System.out.println(separator);

                    break;
                default:
                    break;
            }
        }
    }
}
