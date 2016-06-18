package wimd.common;

import org.fusesource.hawtbuf.Buffer;

public interface CommunicationListener {

    void messageReceived(String topic, Buffer content);
}
