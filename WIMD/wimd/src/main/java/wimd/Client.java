package wimd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import wimd.Interfaces.ClientInterface;

public class Client implements ClientInterface {

    private static final String IP = "192.168.1.100"; //TODO change to your pcs local ip
    private static final String SEPARATOR = "#";

    private BufferedReader in;
    private PrintWriter out;
    private String myLocation = "Unknown";
    private long myTimestamp = Integer.MAX_VALUE;
    private String location;
    private long timestamp;

    public Client() {
        try {
            Socket socket = new Socket(IP, 4031);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    String data = myLocation + SEPARATOR + myTimestamp;
                    out.println(data);

                    try {
                        String line = in.readLine();
                        String[] fields = line.split(SEPARATOR);
                        location = fields[0];
                        timestamp = Integer.parseInt(fields[1]);
                        Thread.sleep(1000);
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void setLocation(String location) {
        myLocation = location;
        myTimestamp = System.currentTimeMillis()/1000;
    }

    @Override
    public String getLocation() {
        setLocation(myLocation);

        if(location==null) return "Unknown location";
        else if(timestamp>360) return "Your partner is probably dead."; // 6 hours
        else if(timestamp>60) return "There currently is no connection.";
        else return "Your partner is at " + location;
    }
}