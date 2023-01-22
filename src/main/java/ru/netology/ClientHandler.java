package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class ClientHandler implements Runnable {
  private Socket socket;

  public ClientHandler(Socket socket) {
    this.socket = socket;
  }

  public void run() {
    final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      // read only request line for simplicity
      // must be in form GET /path HTTP/1.1
      final var requestLine = in.readLine();
      final var parts = requestLine.split(" ");

      if (parts.length != 3) {
        // just close socket
      }

      final var path = parts[1];
      if (!validPaths.contains(path)) {
        reply(socket, 404, null, 0, null, null);
      }

      final var filePath = Path.of(".", "public", path);
      final var mimeType = Files.probeContentType(filePath);

      // special case for classic
      if (path.equals("/classic.html")) {
        final var content = modifyContentByTemplate(filePath, "{time}");
        reply(socket, 200, mimeType, content.length, content, null);
      } else {

        final var length = Files.size(filePath);
        reply(socket, 200, mimeType, length, null, filePath);
      }
    } catch (NoSuchFileException e) {
      throw new RuntimeException("File not found: " + e.getMessage());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void reply(Socket socket, int code, String mimeType, long length, byte[] content, Path filePath) {
    try (final var out = new BufferedOutputStream(socket.getOutputStream())) {
      String reply = null;
      switch (code) {
        case 404:
          reply = "HTTP/1.1 404 Not Found\r\n" +
                  "Content-Length: 0\r\n" +
                  "Connection: close\r\n" +
                  "\r\n";
          break;
        case 200:
          reply = "HTTP/1.1 200 OK\r\n" +
                  "Content-Type: " + mimeType + "\r\n" +
                  "Content-Length: " + length + "\r\n" +
                  "Connection: close\r\n" +
                  "\r\n";
          break;
        default:
          break;
      }
      if (reply != null) {
        out.write((reply).getBytes());
        if (content != null) {
          out.write(content);
        }
        if (filePath != null) {
          Files.copy(filePath, out);
        }
        out.flush();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] modifyContentByTemplate(Path filePath, String template) throws IOException {
    final var file = Files.readString(filePath);
    return file.replace(
            template,
            LocalDateTime.now().toString()
    ).getBytes();
  }
}
