import java.io.Serializable;

public class FileBlockAnswerMessage implements Serializable {

    private final byte[] blockData;
    private final int index;

    public FileBlockAnswerMessage(byte[] blockData, int index) {
        this.blockData = blockData;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public byte[] getBlockBytes() {
        return blockData;
    }

    public int getBlockLength() {
        return blockData.length;
    }
}
