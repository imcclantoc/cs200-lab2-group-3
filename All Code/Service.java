// Declares a public class named Service
public class Service {
    private final String serviceCode;
    private final String serviceName;
    private final double fee;

// Constructor
    public Service(String serviceCode, String serviceName, double fee) {
        this.serviceCode = serviceCode; // Stores the service's unique code
        this.serviceName = serviceName; // Stores the service's display name
        this.fee = fee; // Stores the service's fee in dollars
    }

// Getters
    public String getServiceCode() {
        return serviceCode;
    }


    public String getServiceName() {
        return serviceName;
    }


    public double getFee() {
        return fee;
    }
}
