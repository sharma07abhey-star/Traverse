package ticketing.model;

/** Anything stored in a repository has an integer id (used as a bounded generic type). */
public interface Identifiable {
    int getId();

    void setId(int id);
}
