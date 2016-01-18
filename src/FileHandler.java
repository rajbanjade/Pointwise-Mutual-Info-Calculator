import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class FileHandler {

    /*
     * Gives the list of lines in a file.
     */
    public static ArrayList<String> getLinesFromFile(String fileName) {
        ArrayList<String> lines = new ArrayList<String>();
        try {
            FileInputStream fstream = new FileInputStream(fileName);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine = null;
            while ((strLine = br.readLine()) != null) {
                lines.add(strLine.trim());
            }
            in.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return lines;

    }

    /*
     * Gives the list of lines in a file.
     */
    public static void getDataFromFileSortedByKey(String fileName, String keyValueSeparator, SortedMap<String, Float> sortedMap, int skip) {
        //SortedMap<String,Float> sortedMap = new TreeMap<String, Float>();
        String separator = keyValueSeparator != null ? keyValueSeparator : " ";
        try {
            FileInputStream fstream = new FileInputStream(fileName);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine = null;
            int lineCounter = 0;
            while ((strLine = br.readLine()) != null) {
                lineCounter++;
                if (lineCounter <= skip) {
                    continue; //header, skip
                }
                strLine = strLine.trim();
                if (strLine.length() == 0 || strLine == "") {
                    continue;
                }
                String[] splits = strLine.trim().split(separator);
                sortedMap.put(splits[0] + " " + splits[1], Float.valueOf(splits[2]));
            }
            in.close();
            fstream.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean writeToFile(ArrayList<String> lines, String fileName) {

        try {
            // Create file
            FileWriter fstream = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(fstream);
            for (String line : lines) {
                out.write(line + System.lineSeparator());
            }
            // Close the output stream
            out.close();
            fstream.close();
        } catch (Exception e) {// Catch exception if any
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    /*
     public static boolean writeToFile(SortedMap<String, Integer> sortedMap, String fileName) {

     try {
     // Create file
     FileWriter fstream = new FileWriter(fileName);
     BufferedWriter out = new BufferedWriter(fstream);
     for (String word : sortedMap.keySet()) {
     out.write(String.format("%20s", word) + " " + sortedMap.get(word) + System.lineSeparator());
     }
     // Close the output stream
     out.close();
     fstream.close();
     } catch (Exception e) {// Catch exception if any
     System.err.println("Error: " + e.getMessage());
     e.printStackTrace();
     }
     return true;
     }
     */
    public static boolean writeToFile(Map<String, Integer> map, String fileName) {

        try {
            // Create file
            FileWriter fstream = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(fstream);
            for (String word : map.keySet()) {
                out.write(String.format("%20s", word) + " " + map.get(word) + System.lineSeparator());
            }
            // Close the output stream
            out.close();
            fstream.close();
        } catch (Exception e) {// Catch exception if any
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    public static boolean writeToFile(String headerText, Map<String, Integer> map, String fileName) {

        try {
            // Create file
            FileWriter fstream = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(fstream);

            if (headerText != null) {
                out.write("Summary: \n" + headerText + System.lineSeparator());
                out.write("========================================\r\n");
            }

            for (String word : map.keySet()) {
                out.write(String.format("%20s", word) + " " + map.get(word) + System.lineSeparator());
            }
            // Close the output stream
            out.close();
            fstream.close();
        } catch (Exception e) {// Catch exception if any
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    public static boolean writeToFileFloat(String headerText, Map<String, Float> map, String fileName) {

        try {
            // Create file
            FileWriter fstream = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(fstream);

            if (headerText != null) {
                out.write("Summary: \n" + headerText + System.lineSeparator());
                out.write("========================================\r\n");
            }

            for (String word : map.keySet()) {
                out.write(String.format("%30s", word) + " " + String.format("%.4f", map.get(word)) + System.lineSeparator());
            }
            // Close the output stream
            out.flush();
            out.close();
            fstream.close();
        } catch (Exception e) {// Catch exception if any
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    public static boolean writeToFile(String headerText, Map<String, Integer> map, String fileName, int totalWords) {

        try {
            // Create file
            FileWriter fstream = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(fstream);

            double percent;
            int frequency;

            if (headerText != null) {
                out.write("Summary: \n" + headerText + System.lineSeparator());
                out.write("========================================\r\n");
            }

            for (String word : map.keySet()) {
                frequency = map.get(word);
                percent = 100.0 * frequency / totalWords;
                if (percent < 0.000001) {
                    percent = 0.0;
                }
                out.write(String.format("%25s", word) + " " + String.format("%7d", frequency) + "  " + String.format("%.10f", percent) + "%" + System.lineSeparator());
            }
            // Close the output stream
            out.close();
            fstream.close();
        } catch (Exception e) {// Catch exception if any
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    public static boolean writeToFile(String text, String fileName) {

        try {
            // Create file
            FileWriter fstream = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(text);
            // Close the output stream
            out.close();
            fstream.close();
        } catch (Exception e) {// Catch exception if any
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    /*
     * Append if exists. Else, create and write.
     */
    public static boolean appendToFile(ArrayList<String> lines, String fileName) {
        try {

            File file = new File(fileName);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            // true = append file
            FileWriter fileWritter = new FileWriter(file.getName(), true);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            for (int i = 0; i < lines.size(); i++) {
                bufferWritter.write(lines.get(i)); // assuming new line is given in data itself.
            }
            bufferWritter.flush();
            bufferWritter.close();

            System.out.println("Saved to file!!");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
