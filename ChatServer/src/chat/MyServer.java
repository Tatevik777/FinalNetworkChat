package chat;

import chat.auth.AuthService;
import chat.auth.BaseAuthService;
import chat.handler.ClientHandler;
import clientserver.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
// Логика взаимрлействия клиента и сервера

public class MyServer {
    private final ServerSocket serverSocket;
    private final AuthService authService;
    private final List<ClientHandler> clients = new ArrayList<>();

    public MyServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.authService = new BaseAuthService();
        //Сокет сервера, который принимает клиентские сокеты
    }


    public void start() throws IOException {  //бесконечный цикл по ожиданию подключения
        System.out.println("Сервер запущен!");

        try {
            while (true) {
                waitAndProcessNewClientConnection();
            }
        } catch (IOException e) {
            System.out.println("Ошибка создания нового подключения");
            e.printStackTrace();
        } finally {
            serverSocket.close();
        }
    }

    private void waitAndProcessNewClientConnection() throws IOException {
        System.out.println("Ожидание пользователя...");
        Socket clientSocket = serverSocket.accept();//после подключения пользователя получаем
                                                    // клиентский сокет для вхаимодействия

        clientSocket.setSoTimeout(120000);

        System.out.println("Клиент подключился!");
        processClientConnection(clientSocket);
    }

    private void processClientConnection(Socket clientSocket) throws IOException {
        ClientHandler clientHandler = new ClientHandler(this, clientSocket);
        clientHandler.handle();
    }//новый поток для подключения и взимодействия с пользователем

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized boolean isUsernameBusy(String clientUsername) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(clientUsername)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void subscribe(ClientHandler clientHandler) throws IOException {
        clients.add(clientHandler);
        List<String> usernames = getAllUsernames();
        broadcastMessage(null, Command.updateUsersListCommand(usernames));
    }

    private List<String> getAllUsernames() {
        List<String> usernames = new ArrayList<>();
        for (ClientHandler client : clients) {
            usernames.add(client.getUsername());
        }
        return usernames;
    }

    public synchronized void unSubscribe(ClientHandler clientHandler) throws IOException {
        clients.remove(clientHandler);
        List<String> usernames = getAllUsernames();
        broadcastMessage(null, Command.updateUsersListCommand(usernames));
    }

    public synchronized void broadcastMessage(ClientHandler sender, Command command) throws IOException {
        for (ClientHandler client : clients) {
            if (client == sender) {
                continue;
            }
            client.sendMessage(command);

        }
    }

    public synchronized void sendPrivateMessage(String recipient, Command command) throws IOException {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(recipient)) {
                client.sendMessage(command);
                break;
            }
        }
    }
}