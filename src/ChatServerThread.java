import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by ethan on 9/10/14.
 */
class ChatServerThread implements Runnable {

    /* The client socket and IO we are going to handle in this thread */
    protected Socket socket;
    protected PrintWriter out;
    protected BufferedReader in;
    String clientID = "-1", currentChatRoom;

    public ChatServerThread(Socket socket) {
        /* Assign local variable */
        this.socket = socket;
        
        /* Create the I/O variables */
        try {
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            
            /* Some debug */
            System.out.println("Client connected!");

            clientID = makeValidId();
            putInRoom("lobby", false);
            this.out.println("Welcome to the chat server! \n" +
                    "You are in room: " + currentChatRoom + ". Type '/room <room_name>' to switch.");
            this.out.println("Your id is: " + clientID + ". Type '/id <new_id>' to change it.");
            /* Say hi to the client */
            if (ChatServer.threads.size() > 0) {
                this.out.println("Here's a list of rooms and their users:");
                for (ChatRoom room : ChatServer.rooms) {
                    this.out.println(room.getName() + ": " + room.listCurrentUsers());
                }
            }
            for (ChatServerThread thread : ChatServer.threads)
                thread.out.println(clientID + " has connected to room: " + currentChatRoom);

        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    public void run() {
        /* Our thread is going to read lines from the client and parrot them back.
           It will continue to do this until an exception occurs or the connection ends
           */
        while (true) {
            try {
                /* Get string from client */
                String fromClient = this.in.readLine();

                
                /* If null, connection is closed, so just finish */
                if (fromClient == null) {
                    System.out.println("'" + clientID + "' disconnected");
                    ChatServer.broadcastToARoom(currentChatRoom, clientID, "'" + clientID + "' disconnected");
                    this.in.close();
                    this.out.close();
                    this.socket.close();
                    return;
                }

                String[] fromClientArr = fromClient.split("\\s+"); // split fromClient at spaces

                /* If the client said "/disconnect", close the connection */
                if (fromClient.equals("/disconnect")) {
                    System.out.println(clientID + " is disconnecting.");
                    this.out.println("OK! OKOKOKOKOK!");
                    Thread.sleep(500);
                    this.out.println("(ノಠ益ಠ)ノ彡");
                    System.out.println(currentChatRoom);
                    ChatServer.broadcastToARoom(currentChatRoom, clientID, "'" + clientID + "' disconnected.");

                    this.in.close();
                    this.out.close();
                    this.socket.close();

                    /* Remove client from list of users */
                    int index = -1;
                    for (int i = 0; i < ChatServer.threads.size(); i++)
                        if (ChatServer.threads.get(i).clientID.equals(this.clientID))
                            index = i;
                    ChatServer.threads.remove(index);
                    return;
                } else if (fromClient.length() >= 3 && fromClientArr[0].equals("/id")) {
                    if (fromClient.substring(3).equals("") || fromClient.substring(4).equals("")) {
                        System.out.println(clientID + " submitted an invalid /id command.");
                        this.out.println("Your ID change request did not contain an ID. Please try again. The proper format is '/id <your_new_id>'");
                    }
                    String newID = fromClient.substring(4);
                    if (newID.equals(clientID)) {
                        this.out.println("You're already " + clientID);
                        System.out.println(clientID + " tried to change their name to '" + clientID + "', but that's already his/her name.");
                    }

                    boolean validName = true;
                    for (ChatServerThread thread : ChatServer.threads) {
                        if (newID.equals(thread.clientID)) {
                            System.out.println(clientID + " tried to change names to '" + newID + "', but that's already taken.");
                            this.out.println("'" + newID + "' is already taken.");
                            validName = false;
                        }
                    }
                    if (validName) {
                        for (ChatServerThread thread : ChatServer.threads) {
                            if (thread.clientID.equals(clientID)) continue;
                            thread.out.println("Poof! " + clientID + " is now " + newID + ".");
                        }
                        clientID = newID;
                        System.out.println(clientID + " changed ID to '" + newID + "'.");
                        this.out.println("Poof! You're now '" + clientID + "'.");
                    }
                /* Allow users to whisper to each other by typing '/whisper <recipient> <message>' */
                } else if (fromClientArr[0] != null && fromClientArr[0].equals("/whisper")) {
                    if (fromClientArr[1] == null) {
                        this.out.println("You need to specify a recipient. Correct whisper format is: \n" +
                                "'/whisper <recipient> <message>'.");
                    } else if (fromClientArr[2] == null) {
                        this.out.println("You need to specify a message. Correct whisper format is: \n" +
                                "'/whisper <recipient> <message>'.");
                    } else {
                        String recipient = fromClientArr[1];
                        String message = fromClientArr[2];
                        for (int i = 3; i < fromClientArr.length; i++) {
                            if (fromClientArr[i] != null)
                                message += " " + fromClientArr[i];
                        }
                        boolean notFound = true;
                        if (recipient.equals(clientID)) {
                            System.out.println("'" + clientID + "' tried to whisper to himself.");
                            this.out.println("You can't message yourself!");
                        } else {
                            for (ChatServerThread thread : ChatServer.threads) {
                                if (thread.clientID.equals(recipient)) {
                                    thread.out.println(clientID + " >> " + message);
                                    notFound = false;
                                }
                            }
                            if (notFound) this.out.println("User '" + recipient + "' not found.");
                        }
                    }

                /* Allow user to change rooms by typing '/room <room-name>' */
                } else if (fromClientArr[0] != null && fromClientArr[0].equals("/room")) {
                    String targetRoom = fromClientArr[1];
                    if (targetRoom != null && targetRoom.equals("which")) this.out.println("You are in room: " + currentChatRoom);
                    else if (targetRoom != null) {
                        String roomName = fromClientArr[1];
                        this.putInRoom(roomName);
                    } else {
                        this.out.println("You need to specify a room name.");
                    }

                /* Allow user (if admin) to add admins by typing '/+admin <user-id>'*/
                } else if (fromClientArr[0] != null && fromClientArr[0].equals("/+admin")) {
                    ChatRoom room = ChatServer.getRoom(currentChatRoom);
                    String target = fromClientArr[1];
                    if (room.isAdmin(clientID)) { // check if calling user is admin
                        if (target != null) {
                            room.makeAdmin(fromClientArr[1]);
                            if (room.isAdmin(fromClientArr[1]))
                                ChatServer.broadcastToARoom(currentChatRoom, clientID, target + " is now an admin of this room");
                            else this.out.println("Couldn't find " + target + ".");
                        }
                    } else
                        this.out.println("You're not an admin!!!!!!!!!!!!!!!!!!!11!!!!!!!!!!!!!!21!3#$!!!!14251612!");

                /* Allow user (if admin) to remove admins by typing '/-admin <admin-id>'*/
                } else if (fromClientArr[0] != null && fromClientArr[0].equals("/-admin")) {
                    ChatRoom room = ChatServer.getRoom(currentChatRoom);
                    String target = fromClientArr[1];
                    if (room.isAdmin(clientID)) {
                        if (target != null) {
                            if (room.isAdmin(fromClientArr[1])) {   // check if target is actually an admin
                                room.takeAdmin(fromClientArr[1]);
                                this.out.println(fromClientArr[1] + " is no longer an admin.");
                                ChatServer.broadcastToARoom(currentChatRoom, clientID, target + " has been stripped of admin rights.");
                            } else this.out.println(target + " isn't an admin to begin with.");
                        }
                    } else this.out.println("You're not an admin, n00b!");

                /* Allow user (if admin) to kick users by typing '/kick <user-id>'*/
                } else if (fromClientArr[0] != null && fromClientArr[0].equals("/kick")) {
                    ChatRoom room = ChatServer.getRoom(currentChatRoom);
                    String target = fromClientArr[1];
                    ChatServerThread targetThread = ChatServer.getUser(target);
                    if (room.isAdmin(clientID)) {
                        if (target != null && room.isInRoom(targetThread)) {
                            targetThread.out.println("You have been kicked by " + clientID + ".");
                            targetThread.putInRoom("lobby", false, true, target, clientID);
                        } else if (target != null) this.out.println("You need to specify a target user.");
                    } else this.out.println("You're not an admin, n()()b!");


                } else if (fromClientArr[0] != null && fromClientArr[0].equals("/topic")) {
                    ChatRoom room = ChatServer.getRoom(currentChatRoom);
                    String topic = fromClientArr[1];
                    for (int i = 2; i < fromClientArr.length; i++)
                        topic += " " + fromClientArr[2];
                    if (room.isAdmin(clientID)) {
                        if (topic != null) {
                            room.setTopic(topic);
                            this.out.println("Topic changed successfully!");
                            ChatServer.broadcastToARoom(currentChatRoom, "", "Topic has been changed by " + clientID + " to: " + topic );
                        }
                        else this.out.println("Please enter a valid topic.");
                    } else this.out.println("You need to be an admin to set the topic.");


                } else {
                /* Otherwise send the text to other clients */
                    ChatServer.broadcastToARoom(currentChatRoom, clientID, clientID + ": " + fromClient);
                    System.out.println("'" + clientID + "' said '" + fromClient + "' in room " + currentChatRoom);
                }
            } catch (IOException e) {
                /* On exception, stop the thread */
                System.out.println("IOException: " + e);
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void putInRoom(String roomName, boolean printStuff, boolean wasKicked, String idOfKicked, String idOfKicker) {
        if (roomName.equals(currentChatRoom) && printStuff) {
            this.out.println("You're already in " + currentChatRoom + ". Idiot.");
            return;
        }
        ChatRoom chatRoom = ChatServer.roomExists(roomName);
        if (chatRoom != null) {
            if (printStuff) {
                this.out.println("Switched to room: " + chatRoom.getName());
                System.out.println("'" + clientID + "' switched to room: " + chatRoom.getName());
            }
        } else {
            chatRoom = new ChatRoom(roomName);
            ChatServer.rooms.add(chatRoom);
            if (printStuff) {
                this.out.println("Created and switched to room: " + chatRoom.getName());
                System.out.println("'" + clientID + "' created and switched to room: " + chatRoom.getName());
            }
        }
        if (printStuff) {
            ChatServer.broadcastToARoom(currentChatRoom, clientID, clientID + " has moved to room: " + roomName);
            ChatServer.broadcastToARoom(roomName, clientID, clientID + " has joined this room.");
            chatRoom.printCurrentUsers(this);
        }
        if (wasKicked) {
            ChatServer.broadcastToARoom(currentChatRoom, idOfKicked, idOfKicked + " was KICKED (by " + idOfKicker + ") from this room.");
            ChatServer.broadcastToARoom(roomName, idOfKicked, idOfKicked + " has joined this room (as a result of being kicked).");
        }
        boolean makeAdmin = false;
        if (chatRoom.isEmpty()) makeAdmin = true;
        if (currentChatRoom == null) currentChatRoom = "lobby";
        ChatServer.getRoom(currentChatRoom).removeFromRoom(this);
        chatRoom.addToRoom(this);
        currentChatRoom = roomName;
        if (makeAdmin && !roomName.equals("lobby")) {
            chatRoom.admins.add(this);
            this.out.println("You are now an admin of this room.");
        }
        if (chatRoom.getTopic() != null) this.out.println(chatRoom.getName() + "'s topic is: " + chatRoom.getTopic());

    }

    public void putInRoom(String roomName) {
        putInRoom(roomName, true);
    }

    public void putInRoom(String roomName, boolean printStuff) {
        putInRoom(roomName, printStuff, false);
    }

    public void putInRoom(String roomName, boolean printStuff, boolean wasKicked) {
        putInRoom(roomName, printStuff, wasKicked, "", "");
    }

    public String makeValidId() {
        int id = (int) (Math.random() * 10000 * Math.random() + Math.random());
        for (ChatServerThread thread : ChatServer.threads) {
            if (thread.clientID.equals(Integer.toString(id))) break;
            id++;
        }
        return Integer.toString(id);
    }
}