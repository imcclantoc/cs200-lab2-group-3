public class Provider {
    private final String id;
    private String name;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;


    public Provider(String id, String name, String streetAddress, String city,
            String state, String zipCode) {
        this.id = id;
        this.name = name;
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }


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
}
