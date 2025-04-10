import java.io.Serializable;

public class NewConnectionRequest implements Serializable {

    private final String serverHostName;
    private final int serverPort;
    private final int requesterPort;

    public NewConnectionRequest(String serverHostName, int serverPort, int requesterPort) {
        this.serverHostName = serverHostName;
        this.serverPort = serverPort;
        this.requesterPort = requesterPort;
    }

    public String getServerHostName() {
        return serverHostName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getRequesterPort() {
        return requesterPort;
    }

    @Override
    public String toString() {
        return "NewConnectionRequest{" +
                "serverHostName='" + serverHostName + '\'' +
                ", serverPort=" + serverPort +
                ", requesterPort=" + requesterPort +
                '}';
    }
}
