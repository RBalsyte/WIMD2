package wimd.Interfaces;

import wimd.Fingerprint;

public interface DatabaseInterface {
    public void addFingerprint(Fingerprint fingerprint);
    public Fingerprint getFingerprint(int id);
}
