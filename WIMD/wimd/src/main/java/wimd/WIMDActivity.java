package wimd;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class WIMDActivity extends RoomActivity {
    private static final int EXIT_COMMAND = 0;
    private static final int START_SCAN_COMMAND = 1;

    private Client client;
    private TextView tv;
    private TextView myLocationView;
    private String myLocation;

    @Override
    protected void setupContentView() {
        setContentView(R.layout.wimd);
        myLocationView = (TextView) findViewById(R.id.myLocation);
        tv = (TextView) findViewById(R.id.tvTextView);
        client = new Client(this);
        setMyLocation(PLACES[0]);
        startStalking();
        wifi.startScan();
    }

    // TODO Raminta: review && document
    @Override
    public void onReceiveWifiScanResults(List<ScanResult> results) {
        // Do not update every time
        // TODO Raminta: wrap with a thread and limit to certain number so that calculation is done in parallel? and also scheduleAtFixedRate?
        if (isActive && results!=null && results.size() > 0) {
            Map<String, Integer> m = new HashMap<>();
            for (ScanResult result : results) {
                m.put(result.BSSID, result.level);
            }

            Fingerprint f = app.findRoom(measurements, m);
            if(f!=null) setMyLocation(f.getRoom());
            scanNext();
        }
    }

    public void scanNext() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                wifi.startScan();
            }

        }, WAIT_TIME);
    }

    // TODO Raminta: maybe show some more meaningful message instead of "Unknown". Rename
    public void startStalking() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                while(isActive){
                    setTVLocation(client.getLocation());
                }
            }

        }, 0, WAIT_TIME);
    }

    private void setMyLocation(final String room) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myLocation = room;
                myLocationView.setText(myLocation);
                client.setLocation(myLocation);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, EXIT_COMMAND, Menu.NONE, "Exit");
        menu.add(Menu.NONE, START_SCAN_COMMAND, Menu.NONE, "Update Database");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == EXIT_COMMAND) {
            finish();
        } else if (id == START_SCAN_COMMAND) {
            Intent intent = new Intent(WIMDActivity.this, ScanDataActivity.class);
            startActivity(intent);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void setTVLocation(final String location){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.setText(location);
            }
        });
    }
}