import org.apache.commons.io.FilenameUtils;

import java.nio.file.*;
import java.sql.Date;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

/**
 * Main application class
 */
public class Application {

    /**
     * Watch given directory path for file creation and process the newly added files
     * @param path directory path
     * @throws Exception
     */
    public static void watchDirectory(Path path) throws Exception {
        // Check if path is a folder
        try {
            Boolean isFolder = (Boolean) Files.getAttribute(path, "basic:isDirectory", NOFOLLOW_LINKS);
            if (!isFolder) {
                throw new IllegalArgumentException("Path: " + path + " is not a folder!");
            }
        } catch (IOException e) {
            throw e;
        }

        System.out.println("Watching directory: " + path);

        // Obtain the file system of the Path
        FileSystem fs = path.getFileSystem();

        // Create WatchService
        try (WatchService service = fs.newWatchService()) {

            // Register the path to the service and watch for events when a new file is created/added
            path.register(service, ENTRY_CREATE);

            // Start the infinite loop
            WatchKey key = null;
            while (true) {
                key = service.take();

                // Dequeue events
                WatchEvent.Kind<?> kind = null;
                for (WatchEvent<?> watchEvent : key.pollEvents()) {

                    // Get the type of the event
                    kind = watchEvent.kind();

                    if (OVERFLOW == kind) {
                        continue; // continue loop
                    } else if (ENTRY_CREATE == kind) { // New file was created

                        // This is a rough workaround - for the following exception: the process cannot access the file because it is being used by another process
                        // Suggested workaround on Stackoverflow was to sleep the current thread for a short amount of time to allow the operating system to stop working on the file
                        // because for some reason on some operating systems multiple events can be fired causing the above mentioned exception
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // Get the path of the newly added file
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();

                        // Check if the file is in csv format
                        if (FilenameUtils.getExtension(newPath.toString()).equals("csv")) {

                            System.out.println("New csv file added: " + newPath);

                            // Get the current time just before the start of processing
                            Instant start = Instant.now();

                            // Process the file and get the number of records processed
                            String fullPath = path.toString() + "/" + newPath;
                            int recordsProcessed = Utils.processFile(fullPath);
                            System.out.println("Number of records processed from the file: " + recordsProcessed);

                            // Get the current time after processing
                            Instant end = Instant.now();

                            // Get the delta time - difference between start and end time in milliseconds
                            long millis = Duration.between(start, end).toMillis();

                            // Format and print delta time
                            Date time = new Date(millis);
                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
                            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                            System.out.println("File processing time: " + sdf.format(time) + "\n");

                            // Move the file to the processed directory
                            File jarPath = Utils.getJarPath();
                            String processedDirectoryPath = jarPath.getParentFile().getAbsolutePath() + "/" + "processed/" + newPath;
                            Files.move(Paths.get(fullPath), Paths.get(processedDirectoryPath), StandardCopyOption.REPLACE_EXISTING);

                            // Get total records found count
                            System.out.println("Total records found in the database: " + Database.getInstance().getTotalRecordsCount() + "\n");

                            // Get and print all of the records from the table (timestamps are printed in local time zone)
                            System.out.println("Records in the record table (timestamps localized):\n");
                            Utils.printTable(Database.getInstance().getAllRecords());

                            // Get and print amounts by month
                            System.out.println("Record amounts grouped by month:\n");
                            Utils.printTable(Database.getInstance().getAmountsByMonth());

                        } else {
                            System.out.println("New file added is not csv!");
                        }
                    }
                }

                if (!key.reset()) {
                    break;
                }
            }

        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * Main application method
     */
    public static void main(String[] args) {

        // Get database instance
        Database database = Database.getInstance();

        try {

            // Connect to database
            database.connect();
            System.out.println("Successfully connected to the database!\n");

            // Get jar file path
            File jarPath = Utils.getJarPath();

            // Get incoming directory path
            String incomingDirectoryPath = jarPath.getParentFile().getAbsolutePath() + "/" + "incoming";

            // Watch incoming directory for new files
            watchDirectory(Paths.get(incomingDirectoryPath));

        } catch (Exception e) {
            // Most Exceptions will be passed through the stack until they come here where we print the stack trace and
            // notify the user that something went wrong
            // we also try to close the database connection
            System.out.println("Something went wrong");
            e.printStackTrace();
            database.close();
        }
    }
}
