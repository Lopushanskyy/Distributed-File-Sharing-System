import javax.swing.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class StartDownloadThread extends Thread { //esta thread serve para o programa do utilizador não ficar bloqueado enquanto são feitos os descarregamentos

    Node node;
    FileSearchResult fsr;

    public StartDownloadThread(Node node, FileSearchResult fsr) {
        this.node = node;
        this.fsr = fsr;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        try {
            node.sendFileRequest(fsr);
        } catch (InterruptedException | NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
        long endTime = System.currentTimeMillis();
        int seconds = (int) ((endTime - startTime) / 1000);
        JOptionPane.showMessageDialog(null, node.createDownloadInformation(seconds));
    }
}
