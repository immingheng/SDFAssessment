package ibf2021.day10;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    /*
     * Main web server class:
     * Upon start, it should perform the following -
     * Open TCP connection and listen on port based on input port
     * Check for each of the docRoot path:
     * 1. Path exists
     * 2. Path is a directory
     * 3. Path is readable by server
     */

    // Delegate the IOException to the caller
    public void createServer(int port, List<String> docRoots) throws IOException {
        ServerSocket server = new ServerSocket(port);
        // Print out that server has been started and output a text to indicate so
        System.out.println("TCP connection is opened, currently listening to " + port);

        // Now loop through each of the string in docRoots to carry out verifications
        for (String dr : docRoots) {
            Path path = Paths.get(dr);
            File f = path.toFile();
            if (!f.exists()) {
                // path does not exists - print failure reason on console and stop server and
                // exit program
                System.err.println("The path does not exists!");
                server.close();
                System.exit(1);
                if (!f.isDirectory()) {
                    // path exists and but is directory - print failure reason on console and stop
                    // server and exit program
                    System.err.println("The path is not a directory!");
                    server.close();
                    System.exit(1);
                    if (!f.canRead()) {
                        // path exists and is a directory but cannot be read by server - print failure,
                        // stop server and exit program
                        System.err.println("The path cannot be read by the server!");
                        server.close();
                        System.exit(1);
                    }
                }
            }

        }

        // Create a threadpool with 3 threads:
        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        // Server will then listen on the specified port, accept incoming connections
        // from browser When a new connection is established, this connection should be
        // handled by a thread from the threadpool. The main control thread(server)
        // should go back to waiting for new incoming connections.

        // This block of code will run continuously due to the while(true) condition
        // Can implement watchdog Timer to time-out to shutdown threadpool and server
        // subsequently... but is not required in this assessment
        while (true) {
            System.out.println("Waiting for connection...");
            Socket socket = server.accept();
            HttpClientConnection myHttpThread = new HttpClientConnection(socket, docRoots);
            threadPool.submit(myHttpThread);
        }

    }

}
