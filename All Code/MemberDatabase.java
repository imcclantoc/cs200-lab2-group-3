import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class MemberDatabase {
    private static final String ACTIVE = "Active";
    private static final String SUSPENDED = "Suspended";


    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Path storageFile;
    private long nextMemberId = 1;


    public MemberDatabase() {
        this(Path.of("data", "members.tsv"));
    }


    public MemberDatabase(Path storageFile) {
        this.storageFile = Objects.requireNonNull(storageFile, "storageFile");
        load();
    }


    public synchronized String addMember(String name, String streetAddress, String city,
            String state, String zipCode) {
        requireValue(name, "Name");
        requireValue(streetAddress, "Street address");
        requireValue(city, "City");
        requireValue(state, "State");
        requireValue(zipCode, "ZIP code");


        String memberId = String.format(Locale.ROOT, "%09d", nextMemberId++);
        members.put(memberId, new Member(memberId, name.trim(), streetAddress.trim(),
                city.trim(), state.trim(), zipCode.trim(), ACTIVE));
        save();
        return memberId;
    }


    public synchronized boolean updateMember(String memberId, String name,
            String streetAddress, String city, String state, String zipCode) {
        Member member = members.get(memberId);
        if (member == null) {
            return false;
        }


        requireValue(name, "Name");
        requireValue(streetAddress, "Street address");
        requireValue(city, "City");
        requireValue(state, "State");
        requireValue(zipCode, "ZIP code");


        member.setName(name.trim());
        member.setStreetAddress(streetAddress.trim());
        member.setCity(city.trim());
        member.setState(state.trim());
        member.setZipCode(zipCode.trim());
        save();
        return true;
    }


    public synchronized boolean deleteMember(String memberId) {
        if (members.remove(memberId) == null) {
            return false;
        }
        save();
        return true;
    }


    public synchronized Member getMember(String memberId) {
        Member member = members.get(memberId);
        return member == null ? null : copyMember(member);
    }


    public synchronized String validateMember(String memberId) {
        Member member = members.get(memberId);
        return member == null ? "Invalid member ID" : member.getStatus();
    }


    public synchronized boolean updateMemberStatus(String memberId, String status) {
        Member member = members.get(memberId);
        if (member == null || (!ACTIVE.equals(status) && !SUSPENDED.equals(status))) {
            return false;
        }
        member.setStatus(status);
        save();
        return true;
    }


    public synchronized List<Member> getAllMembers() {
        List<Member> result = new ArrayList<>();
        for (Member member : members.values()) {
            result.add(copyMember(member));
        }
        return result;
    }


    private void load() {
        try {
            for (String[] record : DatabaseFileSupport.readRecords(storageFile, 7)) {
                if (!record[0].matches("\\d{9}")
                        || (!ACTIVE.equals(record[6]) && !SUSPENDED.equals(record[6]))) {
                    throw new IOException("Invalid member record in " + storageFile);
                }
                Member member = new Member(record[0], record[1], record[2], record[3],
                        record[4], record[5], record[6]);
                if (members.putIfAbsent(member.getId(), member) != null) {
                    throw new IOException("Duplicate member ID in " + storageFile);
                }
                nextMemberId = Math.max(nextMemberId, Long.parseLong(member.getId()) + 1);
            }
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Could not load member database from " + storageFile,
                    exception);
        }
    }


    private void save() {
        List<String[]> records = new ArrayList<>();
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


    private Member copyMember(Member member) {
        return new Member(member.getId(), member.getName(), member.getStreetAddress(),
                member.getCity(), member.getState(), member.getZipCode(), member.getStatus());
    }


    private void requireValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }
}
