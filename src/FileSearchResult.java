import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileSearchResult implements Serializable {

    private final int fileSize;
    private final String fileName;
    private final int port; // saber que nó está associado a este ficheiro
    private final String address;
    private final byte[] hash;
    private int numberOfFiles;
    private WordSearchMessage message;

    public FileSearchResult(WordSearchMessage message, byte[] hash, int fileSize, String fileName, int port, String address) {
        this.message = message;
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.port = port;
        this.address = address;
        this.numberOfFiles = 0;
        this.hash = hash;
    }

    public int getFileSize() {
        return fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public int getPort() {
        return port;
    }

    public void setNumberOfFiles(int numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
    }

    public byte[] getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return fileName + " <" + numberOfFiles + ">";
    }


}
