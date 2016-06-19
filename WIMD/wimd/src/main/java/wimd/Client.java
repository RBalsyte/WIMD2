package wimd;

import android.content.Context;

import android.net.wifi.WifiManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import wimd.Interfaces.ClientInterface;

public class Client implements ClientInterface {

    private static final String IP = "192.168.1.100"; //TODO change to your pcs local ip
    private static final String SEPARATOR = "#";

    private String mac;
    private String myLocation = "Unknown";
    private long myTimestamp = Integer.MAX_VALUE;
    private String location;
    private long timestamp;
    private BufferedReader in;
    private PrintWriter out;

    public Client(Context context) {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mac = manager.getConnectionInfo().getMacAddress();

        handle();
    }

    @Override
    public void setLocation(String location) {
        myLocation = location;
        myTimestamp = System.currentTimeMillis()/1000;

        handle();
    }

    @Override
    public String getLocation() {
        setLocation(myLocation);
        long time = System.currentTimeMillis()/1000-timestamp;

        if(location==null) return "Unknown location";
        else if(time>360) return "Your partner is probably dead."; // 6 hours
        else if(time>60) return "There currently is no connection.";
        else return "Your partner is at " + location;
    }

    public void handle() {
        try {
            Socket socket = new Socket(IP, 4031);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String data = myLocation + SEPARATOR + myTimestamp + SEPARATOR + mac;
            out.println(data);

            String line = in.readLine();
            if(line!=null) {
                String[] fields = line.split(SEPARATOR);
                location = fields[0];
                timestamp = Integer.parseInt(fields[1]);
            }
        } catch (IOException e) {
            return;
        }
    }
}