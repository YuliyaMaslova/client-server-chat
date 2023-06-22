package org.example;

import org.example.ChatServer;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class ChatServerTest {
  private static final int PORT = 8085;
  private static final String SETTINGS_FILENAME = "settings.txt";

  private ChatServer server;
  private File log;
  private Thread serverThread;

  @BeforeEach
  void setUp() throws IOException {
    log = File.createTempFile("log_", "log");
    server = new ChatServer(PORT, log.getAbsolutePath(), SETTINGS_FILENAME);
    serverThread = new Thread(server::start);
    serverThread.start();
  }

  @AfterEach
  void tearDown() {
    log.delete();
    serverThread.interrupt();
  }

  @Test
  void testClientConnection() throws IOException {
    Socket socket = new Socket("localhost", PORT);
    assertNotNull(socket);
    socket.close();
  }

  @Test
  void testClientReceivesWelcomeMessage() throws IOException {
    Socket socket = new Socket("localhost", PORT);
    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    assertEquals("Welcome to the chat server!", input.readLine());
    socket.close();
  }

  @Test
  void testClientCanSendMessage() throws IOException {
    Socket socket1 = new Socket("localhost", PORT);
    Socket socket2 = new Socket("localhost", PORT);

    PrintWriter output1 = new PrintWriter(socket1.getOutputStream(), true);
    BufferedReader input2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));

    output1.println("Alice");
    input2.readLine();
    output1.println("Hello, Bob!");

    assertEquals("Welcome to the chat server!", input2.readLine());
    assertEquals("Alice: Hello, Bob!", input2.readLine());

    socket1.close();
    socket2.close();
  }

  @Test
  void testServerLogsMessages() throws IOException, InterruptedException {
    Socket socket = new Socket("localhost", PORT);
    PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

    output.println("Alice");
    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    input.readLine(); // Skip welcome message

    output.println("Hello, Bob!");

    Thread.sleep(100);

    try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
      assertTrue(reader.readLine().contains("Alice has joined the chat"));
      assertTrue(reader.readLine().contains("Alice: Hello, Bob!"));
    }

    socket.close();
  }
}