package library;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a library book with borrowing and reservation state.
 */
public class Book {
    private String isbn;
    private String title;
    private String author;
    private int year;
    private boolean available;
    private String borrowedBy;
    private LocalDate dueDate;
    private final List<String> reservationQueue;

    public Book(String isbn, String title, String author, int year) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.year = year;
        this.available = true;
        this.borrowedBy = null;
        this.dueDate = null;
        this.reservationQueue = new ArrayList<>();
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getBorrowedBy() {
        return borrowedBy;
    }

    public void setBorrowedBy(String borrowedBy) {
        this.borrowedBy = borrowedBy;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public List<String> getReservationQueue() {
        return reservationQueue;
    }

    public boolean isOverdue() {
        if (dueDate == null || available) {
            return false;
        }
        return LocalDate.now().isAfter(dueDate);
    }

    /**
     * Fine is $1.00 per overdue day.
     */
    public double calculateFine() {
        if (!isOverdue()) {
            return 0.0;
        }
        long daysLate = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        return daysLate * 1.0;
    }

    public boolean addReservation(String memberId) {
        if (reservationQueue.contains(memberId)) {
            return false;
        }
        if (memberId != null && memberId.equals(borrowedBy)) {
            return false;
        }
        reservationQueue.add(memberId);
        return true;
    }

    public boolean cancelReservation(String memberId) {
        return reservationQueue.remove(memberId);
    }

    public String peekNextReservation() {
        return reservationQueue.isEmpty() ? null : reservationQueue.get(0);
    }

    public String pollNextReservation() {
        return reservationQueue.isEmpty() ? null : reservationQueue.remove(0);
    }

    @Override
    public String toString() {
        String status;
        if (available) {
            status = "Available";
            if (!reservationQueue.isEmpty()) {
                status += " | Reservations: " + reservationQueue.size();
            }
        } else {
            status = "Borrowed by: " + borrowedBy;
            if (dueDate != null) {
                status += " | Due: " + dueDate;
            }
            if (isOverdue()) {
                status += String.format(" | OVERDUE (fine: $%.2f)", calculateFine());
            }
            if (!reservationQueue.isEmpty()) {
                status += " | Reservations: " + reservationQueue.size();
            }
        }
        return String.format("ISBN: %s | Title: %s | Author: %s | Year: %d | %s",
                isbn, title, author, year, status);
    }
}
