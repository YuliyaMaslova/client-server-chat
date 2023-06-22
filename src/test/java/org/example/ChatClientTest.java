package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class ChatClientTest {
  private static final int PORT = 8085;
  private static final String HOST = "localhost";
  private static final String USERNAME = "Alice";
  private static final String LOG_FILENAME = "client.log";
  private static final String SETTINGS_FILENAME = "settings.txt";

  private ChatClient client;
  private Thread clientThread;
  private Thread serverThread;
  private File serverLog;
  private File clientLog;
  ServerSocket server;

  @BeforeEach
  void setUp() throws IOException {
    server = new ServerSocket(PORT);
    serverLog = File.createTempFile("log_", "log");
    clientLog = File.createTempFile("log_", "log");
//    ChatServer server = new ChatServer(PORT, serverLog.getAbsolutePath(), SETTINGS_FILENAME);
//    serverThread = new Thread(server::start);
    client = new ChatClient(HOST, PORT, USERNAME, clientLog.getAbsolutePath());
    clientThread = new Thread(client::start);

    clientThread.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    clientThread.interrupt();
    serverLog.delete();
    clientLog.delete();
    server.close();
  }

  @Test
  void testConnectToServer() {
    assertDoesNotThrow(() -> new Socket(HOST, PORT));
  }

  @Test
  void testSendMessage() throws IOException, InterruptedException {

    Socket serverSocket1 = server.accept();

    BufferedReader input1 = new BufferedReader(new InputStreamReader(serverSocket1.getInputStream()));
    String actual = input1.readLine();
    assertEquals(USERNAME, actual);
  }

  @Test
  void testClientLogsMessages() throws IOException, InterruptedException {
    Socket clientSocket = new Socket(HOST, PORT);

    client.sendMessage("Hello, Server!");

    Socket serverSocket1 = server.accept();
    BufferedReader input1 = new BufferedReader(new InputStreamReader(serverSocket1.getInputStream()));
    assertEquals(USERNAME, input1.readLine());

    Thread.sleep(100);

    try (BufferedReader reader = new BufferedReader(new FileReader(clientLog))) {
      String line = reader.readLine();
      assertNotNull(line);
      assertTrue(line.contains("Hello, Server!"));
    }

    clientSocket.close();
  }
}