package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class ChatServer {
  private final int port;
  private  String logFilename;
  private final String settingsFilename;

  final List<ClientHandler> clients = new ArrayList<>();

  public ChatServer(Integer port, String logFilename, String settingsFilename) {
    this.port = port;
    this.logFilename = logFilename;
    this.settingsFilename = settingsFilename;

    loadSettings();
  }

  public void start() {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("Chat server started on port " + port);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("New client connected: " + clientSocket);

        ClientHandler clientHandler = new ClientHandler(clientSocket);
        clients.add(clientHandler);
        clientHandler.start();
      }
    } catch (IOException e) {
      System.err.println("Error occurred while starting the chat server: " + e.getMessage());
    }
  }

  private class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final BufferedReader input;
    private final PrintWriter output;
    private String username;

    public ClientHandler(Socket clientSocket) throws IOException {
      this.clientSocket = clientSocket;
      input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      output = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    @Override
    public void run() {
      try {
        output.println("Welcome to the chat server!");
        username = input.readLine();
        System.out.println("New user joined the chat: " + username);

        for (ClientHandler client : clients) {
          if (client != this) {
            client.output.println(username + " has joined the chat.");
          }
        }
        logMessage(username + " has joined the chat.");

        String message;
        while ((message = input.readLine()) != null) {
          if (message.equalsIgnoreCase("exit")){
            break;
          }

          for (ClientHandler client : clients) {
            if (client != this) {
              client.output.println(username + ": " + message);
            }
          }
          logMessage(username + ": " + message);
          System.out.println(username + ": " + message);
        }
      } catch (IOException e) {
        System.err.println("Error occurred while handling client: " + e.getMessage());
      } finally {
        try {
          clientSocket.close();
          clients.remove(this);
          System.out.println(username + " has left the chat.");
          for (ClientHandler client : clients) {
            client.output.println(username + " has left the chat.");
          }
          logMessage(username + " has left the chat.");
        } catch (IOException e) {
          System.err.println("Error occurred while closing client socket: " + e.getMessage());
        }
      }
    }
  }

  private void logMessage(String message) {
    try (FileWriter writer = new FileWriter(logFilename, true)) {
      SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
      String time = formatter.format(new Date());
      writer.write("[" + time + "] " + message + "\n");
    } catch (IOException e) {
      System.err.println("Error occurred while writing to log file: " + e.getMessage());
    }
  }

  private void loadSettings() {
    Properties properties = new Properties();
    Path path = Paths.get(settingsFilename);
    if (Files.exists(path)) {
      try (InputStream input = new FileInputStream(settingsFilename)) {
        properties.load(input);
        if (logFilename == null) {
          String logFilename = properties.getProperty("logFilename");
          if (logFilename != null) {
            this.logFilename = logFilename;
          }
        }
      } catch (IOException e) {
        System.err.println("Error occurred while loading settings file: " + e.getMessage());
      }
    }
  }


  public static void main(String[] args) {
    ChatServer server = new ChatServer(8085, "server.log", "settings.txt");
    server.start();
  }
}
