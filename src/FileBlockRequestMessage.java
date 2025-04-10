import java.io.Serializable;
import java.util.Arrays;

public class FileBlockRequestMessage implements Serializable {

    private final int offset; //offset
    private final int index;
    private final int blockLength;
    private final byte[] hash;

    public FileBlockRequestMessage(int offset, int blockLength, byte[] hash, int index) {
        this.offset = offset;
        this.blockLength = blockLength;
        this.hash = hash;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public int getOffset() {
        return offset;
    }

    public int getBlockLength() {
        return blockLength;
    }

    public byte[] getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return "[" + offset + blockLength + Arrays.toString(hash) + "]";
    }
}

