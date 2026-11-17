import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Manages a collection of {@link Appointment} records, providing thread-safe 
 * addition, validation, and persistent storage using a flat-file backend.
 */
public class AppointmentDatabase {
    /** Strict date formatter enforcing MM-dd-uuuu format. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("MM-dd-uuuu").withResolverStyle(ResolverStyle.STRICT);
            
    /** Formatter for recording the exact timestamp when an appointment is added. */
    private static final DateTimeFormatter CURRENT_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");


    /** In-memory thread-safe list storing active appointments. */
    private final List<Appointment> appointments = new ArrayList<>();
    
    /** The file path where appointment records are persistently stored. */
    private final Path storageFile;


    /**
     * Constructs a new AppointmentDatabase using the default storage path ("data/appointments.tsv").
     * Automatically triggers loading of existing records from disk.
     */
    public AppointmentDatabase() {
        this(Path.of("data", "appointments.tsv"));
    }


    /**
     * Constructs a new AppointmentDatabase using a specified storage file path.
     * Automatically triggers loading of existing records from disk.
     * 
     * @param storageFile the path to the storage file; must not be null
     * @throws NullPointerException if storageFile is null
     */
    public AppointmentDatabase(Path storageFile) {
        this.storageFile = Objects.requireNonNull(storageFile, "storageFile");
        load();
    }


    /**
     * Adds a new appointment without comments, using the current system timestamp.
     * 
     * @param providerId    the 9-digit provider ID
     * @param memberId      the 9-digit member ID
     * @param dateOfService the date of service in MM-DD-YYYY format
     * @param serviceCode   the 6-digit service code
     */
    public synchronized void addAppointment(String providerId, String memberId,
            String dateOfService, String serviceCode) {
        addAppointment(providerId, memberId, dateOfService, serviceCode, "");
    }


    /**
     * Adds a new appointment with comments, validating all inputs and persisting changes to disk.
     * 
     * @param providerId    the 9-digit provider ID
     * @param memberId      the 9-digit member ID
     * @param dateOfService the date of service in MM-DD-YYYY format
     * @param serviceCode   the 6-digit service code
     * @param comments      optional comments or notes regarding the appointment
     * @throws IllegalArgumentException if any validation rule fails
     */
    public synchronized void addAppointment(String providerId, String memberId,
            String dateOfService, String serviceCode, String comments) {
        requireDigits(providerId, 9, "Provider ID");
        requireDigits(memberId, 9, "Member ID");
        requireDigits(serviceCode, 6, "Service code");
        validateDate(dateOfService);


        appointments.add(new Appointment(providerId, memberId, dateOfService, serviceCode,
                comments == null ? "" : comments.trim(),
                LocalDateTime.now().format(CURRENT_DATE_TIME_FORMAT)));
        save();
    }


    /**
     * Returns a thread-safe shallow copy of all stored appointments.
     * 
     * @return a list containing all current appointments
     */
    public synchronized List<Appointment> getAllAppointments() {
        return new ArrayList<>(appointments);
    }


    /**
     * Checks whether the provided date string is valid according to strict date rules.
     * 
     * @param date the date string to evaluate
     * @return true if the date is valid, false otherwise
     */
    public static boolean isValidDate(String date) {
        try {
            validateDate(date);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }


    /**
     * Loads appointment records from the persistent storage file.
     * 
     * @throws IllegalStateException if reading fails or records are malformed
     */
    private void load() {
        try {
            for (String[] record : DatabaseFileSupport.readRecords(storageFile, 4, 6)) {
                requireDigits(record[0], 9, "Provider ID");
                requireDigits(record[1], 9, "Member ID");
                validateDate(record[2]);
                requireDigits(record[3], 6, "Service code");
                String comments = record.length == 5 ? record[4] : "";
                String currentDateTime = record.length == 6 ? record[5] : "";
                appointments.add(new Appointment(record[0], record[1], record[2], record[3],
                    comments, currentDateTime));
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not load appointment database from " + storageFile,
                    exception);
        }
    }


    /**
     * Saves all current appointment records to the persistent storage file.
     * 
     * @throws IllegalStateException if writing to the file fails
     */
    private void save() {
        List<String[]> records = new ArrayList<>();
        for (Appointment appointment : appointments) {
            records.add(new String[] { appointment.getProviderId(), appointment.getMemberId(),
                    appointment.getDateOfService(), appointment.getServiceCode(),
                    appointment.getComments(), appointment.getCurrentDateTime() });
        }
        try {
            DatabaseFileSupport.writeRecords(storageFile, records);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save appointment database to " + storageFile,
                    exception);
        }
    }


    /**
     * Validates that a date string is non-null and conforms to the strict format.
     * 
     * @param date the date string to validate
     * @throws IllegalArgumentException if the date is null or invalid
     */
    private static void validateDate(String date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        try {
            LocalDate.parse(date, DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Date must be a valid date in MM-DD-YYYY format.",
                    exception);
        }
    }


    /**
     * Validates that a value consists entirely of digits and matches the expected length.
     * 
     * @param value      the string value to check
     * @param length     the required exact length
     * @param fieldName  the descriptive name of the field for error messages
     * @throws IllegalArgumentException if the value is null or does not match digit constraints
     */
    private static void requireDigits(String value, int length, String fieldName) {
        if (value == null || !value.matches("\\d{" + length + "}")) {
            throw new IllegalArgumentException(fieldName + " must be " + length + " digits.");
        }
    }
}



