import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Rajendra Banjade, 2013 The University of Memphis, USA
 */
public class PMICalculatorMain {

    SortedMap<String, Integer> validWords; /* N = sizeOf(validWords). We calculate pmi for N*N pairs in the worst case. */

    SortedMap<String, Integer> wordFrequencyTable;

    //In how many epoch you want to finish 
    int totalPartition = 14;
    //how many previous + next words take as context (e.g., if it is 11, look
    //at upto 5 previous and 5 next words)
    int windowSize = 11;
    int totalWordCount = 0;

    //StopWords sw = new StopWords();
    String inputFolder = null;
    String pmiOutputFolder = null;             /* save the preprocessed file */


    public PMICalculatorMain(String inputFolder, String outputFolder) {
        this.inputFolder = inputFolder;
        this.pmiOutputFolder = outputFolder;
    }

    public void buildWordFrequencyTable() {
        WordFrequencyCalculator calculator = new WordFrequencyCalculator();
        calculator.buildWordFrequencyTable(this.inputFolder);
        wordFrequencyTable = calculator.getWordFrequencyTable();
    }

    public void readValidWords(String fileName) {
        ArrayList<String> lines = FileHandler.getLinesFromFile(fileName);
        validWords = new TreeMap<>();
        for (String line : lines) {
            validWords.put(line, -1); //<value> not in use.
        }
    }

    /**
     *
     * Tasa contains around 71K (N) unique words (I have named as valid words),
     * and the pmi calculation is not possible in a single pass as it takes N x
     * N space (in worst case). The idea is to partition the valid words and
     * calculate PMI in multiple pass (i.e. multiple file scan) I have done with
     * 14 iterations (it didn't fit into memory even with 10 partitions). Valid
     * words - All the valid words (i.e. unique words from tasa). We only need
     * all the possible pairs among them. Working set - The subset of valid
     * words among them we form word pairs (not all possible pairs, please see
     * partition below). initially this working set contains all the valid
     * words. Partition - from within the working set, we create a partition. We
     * can form all possible pairs among the words in that partition. And we can
     * also form word pairs from the words of that partition to the rest of the
     * words in the working set (and vice-versa, as we do not consider
     * ordering). But we can't form the word pairs among the words (of course
     * they should be in the working set) not in the current partition. This
     * controls the growth of word pairs. For the very first time, the working
     * set contains all the valid words, so it takes more time and memory. In
     * the subsequent iterations, we can safely discard the previous
     * partition(s) so the new working set becomes small. It forms less number
     * of word pairs, takes less time and memory.
     */
    public void calculatePMIAll() {

        /* these are the valid words, unique words from Tasa */
        String[] validWordsArray = validWords.keySet().toArray(new String[1]); // if doesn't fit.. it creates itself..
        int partitionSize = validWordsArray.length / totalPartition;

        int start = 0; /* starting index of the partition, and working set */

        int partitionNumber = 0; // currently working partition.

        while (start < validWordsArray.length) {
            HashMap<String, Integer> partition = new HashMap<>();
            HashMap<String, Integer> workingSet = new HashMap<>();
            Pmi pmi;
            //wordFrequencyTable = new TreeMap<String, Integer>();
            //totalWordCount = 0; /* total word in the whole collection of input files */

            partitionNumber++;
            System.out.println(" ============== Partition === " + partitionNumber + " out of " + totalPartition);
            Date startTime = new Date();

            // end index (including), of the partition.
            int end = start + partitionSize <= validWordsArray.length ? start + partitionSize - 1 : validWordsArray.length - 1;

            for (int i = start; i <= end; i++) {
                partition.put(validWordsArray[i], 1);
                workingSet.put(validWordsArray[i], 1); // words in the partition are also in the working set.
            }

            //start of the next partition change.
            start += end + 1;  //end + 1?

            // the working set contains rest of the valid words (after that index.., it means it doesn't contains previous partitions)
            for (int i = start; i < validWordsArray.length; i++) {
                workingSet.put(validWordsArray[i], 1);
            }
            // call function to calculate PMI
            pmi = new Pmi(workingSet, partition, partitionNumber);

            pmi.calculatePMI(this.inputFolder, windowSize, wordFrequencyTable);

            // save pmi in a file named with the partition number..
            pmi.savePmi(this.pmiOutputFolder);

            // print the time for that iteration (partition), it decreases on subsequent partitions.. as working set decreases.
            Date finishTime = new Date();
            System.out.println("\n Time for " + partitionNumber + " iteration : " + (TimeUtils.getDateFromMsec(finishTime.getTime() - startTime.getTime())));
        }
    }

    public static void main(String[] args) {
        String inputDataFolder = "data\\";
        String pmiOutputFolder = "data\\pmi-output\\";
        String validWordsFile = "data\\calculate-pmi-for-words.txt";

        PMICalculatorMain calculator = new PMICalculatorMain(inputDataFolder, pmiOutputFolder);
        calculator.readValidWords(validWordsFile);
        calculator.buildWordFrequencyTable();
        calculator.calculatePMIAll();
        System.out.println("Done!");
    }

}
