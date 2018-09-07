import java.io.*;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Utility class
 */
public class Utils {

    /**
     * Get jar path
     * @return jar file path
     * @throws URISyntaxException
     */
    public static File getJarPath() throws URISyntaxException {
        return new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
    }

    /**
     * Print table data to standard output
     * @param table 2-d ArrayList of data
     */
    public static void printTable(ArrayList<ArrayList<String>> table) {
        System.out.println("------------------------------------------------");
        for (ArrayList<String> e : table) {
            for (String k : e) {
                System.out.printf("%-25s", k);
            }
            System.out.println();
        }
        System.out.println("------------------------------------------------\n");
    }

    /**
     * Get configuration properties
     * @return Configuration properties
     */
    public static Properties getConfigurationProperties() {

        Properties properties = new Properties();
        FileInputStream file;
        String path = "./config.properties";

        try {
            file = new FileInputStream(path);
            properties.load(file);
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties;
    }

    /**
     * Process file and return number of records processed
     * @param path file path
     * @return number of records processed
     * @throws Exception
     */
    public static int processFile(String path) throws Exception {
        FileReader fileReader = null;

        try {
            fileReader = new FileReader(path);
        } catch (FileNotFoundException e) {
            throw e;
        }

        int recordsProcessed = 0;

        try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {

                String[] lineSplit = line.trim().split(",");

                if (lineSplit.length == 3) {

                    String recordNumber = lineSplit[0].trim();
                    String timestamp = lineSplit[1].trim();
                    String amount = lineSplit[2].trim();

                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    formatter.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));
                    java.util.Date javaDate = formatter.parse(timestamp);
                    java.sql.Timestamp sqlTimestamp = new java.sql.Timestamp(javaDate.getTime());

                    recordsProcessed++;
                    Database.getInstance().insertRecordData(Integer.parseInt(recordNumber), sqlTimestamp, Double.parseDouble(amount));
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        }

        return recordsProcessed;
    }
}
