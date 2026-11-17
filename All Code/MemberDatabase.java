import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class MemberDatabase {

    // Member status options.
    private static final String ACTIVE = "Active";
    private static final String SUSPENDED = "Suspended";

    // Stores members using their IDs.
    private final Map<String, Member> members = new LinkedHashMap<>();

    // File used to store member information.
    private final Path storageFile;

    // Tracks the next available member ID.
    private long nextMemberId = 1;


    // Creates a database using the default file location.
    public MemberDatabase() {
        this(Path.of("data", "members.tsv"));
    }


    // Creates a database using a specified file and loads its records.
    public MemberDatabase(Path storageFile) {
        this.storageFile = Objects.requireNonNull(storageFile, "storageFile");
        load();
    }


    // Adds a new member and returns their ID.
    public synchronized String addMember(String name, String streetAddress, String city,
            String state, String zipCode) {

        // Check that all required fields are filled in.
        requireValue(name, "Name");
        requireValue(streetAddress, "Street address");
        requireValue(city, "City");
        requireValue(state, "State");
        requireValue(zipCode, "ZIP code");

        // Generate a nine-digit ID.
        String memberId = String.format(Locale.ROOT, "%09d", nextMemberId++);

        // Create and store the new member with Active status.
        members.put(memberId, new Member(memberId, name.trim(), streetAddress.trim(),
                city.trim(), state.trim(), zipCode.trim(), ACTIVE));

        // Save the updated database.
        save();
        return memberId;
    }


    // Updates an existing member's information.
    public synchronized boolean updateMember(String memberId, String name,
            String streetAddress, String city, String state, String zipCode) {

        // Find the member using their ID.
        Member member = members.get(memberId);
        if (member == null) {
            return false;
        }

        // Check that all updated fields are filled in.
        requireValue(name, "Name");
        requireValue(streetAddress, "Street address");
        requireValue(city, "City");
        requireValue(state, "State");
        requireValue(zipCode, "ZIP code");

        // Update the member's information.
        member.setName(name.trim());
        member.setStreetAddress(streetAddress.trim());
        member.setCity(city.trim());
        member.setState(state.trim());
        member.setZipCode(zipCode.trim());

        save();
        return true;
    }


    // Deletes a member using their ID.
    public synchronized boolean deleteMember(String memberId) {
        if (members.remove(memberId) == null) {
            return false;
        }
        save();
        return true;
    }


    // Finds a member and returns a copy of their information.
    public synchronized Member getMember(String memberId) {
        Member member = members.get(memberId);
        return member == null ? null : copyMember(member);
    }


    // Checks whether a member ID exists and returns their status.
    public synchronized String validateMember(String memberId) {
        Member member = members.get(memberId);
        return member == null ? "Invalid member ID" : member.getStatus();
    }


    // Changes a member's status to Active or Suspended.
    public synchronized boolean updateMemberStatus(String memberId, String status) {
        Member member = members.get(memberId);

        // Check that the member and new status are valid.
        if (member == null || (!ACTIVE.equals(status) && !SUSPENDED.equals(status))) {
            return false;
        }

        member.setStatus(status);
        save();
        return true;
    }


    // Returns a list of all members.
    public synchronized List<Member> getAllMembers() {
        List<Member> result = new ArrayList<>();

        // Add a copy of each member to the list.
        for (Member member : members.values()) {
            result.add(copyMember(member));
        }
        return result;
    }


    // Loads member records from the storage file.
    private void load() {
        try {
            // Read records that contain seven fields each.
            for (String[] record : DatabaseFileSupport.readRecords(storageFile, 7)) {

                // Check that the ID and status are valid.
                if (!record[0].matches("\\d{9}")
                        || (!ACTIVE.equals(record[6]) && !SUSPENDED.equals(record[6]))) {
                    throw new IOException("Invalid member record in " + storageFile);
                }

                // Create a member from the saved information.
                Member member = new Member(record[0], record[1], record[2], record[3],
                        record[4], record[5], record[6]);

                // Prevent duplicate member IDs.
                if (members.putIfAbsent(member.getId(), member) != null) {
                    throw new IOException("Duplicate member ID in " + storageFile);
                }

                // Set the next ID based on the existing records.
                nextMemberId = Math.max(nextMemberId, Long.parseLong(member.getId()) + 1);
            }
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Could not load member database from " + storageFile,
                    exception);
        }
    }


    // Saves all members to the storage file.
    private void save() {
        List<String[]> records = new ArrayList<>();

        // Convert each member into a record for the file.
        for (Member member : members.values()) {
            records.add(new String[] { member.getId(), member.getName(), member.getStreetAddress(),
                    member.getCity(), member.getState(), member.getZipCode(), member.getStatus() });
        }

        try {
            DatabaseFileSupport.writeRecords(storageFile, records);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save member database to " + storageFile,
                    exception);
        }
    }


    // Creates a copy of a member.
    private Member copyMember(Member member) {
        return new Member(member.getId(), member.getName(), member.getStreetAddress(),
                member.getCity(), member.getState(), member.getZipCode(), member.getStatus());
    }


    // Checks that a required field is not empty.
    private void requireValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }
}
