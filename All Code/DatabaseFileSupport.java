
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


// Handles reading and writing database records to files.
final class DatabaseFileSupport {

    // Private constructor prevents this utility class from being instantiated.
    private DatabaseFileSupport() {
    }


    // Reads records from a file where each record must have exactly fieldCount fields.
    static List<String[]> readRecords(Path file, int fieldCount) throws IOException {
        return readRecords(file, fieldCount, fieldCount);
    }


    // Reads records from a file where each record must have between
    // minimumFieldCount and maximumFieldCount fields.
    static List<String[]> readRecords(Path file, int minimumFieldCount, int maximumFieldCount)
            throws IOException {

        // Creates a list to store all the records read from the file.
        List<String[]> records = new ArrayList<>();

        // If the file does not exist, return an empty list.
        if (!Files.exists(file)) {
            return records;
        }


        // Reads all lines from the file using UTF-8 encoding.
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        // Processes each line in the file individually.
        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {

            // Splits each line into encoded fields using tabs as separators.
            // The -1 keeps empty fields instead of discarding them.
            String[] encodedFields = lines.get(lineNumber).split("\t", -1);

            // Checks whether the record contains an acceptable number of fields.
            if (encodedFields.length < minimumFieldCount || encodedFields.length > maximumFieldCount) {
                throw new IOException("Invalid record at line " + (lineNumber + 1) + " in " + file);
            }


            // Creates an array to hold the decoded field values.
            String[] fields = new String[encodedFields.length];

            try {

                // Decodes each field from Base64 URL encoding back into a string.
                for (int index = 0; index < encodedFields.length; index++) {
                    fields[index] = new String(Base64.getUrlDecoder().decode(encodedFields[index]),
                            StandardCharsets.UTF_8);
                }

            // If a field contains invalid Base64 data, throws an IOException.
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid encoded record at line " + (lineNumber + 1)
                        + " in " + file, exception);
            }

            // Adds the decoded record to the list of records.
            records.add(fields);
        }

        // Returns all successfully read and decoded records.
        return records;
    }


    // Writes a list of records to a file.
    static void writeRecords(Path file, List<String[]> records) throws IOException {

        // Converts the file path into an absolute path.
        Path absoluteFile = file.toAbsolutePath();

        // Gets the directory containing the file.
        Path parent = absoluteFile.getParent();

        // Creates the parent directory if it does not already exist.
        Files.createDirectories(parent);


        // Creates a temporary file in the same directory as the destination file.
        // This helps prevent data loss if writing is interrupted.
        Path temporaryFile = Files.createTempFile(parent, absoluteFile.getFileName().toString(), ".tmp");

        try {

            // Creates a list to store the formatted lines before writing them.
            List<String> lines = new ArrayList<>();

            // Processes each record in the list.
            for (String[] record : records) {

                // Builds the line that will represent the current record.
                StringBuilder line = new StringBuilder();

                // Processes each field within the current record.
                for (int index = 0; index < record.length; index++) {

                    // Adds a tab between fields, but not before the first field.
                    if (index > 0) {
                        line.append('\t');
                    }

                    // Encodes the field using Base64 URL encoding without padding.
                    // This helps prevent special characters from interfering with file formatting.
                    line.append(Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(record[index].getBytes(StandardCharsets.UTF_8)));
                }

                // Adds the completed record line to the list.
                lines.add(line.toString());
            }

            // Writes all formatted records to the temporary file using UTF-8.
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);

            try {

                // Attempts to move the temporary file to the destination atomically.
                // REPLACE_EXISTING allows an existing database file to be overwritten.
                Files.move(temporaryFile, absoluteFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);

            // If atomic moves are not supported by the file system,
            // performs a regular move instead.
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING);
            }

        } finally {

            // Deletes the temporary file if it still exists.
            // This ensures cleanup even if an error occurs during writing.
            Files.deleteIfExists(temporaryFile);
        }
    }
}
