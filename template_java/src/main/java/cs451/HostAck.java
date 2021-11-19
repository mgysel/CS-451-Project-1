package cs451;

public class HostAck {
    private Host host;
    private boolean receivedAck;

    public HostAck(Host host, boolean receivedAck) {
        this.host = host;
        this.receivedAck = receivedAck;
    }

    public Host getHost() {
        return this.host;
    }

    public boolean getReceivedAck() {
        return this.receivedAck;
    }

    public void setReceivedAck(boolean bool) {
        this.receivedAck = bool;
    }
}
