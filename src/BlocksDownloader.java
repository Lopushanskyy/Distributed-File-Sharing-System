import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class BlocksDownloader extends Thread {

    private final DownloadTasksManager downloadTasksManager;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private final int port;
    private int numberOfBlocksReceived;

    public BlocksDownloader(DownloadTasksManager downloadTasksManager, int port) {
        this.port = port;
        this.downloadTasksManager = downloadTasksManager;
        numberOfBlocksReceived = 0;
    }

    @Override
    public void run() {
        try {
            Socket socket = createSocket();
            System.out.println(socket);
            getStreams(socket);
            while (!downloadTasksManager.getRequestMessages().isEmpty()) {
                FileBlockRequestMessage requestBlock = downloadTasksManager.takeBlockRequest();
                out.writeObject(requestBlock);
                out.flush();
                System.out.println("Bloco enviado");
                Object blockAnswerReceived = in.readObject();
                if (blockAnswerReceived instanceof FileBlockAnswerMessage answerBlock) {
                    System.out.println("Bloco recebido");
                    System.out.println("ANSWER BLOCKSIZE: " + answerBlock.getBlockLength());
                    downloadTasksManager.addBlockAnswer(answerBlock);
                    numberOfBlocksReceived++;
                }
            }
            out.writeObject("FIM");
            out.flush();
            downloadTasksManager.addDownloadInformation(createDownloadInformation(socket));
            closeConnection(socket);

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private String createDownloadInformation(Socket socket) {
        return "Ficheiro: " + downloadTasksManager.getFileSearchResult().getFileName() + "\n" + socket.getInetAddress()
                + ", port=" + socket.getPort() + "]: " + numberOfBlocksReceived + " blocks" + "\n";
    }

    public Socket createSocket() throws IOException {
        return new Socket("localhost", port);

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
