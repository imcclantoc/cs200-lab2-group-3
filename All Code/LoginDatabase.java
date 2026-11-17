
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


// Manages user login information, including creating accounts,
// validating passwords, and storing login records securely.
public class LoginDatabase {

    // Algorithm used to securely hash passwords.
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";

    // Number of times the password hashing algorithm is repeated.
    // More iterations make password cracking more difficult.
    private static final int ITERATIONS = 210_000;

    // Length of the generated password hash in bits.
    private static final int KEY_LENGTH = 256;

    // Length of the randomly generated salt in bytes.
    private static final int SALT_LENGTH = 16;

    // Default ID number and password used for testing.
    private static final String TEST_ID_NUMBER = "123456789";
    private static final String TEST_PASSWORD = "testing";

    // Generates random values for password salts.
    private static final SecureRandom RANDOM = new SecureRandom();

    // List of user types that are allowed to have login accounts.
    private static final List<String> USER_TYPES = List.of(
            "provider", "operator", "acme system", "manager");


    // Stores login records using a unique key for each user type and ID number.
    // LinkedHashMap preserves the order in which records are inserted.
    private final Map<String, LoginRecord> logins = new LinkedHashMap<>();

    // Stores the file path where login information is saved.
    private final Path storageFile;


    // Default constructor.
    // Uses the default login storage file and creates test logins.
    public LoginDatabase() {
        this(Path.of("data", "logins.tsv"));
        seedTesterLogins();
    }


    // Constructor that allows a specific storage file to be provided.
    // Loads existing login records from that file.
    public LoginDatabase(Path storageFile) {
        this.storageFile = Objects.requireNonNull(storageFile, "storageFile");
        load();
    }


    // Creates a new login account with the given user type, ID number, and password.
    public synchronized boolean addLogin(String userType, String idNumber, char[] password) {

        // Standardizes the user type and validates the ID and password.
        String normalizedType = normalizeUserType(userType);
        String normalizedIdNumber = requireIdNumber(idNumber);
        requirePassword(password);

        // Creates a unique key using the user type and ID number.
        String key = createKey(normalizedType, normalizedIdNumber);

        // Prevents duplicate accounts from being created.
        if (logins.containsKey(key)) {
            return false;
        }


        // Generates a random salt to make the password hash unique.
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        // Hashes the password using the generated salt and specified iterations.
        byte[] passwordHash = hashPassword(password, salt, ITERATIONS);

        // Creates and stores the new login record.
        logins.put(key, new LoginRecord(normalizedType, normalizedIdNumber, salt,
                passwordHash, ITERATIONS));

        // Saves the updated login database to the file.
        save();

        // Returns true because the account was successfully added.
        return true;
    }


    // Checks whether the provided login information matches an existing account.
    public synchronized boolean validateLogin(String userType, String idNumber, char[] password) {

        // Rejects a missing or empty password.
        if (password == null || password.length == 0) {
            return false;
        }


        // Attempts to standardize the user type.
        String normalizedType;
        try {
            normalizedType = normalizeUserType(userType);

        // If the user type is invalid, the login attempt fails.
        } catch (IllegalArgumentException exception) {
            return false;
        }

        // Rejects a missing or empty ID number.
        if (idNumber == null || idNumber.trim().isEmpty()) {
            return false;
        }


        // Finds the stored login record using the user type and ID number.
        LoginRecord record = logins.get(createKey(normalizedType, idNumber.trim()));

        // If no matching account exists, the login attempt fails.
        if (record == null) {
            return false;
        }

        // Hashes the entered password using the original salt and iteration count.
        byte[] candidateHash = hashPassword(password, record.salt, record.iterations);

        // Compares the entered password hash with the stored hash.
        // MessageDigest.isEqual helps prevent timing-based comparison attacks.
        return MessageDigest.isEqual(record.passwordHash, candidateHash);
    }


    // Loads existing login records from the storage file.
    private void load() {
        try {

            // Reads each record from the file.
            // Each record must contain exactly three fields.
            for (String[] fields : DatabaseFileSupport.readRecords(storageFile, 3)) {

                // Standardizes and validates the stored user type and ID number.
                String userType = normalizeUserType(fields[0]);
                String idNumber = requireIdNumber(fields[1]);

                // Reconstructs the login record from its stored password information.
                LoginRecord record = LoginRecord.fromStorage(userType, idNumber, fields[2]);

                // Adds the record to the map.
                // If the key already exists, a duplicate record is found and an error is thrown.
                if (logins.putIfAbsent(createKey(userType, idNumber), record) != null) {
                    throw new IOException("Duplicate login record in " + storageFile);
                }
            }

        // Handles problems reading the file or invalid login information.
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not load login database from " + storageFile,
                    exception);
        }
    }


    // Saves all current login records to the storage file.
    private void save() {

        // Creates a list to hold the records in a format that can be written to the file.
        List<String[]> records = new ArrayList<>();

        // Converts each LoginRecord into an array containing its stored information.
        for (LoginRecord record : logins.values()) {
            records.add(new String[] { record.userType, record.idNumber, record.toStorage() });
        }

        try {
            // Writes all records to the storage file.
            DatabaseFileSupport.writeRecords(storageFile, records);

        // Handles errors that occur while saving the database.
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save login database to " + storageFile,
                    exception);
        }
    }


    // Creates default test accounts for each supported user type.
    private synchronized void seedTesterLogins() {

        // Converts the test password into a character array.
        char[] password = TEST_PASSWORD.toCharArray();

        // Tracks whether any new test accounts were added.
        boolean changed = false;

        try {

            // Creates a test login for each supported user type.
            for (String userType : USER_TYPES) {

                // Generates the unique key for the test account.
                String key = createKey(userType, TEST_ID_NUMBER);

                // Only creates the account if it does not already exist.
                if (!logins.containsKey(key)) {

                    // Generates a random salt for the test account.
                    byte[] salt = new byte[SALT_LENGTH];
                    RANDOM.nextBytes(salt);

                    // Hashes the test password.
                    byte[] passwordHash = hashPassword(password, salt, ITERATIONS);

                    // Stores the new test login record.
                    logins.put(key, new LoginRecord(userType, TEST_ID_NUMBER, salt,
                            passwordHash, ITERATIONS));

                    // Indicates that the database has been changed.
                    changed = true;
                }
            }

        } finally {

            // Clears the password from the character array for security.
            Arrays.fill(password, '\0');
        }

        // Saves the database only if at least one test account was added.
        if (changed) {
            save();
        }
    }


    // Validates and standardizes a user type.
    private static String normalizeUserType(String userType) {

        // Makes sure a user type was provided.
        if (userType == null) {
            throw new IllegalArgumentException("User type is required.");
        }

        // Removes extra spaces and converts the user type to lowercase.
        String normalizedType = userType.trim().toLowerCase(Locale.ROOT);

        // Checks whether the user type is in the list of supported types.
        if (!USER_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Unsupported user type: " + userType);
        }

        // Returns the standardized user type.
        return normalizedType;
    }


    // Validates an ID number and removes any extra spaces.
    private static String requireIdNumber(String idNumber) {

        // Makes sure an ID number was provided.
        if (idNumber == null || idNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("ID number is required.");
        }

        // Returns the ID number without leading or trailing spaces.
        return idNumber.trim();
    }


    // Checks that a password was provided and is not empty.
    private static void requirePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password is required.");
        }
    }


    // Creates a unique key by combining the user type and ID number.
    // The null character separates the two values to prevent ambiguity.
    private static String createKey(String userType, String idNumber) {
        return userType + "\u0000" + idNumber;
    }


    // Converts a password into a secure hash using PBKDF2.
    private static byte[] hashPassword(char[] password, byte[] salt, int iterations) {

        // Creates a password-based key specification using the password,
        // random salt, number of iterations, and desired key length.
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);

        try {

            // Generates and returns the password hash using the selected algorithm.
            return SecretKeyFactory.getInstance(HASH_ALGORITHM).generateSecret(keySpec).getEncoded();

        // Handles errors if the hashing algorithm is unavailable.
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);

        } finally {

            // Clears the password from the key specification after hashing.
            keySpec.clearPassword();
        }
    }


    // Represents one user's stored login information.
    private static final class LoginRecord {

        // Stores the account's user type and ID number.
        private final String userType;
        private final String idNumber;

        // Stores the random salt and hashed password.
        private final byte[] salt;
        private final byte[] passwordHash;

        // Stores the number of iterations used when hashing the password.
        private final int iterations;


        // Constructor that initializes all information for a login record.
        private LoginRecord(String userType, String idNumber, byte[] salt, byte[] passwordHash,
                int iterations) {
            this.userType = userType;
            this.idNumber = idNumber;
            this.salt = salt;
            this.passwordHash = passwordHash;
            this.iterations = iterations;
        }


        // Converts the login record's password information into a string for storage.
        private String toStorage() {

            // Creates a Base64 URL encoder without padding characters.
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

            // Combines the algorithm, iteration count, salt, and password hash.
            // Dollar signs separate the different pieces of information.
            return HASH_ALGORITHM + "$" + iterations + "$" + encoder.encodeToString(salt)
                    + "$" + encoder.encodeToString(passwordHash);
        }


        // Reconstructs a LoginRecord from the information stored in the database file.
        private static LoginRecord fromStorage(String userType, String idNumber,
                String storedPassword) throws IOException {

            // Splits the stored password information into its four components.
            String[] parts = storedPassword.split("\\$", -1);

            // Makes sure the correct number of components and hashing algorithm are present.
            if (parts.length != 4 || !HASH_ALGORITHM.equals(parts[0])) {
                throw new IOException("Invalid password record.");
            }

            try {

                // Converts the stored iteration count back into an integer.
                int iterations = Integer.parseInt(parts[1]);

                // Checks that the iteration count is within an acceptable range.
                if (iterations < 1 || iterations > 1_000_000) {
                    throw new IOException("Invalid password iteration count.");
                }

                // Creates a Base64 decoder to decode the stored salt and password hash.
                Base64.Decoder decoder = Base64.getUrlDecoder();
                byte[] salt = decoder.decode(parts[2]);
                byte[] passwordHash = decoder.decode(parts[3]);

                // Verifies that the salt and password hash have the expected lengths.
                if (salt.length != SALT_LENGTH || passwordHash.length != KEY_LENGTH / 8) {
                    throw new IOException("Invalid password hash.");
                }

                // Creates and returns a LoginRecord using the decoded information.
                return new LoginRecord(userType, idNumber, salt, passwordHash, iterations);

            // Handles invalid numbers or incorrectly encoded Base64 information.
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid password record.", exception);
            }
        }
    }
}
