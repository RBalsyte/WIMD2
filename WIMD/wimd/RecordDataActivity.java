package wimd;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

//TODO Raminta: rename everything to "scan" instead of "record(ing)"?

public class RecordDataActivity extends RoomActivity {

    private static final String RECORDING = " Recording...";

    /* The minimum amount of records need to be taken for a successful recording */
    private static final int MIN_COUNT = 3;
    /* Count of records taken for average calculation*/
    private static final int COUNT = 3;
    /* Counts how many records should we still take*/
    private int counter = 0;

    private int START_COMMAND;
    private int EXIT_COMMAND;
    private int TOGGLE_FINGERPRINTS_COMMAND;
    private int DELETE_FINGERPRINTS_COMMAND;
    private int ROOM_COMMAND;
    private int NO_ROOM_COMMAND;

    /* A map containing command ids and their names <command_id, command_name>*/
    private Map<Integer, String> commands;
    /* Bar that is displayed during the scan */
    private ProgressDialog pDialog;
    /* Sub Menu when prompted to select a room for the scan*/
    protected SubMenu rSubMenu;
    /* Sub Menu containing options related to fingerprints (show/hide/delete)*/
    protected SubMenu fSubMenu;
    protected MenuItem toggleFingerprintVisibility;

    private int selectedRoom;
    private boolean isFingerprintsVisible = true;

    AlertDialog noRoomSelectedDialog;
    AlertDialog deleteAllDialog;

    @Override
    protected void setupContentView() {
        setTitle("Recording Fingerprints");
        setContentView(R.layout.recording_layout);
        setupDialogs();
    }

    private void setupDialogs(){
        noRoomSelectedDialog = new AlertDialog.Builder(RecordDataActivity.this).create();
        noRoomSelectedDialog.setTitle("Alert");
        noRoomSelectedDialog.setMessage("No Room selected.\nPlease select a room to start recording.");
        noRoomSelectedDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );

        AlertDialog.Builder builder =  new AlertDialog.Builder(RecordDataActivity.this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are sure you want to delete all fingerprints from the database?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // delete all fingerprints from the database
                app.deleteAllFingerprints();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        deleteAllDialog = builder.create();
    }

    // TODO Raminta: document this
    @Override
    public void onReceiveWifiScanResults(List<ScanResult> results) {
        if (isActive && counter != 0 && selectedRoom > NO_ROOM_COMMAND) {
            // accept only scans with enough found access points
            if (results.size() >= MIN_COUNT) {
                counter--;

                // add scan results to hashmap
                Map<String, Integer> m = new HashMap<>();
                for (ScanResult result : results) {
                    m.put(result.BSSID, result.level);
                }

                TreeSet<String> keys = new TreeSet<>();
                keys.addAll(measurements.keySet());
                keys.addAll(m.keySet());

                // go through scans results and calculate new sum values for each measurement
                for (String key : keys) {
                    Integer value = measurements.get(key);
                    Integer oldValue = m.get(key);

                    // calculate new value for each measurement (sum of all part-scans)
                    if (oldValue == null) {
                        measurements.put(key, value + (Fingerprint.DEFAULT_RSS * (COUNT - 1 - counter)));
                    } else if (value == null) {
                        measurements.put(key, Fingerprint.DEFAULT_RSS + oldValue);
                    } else {
                        measurements.put(key, value + oldValue);
                    }
                }


                if (counter > 0) { // keep on scanning
                    scanNext();
                } else { // calculate averages from sum values of measurements and add them to fingerprint
                    // calculate average for each measurement
                    for (String key : measurements.keySet()) {
                        int value = measurements.get(key) / COUNT;
                        measurements.put(key, value);
                    }

                    // create fingerprint with the calculated measurement averages
                    Fingerprint f = new Fingerprint(measurements, commands.get(selectedRoom));
                    app.addFingerprint(f); // add to database
                    pDialog.dismiss(); // hide loading bar
                }
            } else { // did not find enough access points, show error to user
                pDialog.dismiss(); // hide loading bar
                Toast.makeText(getApplicationContext(), "Failed to create fingerprint. Could not find enough access points (found "
                        + results.size() + ", need at least " + MIN_COUNT + ").", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void startScan() {
        if (selectedRoom > NO_ROOM_COMMAND) {
            noRoomSelectedDialog.show();
        } else {
            counter = COUNT;
            measurements = new HashMap<>();
            pDialog = ProgressDialog.show(this, "", RECORDING, true);
            wifi.startScan();
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

    // TODO Raminta: implement some kind of list for showing all fingerprints for selected room
    public void setFingerprintsVisible(boolean visible) {
        isFingerprintsVisible = visible;
        toggleFingerprintVisibility.setTitle(isFingerprintsVisible ? commands.get(TOGGLE_FINGERPRINTS_COMMAND + 1) : commands.get(TOGGLE_FINGERPRINTS_COMMAND));
    }

    public void deleteAllFingerprints() {
        deleteAllDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        commands = new HashMap<>();

        int id = 0;

        START_COMMAND = id;
        commands.put(id++, "Start Recording");
        EXIT_COMMAND = id;
        commands.put(id++, "Exit");
        int fingerprint_cmd = id;
        commands.put(id++, "Fingerprints");
        TOGGLE_FINGERPRINTS_COMMAND = id;
        commands.put(id++, "ShowFingerprints");
        commands.put(id++, "HideFingerprints");
        DELETE_FINGERPRINTS_COMMAND = id;

        commands.put(id++, "Hide Fingerprints");
        commands.put(id++, "Show Fingerprints");
        commands.put(id++, "Delete All Fingerprints");
        ROOM_COMMAND = id;
        commands.put(id++, "Choose Room");
        for (int place = 0; id < PLACES.length; place++, id++) {
            commands.put(id, PLACES[place]);
        }

        for (id = 0; id < commands.size(); id++) {
            if (id == START_COMMAND || id == EXIT_COMMAND) {
                menu.add(Menu.NONE, id, Menu.NONE, commands.get(id));
            } else if (id == fingerprint_cmd) {
                fSubMenu = menu.addSubMenu(Menu.NONE, id, Menu.NONE, commands.get(id));
            } else if (id == TOGGLE_FINGERPRINTS_COMMAND) {
                toggleFingerprintVisibility = fSubMenu.add(Menu.NONE, TOGGLE_FINGERPRINTS_COMMAND, Menu.NONE, (isFingerprintsVisible ? commands.get(id + 1) : commands.get(id)));
            } else if (id == DELETE_FINGERPRINTS_COMMAND) {
                fSubMenu.add(Menu.NONE, id, Menu.NONE, commands.get(id));
            } else if (id == ROOM_COMMAND) {
                rSubMenu = menu.addSubMenu(Menu.NONE, id, Menu.NONE, commands.get(id));
            } else if (id == ROOM_COMMAND + 1){
                NO_ROOM_COMMAND = id;
                rSubMenu.add(Menu.NONE, id, Menu.NONE, commands.get(id));
            } else if (id > ROOM_COMMAND) {
                rSubMenu.add(Menu.NONE, id, Menu.NONE, commands.get(id));
            }
        }
        super.onCreateOptionsMenu(menu);

        TextView selectedRoomView = (TextView) findViewById(R.id.selected_room_View);
        selectedRoomView.setText(commands.get(ROOM_COMMAND + 1));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == START_COMMAND) {
            startScan();
        } else if (id == EXIT_COMMAND) {
            finish();
        } else if (id == TOGGLE_FINGERPRINTS_COMMAND) {
            setFingerprintsVisible(!isFingerprintsVisible);
        } else if (id == DELETE_FINGERPRINTS_COMMAND) {
            deleteAllFingerprints();
        } else if (id > ROOM_COMMAND && id < commands.size()) {
            selectedRoom = id;
            TextView selectedRoomView = (TextView) findViewById(R.id.selected_room_View);
            selectedRoomView.setText(commands.get(id));
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
