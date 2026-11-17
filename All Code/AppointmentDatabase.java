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


public class AppointmentDatabase {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("MM-dd-uuuu").withResolverStyle(ResolverStyle.STRICT);
        private static final DateTimeFormatter CURRENT_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");


    private final List<Appointment> appointments = new ArrayList<>();
    private final Path storageFile;


    public AppointmentDatabase() {
        this(Path.of("data", "appointments.tsv"));
    }


    public AppointmentDatabase(Path storageFile) {
        this.storageFile = Objects.requireNonNull(storageFile, "storageFile");
        load();
    }


    public synchronized void addAppointment(String providerId, String memberId,
            String dateOfService, String serviceCode) {
        addAppointment(providerId, memberId, dateOfService, serviceCode, "");
        }


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


    public synchronized List<Appointment> getAllAppointments() {
        return new ArrayList<>(appointments);
    }


    public static boolean isValidDate(String date) {
        try {
            validateDate(date);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }


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


    private static void requireDigits(String value, int length, String fieldName) {
        if (value == null || !value.matches("\\d{" + length + "}")) {
            throw new IllegalArgumentException(fieldName + " must be " + length + " digits.");
        }
    }
}

