import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


/**
 * Stores, updates, and loads providers.
 */
public class ProviderDatabase {
    private static final String TEST_PROVIDER_ID = "123456789";


    private final Map<String, Provider> providers = new LinkedHashMap<>();
    private final Path storageFile;
    private long nextProviderId = 1;


    /**
     * Creates a database using the default provider file.
     */
    public ProviderDatabase() {
        this(Path.of("data", "providers.tsv"));
        seedTesterProvider();
    }


    /**
     * Creates a database using the given provider file.
     */
    public ProviderDatabase(Path storageFile) {
        this.storageFile = Objects.requireNonNull(storageFile, "storageFile");
        load();
    }


    /**
     * Adds a provider and returns its assigned ID.
     */
    public synchronized String addProvider(String name, String streetAddress, String city,
            String state, String zipCode) {
        requireValue(name, "Name");
        requireValue(streetAddress, "Street address");
        requireValue(city, "City");
        requireValue(state, "State");
        requireValue(zipCode, "ZIP code");


        String providerId = allocateProviderId();
        providers.put(providerId, new Provider(providerId, name.trim(), streetAddress.trim(),
                city.trim(), state.trim(), zipCode.trim()));
        save();
        return providerId;
    }


    /**
     * Updates a provider. Returns false if the provider ID does not exist.
     */
    public synchronized boolean updateProvider(String providerId, String name,
            String streetAddress, String city, String state, String zipCode) {
        Provider provider = providers.get(providerId);
        if (provider == null) {
            return false;
        }


        requireValue(name, "Name");
        requireValue(streetAddress, "Street address");
        requireValue(city, "City");
        requireValue(state, "State");
        requireValue(zipCode, "ZIP code");


        provider.setName(name.trim());
        provider.setStreetAddress(streetAddress.trim());
        provider.setCity(city.trim());
        provider.setState(state.trim());
        provider.setZipCode(zipCode.trim());
        save();
        return true;
    }


    /**
     * Deletes a provider. Returns false if the provider ID does not exist.
     */
    public synchronized boolean deleteProvider(String providerId) {
        if (providers.remove(providerId) == null) {
            return false;
        }
        save();
        return true;
    }


    /**
     * Returns a copy of the provider, or null if it does not exist.
     */
    public synchronized Provider getProvider(String providerId) {
        Provider provider = providers.get(providerId);
        return provider == null ? null : copyProvider(provider);
    }


    /**
     * Reports whether a provider ID exists.
     */
    public synchronized boolean validateProvider(String providerId) {
        return providers.containsKey(providerId);
    }


    /**
     * Returns copies of all providers.
     */
    public synchronized List<Provider> getAllProviders() {
        List<Provider> result = new ArrayList<>();
        for (Provider provider : providers.values()) {
            result.add(copyProvider(provider));
        }
        return result;
    }


    private void load() {
        try {
            for (String[] record : DatabaseFileSupport.readRecords(storageFile, 6)) {
                if (!record[0].matches("\\d{9}")) {
                    throw new IOException("Invalid provider record in " + storageFile);
                }
                Provider provider = new Provider(record[0], record[1], record[2], record[3],
                        record[4], record[5]);
                if (providers.putIfAbsent(provider.getId(), provider) != null) {
                    throw new IOException("Duplicate provider ID in " + storageFile);
                }
                nextProviderId = Math.max(nextProviderId, Long.parseLong(provider.getId()) + 1);
            }
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Could not load provider database from " + storageFile,
                    exception);
        }
    }


    private synchronized void seedTesterProvider() {
        if (!providers.containsKey(TEST_PROVIDER_ID)) {
            providers.put(TEST_PROVIDER_ID, new Provider(TEST_PROVIDER_ID, "Test Provider",
                    "123 Test Street", "Test City", "TS", "12345"));
            save();
        }
    }


    private String allocateProviderId() {
        long maximumId = 999_999_999L;
        if (providers.size() >= maximumId) {
            throw new IllegalStateException("No provider IDs are available.");
        }


        if (nextProviderId > maximumId) {
            nextProviderId = 1;
        }
        long firstCandidate = nextProviderId;
        do {
            String candidate = String.format(Locale.ROOT, "%09d", nextProviderId);
            nextProviderId = nextProviderId == maximumId ? 1 : nextProviderId + 1;
            if (!providers.containsKey(candidate)) {
                return candidate;
            }
        } while (nextProviderId != firstCandidate);
        throw new IllegalStateException("No provider IDs are available.");
    }


    private void save() {
        List<String[]> records = new ArrayList<>();
        for (Provider provider : providers.values()) {
            records.add(new String[] { provider.getId(), provider.getName(),
                    provider.getStreetAddress(), provider.getCity(), provider.getState(),
                    provider.getZipCode() });
        }
        try {
            DatabaseFileSupport.writeRecords(storageFile, records);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save provider database to " + storageFile,
                    exception);
        }
    }


    private Provider copyProvider(Provider provider) {
        return new Provider(provider.getId(), provider.getName(), provider.getStreetAddress(),
                provider.getCity(), provider.getState(), provider.getZipCode());
    }


    private void requireValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }
}
