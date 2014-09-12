import java.util.ArrayList;

/**
 * Created by ethan on 9/11/14.
 */
public class ChatRoom {
    public String name;
    public ArrayList<ChatServerThread> users = new ArrayList<ChatServerThread>(0);
    public ArrayList<ChatServerThread> admins = new ArrayList<ChatServerThread>(0);

    public ChatRoom(String name) {
        this.name = name;
    }

    public boolean setName(String name) {
        if (name == null) return false; // TODO make System and thread print out error messages
        for (ChatRoom room : ChatServer.rooms) {
            if (name.equals(room.name)) return false;
        }
        this.name = name;
        return true;
    }

    public String getName() {
        return name;
    }

    public void addToRoom(ChatServerThread user) {
        users.add(user);
    }

    public void removeFromRoom(ChatServerThread user) {
        users.remove(user);
    }

    public boolean isInRoom(ChatServerThread user) {
        for (ChatServerThread thread : users) {
            if (user == thread) return true;
        }
        return false;
    }

    public void announceToRoom(String id, String message) {
        for (ChatServerThread thread : users) {
            if (thread.clientID.equals(id)) continue;
            thread.out.println(message);
        }
    }

    public void printCurrentUsers(ChatServerThread thread) {
        for (ChatServerThread user : users) {
            thread.out.print("    " + user.clientID);
        }
        if (users.size() == 0) thread.out.println("You're the only one here :(");
    }

    public String listCurrentUsers() {
        if (users.size() == 0) return "empty";
        String output = "";
        for (ChatServerThread user : users)
            output += user.clientID + " ";
        return output;

    }

    public boolean isEmpty() {
        if (users.size() == 0) return true;
        return false;
    }

    public boolean isAdmin(String id) {
        for (ChatServerThread admin : admins)
            if (id.equals(admin.clientID)) return true;
        return false;
    }

    public void makeAdmin(String id) {
        for (ChatServerThread user : users) {
            if(user.clientID.equals(id)) admins.add(user);
        }
    }

    public void takeAdmin(String clientID) {
        for (ChatServerThread admin : admins) {
            if (admin.clientID.equals(clientID)) admins.remove(admin);
        }
    }


}
