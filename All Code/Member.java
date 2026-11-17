// This identifies all the variables associated with the Member class
public class Member {
    private final String id;
    private String name;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private String status;

    // Member sets all variables with input data to this (itself)
    public Member(String id, String name, String streetAddress, String city,
            String state, String zipCode, String status) {
        this.id = id;
        this.name = name;
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.status = status;
    }

    // All of these following methods return the string associated with the variable

    public String getId() {
        return id;
    }


    public String getName() {
        return name;
    }


    public String getStreetAddress() {
        return streetAddress;
    }


    public String getCity() {
        return city;
    }


    public String getState() {
        return state;
    }


    public String getZipCode() {
        return zipCode;
    }


    public String getStatus() {
        return status;
    }

    // The following methods are used to set individual variables
    
    public void setName(String name) {
        this.name = name;
    }


    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }


    public void setCity(String city) {
        this.city = city;
    }


    public void setState(String state) {
        this.state = state;
    }


    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }


    public void setStatus(String status) {
        this.status = status;
    }

    // Determines member status based on whether the member has paid
    public boolean isSuspended() {
        return "Suspended".equals(status);
    }
}
