package ticketing.model;

public class Passenger implements Identifiable {
    private int id;
    private String name;
    private String email;
    private String phone;
    private String passportNo;

    public Passenger(String name, String email, String phone, String passportNo) {
        this(0, name, email, phone, passportNo);
    }

    public Passenger(int id, String name, String email, String phone, String passportNo) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passportNo = passportNo;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassportNo() {
        return passportNo;
    }

    public void setPassportNo(String passportNo) {
        this.passportNo = passportNo;
    }

    @Override
    public String toString() {
        return String.format("#%-3d %-22s %-26s %-11s %s", id, name, email, phone, passportNo);
    }
}
