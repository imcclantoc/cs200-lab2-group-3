import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;


// Declares a public class named ServiceDatabase
public class ServiceDatabase {
    private final Map<String, Service> services = new LinkedHashMap<>();
    private final Path storageFile;


// Constructors
    public ServiceDatabase() {
        this(Path.of("data", "services.tsv")); // Uses the default storage file
        seedChocolateServices(); // Adds the default services if they are missing
    }


    public ServiceDatabase(Path storageFile) {
        this.storageFile = Objects.requireNonNull(storageFile, "storageFile"); // Stores the file path, rejects null
        load(); // Reads existing services from the file
    }


// Adds a new service, returns false if the code already exists
    public synchronized boolean addService(String serviceCode, String serviceName, double fee) {
        requireValidService(serviceCode, serviceName, fee); // Checks the input is valid
        if (services.containsKey(serviceCode)) {
            return false; // Code is already in use
        }
        services.put(serviceCode, new Service(serviceCode, serviceName.trim(), fee)); // Adds the service with a trimmed name
        save(); // Writes the change to the file
        return true; // Service was added
    }


// Getters
    public synchronized Service getServiceByCode(String serviceCode) {
        Service service = services.get(serviceCode); // Looks up the service by its code
        return service == null ? null // Returns null if not found
                : new Service(service.getServiceCode(), service.getServiceName(), service.getFee()); // Otherwise returns a copy
    }


    public synchronized List<Service> getAllServices() {
        List<Service> result = new ArrayList<>(); // Holds the copies
        for (Service service : services.values()) { // Loops through every service
            result.add(new Service(service.getServiceCode(), service.getServiceName(),
                    service.getFee())); // Adds a copy to the list
        }
        return result; // Returns the list of copies
    }


// Reads services from the file into the map
    private void load() {
        try {
            for (String[] record : DatabaseFileSupport.readRecords(storageFile, 3)) { // Reads each record with 3 columns
                double fee = Double.parseDouble(record[2]); // Converts the fee text to a number
                requireValidService(record[0], record[1], fee); // Checks the data is valid
                if (services.putIfAbsent(record[0], new Service(record[0], record[1], fee)) != null) { // Adds the service, fails if code exists
                    throw new IOException("Duplicate service code in " + storageFile); // Duplicate code found
                }
            }
        } catch (IOException | IllegalArgumentException exception) { // Handles file or data problems
            throw new IllegalStateException("Could not load service database from " + storageFile,
                    exception); // Stops startup with a clear error
        }
    }


// Writes all services to the file
    private void save() {
        List<String[]> records = new ArrayList<>(); // Holds one record per service
        for (Service service : services.values()) { // Loops through every service
            records.add(new String[] { service.getServiceCode(), service.getServiceName(),
                    Double.toString(service.getFee()) }); // Converts the service to a record
        }
        try {
            DatabaseFileSupport.writeRecords(storageFile, records); // Writes the records to the file
        } catch (IOException exception) { // Handles file writing problems
            throw new IllegalStateException("Could not save service database to " + storageFile,
                    exception); // Stops with a clear error
        }
    }


// Adds the default services and gives random fees to ones with a fee of 0
    private synchronized void seedChocolateServices() {
        String[] serviceNames = {
                "Cocoa Wellness Visit",
                "Truffle Heart Check",
                "Fudge Fitness Exam",
                "Mocha Vision Test",
                "Ganache Skin Consult",
                "Cacao Dental Check"
        };


        int nextCode = 100000; // First code to try for new services
        boolean changed = false; // Tracks whether the file needs to be saved
        for (String serviceName : serviceNames) { // Loops through each default name
            Service existingService = services.values().stream()
                .filter(service -> service.getServiceName().equalsIgnoreCase(serviceName)) // Finds a service with the same name
                .findFirst().orElse(null); // Null if none found
            if (existingService != null) {
            if (existingService.getFee() == 0.0) {
                services.put(existingService.getServiceCode(), new Service(
                    existingService.getServiceCode(), existingService.getServiceName(),
                    randomFee())); // Replaces it with a random fee
                changed = true;
            }
                continue; // Moves on to the next name
            }


            String serviceCode;
            do {
                if (nextCode > 999999) {
                    throw new IllegalStateException("No six-digit service codes are available."); // Ran out of codes
                }
                serviceCode = String.format(Locale.ROOT, "%06d", nextCode++); // Makes a six-digit code
            } while (services.containsKey(serviceCode)); // Repeats if the code is taken
            services.put(serviceCode, new Service(serviceCode, serviceName, randomFee())); // Adds the new service
            changed = true;
        }
        if (changed) {
            save(); // Saves only if something changed
        }
    }


// Returns a random fee from 0.01 to 150.00
    private double randomFee() {
        int cents = ThreadLocalRandom.current().nextInt(1, 15001); // Picks a random number of cents
        return cents / 100.0; // Converts cents to dollars
    }


// Checks that the code, name, and fee follow the rules
    private void requireValidService(String serviceCode, String serviceName, double fee) {
        if (serviceCode == null || !serviceCode.matches("\\d{6}")) { // Code must be exactly six digits
            throw new IllegalArgumentException("Service code must be six digits.");
        }
        if (serviceName == null || serviceName.trim().isEmpty() || serviceName.trim().length() > 20) { // Name must be 1 to 20 characters
            throw new IllegalArgumentException("Service name must contain 1 to 20 characters.");
        }
        if (!Double.isFinite(fee) || fee < 0 || fee > 999.99) { // Fee must be a real number from 0 to 999.99
            throw new IllegalArgumentException("Fee must be between 0 and 999.99.");
        }
    }
}