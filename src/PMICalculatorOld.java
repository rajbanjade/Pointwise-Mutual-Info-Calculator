import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class PMICalculatorOld {

    SortedMap<String, Integer> validWords = new TreeMap<String, Integer>(); /* from tasa corpus */


    public HashMap<String, Integer> getPartition() {
        return partition;
    }

    public void setPartition(HashMap<String, Integer> partition) {
        this.partition = partition;
    }

    public HashMap<String, Integer> getWorkingSet() {
        return workingSet;
    }

    public void setWorkingSet(HashMap<String, Integer> workingSet) {
        this.workingSet = workingSet;
    }

    HashMap<String, Integer> partition = new HashMap<String, Integer>();
    HashMap<String, Integer> workingSet = new HashMap<String, Integer>();

    SortedMap<String, Float> pmiTable = new TreeMap<String, Float>();
    SortedMap<String, Integer> wordFrequencyTable = new TreeMap<String, Integer>();
    String inputFolder = null;
    String pmiOutputFolder = null;             /* save the preprocessed file */

    String wordFrequencyOutputSortedByFreqFile = null;

    //In how many epoch you want to finish 
    int totalPartition = 14;
    //how many previous + next words take as context (e.g., if it is 11, look
    //at upto 5 previous and 5 next words)
    int windowSize = 11;
    int partitionNumber = 0; // currently working partition.

    String wordFrequencyOutputFile = null;  /* dump the word frequency table */

    int totalWordCount = 0;

    /////////////<<<<<<<<<<<<< Just to count words, removing or without removing stop words.
    int stopWordsCounter = 0;
    int documentCounter = 0;
    Set<String> wordTypes = new HashSet<>();
    Set<String> stopWordTypes = new HashSet<>();
    StopWords sw = new StopWords();

    ///////////////////////////////>>>>>>>>>>>>>>
    public String getWordFrequencyOutputSortedByFreqFile() {
        return wordFrequencyOutputSortedByFreqFile;
    }

    public void setWordFrequencyOutputSortedByFreqFile(
            String wordFrequencyOutputSortedByFreqFile) {
        this.wordFrequencyOutputSortedByFreqFile = wordFrequencyOutputSortedByFreqFile;
    }

    //The PMI data will be splitted into multiple files, based on the number of partitions.
    public String getPmiOutputFolder() {
        return pmiOutputFolder;
    }

    public void setPmiOutputFolder(String pmiOutputFolder) {
        this.pmiOutputFolder = pmiOutputFolder;
    }

    public int getTotalWordCount() {
        return totalWordCount;
    }

    public void setTotalWordCount(int totalWordCount) {
        this.totalWordCount = totalWordCount;
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(String inputFolder) {
        this.inputFolder = inputFolder;
    }

    public String getWordFrequencyOutputFile() {
        return wordFrequencyOutputFile;
    }

    public void setWordFrequencyOutputFile(String wordFrequencyOutputFile) {
        this.wordFrequencyOutputFile = wordFrequencyOutputFile;
    }

    public PMICalculatorOld(String inputFolder, String outputFolder) {
        this.inputFolder = inputFolder;
        this.pmiOutputFolder = outputFolder;
    }

    // NOTE:
    // Tasa contains around 71K (N) unique words (I have named as valid words), and the pmi calculation is not possible in a single pass
    // as it takes N x N space (in worst case). The idea is to partition the valid words and calculate PMI in multiple pass (i.e. multiple file scan)
    // I have done with 14 iterations (it didn't fit into memory even with 10 partitions).
    // Valid words - All the valid words (i.e. unique words from tasa). We only need all the possible pairs among them.
    // Working set - The subset of valid words among them we form word pairs (not all possible pairs, please see partition below). 
    //               initially this working set contains all the valid words. 
    // Partition - from within the working set, we create a partition. 
    //             We can form all possible pairs among the words in that partition. And we can also form word pairs  
    //			   from the words of that partition to the rest of the words in the working set (and vice-versa, as we do not consider ordering). 
    //			   But we can't form the word pairs among the words (of course they should be in the working set) not in the current partition. 
    //			   This controls the growth of word pairs.
    // For the very first time, the working set contains all the valid words, so it takes more time and memory.
    // In the subsequent iterations, we can safely discard the previous partition(s) so the new working set becomes small. 
    // It forms less number of word pairs, takes less time and memory.  
    public void calculatePmiAll() {

        /* these are the valid words, unique words from Tasa */
        String[] validWordsArray = validWords.keySet().toArray(new String[1]); // if doesn't fit.. it creates itself..
        int partitionSize = validWordsArray.length / totalPartition;

        int start = 0; /* starting index of the partition, and working set */

        while (start < validWordsArray.length) {
            partition = new HashMap<String, Integer>();
            workingSet = new HashMap<String, Integer>();
            pmiTable = new TreeMap<String, Float>();
            wordFrequencyTable = new TreeMap<String, Integer>();
            totalWordCount = 0; /* total word in the whole collection of input files */

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
            start += partitionSize;  //end + 1?

            // the working set contains rest of the valid words (after that index.., it means it doesn't contains previous partitions)
            for (int i = start; i < validWordsArray.length; i++) {
                workingSet.put(validWordsArray[i], 1);
            }
            // call function to calculate PMI
            calculateWordFrequencyAndPmi();

            // first time?, write the word frequency table as it has the complete set.
            if (partitionNumber == 1) {
                writeWordFrequencyTableSortedByWord(wordFrequencyOutputFile);
                writeWordFrequencyTableSortedByFrequency(wordFrequencyOutputSortedByFreqFile, false);
            }
            // save pmi in a file named with the partition number..
            savePmi(partitionNumber);

            // print the time for that iteration (partition), it decreases on subsequent partitions.. as working set decreases.
            Date finishTime = new Date();
            System.out.println("\n Time for " + partitionNumber + " iteration : " + (TimeUtils.getDateFromMsec(finishTime.getTime() - startTime.getTime())));
        }
    }

    /**
     *
     * @return
     */
    public boolean calculateWordFrequencyAndPmi() {

        File dir = new File(inputFolder);

        File[] subDirs = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        int folderCounter = 0;
        for (File subDir : subDirs) {
            folderCounter++;
            calculateWordAndPairFrequencyFromFiles(subDir.getAbsolutePath());
            System.out.println("processed: " + folderCounter + " folders out of " + subDirs.length);
            System.out.println("Now, the word pair table size is : " + pmiTable.size());
        }
        //System.out.println("Number of words found in Tasa but not in wiki : " + (validWords.size() - wordFrequencyTable.size()));
        //can save the word pair frequency as well. do it here.. before calculating the pmi score.
        saveWordPairFrequency(partitionNumber);
        calculatePmi();
        return true;
    }

    /*
     * process the files from a folder, no subdirs.
     */
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

    /*
     * 
     */
    private boolean calculateWordAndPairFrequencyFromFile(File file) {
        ArrayList<String> lines = FileHandler.getLinesFromFile(file.getAbsolutePath());
        for (String line : lines) {
            line = line.trim();
            if (line == null || line == "" || line.length() == 0) {
                continue;
            }
            if (line.startsWith("<doc") || line.startsWith("</doc")) {
                continue;
            }
            calculateWordAndPairFrequency(line);
        }
        return true;
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
    public void calculateWordAndPairFrequency(String line) {

        //System.out.print("debug message: method updated");
        String words[] = line.split("\\s+");

        int length = words.length;

        int i = 0;         // start index 
        int end = -1;      // end of the window (including)

        Float pairFreq = 0.0f;

        Integer wordFreq = 0;

        String wordPair = "";

        // keep track of already formed pairs... prevent using the same word again
        HashMap<Integer, HashSet<String>> alreadyDone = new HashMap<Integer, HashSet<String>>();

        totalWordCount += words.length;

        for (i = 0; i < length - 1; i++) {

            // end of the sliding window, including this.
            end = length - i >= windowSize ? i + windowSize - 1 : length - 1;

            // if not in the working set (can be valid but fell into previously processed partition, or invalid), just skip
            if (!workingSet.containsKey(words[i])) {
                continue;
            }

            // Update the word frequency
            wordFreq = wordFrequencyTable.get(words[i]);
            wordFrequencyTable.put(words[i], wordFreq != null ? ++wordFreq : 1);

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
        // if it is the last word in the document
        if (workingSet.containsKey(words[i])) {
            wordFreq = wordFrequencyTable.get(words[i]);
            wordFrequencyTable.put(words[i], wordFreq != null ? ++wordFreq : 1);
        }
    }

    //======================================================= Just word frequency ==================================================
    /**
     * Just calculate the word frequency
     *
     * @param folder
     */
    public void justCalculateWordFrequency(String docRootFolder) {
        File dir = new File(docRootFolder);
        File[] subDirs = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (File subDir : subDirs) {
            File[] files = subDir.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }
            });

            for (File file : files) {
                justCalculateWordFrequencyFromFile(file.getAbsolutePath());
            }
        }
        printCountSummary();
    }

    /**
     * print the summary.
     */
    private void printCountSummary() {

        System.out.println("======================");
        System.out.println("\n Total articles: " + documentCounter);
        System.out.println("\n Total tokens (keeping stop words) :" + totalWordCount);
        System.out.println("\n Total stop words :" + stopWordsCounter);
        System.out.println("\n Total word types :" + wordTypes.size());
        System.out.println("\n Total stop word types :" + stopWordTypes.size());
        System.out.println("\n Words after stop word:" + (totalWordCount - stopWordsCounter));
    }

    /**
     * Just calculate the word frequency from the file. For Wiki.
     *
     * @param fileName
     */
    private void justCalculateWordFrequencyFromFile(String fileName) {
        ArrayList<String> lines = FileHandler.getLinesFromFile(fileName);
        for (String line : lines) {
            line = line.trim();
            if (line == null || line == "" || line.length() == 0) {
                continue;
            }
            if (line.startsWith("<doc") || line.startsWith("</doc")) {
                if (line.startsWith("<doc")) {
                    documentCounter++;
                }
                continue;
            }
            justCalculateWordFrequencyFromLine(line);
        }
    }

    /**
     * For tasa file.
     *
     * @param fileName
     */
    public void justCalculateWordFrequencyFromTasaFile(String fileName) {
        ArrayList<String> lines = FileHandler.getLinesFromFile(fileName);
        //ArrayList<String> outLines = new ArrayList<String>();
        for (String line : lines) {
            line = line.trim();
            if (line == null || line == "" || line.length() == 0) {
                continue;
            }
            //if (line.contains("] [P#")) {
            //	 documentCounter++;
            //	continue;
            //}
            //if (line.startsWith("[S]")) {
            //	line = line.replaceFirst("\\[S\\]", "");
            //}
            justCalculateWordFrequencyFromLine(line);
            //outLines.add(line);
        }
        //FileHandler.writeToFile(outLines, "C:\\Users\\Rajendra\\data\\Tasa\\TasaOriginal_MetaInfoRemoved.txt");
        //System.out.println("Total documents :" + documentCounter);
        printCountSummary();
    }

    /**
     * Just calculate word frequency from the given line.. not pairs, ..
     *
     * @param line
     */
    private void justCalculateWordFrequencyFromLine(String line) {
        String tokens[] = line.split("\\s+");
        for (String token : tokens) {
            totalWordCount++;
            if (sw.isStopWord(token)) {
                stopWordsCounter++;
                stopWordTypes.add(token);
                continue;
            }
            wordTypes.add(token);
        }
    }

    //==============================================================================================
    public SortedMap<String, Integer> getValidWords() {
        return validWords;
    }

    public void setValidWords(SortedMap<String, Integer> validWords) {
        this.validWords = validWords;
    }

    public SortedMap<String, Float> getPmiTable() {
        return pmiTable;
    }

    public void setPmiTable(SortedMap<String, Float> pmiTable) {
        this.pmiTable = pmiTable;
    }

    public SortedMap<String, Integer> getWordFrequencyTable() {
        return wordFrequencyTable;
    }

    public void setWordFrequencyTable(SortedMap<String, Integer> wordFrequencyTable) {
        this.wordFrequencyTable = wordFrequencyTable;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    /*
     * Calculates the normalized PMI (the formula is taken from Wiki: http://en.wikipedia.org/wiki/Pointwise_mutual_information
     * 
     * NOTE: To use less memory, i have been using float for pmi value (in the pmi table). 
     * 		 And to avoid the apparant overflow issues.. i have used few double variables... and code is not clean. 
     *       But it appears that if everything is ready, just the calculation of PMI is not that major (or error prone).   
     */
    private void calculatePmi() {

        //String wordPair = null;
        int w1Freq = 0;
        int w2Freq = 0;
        Float wpFreq = 0.0f;
        //String [] words = wordFrequencyTable.keySet().toArray(new String[1]); // null???
        double logPxy = 0;
        double tempF;
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

    private void savePmi(int fileId) {
        String fileName = pmiOutputFolder + "\\" + fileId + ".txt";
        writePMITableSortedByWordPair(fileName);
    }

    private void saveWordPairFrequency(int fileId) {
        String fileName = pmiOutputFolder + "\\" + fileId + "_freq.txt";
        writePMITableSortedByWordPair(fileName);
    }

    public void writeWordFrequencyTableSortedByWord(String fileName) {
        String headerText = "Total words (pure ascii): " + totalWordCount + "\r\n"
                + "Total Unique words : " + wordFrequencyTable.size();
        FileHandler.writeToFile(headerText, wordFrequencyTable, fileName, totalWordCount);
    }

    public void writeWordFrequencyTableSortedByFrequency(String fileName, boolean ascending) {
        String headerText = "Total words (pure ascii): " + totalWordCount + "\r\n"
                + "Total Unique words : " + wordFrequencyTable.size();
        FileHandler.writeToFile(headerText, MapUtils.sortByComparator(wordFrequencyTable, ascending), fileName, totalWordCount);
    }

    public void writePMITableSortedByPmiValue(String fileName, boolean ascending) {
        String headerText = "Total words (pure ascii): " + totalWordCount + "\r\n"
                + "Total Unique words : " + wordFrequencyTable.size();
        FileHandler.writeToFileFloat(headerText, MapUtils.sortByComparatorFloat(pmiTable, ascending), fileName);
    }

    public void writePMITableSortedByWordPair(String fileName) {
        String headerText = "Total word pairs: " + pmiTable.size() + "\r\n";
        FileHandler.writeToFileFloat(headerText, pmiTable, fileName);
    }

    public int getUniqueWordCount() {
        return wordFrequencyTable.size();
    }

    public void pmiTest() {

        //String wordPair = null;
        int w1Freq = 0;
        int w2Freq = 0;
        Float wpFreq = 0.0f;
        double tempF;
        //String [] words = wordFrequencyTable.keySet().toArray(new String[1]); // null???
        double logPxy = 0;

        //pmiTable.put("books united", 99.0f);
        //wordFrequencyTable.put("books", 454);
        //wordFrequencyTable.put("united", 1127226);
        totalWordCount = 541520;
        //
        pmiTable.put("w1 w2", 10.0f);
        wordFrequencyTable.put("w1", 10);
        wordFrequencyTable.put("w2", 15);

        pmiTable.put("w3 w4", 1.0f);
        wordFrequencyTable.put("w3", 10);
        wordFrequencyTable.put("w4", 15);

        pmiTable.put("w5 w6", 50.0f);
        wordFrequencyTable.put("w5", 50);
        wordFrequencyTable.put("w6", 1000);

        pmiTable.put("w7 w8", 5.0f);
        wordFrequencyTable.put("w7", 50);
        wordFrequencyTable.put("w8", 1000);

        pmiTable.put("w9 w10", 1000.0f);
        wordFrequencyTable.put("w9", 1500);
        wordFrequencyTable.put("w10", 1000);

        pmiTable.put("w11 w12", 50.0f);
        wordFrequencyTable.put("w11", 1500);
        wordFrequencyTable.put("w12", 1000);

        for (String wordPair : pmiTable.keySet()) {
            String[] words = wordPair.split(" ");
            w1Freq = wordFrequencyTable.get(words[0]);
            w2Freq = wordFrequencyTable.get(words[1]);
            System.out.print(words[0] + " " + w1Freq + " " + words[1] + " " + w2Freq);
            wpFreq = pmiTable.get(wordPair);
            System.out.print(" " + wpFreq);
            //System.out.println("Word pair: " + wordPair);
            tempF = ((totalWordCount * 1.0 / w1Freq) / w2Freq);  // totalWordCount/(w1Freq*w2Freq) = works? not always.
            tempF *= wpFreq;
            double temp1 = wpFreq;  // just to make sure.. it always works correctly.
            //logPxy = Math.log(wpFreq/totalWordCount)/Math.log(2.0);  // which one is good (this or next line): float/int??
            logPxy = Math.log(temp1 / totalWordCount) / Math.log(2.0);

            tempF = Math.log(tempF) / Math.log(2.0);  // pmi
            // now calculate normalized pmi (divide by (-1 * log(pxy))					
            wpFreq = (float) (tempF / ((-1) * logPxy)); // Normalized -1 to +1
            pmiTable.put(wordPair, wpFreq);
            System.out.print(" " + wpFreq + "\r\n\r\n"); // this is pmi.. though the variable is different.

        }
    }

    public static void main(String[] args) {
        PMICalculatorOld wp = new PMICalculatorOld(null, null);

        /*wp.pmiTest();
	
         SortedMap<String,Integer> valids = new TreeMap<String, Integer>();
         String inputString = "pursuit accident claim car driver exclude soak motorist company car driver risky company car driver tend travel farther job engineer disappear fall mechanical engineer car industry worst affect sign recession car industry brightest engineer moment car industry yugoslavia benefit direct investment automobile industry acreage expand emergence automobile industry automobile industry among hardest hit recession automobile industry largely male force component supplier automobile industry expand client industry manufacturer component automobile industry";
	
         int counter = 0;
         for (String vw : inputString.trim().split(" ")) {
         valids.put(vw, 1);
         wp.partition.put(vw, 1);
         wp.workingSet.put(vw, 1);
         if (counter < 11) {
         System.out.print(vw + " ");
         }
         }
         wp.setValidWords(valids);
		
         //wp.pmiTest();
         wp.calculateWordAndPairFrequency(inputString.trim());
         counter = 0;
         System.out.println(wp.pmiTable.size());
         for (String wordPair : wp.pmiTable.keySet()) {
         if (wordPair.contains("car")) {
         System.out.println(++counter + " :  " +wordPair + " " + wp.pmiTable.get(wordPair));
         }
         }
		
         //
         counter = 0;
         System.out.println("====================");
         System.out.println(wp.pmiTable.size());
         for (String wordPair : wp.pmiTable.keySet()) {
         if (wordPair.contains("automobile")) {
         System.out.println(++counter + " :  " +wordPair + " " + wp.pmiTable.get(wordPair));
         }
         }
         */
        System.out.println("Wait");
        //wp.justCalculateWordFrequency("C:\\Users\\Rajendra\\data\\wiki-articles-punctuations-and-digit-removed-lowercased");
        wp.justCalculateWordFrequencyFromTasaFile("C:\\Users\\Rajendra\\data\\Tasa\\Tasa_Meta_And_PunctuationsRemoved.txt"); //,:\\Users\\Rajendra\\data\\Tasa\\TasaOriginal_MetaInfoRemoved.txt

    }
}
