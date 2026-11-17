/**
 * Stores the identifying and contact information for a provider.
 */
public class Provider {
    private final String id;
    private String name;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;


    /**
     * Creates a provider.
     */
    public Provider(String id, String name, String streetAddress, String city,
            String state, String zipCode) {
        this.id = id;
        this.name = name;
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }


    /**
     * Returns the provider ID.
     */
    public String getId() {
        return id;
    }


    /**
     * Returns the provider name.
     */
    public String getName() {
        return name;
    }


    /**
     * Returns the provider street address.
     */
    public String getStreetAddress() {
        return streetAddress;
    }


    /**
     * Returns the provider city.
     */
    public String getCity() {
        return city;
    }


    /**
     * Returns the provider state.
     */
    public String getState() {
        return state;
    }


    /**
     * Returns the provider ZIP code.
     */
    public String getZipCode() {
        return zipCode;
    }


    /**
     * Sets the provider name.
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Sets the provider street address.
     */
    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }


    /**
     * Sets the provider city.
     */
    public void setCity(String city) {
        this.city = city;
    }


    /**
     * Sets the provider state.
     */
    public void setState(String state) {
        this.state = state;
    }


    /**
     * Sets the provider ZIP code.
     */
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}
