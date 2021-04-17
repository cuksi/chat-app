package com.codegym.task.task30.task3008.client;

import com.codegym.task.task30.task3008.Connection;
import com.codegym.task.task30.task3008.ConsoleHelper;
import com.codegym.task.task30.task3008.Message;
import com.codegym.task.task30.task3008.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {

    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {

        Client client = new Client();
        client.run();

    }


    public class SocketThread extends Thread {

        protected void processIncomingMessage (String message) {

            ConsoleHelper.writeMessage(message);

        }

        protected void informAboutAddingNewUser(String userName) {

            ConsoleHelper.writeMessage(userName + " has joined the chat.");

        }

        protected void informAboutDeletingNewUser(String userName) {

            ConsoleHelper.writeMessage(userName + " has left the chat.");

        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {

            Client.this.clientConnected = clientConnected;

            synchronized (Client.this) {

                Client.this.notify();
            }

        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {

            while (true) {

                Message receivedMessage = connection.receive();

                if (receivedMessage.getType() == MessageType.NAME_REQUEST) {

                    connection.send(new Message(MessageType.USER_NAME, getUserName()));

                } else if (receivedMessage.getType() == MessageType.NAME_ACCEPTED) {

                    notifyConnectionStatusChanged(true);

                    break;

                } else {

                    throw new IOException("Unexpected MessageType");

                }


            }

        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {

            while (true) {

                Message receivedMessage = connection.receive();

                if (receivedMessage.getType() == MessageType.TEXT) {

                    processIncomingMessage(receivedMessage.getData());

                }else if (receivedMessage.getType() == MessageType.USER_ADDED) {

                    informAboutAddingNewUser(receivedMessage.getData());

                }else if (receivedMessage.getType() == MessageType.USER_REMOVED) {

                    informAboutDeletingNewUser(receivedMessage.getData());

                } else {

                    throw new IOException("Unexpected MessageType");

                }

            }

        }

        @Override
        public void run() {

            String serverAddress = getServerAddress();
            int serverPort = getServerPort();

            try {

                Socket socket = new Socket(serverAddress, serverPort);

                connection = new Connection(socket);

                clientHandshake();
                clientMainLoop();

            } catch (IOException | ClassNotFoundException e) {

                notifyConnectionStatusChanged(false);

            }

        }
    }

    protected String getServerAddress() {

        ConsoleHelper.writeMessage("Enter server address: ");

        return ConsoleHelper.readString();

    }

    protected int getServerPort() {

        ConsoleHelper.writeMessage("Enter server port:");

        return ConsoleHelper.readInt();

    }

    protected String getUserName() {

        ConsoleHelper.writeMessage("Choose user name:");

        return ConsoleHelper.readString();

    }

    protected boolean shouldSendTextFromConsole() {

        return true;

    }

    protected SocketThread getSocketThread() {

        return new SocketThread();

    }

    protected void sendTextMessage(String text) {

        try {

            connection.send(new Message(MessageType.TEXT, text));

        }catch (IOException e) {

            ConsoleHelper.writeMessage("Error sending message!");

            clientConnected = false;

        }

    }

    public void run() {

        Thread helpThread = getSocketThread();
        helpThread.setDaemon(true);

        helpThread.start();

        try {

            synchronized (this) {

                wait();

            }

        }catch (InterruptedException e) {

            ConsoleHelper.writeMessage("Error");
            return;

        }

        if (clientConnected)
            ConsoleHelper.writeMessage("A connection has been established. To exit, enter 'exit'.");
        else
            ConsoleHelper.writeMessage("An error occurred while running the client.");

        while (clientConnected) {

            String text = ConsoleHelper.readString();

            if (text.equals("exit")) {

                clientConnected = false;
                break;

            }

            if (shouldSendTextFromConsole()) {

                sendTextMessage(text);

            }

        }

    }
}
