import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import client.ClientRequestHandler;

public class Main {
  private static final int PORT = 4221;
  private static final int THREAD_POOL_SIZE = 3;
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");
    System.out.println("args: ");
    for(String arg: args) {
      System.out.println(arg);
    }
    ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    try(ServerSocket serverSocket = new ServerSocket(PORT)) {
      serverSocket.setReuseAddress(true);
      while(true) {
        Socket clientSocket = serverSocket.accept();
        ClientRequestHandler clientRequestHandler = new ClientRequestHandler(clientSocket, args);
        executorService.submit(clientRequestHandler);
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      executorService.shutdown();
    }
  }
}
