/**
 * Represents an appointment containing immutable details regarding 
 * the provider, member, service date, service code, and associated notes.
 */
public class Appointment {
    private final String currentDateTime;
    private final String providerId;
    private final String memberId;
    private final String dateOfService;
    private final String serviceCode;
    private final String comments;


    /**
     * Constructs a new Appointment with default empty comments and date/time.
     * 
     * @param providerId    the unique identifier of the provider
     * @param memberId      the unique identifier of the member/patient
     * @param dateOfService the date when the service is performed
     * @param serviceCode   the code representing the specific service
     */
    public Appointment(String providerId, String memberId, String dateOfService,
            String serviceCode) {
        this(providerId, memberId, dateOfService, serviceCode, "", "");
    }


    /**
     * Constructs a new Appointment with comments and an empty default date/time.
     * 
     * @param providerId    the unique identifier of the provider
     * @param memberId      the unique identifier of the member/patient
     * @param dateOfService the date when the service is performed
     * @param serviceCode   the code representing the specific service
     * @param comments      additional notes or comments regarding the appointment
     */
    public Appointment(String providerId, String memberId, String dateOfService,
            String serviceCode, String comments) {
        this(providerId, memberId, dateOfService, serviceCode, comments, "");
    }


    /**
     * Constructs a fully specified Appointment record.
     * 
     * @param providerId      the unique identifier of the provider
     * @param memberId        the unique identifier of the member/patient
     * @param dateOfService   the date when the service is performed
     * @param serviceCode     the code representing the specific service
     * @param comments        additional notes or comments regarding the appointment
     * @param currentDateTime the timestamp when the appointment record was created or processed
     */
    public Appointment(String providerId, String memberId, String dateOfService,
            String serviceCode, String comments, String currentDateTime) {
        this.currentDateTime = currentDateTime;
        this.providerId = providerId;
        this.memberId = memberId;
        this.dateOfService = dateOfService;
        this.serviceCode = serviceCode;
        this.comments = comments;
    }


    /**
     * Returns the timestamp when the appointment record was created.
     * 
     * @return the current date and time string
     */
    public String getCurrentDateTime() {
        return currentDateTime;
    }


    /**
     * Returns the unique identifier of the provider.
     * 
     * @return the provider ID
     */
    public String getProviderId() {
        return providerId;
    }


    /**
     * Returns the unique identifier of the member.
     * 
     * @return the member ID
     */
    public String getMemberId() {
        return memberId;
    }


    /**
     * Returns the date when the service was performed.
     * 
     * @return the date of service
     */
    public String getDateOfService() {
        return dateOfService;
    }


    /**
     * Returns the service code associated with the appointment.
     * 
     * @return the service code
     */
    public String getServiceCode() {
        return serviceCode;
    }


    /**
     * Returns any additional comments or notes for the appointment.
     * 
     * @return the comments string
     */
    public String getComments() {
        return comments;
    }
}



