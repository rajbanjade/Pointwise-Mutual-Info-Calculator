import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeSet;

public class StopWords {

    TreeSet<String> stopWords = null;
    boolean stopWordsLoaded = false;

    public StopWords() {
    }

    void LoadStopWords() {
        if (stopWords != null) {
            return;
        }

        try {
            FileInputStream fstream = new FileInputStream("stopwords.txt");
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader swFile = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            String stopword = null;
            stopWords = new TreeSet<String>();
            while ((stopword = swFile.readLine()) != null) {
                stopWords.add(stopword);
            }
            swFile.close();
            stopWordsLoaded = true;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean isStopWord(String word) {
        if (!stopWordsLoaded) {
            LoadStopWords();
        }
        return stopWords.contains(word.toLowerCase());
    }
}
