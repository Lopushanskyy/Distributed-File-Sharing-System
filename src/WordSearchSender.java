import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class WordSearchSender extends Thread {

    private final NewConnectionRequest newConnectionRequest;
    private final Node node;
    private final int requesterPort;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private final WordSearchMessage word;


    public WordSearchSender(Node node, NewConnectionRequest newConnectionRequest, int requesterPort, WordSearchMessage word) {
        this.newConnectionRequest = newConnectionRequest;
        this.requesterPort = requesterPort;
        this.word = word;
        this.node = node;
    }

    @Override
    public void run() {
        try {
            Socket socket = createSocket();
            getStreams(socket);
            out.writeObject(word);
            out.flush();
            Object filesListReceived = in.readObject();
            if (filesListReceived instanceof ArrayList<?>) {
                node.concatFilesList((ArrayList<FileSearchResult>) filesListReceived); //metodo sincronizado na classe Node
            }
            closeConnection(socket);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket createSocket() throws IOException {
        Socket socket;
        if (newConnectionRequest.getServerPort() != requesterPort) {
            socket = new Socket(InetAddress.getByName("localhost"), newConnectionRequest.getServerPort());
            //somos quem enviou o pedido, logo procuramos nas pastas de quem enviamos uma conexao
        } else {
            socket = new Socket(InetAddress.getByName("localhost"), newConnectionRequest.getRequesterPort());
            //somos quem recebeu o pedido, logo nao procuramos dentro da nossa pasta, mas sim nos nodes que nos enviaram o pedido de conexao
        }
        return socket;
    }

    private void getStreams(Socket socket) throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    private void closeConnection(Socket connection) {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (connection != null) connection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}



