package wimd;

import java.net.*;
import java.io.*;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.content.Context;

import wimd.Interfaces.WebclientInterface;

public class Webclient implements WebclientInterface {

    protected static final String SEPARATOR = "#";

    private String mac;
    private Socket socket;
    private PrintWriter pw;

    private String location = null;
    private int timestamp = Integer.MAX_VALUE;

    public Webclient(Context context) {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        mac = info.getMacAddress();

        try {
            socket = new Socket("192.168.1.100",4031); //TODO change to ip where the server is running
            pw= new PrintWriter(socket.getOutputStream(),true);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setLocation(String location) {
        pw.println(mac + SEPARATOR + System.currentTimeMillis() / 1000 + SEPARATOR + location);

        try {
            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();

            String[] fields = line.split(SEPARATOR);
            location = fields[0];
            timestamp = Integer.parseInt(fields[1]);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getLocation() {
        if(location==null) return "Unknown location";
        else if(timestamp>360) return "Your partner is probably dead."; // 6 hours
        else if(timestamp>60) return "There currently is no connection.";
        else return "Your partner is at " + location;
    }
}
