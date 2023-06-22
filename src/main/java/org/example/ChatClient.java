package org.example;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

public class ChatClient {
  private final String host;
  private final int port;
  private final String username;
  private final String logFilename;

  public ChatClient(String host, int port, String username, String logFilename) {
    this.host = host;
    this.port = port;
    this.username = username;
    this.logFilename = logFilename;
  }

  public void start() {
    try (Socket socket = new Socket(host, port);
         BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
         Scanner scanner = new Scanner(System.in)) {

      output.println(username);

      new Thread(() -> {
        try {
          String message;
          while ((message = input.readLine()) != null) {
            System.out.println(message);
            logMessage(message);
          }
        } catch (IOException e) {
          System.err.println("Error occurred while receiving message from server: " + e.getMessage());
        }
      }).start();

      String message;
      while ((message = scanner.nextLine()) != null) {
        output.println(message);
        if (message.equalsIgnoreCase("/exit")) {
          break;
        }
      }
    } catch (IOException e) {
      System.err.println("Error occurred while connecting to server: " + e.getMessage());
    }
  }

  public void sendMessage(String message) {
    try (PrintWriter output = new PrintWriter(new Socket(host, port).getOutputStream(), true)) {
      output.println(message);
      logMessage(message);
    } catch (IOException e) {
      throw new RuntimeException(e);
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

  private static String loadSetting(String key, String defaultValue, String filename) {
    Properties properties = new Properties();
    Path path = Paths.get(filename);
    if (Files.exists(path)) {
      try (InputStream input = new FileInputStream(filename)) {
        properties.load(input);
        String value = properties.getProperty(key);
        if (value != null) {
          return value;
        }
      } catch (IOException e) {
        System.err.println("Error occurred while loading settings file: " + e.getMessage());
      }
    }
    return defaultValue;
  }

  public static void main(String[] args) {
    String host = loadSetting("host", "localhost", "settings.txt");
    int port = Integer.parseInt(loadSetting("port", "8085", "settings.txt"));
    String logFilename = loadSetting("logFilename", "client.log", "settings.txt");
    Scanner scanner = new Scanner(System.in);
    System.out.println("Enter your name:");
    String username = scanner.nextLine();

    ChatClient client = new ChatClient(host, port, username, logFilename);
    client.start();
  }
}

