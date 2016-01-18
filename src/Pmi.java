import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Rajendra Banjade, 2013 The University of Memphis, TN, USA
 */
public class Pmi {

    private HashMap<String, Integer> workingSet;
    private HashMap<String, Integer> partition;
    //pmiTable is first used to store the word pair frequency and later used to store pmi values calculated at the end. 
    private SortedMap<String, Float> pmiTable;
    private int partitionNumber;

    public Pmi(HashMap<String, Integer> workingSet, HashMap<String, Integer> partition, int partitionNumber) {
        this.pmiTable = new TreeMap<>();
        this.workingSet = workingSet;
        this.partition = partition;
        this.partitionNumber = partitionNumber;
    }

    /*
     * calculate word, and word pair frequency for the wiki page (text coming as a single line) 
     */
    /*
     * Note: 
     * In general, When we have a sliding window, there can be cases like 
     * (a) One word may appear more than once in that window (such as b a c d a, and window of 5, the actual word pair count for <a b> is 1 but the
     *     simple sliding window process gives it 2 (checking the duplicate is not that difficult in this case).  
     * (b) A single word (X) may form a pair with another word (Y) more than once if the word Y is reachable from X either in forward or backward direction. 
     *     For example, such as b a c d m p k l b, and window of 5, the word m forms pair with b twice (one b is reachable from m in backward direction, and 
     *     another is reachable in forward direction). The frequency of word pair can't be more than the frequency of individual word but it is possible here.  
     * (c) If we set the frequency of word pair to the minimum of frequencies of the combined words (in file level, in corpus level), it is still not perfect.         
     * (d) If we count the frequency of word pairs in the order of their occurrence in the file (say, count the frequency of <X Y> and <Y X> separately) and then 
     *     aggregate frequencies (F<X Y> + F<Y X>) when we calculate PMI, it takes more memory because we have to store F<X Y> and F<Y X> separately until the PMI is
     *     calculated. So in words case, the word pairs can be N x N + N x N. Even a single N x N matrix does not fit into memory (but can be managed to calculate pmi for all
     *     possible pairs (i.e. N x N), by partitioning. Please see the section partitioning section.)   
     * 
     *  So, the idea is to keep track of whether a word (X) has already formed a pair with the given word (Y) 
     *  (need to keep track up to the reachable distance, i.e. up to the size of sliding window). Dan helped me on this.  
     *            
     */
    public void calculateWordAndPairFrequency(String line, int windowSize) {

        String words[] = line.split("\\s+");

        int length = words.length;

        int i = 0;         // start index 
        int end = -1;      // end of the window (including)

        Float pairFreq = 0.0f;

        String wordPair = "";

        // keep track of already formed pairs... prevent using the same word again
        HashMap<Integer, HashSet<String>> alreadyDone = new HashMap<Integer, HashSet<String>>();

        for (i = 0; i < length - 1; i++) {

            // end of the sliding window, including this.
            end = length - i >= windowSize ? i + windowSize - 1 : length - 1;

            // if not in the working set (can be valid but fell into previously processed partition, or invalid), just skip
            if (!workingSet.containsKey(words[i])) {
                continue;
            }

            // if this word is in the current partition, turn on the flag that means:
            // form all possible pairs in the working set (includes words from the partition itself).
            // else
            // if the word is in the working set but not in the partition, just pair with the words in the current partition but not among themselves.
            boolean allPossiblePairs = false;
            if (partition.containsKey(words[i])) {
                allPossiblePairs = true;
            }

            // make a hash set to keep track with which the current position word has formed pairs so far. 
            if (!alreadyDone.containsKey(i)) {
                alreadyDone.put(i, new HashSet<String>());
            }

            // for each remaining words in the window
            for (int j = i + 1; j <= end; j++) {

                // Lets Say X for word[i] and Y for word[j]
                // if Y is not in the working set (can be valid but fell into previously processed partition, or invalid), just skip.
                if (!workingSet.containsKey(words[j])) {
                    continue;
                }

                // if X is not in the partition and Y is not in the partition, just skip (because we form all possible pairs of the words from the
                // partition only). Otherwise, form the pair. 
                if (!allPossiblePairs && !partition.containsKey(words[j])) {
                    continue;
                }

                // do not form pair with itself
                if (!words[i].equals(words[j])) {

                    if (!alreadyDone.containsKey(j)) {
                        alreadyDone.put(j, new HashSet<String>());
                    }

                    //if (!alreadyDone.containsKey(startIndex) || !alreadyDone.get(startIndex).contains(words[j])) {  // if this word is appearing first time in the current window
                    //if (!alreadyDone.containsKey(j) || !alreadyDone.get(j).contains(words[startIndex])) { // and 
                    if (!alreadyDone.get(i).contains(words[j]) && !alreadyDone.get(j).contains(words[i])) {  // if this word is appearing first time in the current window

                        //addPair(words[i], words[j]);
                        //sort the word pair in the lexical order, and update word pair frequency
                        wordPair = words[i].compareTo(words[j]) <= 0 ? words[i] + " " + words[j] : words[j] + " " + words[i];
                        pairFreq = pmiTable.get(wordPair);
                        pmiTable.put(wordPair, pairFreq != null ? ++pairFreq : 1);

                        // these two if blocks has been moved up..
                        // if (!alreadyDone.containsKey(j))
                        //	alreadyDone.put(j, new HashSet<String>());
                        //if (!alreadyDone.containsKey(startIndex))
                        //	alreadyDone.put(startIndex, new HashSet<String>());
                        alreadyDone.get(j).add(words[i]);

                        alreadyDone.get(i).add(words[j]);
                    }
                    //}
                }
            }
            // we are done with the current word
            alreadyDone.remove(i);
        }
    }

    /*
     * Calculates the normalized PMI (the formula is taken from Wiki: http://en.wikipedia.org/wiki/Pointwise_mutual_information
     * 
     * NOTE: To use less memory, i have been using float for pmi value (in the pmi table). 
     * 		 And to avoid the apparant overflow issues.. i have used few double variables... and code is not clean. 
     *       But it appears that if everything is ready, just the calculation of PMI is not that major (or error prone).   
     */
    private void calculatePmi(SortedMap<String, Integer> wordFrequencyTable) {

        //String wordPair = null;
        int w1Freq = 0;
        int w2Freq = 0;
        Float wpFreq = 0.0f;
        //String [] words = wordFrequencyTable.keySet().toArray(new String[1]); // null???
        double logPxy = 0;
        double tempF;
        int totalWordCount = wordFrequencyTable.size();
        if (pmiTable == null) {
            System.out.println("Error: The PMI table is null.. exitig......!!!");
        }

        // normalized pmi = -1 for never occuring together, 0 for independence, >0 co-occurence.
        //System.out.println("Frequency of w1 " + wordFrequencyTable.get("borda") + "   And   w2 " + wordFrequencyTable.get("mamani"));
        System.out.println("Total number of words (subset of rest..) from wiki files (can be very few if test folder is given) : " + wordFrequencyTable.size());
        //System.out.println("Frequency of pair booths  exemplifying: " + pmiTable.get("borda mamani"));

        for (String wordPair : pmiTable.keySet()) {
            String[] words = wordPair.split(" ");
            wpFreq = pmiTable.get(wordPair);
            //System.out.println("Word pair: " + wordPair);
            w1Freq = wordFrequencyTable.get(words[0]);
            w2Freq = wordFrequencyTable.get(words[1]);
            tempF = ((totalWordCount * 1.0 / w1Freq) / w2Freq);  // totalWordCount*1.0/(w1Freq*w2Freq) = works? not always.
            tempF *= wpFreq;
            double temp1 = wpFreq;  // just to make sure.. it always works correctly.
            //logPxy = Math.log(wpFreq/totalWordCount)/Math.log(2.0);  // which one is good (this or next line): float/int??
            logPxy = Math.log(temp1 / totalWordCount) / Math.log(2.0);
            tempF = Math.log(tempF) / Math.log(2.0);  // pmi
            // now calculate normalized pmi (divide by (-1 * log(pxy))					
            wpFreq = (float) (tempF / ((-1) * logPxy)); // Normalized -1 to +1
            pmiTable.put(wordPair, wpFreq);
        }
    }

    /*
     * process the files from a folder, no subdirs.
     */
    public boolean calculateWordPairFrequencyFromFiles(String folderName, int windowSize) {

        File dir = new File(folderName);

        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
        for (File file : files) {
            calculateWordPairFrequencyFromFile(file, windowSize);
        }
        return true;
    }

    private boolean calculateWordPairFrequencyFromFile(File file, int windowSize) {
        ArrayList<String> lines = FileHandler.getLinesFromFile(file.getAbsolutePath());
        for (String line : lines) {
            line = line.trim();
            if (line == null || line.isEmpty()) {
                continue;
            }
            if (line.startsWith("<doc") || line.startsWith("</doc")) {
                continue;
            }
            calculateWordAndPairFrequency(line, windowSize);
        }
        return true;
    }

    public void calculatePMI(String inputDataFolder, int windowSize, SortedMap<String, Integer> wordFrequencyTable) {
        //first calculate word pair frequency. 
        calculateWordPairFrequencyFromFiles(inputDataFolder, windowSize);
        //calculate Pmi and store in pmiTable which initially stored word pair frequencies.
        calculatePmi(wordFrequencyTable);
    }

    public void savePmi(String outputFolder) {
        String fileName = outputFolder + "\\" + this.partitionNumber + ".txt";
        writePMITableSortedByWordPair(fileName);
    }

    private void saveWordPairFrequency(String outputFolder, int fileId) {
        String fileName = outputFolder + "\\" + fileId + "_freq.txt";
        writePMITableSortedByWordPair(fileName);
    }

    public void writePMITableSortedByPmiValue(String fileName, int totalWordCount, int totalUniqueWordCount, boolean ascending) {
        String headerText = "Total words (pure ascii): " + totalWordCount + "\r\n"
                + "Total Unique words : " + totalUniqueWordCount;
        FileHandler.writeToFileFloat(headerText, MapUtils.sortByComparatorFloat(pmiTable, ascending), fileName);
    }

    public void writePMITableSortedByWordPair(String fileName) {
        String headerText = "Total word pairs: " + pmiTable.size() + "\r\n";
        FileHandler.writeToFileFloat(headerText, pmiTable, fileName);
    }

}
