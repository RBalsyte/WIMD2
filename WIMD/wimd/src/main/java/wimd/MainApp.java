package wimd;

import android.app.Application;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

public class MainApp extends Application {

    private ArrayList<Fingerprint> fingerprints;
    private FingerprintDatabaseHandler fingerprintDatabaseHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        fingerprintDatabaseHandler = new FingerprintDatabaseHandler(this);
        fingerprints = fingerprintDatabaseHandler.getAllFingerprints();
    }

    public ArrayList<Fingerprint> getFingerprintData() {
        return fingerprints;
    }

    public Fingerprint findRoom(Map<String, Integer> measurements, Map<String, Integer> newMeasurements){
        TreeSet<String> keys = new TreeSet<String>();
        keys.addAll(measurements.keySet());
        keys.addAll(newMeasurements.keySet());

        // calculate access point signal strengths with weighted averages
        // (adjust to sudden big changes in received signal strengths)
        for (String key : keys) {
            Integer value = newMeasurements.get(key);
            Integer oldValue = measurements.get(key);
            if(oldValue == null) {
                measurements.put(key, value);
            } else if(value == null) {
                measurements.remove(key);
            } else {
                value = (int) (oldValue * 0.4f + value * 0.6f);
                measurements.put(key, value);
            }
        }

        Fingerprint f = new Fingerprint(measurements);

        // find fingerprint closest to our location (one with the smallest euclidean distance to us)
        return f.getClosestMatch(getFingerprintData());
    }

    public ArrayList<Fingerprint> getFingerprintData(String room) {
        ArrayList<Fingerprint> fingerprints = new ArrayList<>();
        for(Fingerprint fingerprint : fingerprints) {
            if(fingerprint.getRoom().compareTo(room) == 0) {
                fingerprints.add(fingerprint);
            }
        }

        return fingerprints;
    }

    public void addFingerprint(Fingerprint fingerprint) {
        fingerprints.add(fingerprint); // add to fingerprint arraylist
        fingerprintDatabaseHandler.addFingerprint(fingerprint); // add to database
    }

    public void deleteAllFingerprints() {
        fingerprints.clear(); // delete all fingerprints from arraylist
        fingerprintDatabaseHandler.deleteAllFingerprints(); // delete all fingerprints from database
    }

    public void deleteAllFingerprints(String map) {
        ArrayList<Fingerprint> itemsToRemove = new ArrayList<>();

        // collect fingerprints that need to be deleted
        for(Fingerprint fingerprint : fingerprints) {
            if(fingerprint.getRoom().compareTo(map) == 0) {
                itemsToRemove.add(fingerprint);
            }
        }

        // delete collected fingerprints
        for(Fingerprint fingerprint : itemsToRemove) {
            fingerprintDatabaseHandler.deleteFingerprint(fingerprint); // delete from database
            fingerprints.remove(fingerprint); // delete from arraylist
        }
    }
}