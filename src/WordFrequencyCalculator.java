import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Rajendra
 */
public class WordFrequencyCalculator {

    private SortedMap<String, Integer> wordFrequencyTable;

    public WordFrequencyCalculator() {
        wordFrequencyTable = new TreeMap<>();
    }

    public SortedMap<String, Integer> getWordFrequencyTable() {
        return wordFrequencyTable;
    }

    public void buildWordFrequencyTable(String inputDataFolder) {
        calculateWordAndPairFrequencyFromFiles(inputDataFolder);
    }

    public boolean calculateWordAndPairFrequencyFromFiles(String folderName) {

        File dir = new File(folderName);

        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
        for (File file : files) {
            calculateWordAndPairFrequencyFromFile(file);
        }
        return true;
    }

    private boolean calculateWordAndPairFrequencyFromFile(File file) {
        ArrayList<String> lines = FileHandler.getLinesFromFile(file.getAbsolutePath());
        for (String line : lines) {
            line = line.trim();
            if (line == null || line.isEmpty()) {
                continue;
            }
            if (line.startsWith("<doc") || line.startsWith("</doc")) {
                continue;
            }
            String words[] = line.split("\\s+");

            for (String w : words) {
                // Update the word frequency
                Integer wordFreq = wordFrequencyTable.get(w);
                wordFrequencyTable.put(w, wordFreq != null ? ++wordFreq : 1);

            }
        }
        return true;
    }

    public void writeWordFrequencyTableSortedByWord(String fileName, int totalWordCount) {
        String headerText = "Total words (pure ascii): " + totalWordCount + "\r\n"
                + "Total Unique words : " + wordFrequencyTable.size();
        FileHandler.writeToFile(headerText, wordFrequencyTable, fileName, totalWordCount);
    }

    public void writeWordFrequencyTableSortedByFrequency(String fileName, int totalWordCount, boolean ascending) {
        String headerText = "Total words (pure ascii): " + totalWordCount + "\r\n"
                + "Total Unique words : " + wordFrequencyTable.size();
        FileHandler.writeToFile(headerText, MapUtils.sortByComparator(wordFrequencyTable, ascending), fileName, totalWordCount);
    }
}
