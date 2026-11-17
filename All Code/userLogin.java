import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class userLogin {
    private static final DateTimeFormatter REPORT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd-uuuu");
    private static final String ROLE_SELECTION = "roleSelection";
    private static final String OPERATOR_MENU = "operatorMenu";
    private static final String MANAGER_MENU = "managerMenu";
    private static final String MEMBER_MENU = "memberMenu";
    private static final String PROVIDER_DATA_MENU = "providerDataMenu";
    private static final String PROVIDER_MENU = "providerMenu";
    private static final String PROVIDER_BILL_SERVICE = "providerBillService";
    private static final String PROVIDER_BILL_DATE = "providerBillDate";
    private static final String PROVIDER_BILL_CODE = "providerBillCode";
    private static final String PROVIDER_BILL_COMMENTS = "providerBillComments";
    private static final String ADD_MEMBER = "addMember";
    private static final String DELETE_MEMBER = "deleteMember";
    private static final String VIEW_MEMBER = "viewMember";
    private static final String ADD_PROVIDER = "addProvider";
    private static final String DELETE_PROVIDER = "deleteProvider";
    private static final String EDIT_MEMBER_DATA = "editMemberData";
    private static final String EDIT_PROVIDER_DATA = "editProviderData";
    private static final String VIEW_PROVIDER = "viewProvider";
    private static final String UPDATE_MEMBER_STATUS = "updateMemberStatus";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screens = new JPanel(cardLayout);
    private final LoginDatabase loginDatabase = new LoginDatabase();
    private final ProviderDatabase providerDatabase = new ProviderDatabase();
    private final MemberDatabase memberDatabase = new MemberDatabase();
    private final ServiceDatabase serviceDatabase = new ServiceDatabase();
    private final AppointmentDatabase appointmentDatabase = new AppointmentDatabase();
    private final ScheduledExecutorService accountingScheduler =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "weekly-accounting-scheduler");
                thread.setDaemon(true);
                return thread;
            });
    private String signedInProviderId;
    private String billingMemberId;
    private String billingDate;
    private String billingServiceCode;

    public userLogin() {
        screens.add(createRoleSelectionScreen(), ROLE_SELECTION);
        screens.add(createSignInScreen("Operator"), "Operator");
        screens.add(createSignInScreen("Manager"), "Manager");
        screens.add(createSignInScreen("Provider"), "Provider");
        screens.add(createSignInScreen("ACME System"), "ACME System");
        screens.add(createOperatorMenuScreen(), OPERATOR_MENU);
        screens.add(createManagerMenuScreen(), MANAGER_MENU);
        screens.add(createProviderDataMenuScreen(), PROVIDER_DATA_MENU);
        screens.add(createProviderMenuScreen(), PROVIDER_MENU);
        screens.add(createBillForServiceScreen(), PROVIDER_BILL_SERVICE);
        screens.add(createBillDateScreen(), PROVIDER_BILL_DATE);
        screens.add(createBillServiceCodeScreen(), PROVIDER_BILL_CODE);
        screens.add(createBillCommentsScreen(), PROVIDER_BILL_COMMENTS);
        screens.add(createMemberMenuScreen(), MEMBER_MENU);
        screens.add(createAddMemberScreen(), ADD_MEMBER);
        screens.add(createDeleteMemberScreen(), DELETE_MEMBER);
        screens.add(createViewMemberScreen(), VIEW_MEMBER);
        screens.add(createAddProviderScreen(), ADD_PROVIDER);
        screens.add(createDeleteProviderScreen(), DELETE_PROVIDER);
        screens.add(createEditMemberScreen(), EDIT_MEMBER_DATA);
        screens.add(createEditProviderScreen(), EDIT_PROVIDER_DATA);
        screens.add(createViewProviderScreen(), VIEW_PROVIDER);
        screens.add(createUpdateMemberStatusScreen(), UPDATE_MEMBER_STATUS);

        JFrame frame = new JFrame("Sign In");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(screens);
        frame.setSize(520, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        scheduleNextAccountingRun();
    }

    private JPanel createRoleSelectionScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Choose a sign-in page", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(4, 1, 8, 8));
        addRoleButton(buttons, "Operator");
        addRoleButton(buttons, "Manager");
        addRoleButton(buttons, "Provider");
        addRoleButton(buttons, "ACME System");
        panel.add(buttons, BorderLayout.CENTER);
        return panel;
    }

    private void addRoleButton(JPanel panel, String role) {
        JButton button = new JButton(role);
        button.addActionListener(event -> cardLayout.show(screens, role));
        panel.add(button);
    }

    private JPanel createSignInScreen(String role) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel(role + " Sign In", SwingConstants.CENTER), BorderLayout.NORTH);

        boolean providerSignIn = role.equals("Provider");
        JTextField idNumberField = new JTextField();
        JPasswordField passwordField = providerSignIn ? null : new JPasswordField();
        JPanel fields;
        if (providerSignIn) {
            fields = new JPanel(new GridLayout(1, 2, 8, 8));
            fields.add(new JLabel("Provider ID:"));
            fields.add(idNumberField);
        } else {
            fields = new JPanel(new GridLayout(2, 2, 8, 8));
            fields.add(new JLabel("ID Number:"));
            fields.add(idNumberField);
            fields.add(new JLabel("Password:"));
            fields.add(passwordField);
        }
        panel.add(fields, BorderLayout.CENTER);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(event -> cardLayout.show(screens, ROLE_SELECTION));
        JButton submitButton = new JButton("Submit");
        JPasswordField submittedPasswordField = passwordField;
        submitButton.addActionListener(event -> {
            char[] password = submittedPasswordField == null
                ? new char[0]
                : submittedPasswordField.getPassword();
            if (idNumberField.getText().trim().isEmpty()
                    || (!providerSignIn && password.length == 0)) {
                Arrays.fill(password, '\0');
                String requiredFields = providerSignIn
                        ? "Enter your provider ID."
                        : "Enter both an ID number and password.";
            JOptionPane.showMessageDialog(panel, requiredFields,
                        "Missing information", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                boolean validLogin = providerSignIn
                        ? providerDatabase.validateProvider(idNumberField.getText().trim())
                        : loginDatabase.validateLogin(role, idNumberField.getText(), password);
                if (validLogin) {
                    if (role.equals("Provider")) {
                        signedInProviderId = idNumberField.getText().trim();
                        cardLayout.show(screens, PROVIDER_MENU);
                    } else if (role.equals("Manager")) {
                        cardLayout.show(screens, MANAGER_MENU);
                    } else if (role.equals("Operator") || role.equals("ACME System")) {
                        cardLayout.show(screens, OPERATOR_MENU);
                    } else {
                        JOptionPane.showMessageDialog(panel,
                                "Sign in successful for " + role + ".");
                    }
                } else {
                    String failureMessage = providerSignIn
                            ? "Invalid provider ID."
                            : "Invalid ID number or password.";
                    JOptionPane.showMessageDialog(panel, failureMessage,
                            "Sign in failed", JOptionPane.ERROR_MESSAGE);
                }
            } finally {
                Arrays.fill(password, '\0');
                if (submittedPasswordField != null) {
                    submittedPasswordField.setText("");
                }
            }
        });

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 8));
        actions.add(backButton);
        actions.add(submitButton);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createOperatorMenuScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("System Management Menu", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(2, 1, 8, 8));
        addMenuOption(choices, "Edit Member", MEMBER_MENU);
        addMenuOption(choices, "Edit Provider", PROVIDER_DATA_MENU);
        panel.add(choices, BorderLayout.CENTER);

        JButton signOutButton = new JButton("Sign Out");
        signOutButton.addActionListener(event -> cardLayout.show(screens, ROLE_SELECTION));
        panel.add(signOutButton, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createManagerMenuScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Manager Menu", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(2, 2, 8, 8));
        JButton accountingButton = new JButton("Main Accounting Procedure");
        accountingButton.addActionListener(event -> showMainAccountingProcedure(panel));
        choices.add(accountingButton);
        JButton providerReportButton = new JButton("Generate Provider Report");
        providerReportButton.addActionListener(event -> showProviderReport(panel));
        choices.add(providerReportButton);
        JButton memberReportButton = new JButton("Generate Member Report");
        memberReportButton.addActionListener(event -> showMemberReport(panel));
        choices.add(memberReportButton);
        JButton summaryReportButton = new JButton("Generate Summary Report");
        summaryReportButton.addActionListener(event -> showSummaryReport(panel));
        choices.add(summaryReportButton);
        panel.add(choices, BorderLayout.CENTER);

        JButton signOutButton = new JButton("Sign Out");
        signOutButton.addActionListener(event -> cardLayout.show(screens, ROLE_SELECTION));
        panel.add(signOutButton, BorderLayout.SOUTH);
        return panel;
    }

    private void scheduleNextAccountingRun() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextRun = now.with(DayOfWeek.FRIDAY).toLocalDate()
                .atStartOfDay(now.getZone());
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusWeeks(1);
        }

        long delay = Duration.between(now, nextRun).toMillis();
        accountingScheduler.schedule(() -> {
            SwingUtilities.invokeLater(() -> showMainAccountingProcedure(screens));
            scheduleNextAccountingRun();
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void showMainAccountingProcedure(JPanel parent) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        List<Appointment> weeklyAppointments = getAppointmentsForWeek(weekStart, today);
        Set<String> memberIds = new TreeSet<>();
        Set<String> providerIds = new TreeSet<>();
        for (Appointment appointment : weeklyAppointments) {
            memberIds.add(appointment.getMemberId());
            providerIds.add(appointment.getProviderId());
        }

        StringBuilder report = new StringBuilder("MAIN ACCOUNTING PROCEDURE\n")
                .append("Reporting period: ")
                .append(weekStart.format(REPORT_DATE_FORMAT)).append(" through ")
                .append(today.format(REPORT_DATE_FORMAT)).append("\n\n")
                .append("MEMBER REPORTS\n\n");
        if (memberIds.isEmpty()) {
            report.append("No member services recorded this week.\n\n");
        } else {
            for (String memberId : memberIds) {
                appendWeeklyMemberReport(report, memberId, weeklyAppointments);
                report.append('\n');
            }
        }

        report.append("PROVIDER REPORTS\n\n");
        if (providerIds.isEmpty()) {
            report.append("No provider payments due this week.\n\n");
        } else {
            for (String providerId : providerIds) {
                appendWeeklyProviderReport(report, providerId, weeklyAppointments, weekStart, today);
                report.append('\n');
            }
        }

        report.append("SUMMARY REPORT\n\n");
        appendWeeklySummaryReport(report, providerIds, weeklyAppointments);

        JTextArea reportText = new JTextArea(report.toString(), 28, 90);
        reportText.setEditable(false);
        JOptionPane.showMessageDialog(parent, new JScrollPane(reportText),
                "Main Accounting Procedure", JOptionPane.INFORMATION_MESSAGE);
    }

    private List<Appointment> getAppointmentsForWeek(LocalDate weekStart, LocalDate weekEnd) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment appointment : appointmentDatabase.getAllAppointments()) {
            LocalDate serviceDate = LocalDate.parse(appointment.getDateOfService(),
                    REPORT_DATE_FORMAT);
            if (!serviceDate.isBefore(weekStart) && !serviceDate.isAfter(weekEnd)) {
                result.add(appointment);
            }
        }
        result.sort(Comparator.comparing(appointment ->
                LocalDate.parse(appointment.getDateOfService(), REPORT_DATE_FORMAT)));
        return result;
    }

    private void appendWeeklyMemberReport(StringBuilder report, String memberId,
            List<Appointment> weeklyAppointments) {
        Member member = memberDatabase.getMember(memberId);
        report.append("Member Name: ").append(member == null ? "Unknown member" : member.getName())
                .append("\nMember ID Number: ").append(memberId)
                .append("\nMember Street Address: ")
                .append(member == null ? "Unknown" : member.getStreetAddress())
                .append("\nMember City: ").append(member == null ? "Unknown" : member.getCity())
                .append("\nMember State: ").append(member == null ? "Unknown" : member.getState())
                .append("\nMember ZIP Code: ")
                .append(member == null ? "Unknown" : member.getZipCode())
                .append("\nDate of Service | Provider Name | Service Name\n");

        for (Appointment appointment : weeklyAppointments) {
            if (!memberId.equals(appointment.getMemberId())) {
                continue;
            }
            Provider provider = providerDatabase.getProvider(appointment.getProviderId());
            Service service = serviceDatabase.getServiceByCode(appointment.getServiceCode());
            report.append(appointment.getDateOfService()).append(" | ")
                    .append(provider == null ? "Unknown provider" : provider.getName())
                    .append(" | ")
                    .append(service == null ? "Unknown service" : service.getServiceName())
                    .append('\n');
        }
    }

    private void appendWeeklyProviderReport(StringBuilder report, String providerId,
            List<Appointment> weeklyAppointments, LocalDate weekStart, LocalDate weekEnd) {
        Provider provider = providerDatabase.getProvider(providerId);
        int consultations = 0;
        double totalFees = 0;
        report.append("Services from ").append(weekStart.format(REPORT_DATE_FORMAT))
                .append(" through ").append(weekEnd.format(REPORT_DATE_FORMAT))
                .append("\nDate of Service | Member Name | Member ID | Service Code | Fee\n");

        for (Appointment appointment : weeklyAppointments) {
            if (!providerId.equals(appointment.getProviderId())) {
                continue;
            }
            consultations++;
            Member member = memberDatabase.getMember(appointment.getMemberId());
            Service service = serviceDatabase.getServiceByCode(appointment.getServiceCode());
            double fee = service == null ? 0 : service.getFee();
            totalFees += fee;
            report.append(appointment.getDateOfService()).append(" | ")
                    .append(member == null ? "Unknown member" : member.getName())
                    .append(" | ").append(appointment.getMemberId())
                    .append(" | ").append(appointment.getServiceCode()).append(" | ")
                    .append(service == null ? "Unknown fee"
                            : String.format(Locale.ROOT, "$%.2f", fee))
                    .append('\n');
        }

        report.append("Provider Name: ").append(provider == null ? "Unknown provider" : provider.getName())
                .append("\nProvider Number: ").append(providerId)
                .append("\nProvider Street Address: ")
                .append(provider == null ? "Unknown" : provider.getStreetAddress())
                .append("\nProvider City: ").append(provider == null ? "Unknown" : provider.getCity())
                .append("\nProvider State: ").append(provider == null ? "Unknown" : provider.getState())
                .append("\nProvider ZIP Code: ")
                .append(provider == null ? "Unknown" : provider.getZipCode())
                .append("\nTotal Consultations: ").append(consultations)
                .append("\nTotal Fees: ").append(String.format(Locale.ROOT, "$%.2f", totalFees))
                .append('\n');
    }

    private void appendWeeklySummaryReport(StringBuilder report, Set<String> providerIds,
            List<Appointment> weeklyAppointments) {
        report.append("Provider Number | Provider Name | Consultations | Total Fees Owed\n");
        int totalConsultations = 0;
        double totalFees = 0;
        for (String providerId : providerIds) {
            Provider provider = providerDatabase.getProvider(providerId);
            int consultations = 0;
            double providerFees = 0;
            for (Appointment appointment : weeklyAppointments) {
                if (!providerId.equals(appointment.getProviderId())) {
                    continue;
                }
                consultations++;
                Service service = serviceDatabase.getServiceByCode(appointment.getServiceCode());
                if (service != null) {
                    providerFees += service.getFee();
                }
            }
            totalConsultations += consultations;
            totalFees += providerFees;
            report.append(providerId).append(" | ")
                    .append(provider == null ? "Unknown provider" : provider.getName())
                    .append(" | ").append(consultations).append(" | ")
                    .append(String.format(Locale.ROOT, "$%.2f", providerFees)).append('\n');
        }
        if (totalConsultations == 0) {
            report.append("No provider payments due this week.\n");
        }
        report.append("Overall Consultations: ").append(totalConsultations)
                .append("\nOverall Fees Owed: ")
                .append(String.format(Locale.ROOT, "$%.2f", totalFees));
    }

    private void showProviderReport(JPanel parent) {
        String providerId = JOptionPane.showInputDialog(parent, "Enter provider ID:",
                "Generate Provider Report", JOptionPane.QUESTION_MESSAGE);
        if (providerId == null) {
            return;
        }
        providerId = providerId.trim();
        if (providerId.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Enter a provider ID.", "Missing information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Provider provider = providerDatabase.getProvider(providerId);
        if (provider == null) {
            JOptionPane.showMessageDialog(parent, "Provider ID not found.", "Report unavailable",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        List<Appointment> weeklyAppointments = new ArrayList<>();
        for (Appointment appointment : appointmentDatabase.getAllAppointments()) {
            if (!providerId.equals(appointment.getProviderId())) {
                continue;
            }
            LocalDate serviceDate = LocalDate.parse(appointment.getDateOfService(),
                    REPORT_DATE_FORMAT);
            if (!serviceDate.isBefore(weekStart) && !serviceDate.isAfter(today)) {
                weeklyAppointments.add(appointment);
            }
        }
        weeklyAppointments.sort(Comparator.comparing(appointment ->
                LocalDate.parse(appointment.getDateOfService(), REPORT_DATE_FORMAT)));

        double totalFees = 0;
        StringBuilder report = new StringBuilder("Services from ")
                .append(weekStart.format(REPORT_DATE_FORMAT)).append(" through ")
                .append(today.format(REPORT_DATE_FORMAT))
                .append("\n\nDate of Service | Member Name | Member ID | Service Code | Fee\n");
        for (Appointment appointment : weeklyAppointments) {
            Member member = memberDatabase.getMember(appointment.getMemberId());
            Service service = serviceDatabase.getServiceByCode(appointment.getServiceCode());
            String memberName = member == null ? "Unknown member" : member.getName();
            double fee = service == null ? 0 : service.getFee();
            totalFees += fee;
            report.append(appointment.getDateOfService()).append(" | ")
                    .append(memberName).append(" | ").append(appointment.getMemberId())
                    .append(" | ").append(appointment.getServiceCode()).append(" | ")
                    .append(service == null ? "Unknown fee"
                            : String.format(Locale.ROOT, "$%.2f", fee))
                    .append('\n');
        }
        if (weeklyAppointments.isEmpty()) {
            report.append("No services recorded during this period.\n");
        }

        report.append("\nProvider Name: ").append(provider.getName())
                .append("\nProvider Number: ").append(provider.getId())
                .append("\nProvider Street Address: ").append(provider.getStreetAddress())
                .append("\nProvider City: ").append(provider.getCity())
                .append("\nProvider State: ").append(provider.getState())
                .append("\nProvider ZIP Code: ").append(provider.getZipCode())
                .append("\nTotal Consultations: ").append(weeklyAppointments.size())
                .append("\nTotal Fees: ").append(String.format(Locale.ROOT, "$%.2f", totalFees));

        JTextArea reportText = new JTextArea(report.toString(), 20, 72);
        reportText.setEditable(false);
        JOptionPane.showMessageDialog(parent, new JScrollPane(reportText),
                "Provider Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSummaryReport(JPanel parent) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        List<Appointment> weeklyAppointments = new ArrayList<>();
        for (Appointment appointment : appointmentDatabase.getAllAppointments()) {
            LocalDate serviceDate = LocalDate.parse(appointment.getDateOfService(),
                    REPORT_DATE_FORMAT);
            if (!serviceDate.isBefore(weekStart) && !serviceDate.isAfter(today)) {
                weeklyAppointments.add(appointment);
            }
        }

        List<Provider> providers = new ArrayList<>(providerDatabase.getAllProviders());
        providers.sort(Comparator.comparing(Provider::getName)
                .thenComparing(Provider::getId));

        int totalConsultations = 0;
        double totalFees = 0;
        StringBuilder report = new StringBuilder(
                "Provider Number | Provider Name | Consultations | Total Fees Owed\n");
        for (Provider provider : providers) {
            int consultations = 0;
            double providerFees = 0;
            for (Appointment appointment : weeklyAppointments) {
                if (!provider.getId().equals(appointment.getProviderId())) {
                    continue;
                }
                consultations++;
                Service service = serviceDatabase.getServiceByCode(appointment.getServiceCode());
                if (service != null) {
                    providerFees += service.getFee();
                }
            }
            if (consultations == 0) {
                continue;
            }

            totalConsultations += consultations;
            totalFees += providerFees;
            report.append(provider.getId()).append(" | ").append(provider.getName())
                    .append(" | ").append(consultations).append(" | ")
                    .append(String.format(Locale.ROOT, "$%.2f", providerFees)).append('\n');
        }

        if (totalConsultations == 0) {
            report.append("No provider payments due for this period.\n");
        }
        report.append("\nOverall Consultations: ").append(totalConsultations)
                .append("\nOverall Fees Owed: ")
                .append(String.format(Locale.ROOT, "$%.2f", totalFees));

        JTextArea reportText = new JTextArea(report.toString(), 18, 72);
        reportText.setEditable(false);
        JOptionPane.showMessageDialog(parent, new JScrollPane(reportText),
                "Summary Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showMemberReport(JPanel parent) {
        String memberId = JOptionPane.showInputDialog(parent, "Enter member ID:",
                "Generate Member Report", JOptionPane.QUESTION_MESSAGE);
        if (memberId == null) {
            return;
        }
        memberId = memberId.trim();
        if (memberId.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Enter a member ID.", "Missing information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Member member = memberDatabase.getMember(memberId);
        if (member == null) {
            JOptionPane.showMessageDialog(parent, "Member ID not found.", "Report unavailable",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Appointment> memberAppointments = new ArrayList<>();
        for (Appointment appointment : appointmentDatabase.getAllAppointments()) {
            if (memberId.equals(appointment.getMemberId())) {
                memberAppointments.add(appointment);
            }
        }
        memberAppointments.sort(Comparator.comparing(appointment ->
                LocalDate.parse(appointment.getDateOfService(), REPORT_DATE_FORMAT)));

        StringBuilder report = new StringBuilder();
        report.append("Member Name: ").append(member.getName())
                .append("\nMember ID Number: ").append(member.getId())
                .append("\nMember Street Address: ").append(member.getStreetAddress())
                .append("\nMember City: ").append(member.getCity())
                .append("\nMember State: ").append(member.getState())
                .append("\nMember ZIP Code: ").append(member.getZipCode())
                .append("\n\nDate of Service | Provider Name | Service Name | Service Fee\n");

        if (memberAppointments.isEmpty()) {
            report.append("No services recorded for this member.");
        } else {
            for (Appointment appointment : memberAppointments) {
                Provider provider = providerDatabase.getProvider(appointment.getProviderId());
                Service service = serviceDatabase.getServiceByCode(appointment.getServiceCode());
                String providerName = provider == null ? "Unknown provider" : provider.getName();
                String serviceName = service == null ? "Unknown service"
                        : service.getServiceName();
                String serviceFee = service == null ? "Unknown fee"
                    : String.format(Locale.ROOT, "$%.2f", service.getFee());
                report.append(appointment.getDateOfService()).append(" | ")
                    .append(providerName).append(" | ").append(serviceName).append(" | ")
                    .append(serviceFee).append('\n');
            }
        }

        JTextArea reportText = new JTextArea(report.toString(), 18, 60);
        reportText.setEditable(false);
        JOptionPane.showMessageDialog(parent, new JScrollPane(reportText),
                "Member Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createMemberMenuScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Member Data", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(5, 1, 8, 8));
        addMenuOption(choices, "Add Member", ADD_MEMBER);
        addMenuOption(choices, "Delete Member", DELETE_MEMBER);
        addMenuOption(choices, "Edit Member", EDIT_MEMBER_DATA);
        addMenuOption(choices, "View Member", VIEW_MEMBER);
        addMenuOption(choices, "Update Member Status", UPDATE_MEMBER_STATUS);
        panel.add(choices, BorderLayout.CENTER);
        panel.add(createBackAction(OPERATOR_MENU), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createProviderMenuScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Provider Menu", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(2, 1, 8, 8));
        addMenuOption(choices, "Bill for Service", PROVIDER_BILL_SERVICE);
        JButton directoryButton = new JButton("Request Provider Directory");
        directoryButton.addActionListener(event -> showServiceCatalog(panel));
        choices.add(directoryButton);
        panel.add(choices, BorderLayout.CENTER);

        JButton signOutButton = new JButton("Sign Out");
        signOutButton.addActionListener(event -> cardLayout.show(screens, ROLE_SELECTION));
        panel.add(signOutButton, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createProviderDataMenuScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Provider Data", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(4, 1, 8, 8));
        addMenuOption(choices, "Add Provider", ADD_PROVIDER);
        addMenuOption(choices, "Delete Provider", DELETE_PROVIDER);
        addMenuOption(choices, "Edit Provider", EDIT_PROVIDER_DATA);
        addMenuOption(choices, "View Provider", VIEW_PROVIDER);
        panel.add(choices, BorderLayout.CENTER);
        panel.add(createBackAction(OPERATOR_MENU), BorderLayout.SOUTH);
        return panel;
    }

    private void addPlaceholderMenuButton(JPanel panel, String actionName) {
        JButton button = new JButton(actionName);
        button.addActionListener(event -> JOptionPane.showMessageDialog(panel,
                actionName + " is not implemented yet.", "Unavailable",
                JOptionPane.INFORMATION_MESSAGE));
        panel.add(button);
    }

    private JPanel createBillForServiceScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Bill for Service", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField memberIdField = new JTextField();
        JPanel fields = new JPanel(new GridLayout(1, 2, 8, 8));
        addFormField(fields, "Member ID:", memberIdField);
        panel.add(fields, BorderLayout.CENTER);

        panel.add(createFormActions("Continue", PROVIDER_MENU, () -> {
            String memberId = memberIdField.getText().trim();
            String memberStatus = memberDatabase.validateMember(memberId);
            if (!"Active".equals(memberStatus)) {
                String message = "Invalid member ID".equals(memberStatus)
                        ? "Member ID not found."
                        : "This member is suspended and cannot receive service.";
                JOptionPane.showMessageDialog(panel, message, "Member validation failed",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            billingMemberId = memberId;
            cardLayout.show(screens, PROVIDER_BILL_DATE);
        }), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createBillDateScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Enter Date of Service", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField dateField = new JTextField();
        JPanel fields = new JPanel(new GridLayout(1, 2, 8, 8));
        addFormField(fields, "Date (MM-DD-YYYY):", dateField);
        panel.add(fields, BorderLayout.CENTER);

        panel.add(createFormActions("Continue", PROVIDER_BILL_SERVICE, () -> {
            String date = dateField.getText().trim();
            if (!AppointmentDatabase.isValidDate(date)) {
                JOptionPane.showMessageDialog(panel,
                        "Enter a valid date in MM-DD-YYYY format.", "Invalid date",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            billingDate = date;
            cardLayout.show(screens, PROVIDER_BILL_CODE);
        }), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createBillServiceCodeScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Enter Service Code", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField serviceCodeField = new JTextField();
        JPanel fields = new JPanel(new GridLayout(1, 2, 8, 8));
        addFormField(fields, "Service Code:", serviceCodeField);
        panel.add(fields, BorderLayout.CENTER);

        panel.add(createFormActions("Look Up Service", PROVIDER_BILL_DATE, () -> {
            String serviceCode = serviceCodeField.getText().trim();
            Service service = serviceDatabase.getServiceByCode(serviceCode);
            if (service == null) {
                JOptionPane.showMessageDialog(panel, "Service code not found. Enter another code.",
                        "Unknown service", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirmation = JOptionPane.showConfirmDialog(panel,
                    "Service: " + service.getServiceName() + "\nIs this the correct service?",
                    "Confirm Service", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirmation != JOptionPane.YES_OPTION) {
                return;
            }
            billingServiceCode = service.getServiceCode();
            cardLayout.show(screens, PROVIDER_BILL_COMMENTS);
        }), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createBillCommentsScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Service Comments", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextArea commentsField = new JTextArea(8, 32);
        commentsField.setLineWrap(true);
        commentsField.setWrapStyleWord(true);
        panel.add(new JScrollPane(commentsField), BorderLayout.CENTER);
        panel.add(createFormActions("Save Service Record", PROVIDER_BILL_CODE, () -> {
            Member member = memberDatabase.getMember(billingMemberId);
            if (member == null || !"Active".equals(member.getStatus())) {
                JOptionPane.showMessageDialog(panel,
                        "Member is no longer active. The service was not saved.",
                        "Member validation failed", JOptionPane.WARNING_MESSAGE);
                cardLayout.show(screens, PROVIDER_BILL_SERVICE);
                return;
            }
            if (signedInProviderId == null
                    || !providerDatabase.validateProvider(signedInProviderId)) {
                JOptionPane.showMessageDialog(panel,
                        "Provider sign-in is no longer valid. The service was not saved.",
                        "Provider validation failed", JOptionPane.ERROR_MESSAGE);
                cardLayout.show(screens, ROLE_SELECTION);
                return;
            }

            try {
                appointmentDatabase.addAppointment(signedInProviderId, billingMemberId,
                        billingDate, billingServiceCode, commentsField.getText());
                JOptionPane.showMessageDialog(panel, "Service record saved.");
                commentsField.setText("");
                cardLayout.show(screens, PROVIDER_MENU);
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(panel, exception.getMessage(), "Invalid data",
                        JOptionPane.WARNING_MESSAGE);
            }
        }), BorderLayout.SOUTH);
        return panel;
    }

    private void showServiceCatalog(JPanel parent) {
        List<Service> services = serviceDatabase.getAllServices();
        StringBuilder catalog = new StringBuilder(String.format(Locale.ROOT,
                "%-12s %-22s %8s%n%n", "Code", "Service Name", "Fee"));
        for (Service service : services) {
            catalog.append(String.format(Locale.ROOT, "%-12s %-22s $%7.2f%n",
                    service.getServiceCode(), service.getServiceName(), service.getFee()));
        }
        if (services.isEmpty()) {
            catalog.append("No services are currently available.");
        }
        JTextArea catalogText = new JTextArea(catalog.toString(), 12, 48);
        catalogText.setEditable(false);
        JOptionPane.showMessageDialog(parent, new JScrollPane(catalogText),
                "Service Catalog", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addMenuOption(JPanel panel, String label, String screen) {
        JButton button = new JButton(label);
        button.addActionListener(event -> cardLayout.show(screens, screen));
        panel.add(button);
    }

    private JPanel createEditMemberScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Edit Member Data", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField streetField = new JTextField();
        JTextField cityField = new JTextField();
        JTextField stateField = new JTextField();
        JTextField zipField = new JTextField();
        JPanel fields = new JPanel(new GridLayout(6, 2, 8, 8));
        addFormField(fields, "Member ID:", idField);
        addFormField(fields, "Name:", nameField);
        addFormField(fields, "Street Address:", streetField);
        addFormField(fields, "City:", cityField);
        addFormField(fields, "State:", stateField);
        addFormField(fields, "ZIP Code:", zipField);
        panel.add(fields, BorderLayout.CENTER);

        panel.add(createFormActions("Save Changes", MEMBER_MENU, () -> {
            try {
                boolean updated = memberDatabase.updateMember(idField.getText().trim(),
                        nameField.getText(), streetField.getText(), cityField.getText(),
                        stateField.getText(), zipField.getText());
                String message = updated ? "Member data updated." : "Member ID not found.";
                JOptionPane.showMessageDialog(panel, message);
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(panel, exception.getMessage(), "Invalid data",
                        JOptionPane.WARNING_MESSAGE);
            }
        }), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAddMemberScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Add Member", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField nameField = new JTextField();
        JTextField streetField = new JTextField();
        JTextField cityField = new JTextField();
        JTextField stateField = new JTextField();
        JTextField zipField = new JTextField();
        JPanel fields = new JPanel(new GridLayout(5, 2, 8, 8));
        addFormField(fields, "Name:", nameField);
        addFormField(fields, "Street Address:", streetField);
        addFormField(fields, "City:", cityField);
        addFormField(fields, "State:", stateField);
        addFormField(fields, "ZIP Code:", zipField);
        panel.add(fields, BorderLayout.CENTER);

        panel.add(createFormActions("Add Member", MEMBER_MENU, () -> {
            try {
                String memberId = memberDatabase.addMember(nameField.getText(),
                        streetField.getText(), cityField.getText(), stateField.getText(),
                        zipField.getText());
                JOptionPane.showMessageDialog(panel, "Member added. Member ID: " + memberId);
                nameField.setText("");
                streetField.setText("");
                cityField.setText("");
                stateField.setText("");
                zipField.setText("");
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(panel, exception.getMessage(), "Invalid data",
                        JOptionPane.WARNING_MESSAGE);
            }
        }), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createDeleteMemberScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Delete Member", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField idField = new JTextField();
        JPanel fields = new JPanel(new GridLayout(1, 2, 8, 8));
        addFormField(fields, "Member ID:", idField);
        panel.add(fields, BorderLayout.CENTER);

        panel.add(createFormActions("Delete Member", MEMBER_MENU, () -> {
            String memberId = idField.getText().trim();
            if (memberId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter a member ID.", "Missing information",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(panel,
                    "Delete member " + memberId + "? This cannot be undone.",
                    "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                boolean deleted = memberDatabase.deleteMember(memberId);
                JOptionPane.showMessageDialog(panel,
                        deleted ? "Member deleted." : "Member ID not found.");
                if (deleted) {
                    idField.setText("");
                }
            }
        }), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createViewMemberScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("View Member", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField idField = new JTextField();
        JButton searchButton = new JButton("Search");
        JTextArea details = new JTextArea();
        details.setEditable(false);
        JPanel searchRow = new JPanel(new GridLayout(1, 3, 8, 8));
        addFormField(searchRow, "Member ID:", idField);
        searchRow.add(searchButton);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(searchRow, BorderLayout.NORTH);
        content.add(new JScrollPane(details), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        searchButton.addActionListener(event -> {
            Member member = memberDatabase.getMember(idField.getText().trim());
            details.setText(member == null ? "Member ID not found."
                    : "ID: " + member.getId() + "\nName: " + member.getName()
                            + "\nStreet Address: " + member.getStreetAddress()
                            + "\nCity: " + member.getCity() + "\nState: " + member.getState()
                            + "\nZIP Code: " + member.getZipCode()
                            + "\nStatus: " + member.getStatus());
        });
        panel.add(createBackAction(MEMBER_MENU), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAddProviderScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Add Provider", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField nameField = new JTextField();
        JTextField streetField = new JTextField();
        JTextField cityField = new JTextField();
        JTextField stateField = new JTextField();
        JTextField zipField = new JTextField();
        JPanel fields = new JPanel(new GridLayout(5, 2, 8, 8));
        addFormField(fields, "Name:", nameField);
        addFormField(fields, "Street Address:", streetField);
        addFormField(fields, "City:", cityField);
        addFormField(fields, "State:", stateField);
        addFormField(fields, "ZIP Code:", zipField);
        panel.add(fields, BorderLayout.CENTER);

        panel.add(createFormActions("Add Provider", PROVIDER_DATA_MENU, () -> {
            try {
                String providerId = providerDatabase.addProvider(nameField.getText(),
                        streetField.getText(), cityField.getText(), stateField.getText(),
                        zipField.getText());
                JOptionPane.showMessageDialog(panel, "Provider added. Provider ID: " + providerId);
                nameField.setText("");
                streetField.setText("");
                cityField.setText("");
                stateField.setText("");
                zipField.setText("");
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(panel, exception.getMessage(), "Invalid data",
                        JOptionPane.WARNING_MESSAGE);
            }
        }), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createDeleteProviderScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Delete Provider", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField idField = new JTextField();
        JPanel fields = new JPanel(new GridLayout(1, 2, 8, 8));
        addFormField(fields, "Provider ID:", idField);
        panel.add(fields, BorderLayout.CENTER);

        panel.add(createFormActions("Delete Provider", PROVIDER_DATA_MENU, () -> {
            String providerId = idField.getText().trim();
            if (providerId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter a provider ID.", "Missing information",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(panel,
                    "Delete provider " + providerId + "? This cannot be undone.",
                    "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                boolean deleted = providerDatabase.deleteProvider(providerId);
                JOptionPane.showMessageDialog(panel,
                        deleted ? "Provider deleted." : "Provider ID not found.");
                if (deleted) {
                    idField.setText("");
                }
            }
        }), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createViewProviderScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("View Provider", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField idField = new JTextField();
        JButton searchButton = new JButton("Search");
        JTextArea details = new JTextArea();
        details.setEditable(false);
        JPanel searchRow = new JPanel(new GridLayout(1, 3, 8, 8));
        addFormField(searchRow, "Provider ID:", idField);
        searchRow.add(searchButton);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(searchRow, BorderLayout.NORTH);
        content.add(new JScrollPane(details), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        searchButton.addActionListener(event -> {
            Provider provider = providerDatabase.getProvider(idField.getText().trim());
            details.setText(provider == null ? "Provider ID not found."
                    : "ID: " + provider.getId() + "\nName: " + provider.getName()
                            + "\nStreet Address: " + provider.getStreetAddress()
                            + "\nCity: " + provider.getCity() + "\nState: " + provider.getState()
                            + "\nZIP Code: " + provider.getZipCode());
        });
        panel.add(createBackAction(PROVIDER_DATA_MENU), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createEditProviderScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Edit Provider Data", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField streetField = new JTextField();
        JTextField cityField = new JTextField();
        JTextField stateField = new JTextField();
        JTextField zipField = new JTextField();
        JPanel fields = new JPanel(new GridLayout(6, 2, 8, 8));
        addFormField(fields, "Provider ID:", idField);
        addFormField(fields, "Name:", nameField);
        addFormField(fields, "Street Address:", streetField);
        addFormField(fields, "City:", cityField);
        addFormField(fields, "State:", stateField);
        addFormField(fields, "ZIP Code:", zipField);
        panel.add(fields, BorderLayout.CENTER);

        panel.add(createFormActions("Save Changes", PROVIDER_DATA_MENU, () -> {
            try {
                boolean updated = providerDatabase.updateProvider(idField.getText().trim(),
                        nameField.getText(), streetField.getText(), cityField.getText(),
                        stateField.getText(), zipField.getText());
                String message = updated ? "Provider data updated." : "Provider ID not found.";
                JOptionPane.showMessageDialog(panel, message);
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(panel, exception.getMessage(), "Invalid data",
                        JOptionPane.WARNING_MESSAGE);
            }
        }), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createUpdateMemberStatusScreen() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("Update Member Status", SwingConstants.CENTER), BorderLayout.NORTH);

        JTextField idField = new JTextField();
        JComboBox<String> statusField = new JComboBox<>(new String[] { "Active", "Suspended" });
        JPanel fields = new JPanel(new GridLayout(2, 2, 8, 8));
        addFormField(fields, "Member ID:", idField);
        addFormField(fields, "Status:", statusField);
        panel.add(fields, BorderLayout.CENTER);

        panel.add(createFormActions("Update Status", MEMBER_MENU, () -> {
            boolean updated = memberDatabase.updateMemberStatus(idField.getText().trim(),
                    (String) statusField.getSelectedItem());
            String message = updated ? "Member status updated." : "Member ID not found.";
            JOptionPane.showMessageDialog(panel, message);
        }), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createFormActions(String submitLabel, String backScreen, Runnable submitAction) {
        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 8));
        JButton backButton = new JButton("Back");
        backButton.addActionListener(event -> cardLayout.show(screens, backScreen));
        JButton submitButton = new JButton(submitLabel);
        submitButton.addActionListener(event -> submitAction.run());
        actions.add(backButton);
        actions.add(submitButton);
        return actions;
    }

    private JPanel createBackAction(String backScreen) {
        JPanel actions = new JPanel(new GridLayout(1, 1));
        JButton backButton = new JButton("Back");
        backButton.addActionListener(event -> cardLayout.show(screens, backScreen));
        actions.add(backButton);
        return actions;
    }

    private void addFormField(JPanel panel, String label, java.awt.Component field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(userLogin::new);
    }
}
