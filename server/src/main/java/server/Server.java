package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int PORT = 9090;
    private ServerSocket server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private List<ClientHandler> clients;
    private AuthService authService;

    private Connection connection;
    private Statement statement;

    public Server() {
        clients = new CopyOnWriteArrayList<>();
//        authService = new SimpleAuthService();
        //===========//
        if (!SQLHandler.connect()) {
            throw new RuntimeException("Не удалось подключиться к БД!");
        }
        authService = new DBAuthService();
        //===========//

        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started");

            while (true) {
                socket = server.accept();
                System.out.println("Client connected");
                System.out.println("Client: " + socket.getRemoteSocketAddress());
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SQLHandler.disconnect();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMessage(ClientHandler sender, String msg) {
        String message = String.format("[ %s ]: %s", sender.getNickname(), msg);
        //============//
        SQLHandler.addMessage(sender.getNickname(), "null", msg, "once upon a time");
        //============//

        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void privateMessage(ClientHandler sender, String receiver, String msg) {
        String message = String.format("[ %s ] to [ %s ]: %s", sender.getNickname(), receiver, msg);
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(receiver)) {
                client.sendMessage(message);
                //============//
                SQLHandler.addMessage(sender.getNickname(), receiver, msg, "once upon a time");
                //============//

                if (!client.equals(sender)) {
                    sender.sendMessage(message);
                }
                return;
            }
        }
        sender.sendMessage("Пользователь " + receiver + " не найден.");
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientlist();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientlist();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler client : clients) {
            if (client.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientlist() {
        StringBuilder stringBuilder = new StringBuilder(Command.CLIENT_LIST);
        for (ClientHandler client : clients) {
            stringBuilder.append(" ").append(client.getNickname());
        }

        String message = stringBuilder.toString();

        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:db_NetworkChat");
        statement = connection.createStatement();
    }

    private void disconnect() {
        try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
