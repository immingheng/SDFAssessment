package ibf2021.day10;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// Class that handles the request and response communication between server and
// client and it reads the first line from incoming request and perform an action accordingly
public class HttpClientConnection implements Runnable {

    // Initialisation of members
    private final Socket socket;
    private List<String> docRoots;
    private boolean resourceExists;
    private boolean resourceEndsWPNG;
    private Path fp;

    // Constructor to use this socket.
    public HttpClientConnection(Socket socket, List<String> docRoots) {
        this.socket = socket;
        this.docRoots = docRoots;
    }

    @Override
    public void run() {
        try (InputStream is = socket.getInputStream(); OutputStream os = socket.getOutputStream()) {
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            Reader reader = new InputStreamReader(dis);
            BufferedReader br = new BufferedReader(reader);

            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            Writer writer = new OutputStreamWriter(dos);
            BufferedWriter bw = new BufferedWriter(writer);

            // Reads the first line from the browser (Client)
            String line = br.readLine();
            System.out.println(line); // Check what does the browser send back
            // The line will be in the format of : GET /index.html HTTP/1.1
            // Split client input based on space delimiter and assign it to a string list
            List<String> ClientInput = new ArrayList<>();
            String[] lineArr = line.split(" ");
            for (String item : lineArr) {
                ClientInput.add(item);
            }
            // Can add an additional method here to check ClientInput is in the right format
            // However, due to time constraint, simply assume user input is in the right
            // format....

            // Handle various response now:
            String msg = "";
            while (!socket.isClosed()) {
                // 1. If request is not a GET method, return a response
                if (!ClientInput.get(0).equals("GET")) {
                    msg = "HTTP/1.1 405 Method Not Allowed\r\n\r\n" + ClientInput.get(0)
                            + " not supported\r\n";
                    bw.write(msg);
                    bw.flush();
                    socket.close(); // close socket and terminate loop hence exiting the thread
                } else {
                    // Check if resource name is simply a "/", if it is, reassign ClientInput to be
                    // "/index.html" prior to file search.
                    if (ClientInput.get(1).equals("/")) {
                        // remove existing
                        ClientInput.remove(1);
                        // add new
                        ClientInput.add(1, "/index.html");
                    }

                    // Search if resource exists and returns a boolean accordingly
                    for (String s : docRoots) {
                        String filePath = s + ClientInput.get(1); // Concatenate the path and the file resource
                        File resourceFile = Paths.get(filePath).toFile(); // Create a file instance of the abstract
                        if (resourceFile.exists()) {
                            if (ClientInput.get(1).endsWith(".png")) {
                                resourceEndsWPNG = true;
                            }
                            fp = resourceFile.toPath(); // fp is used subsequently to readAllbytes within the file
                            resourceExists = true;
                        } else {
                            resourceExists = false;
                        }

                    }
                    // 2. If requested resource is not found, return a response
                    if (resourceExists == false) {
                        msg = "HTTP/1.1 404 Not Found\r\n\r\n" + ClientInput.get(1) + " not found\r\n";
                        bw.write(msg);
                        bw.flush();
                        socket.close();
                        // 4. Requested resource exists and is a png file, send the resource contents
                        // as Byte back to the client. As this condition is stricter than the check
                        // file.exists, it comes before that condition.
                    } else if (resourceExists == true && resourceEndsWPNG == true) {
                        msg = "HTTP/1.1 200 OK\r\nContent-Type: image/png\r\n\r\n";
                        bw.write(msg);
                        bw.flush();
                        byte[] resourceContent = Files.readAllBytes(fp);
                        dos.write(resourceContent, 0, resourceContent.length);
                        dos.flush();
                        socket.close();
                        // 3. Requested resource exists - return string and resource Bytes to Client.
                    } else if (resourceExists == true) {
                        // Send the string text over
                        msg = "HTTP/1.1 200 OK\r\n\r\n";
                        bw.write(msg);
                        bw.flush();
                        // Read resource content as bytes via Files and write it over to Client
                        byte[] resourceContent = Files.readAllBytes(fp);
                        dos.write(resourceContent, 0, resourceContent.length);
                        dos.flush();
                        socket.close();
                    }

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
