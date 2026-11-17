public class Appointment {
    private final String currentDateTime;
    private final String providerId;
    private final String memberId;
    private final String dateOfService;
    private final String serviceCode;
    private final String comments;


    public Appointment(String providerId, String memberId, String dateOfService,
            String serviceCode) {
        this(providerId, memberId, dateOfService, serviceCode, "", "");
    }


    public Appointment(String providerId, String memberId, String dateOfService,
            String serviceCode, String comments) {
        this(providerId, memberId, dateOfService, serviceCode, comments, "");
    }


    public Appointment(String providerId, String memberId, String dateOfService,
            String serviceCode, String comments, String currentDateTime) {
        this.currentDateTime = currentDateTime;
        this.providerId = providerId;
        this.memberId = memberId;
        this.dateOfService = dateOfService;
        this.serviceCode = serviceCode;
        this.comments = comments;
    }


    public String getCurrentDateTime() {
        return currentDateTime;
    }


    public String getProviderId() {
        return providerId;
    }


    public String getMemberId() {
        return memberId;
    }


    public String getDateOfService() {
        return dateOfService;
    }


    public String getServiceCode() {
        return serviceCode;
    }


    public String getComments() {
        return comments;
    }
}

