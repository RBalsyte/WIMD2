package wimd;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

public class Fingerprint {

    public static final int DEFAULT_RSS = -120;

    private int id;
    private String room;
    private PointF location;
    private Map<String, Integer> measurements;

    public Fingerprint(){
        id = -1;
        room = "";
    }

    public Fingerprint(Map<String, Integer> measurements){
        this();
        this.measurements = measurements;
    }

    public Fingerprint(Map<String, Integer> measurements, String room){
        this(measurements);
        this.room = room;
    }

    public Fingerprint(int id, String room, PointF location){
        this();
        this.room = room;
        this.location = location;
    }

    public Fingerprint(int id, String room, PointF location, Map<String, Integer> measurements){
        this(id, room, location);
        this.measurements = measurements;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<String, Integer> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(Map<String, Integer> measurements) {
        this.measurements = measurements;
    }

    public void setLocation(PointF location) {
        this.location = location;
    }

    public void setLocation(float x, float y) {
       location = new PointF(x, y);
    }

    public PointF getLocation(){
        return location;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    //TODO Raminta: explain why euclidean
    /** calculates the (squared) euclidean distance to the given fingerprint */
    public float compare(Fingerprint fingerprint) {
        float result = 0f;

        Map<String, Integer> fingerprintMeasurements = fingerprint.getMeasurements();
        TreeSet<String> keys = new TreeSet<String>();
        keys.addAll(measurements.keySet());
        keys.addAll(fingerprintMeasurements.keySet());

        for (String key : keys) {
            int value = 0;
            Integer fValue = fingerprintMeasurements.get(key);
            Integer mValue = measurements.get(key);
            value = (fValue == null) ? DEFAULT_RSS : (int) fValue;
            value -= (mValue == null) ? DEFAULT_RSS : (int) mValue;
            result += value * value;
        }

        return result;
    }

    // TODO Raminta: explain why euclidean, and why at all search for this
    /** compares the fingerprint to a set of fingerprints and returns the fingerprint with the smallest euclidean distance to it */
    public Fingerprint getClosestMatch(ArrayList<Fingerprint> fingerprints) {
        Fingerprint closest = null;
        float bestScore = -1;

        if(fingerprints != null) {
            for(Fingerprint fingerprint : fingerprints) {
                float score = compare(fingerprint);
                if(bestScore == -1 || bestScore > score) {
                    bestScore = score;
                    closest = fingerprint;
                }
            }
        }
        return closest;
    }

}
