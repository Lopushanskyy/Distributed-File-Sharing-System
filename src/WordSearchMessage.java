import java.io.Serializable;

public class WordSearchMessage implements Serializable {

    private final String word;

    public WordSearchMessage(String word) {
        this.word = word;
    }

    public String getWord() {
        return word;
    }

    @Override
    public String toString() {
        return "FileSearchResult{" +
                "searchMessage=" + word +
                '}';
    }

}
