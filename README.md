# Flight Booking & Baggage System

**Made by:** Abhey Sharma, Moksh Sharma, Janit, Harshit

CE-3 project (covers topics up to Lecture 54).

## 1. What the project does

A passenger books a seat on a trip **and** registers baggage in a single workflow.

| Feature | How it is done |
|---|---|
| Passenger / Trip / Booking management | Add, search, update, delete (CRUD) with validation |
| No double-booking | `synchronized` per-trip lock + synchronized repository + UNIQUE DB index |
| Baggage fees | Free allowance per class, extra kilos charged at a class-specific rate; tag numbers assigned |
| Check-in -> loading | Producer-consumer queue (`BoundedBuffer`, `wait()` / `notifyAll()`) |
| Booking + baggage consistency | JDBC transaction: `commit()` or `rollback()` |
| Reports | Lambdas + Streams (revenue, occupancy, baggage fees, top passengers) |
| Files | CSV import (validated), ticket export (.txt), bookings export (.csv), backup copy (byte streams), `db.properties` (Properties) |

### Baggage pricing (see `TravelClass.java`)

| Class | Fare x | Free kg per bag | Max bags | Rs per extra kg |
|---|---|---|---|---|
| ECONOMY | 1.0 | 20 | 2 | 400 |
| BUSINESS | 1.8 | 30 | 3 | 300 |
| FIRST | 2.5 | 40 | 4 | 200 |

Example: Economy bag of 27.5 kg -> (27.5 - 20) x 400 = Rs 3000.

## 2. How to run

Requires **JDK 11 or newer**.

```
Windows : compile.bat   then   run.bat            (run.bat --demo for the scripted demo)
Linux/Mac: ./compile.sh   then   ./run.sh         (./run.sh --demo)
```

Options: `--demo` (runs every feature automatically, great for the viva), `--memory` (force collection storage).

### Storage modes (MySQL)

1. Install and start **MySQL Server 8.x** (or 5.7+). You need a user that can create a database (e.g. `root`).
2. Download the **MySQL Connector/J** jar (https://dev.mysql.com/downloads/connector/j/ -> "Platform Independent",
   unzip, copy `mysql-connector-j-x.y.z.jar`) and put it in the `lib/` folder.
3. Open `db.properties` and set your MySQL `user` and `password` (and the `url` if MySQL is not on localhost:3306).
4. Run the program. The database `ticketing_db` and all tables are created automatically on first run.

The run scripts add every jar in `lib/` to the classpath automatically. The first line printed shows the mode:
`JDBC / MySQL (...)`.

**In-memory fallback:** if the connector jar is missing or MySQL is not running, the program prints an info line
and works with Java collections instead (`ConcurrentHashMap`). Everything works, but data is lost on exit.

> Note: MySQL/JDBC was tested on MySQL 8 (Homebrew). Make sure MySQL is running and the password in `db.properties` is correct before starting.

## 3. Package / class structure

```
ticketing
 |- Main                      entry point (menu / --demo / --memory)
 |- model        Passenger, Trip, Booking, Baggage, TravelClass, BookingStatus, BaggageStatus, Identifiable
 |- repository   Repository<T extends Identifiable> (generic), BookingRepository,
 |               InMemoryRepository<T>, InMemoryBookingRepository
 |- jdbc         DbConnection, SchemaInitializer, JdbcPassengerRepository, JdbcTripRepository,
 |               JdbcBookingRepository (transactions)
 |- service      PassengerService, TripService, BookingService (the core), BaggageFeeCalculator,
 |               ReportService (Streams), ConcurrencySimulation
 |- concurrent   BoundedBuffer<T> (wait/notifyAll), BaggageProcessor (consumer thread), StartGate
 |- io           CsvImporter, TicketExporter, FileBackup, ImportResult
 |- ui           ConsoleApp, InputReader, AppContext, DemoRunner
 |- exception    TicketingException + Validation / NotFound / SeatUnavailable / Persistence
 |- util         Validator, TagGenerator
```

Layering: `ui -> service -> repository (interface) -> in-memory OR jdbc`.
The service layer does not know which storage is used.

## 4. Database design

```
passenger(id PK, name, email, phone, passport_no UNIQUE)
trip(id PK, flight_no, origin, destination, departure, total_seats, base_fare)
booking(id PK, passenger_id FK, trip_id FK, seat_no, travel_class, fare, baggage_fee, status, booked_at,
        active_key = generated column: 1 if CONFIRMED else NULL)
         UNIQUE KEY (trip_id, seat_no, active_key)  -> only one CONFIRMED booking per seat
baggage(id PK, booking_id FK, tag_no, weight_kg, fee, status)
```

Cancelling a booking sets `status = CANCELLED` (history is kept for revenue reports) and frees the seat,
because NULLs are distinct in a MySQL unique key, so cancelled rows never block a seat.
Storage engine is InnoDB (needed for transactions and foreign keys).

## 5. Syllabus mapping (where each topic is used)

| Syllabus topic | Where |
|---|---|
| Threads, synchronization, monitors | `BookingService.bookSeat` (`synchronized (lockFor(tripId))`), `InMemoryBookingRepository`, `ConcurrencySimulation` |
| `wait()`, `notify()`, `notifyAll()`, guarded blocks | `BoundedBuffer`, `StartGate`, `BaggageProcessor.awaitIdle` |
| Producer-consumer | `BookingService` (producer) -> `BoundedBuffer` -> `BaggageProcessor` loader thread (consumer) |
| JDBC architecture, URL, DriverManager, Connection | `DbConnection` (reads `db.properties`) |
| Statement / PreparedStatement | `SchemaInitializer` (Statement), all `Jdbc*Repository` (PreparedStatement, parameter binding) |
| executeQuery / executeUpdate, resource management | try-with-resources in every JDBC method |
| CRUD, ResultSet, typed getters, object mapping | `Jdbc*Repository.map...()` |
| Transactions (commit / rollback) | `JdbcBookingRepository.saveWithBaggage`, `cancelBooking` |
| Lambdas, functional interfaces, method references | `ReportService`, services (`Booking::getTotal`, `p -> ...`), `Thread(this::consumeLoop)` |
| Stream pipelines: filter, map, sorted, distinct, limit, skip | `ReportService`, `PassengerService.search`, `BookingService.availableSeats` |
| Generics (classes, methods, bounded types) | `Repository<T extends Identifiable>`, `InMemoryRepository<T>`, `BoundedBuffer<T>` |
| Byte streams | `FileBackup` (FileInputStream / FileOutputStream) |
| Character streams | `CsvImporter` (FileReader), `TicketExporter` (FileWriter / PrintWriter) |
| StreamTokenizer | `InputReader.parseWeights` |
| CSV + collections, validation, exception-safe I/O | `CsvImporter`, `Validator`, custom exceptions, try-with-resources |

## 6. Sample data

`data/passengers.csv` and `data/trips.csv` contain good rows and deliberately bad rows (bad e-mail, short phone,
duplicate passport, wrong column count, same origin/destination, past date, impossible date) to show validation.

## 7. Left for CA-4 (the remaining ~40 %)

* Unit tests, extra input-validation edge cases, code review and refactoring (duplication in the JDBC repositories)
* Seat classes by cabin zone (currently class is chosen per booking), payments, user login
* Documentation: architecture / data-flow diagrams, test evidence, viva walkthrough
