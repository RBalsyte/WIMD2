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
    private String myLocation;
    private String location;
    private int timestamp;

    public Client() {
        try {
            Socket socket = new Socket(IP, 4031);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setLocation(String location) {
        myLocation = location;
        String data = location + SEPARATOR + System.currentTimeMillis() / 1000;
        out.println(data);

        try {
            String line = in.readLine();
            String[] fields = line.split(SEPARATOR);
            this.location = fields[0];
            timestamp = Integer.parseInt(fields[1]);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
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