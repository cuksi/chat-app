package com.codegym.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Enter server port:");
        int serverPort = ConsoleHelper.readInt();

        try(ServerSocket ss = new ServerSocket(serverPort)) {

            ConsoleHelper.writeMessage("Server is running");

            while (true) {

                Handler handler = new Handler(ss.accept());
                handler.start();



            }


        } catch (IOException e) {

            ConsoleHelper.writeMessage("Error");;

        }


    }

    private static class Handler extends Thread {

        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }


        @Override
        public void run() {

            ConsoleHelper.writeMessage("New connection established " + socket.getRemoteSocketAddress());

            String newClient= null;

            try (Connection connection = new Connection(socket)) {

                newClient = serverHandshake(connection);

                sendBroadcastMessage(new Message(MessageType.USER_ADDED, newClient));

                notifyUsers(connection, newClient);

                serverMainLoop(connection, newClient);


            } catch (IOException | ClassNotFoundException e) {

                ConsoleHelper.writeMessage("error occurred while communicating with the remote address " + socket.getRemoteSocketAddress());


            }

            if (newClient != null) {

                connectionMap.remove(newClient);

                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, newClient));

            }

            ConsoleHelper.writeMessage("connection is closed with the remote address: " + socket.getRemoteSocketAddress() );


        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {

            while (true){

                connection.send(new Message(MessageType.NAME_REQUEST));

                Message receivedMessage = connection.receive();

                String userName = receivedMessage.getData();

                if (receivedMessage.getType() != MessageType.USER_NAME) {

                    ConsoleHelper.writeMessage("Invalid user name " + socket.getRemoteSocketAddress());

                    continue;

                }

                if (userName.isEmpty()) {

                    ConsoleHelper.writeMessage("Invalid user name " + socket.getRemoteSocketAddress());

                    continue;


                }

                if (connectionMap.containsKey(userName)) {

                    ConsoleHelper.writeMessage("Invalid user name " + socket.getRemoteSocketAddress());

                    continue;
                }


                connection.send(new Message(MessageType.NAME_ACCEPTED));

                connectionMap.put(userName, connection);

                return userName;

            }



        }

        private void notifyUsers(Connection connection, String userName) throws IOException {

            for (Map.Entry<String, Connection> map : connectionMap.entrySet()) {

                if (map.getKey().equals(userName)) {

                    continue;
                }

                connection.send(new Message(MessageType.USER_ADDED, map.getKey()));

            }

        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {


            while (true) {

                Message message = connection.receive();

                if (message.getType() == MessageType.TEXT) {

                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));

                } else {

                    ConsoleHelper.writeMessage("Error - message not accepted " + socket.getRemoteSocketAddress());

                }

            }

        }

    }

    public static void sendBroadcastMessage(Message message) {

        for (Map.Entry<String, Connection> map : connectionMap.entrySet()) {

            try {

                map.getValue().send(message);

            } catch (IOException e) {

                ConsoleHelper.writeMessage("Message couldn't be sent.");

            }

        }

    }

}
