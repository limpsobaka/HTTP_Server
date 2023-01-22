package ru.netology;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
  private final int threadsQuantity = 64;
  private static Server instance;

  private Server() {
  }

  public static Server getInstance() {
    if (instance == null) {
      instance = new Server();
    }
    return instance;
  }

  public void startServer() {
    try (final var serverSocket = new ServerSocket(9999)) {
      ExecutorService executor = Executors.newFixedThreadPool(threadsQuantity);
      while (true) {
        final var socket = serverSocket.accept();
        ClientHandler clientHandler = new ClientHandler(socket);
        executor.execute(clientHandler);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
