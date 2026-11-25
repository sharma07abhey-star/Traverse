package ticketing.service;

import java.util.List;
import java.util.stream.Collectors;

import ticketing.exception.NotFoundException;
import ticketing.exception.PersistenceException;
import ticketing.exception.ValidationException;
import ticketing.model.Passenger;
import ticketing.repository.BookingRepository;
import ticketing.repository.Repository;
import ticketing.util.Validator;

public class PassengerService {
    private final Repository<Passenger> repo;
    private final BookingRepository bookings;

    public PassengerService(Repository<Passenger> repo, BookingRepository bookings) {
        this.repo = repo;
        this.bookings = bookings;
    }

    /** synchronized so two threads cannot both pass the duplicate-passport check. */
    public synchronized Passenger add(String name, String email, String phone, String passportNo) {
        String n = Validator.requireNonBlank(name, "Name");
        String e = Validator.email(email);
        String ph = Validator.phone(phone);
        String pp = Validator.passport(passportNo);
        boolean duplicate = repo.findAll().stream().anyMatch(p -> p.getPassportNo().equalsIgnoreCase(pp));
        if (duplicate) {
            throw new ValidationException("A passenger with passport " + pp + " already exists");
        }
        return repo.save(new Passenger(n, e, ph, pp));
    }

    public Passenger get(int id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("No passenger with id " + id));
    }

    public List<Passenger> list() {
        return repo.findAll();
    }

    /** Case-insensitive search on name, email or passport number. */
    public List<Passenger> search(String keyword) {
        String k = keyword.trim().toLowerCase();
        return repo.findAll().stream()
                .filter(p -> p.getName().toLowerCase().contains(k)
                        || p.getEmail().toLowerCase().contains(k)
                        || p.getPassportNo().toLowerCase().contains(k))
                .collect(Collectors.toList());
    }

    public synchronized Passenger update(int id, String name, String email, String phone) {
        Passenger p = get(id);
        p.setName(Validator.requireNonBlank(name, "Name"));
        p.setEmail(Validator.email(email));
        p.setPhone(Validator.phone(phone));
        if (!repo.update(p)) {
            throw new PersistenceException("Update failed for passenger " + id, null);
        }
        return p;
    }

    public synchronized void delete(int id) {
        get(id);
        boolean hasBookings = bookings.findAll().stream().anyMatch(b -> b.getPassengerId() == id);
        if (hasBookings) {
            throw new ValidationException("Passenger " + id + " has bookings and cannot be deleted");
        }
        repo.delete(id);
    }
}
