package library;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Core library service: books, members, borrow/return, fines, reservations, stats.
 */
public class Library {
    private static final int LOAN_WEEKS = 2;
    private static final int MAX_BORROW_LIMIT = 5;

    private List<Book> books;
    private List<Member> members;
    private final FileHandler fileHandler;

    public Library() {
        this.books = new ArrayList<>();
        this.members = new ArrayList<>();
        this.fileHandler = new FileHandler();
        loadData();
    }

    private void loadData() {
        books = fileHandler.loadBooks();
        members = fileHandler.loadMembers();
        System.out.println("Loaded " + books.size() + " books and " + members.size() + " members.");
    }

    private void persist() {
        fileHandler.saveBooks(books);
        fileHandler.saveMembers(members);
    }

    // --- Book operations ---

    public boolean addBook(Book book) {
        if (findBookByIsbn(book.getIsbn()) != null) {
            System.out.println("A book with ISBN " + book.getIsbn() + " already exists.");
            return false;
        }
        books.add(book);
        fileHandler.saveBooks(books);
        System.out.println("Book added successfully: " + book.getTitle());
        return true;
    }

    public boolean removeBook(String isbn) {
        Book book = findBookByIsbn(isbn);
        if (book == null) {
            System.out.println("Book not found with ISBN: " + isbn);
            return false;
        }
        if (!book.isAvailable()) {
            System.out.println("Cannot remove a book that is currently borrowed.");
            return false;
        }
        if (!book.getReservationQueue().isEmpty()) {
            System.out.println("Cannot remove a book that has pending reservations.");
            return false;
        }
        books.remove(book);
        fileHandler.saveBooks(books);
        System.out.println("Book removed successfully: " + book.getTitle());
        return true;
    }

    public Book findBookByIsbn(String isbn) {
        return books.stream()
                .filter(book -> book.getIsbn().equalsIgnoreCase(isbn))
                .findFirst()
                .orElse(null);
    }

    public List<Book> searchBooks(String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        return books.stream()
                .filter(book -> book.getTitle().toLowerCase(Locale.ROOT).contains(lower)
                        || book.getAuthor().toLowerCase(Locale.ROOT).contains(lower)
                        || book.getIsbn().toLowerCase(Locale.ROOT).contains(lower))
                .collect(Collectors.toList());
    }

    public List<Book> filterBooksByAvailability(boolean availableOnly) {
        return books.stream()
                .filter(book -> book.isAvailable() == availableOnly)
                .collect(Collectors.toList());
    }

    public void displayAllBooks() {
        if (books.isEmpty()) {
            System.out.println("No books in the library.");
            return;
        }
        System.out.println("\n=== ALL BOOKS ===");
        System.out.println("Total books: " + books.size());
        System.out.println("-".repeat(80));
        for (int i = 0; i < books.size(); i++) {
            System.out.println((i + 1) + ". " + books.get(i));
        }
    }

    public void displayBooks(List<Book> list, String heading) {
        if (list.isEmpty()) {
            System.out.println("No books found.");
            return;
        }
        System.out.println("\n=== " + heading + " ===");
        System.out.println("Results: " + list.size());
        System.out.println("-".repeat(80));
        for (int i = 0; i < list.size(); i++) {
            System.out.println((i + 1) + ". " + list.get(i));
        }
    }

    // --- Member operations ---

    public boolean registerMember(Member member) {
        if (findMemberById(member.getId()) != null) {
            System.out.println("A member with ID " + member.getId() + " already exists.");
            return false;
        }
        members.add(member);
        fileHandler.saveMembers(members);
        System.out.println("Member registered successfully: " + member.getName());
        return true;
    }

    public Member findMemberById(String id) {
        return members.stream()
                .filter(member -> member.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    public void displayAllMembers() {
        if (members.isEmpty()) {
            System.out.println("No members registered.");
            return;
        }
        System.out.println("\n=== ALL MEMBERS ===");
        System.out.println("Total members: " + members.size());
        System.out.println("-".repeat(80));
        for (int i = 0; i < members.size(); i++) {
            System.out.println((i + 1) + ". " + members.get(i));
        }
    }

    // --- Borrow / return ---

    public boolean borrowBook(String isbn, String memberId) {
        Book book = findBookByIsbn(isbn);
        Member member = findMemberById(memberId);

        if (book == null) {
            System.out.println("Book not found!");
            return false;
        }
        if (member == null) {
            System.out.println("Member not found!");
            return false;
        }
        if (!book.isAvailable()) {
            System.out.println("Book is already borrowed!");
            System.out.println("Tip: use the reservation menu to join the waitlist.");
            return false;
        }
        if (member.getBorrowedCount() >= MAX_BORROW_LIMIT) {
            System.out.println("Member has reached the borrow limit of " + MAX_BORROW_LIMIT + " books.");
            return false;
        }

        String nextReserved = book.peekNextReservation();
        if (nextReserved != null && !nextReserved.equalsIgnoreCase(memberId)) {
            System.out.println("This book is reserved for member: " + nextReserved);
            return false;
        }
        if (nextReserved != null && nextReserved.equalsIgnoreCase(memberId)) {
            book.pollNextReservation();
        }

        book.setAvailable(false);
        book.setBorrowedBy(memberId);
        book.setDueDate(LocalDate.now().plusWeeks(LOAN_WEEKS));
        member.borrowBook(isbn);
        persist();

        System.out.println("Book borrowed successfully!");
        System.out.println("Due date: " + book.getDueDate() + " (" + LOAN_WEEKS + " week loan)");
        return true;
    }

    public boolean returnBook(String isbn, String memberId) {
        Book book = findBookByIsbn(isbn);
        Member member = findMemberById(memberId);

        if (book == null) {
            System.out.println("Book not found!");
            return false;
        }
        if (member == null) {
            System.out.println("Member not found!");
            return false;
        }
        if (book.isAvailable()) {
            System.out.println("This book is not currently borrowed.");
            return false;
        }
        if (book.getBorrowedBy() == null || !book.getBorrowedBy().equalsIgnoreCase(memberId)) {
            System.out.println("This book was not borrowed by member " + memberId + ".");
            return false;
        }

        double fine = book.calculateFine();
        boolean wasOverdue = book.isOverdue();

        book.setAvailable(true);
        book.setBorrowedBy(null);
        book.setDueDate(null);
        member.returnBook(isbn);
        persist();

        System.out.println("Book returned successfully: " + book.getTitle());
        if (wasOverdue) {
            System.out.printf("Overdue fine owed: $%.2f%n", fine);
        } else {
            System.out.println("Returned on time. No fine.");
        }

        String next = book.peekNextReservation();
        if (next != null) {
            System.out.println("Note: this book is reserved next for member " + next + ".");
        }
        return true;
    }

    // --- Reservations ---

    public boolean reserveBook(String isbn, String memberId) {
        Book book = findBookByIsbn(isbn);
        Member member = findMemberById(memberId);

        if (book == null) {
            System.out.println("Book not found!");
            return false;
        }
        if (member == null) {
            System.out.println("Member not found!");
            return false;
        }
        if (book.isAvailable() && book.getReservationQueue().isEmpty()) {
            System.out.println("Book is available — borrow it instead of reserving.");
            return false;
        }
        if (!book.addReservation(memberId)) {
            System.out.println("Could not reserve (already reserved by you, or you currently have it).");
            return false;
        }
        fileHandler.saveBooks(books);
        System.out.println("Reservation added. Queue position: " + book.getReservationQueue().size());
        return true;
    }

    public boolean cancelReservation(String isbn, String memberId) {
        Book book = findBookByIsbn(isbn);
        if (book == null) {
            System.out.println("Book not found!");
            return false;
        }
        if (!book.cancelReservation(memberId)) {
            System.out.println("No reservation found for that member on this book.");
            return false;
        }
        fileHandler.saveBooks(books);
        System.out.println("Reservation cancelled.");
        return true;
    }

    // --- Fines / stats / export ---

    public void displayOverdueBooks() {
        List<Book> overdue = books.stream()
                .filter(Book::isOverdue)
                .sorted(Comparator.comparing(Book::getDueDate))
                .collect(Collectors.toList());

        if (overdue.isEmpty()) {
            System.out.println("No overdue books.");
            return;
        }

        System.out.println("\n=== OVERDUE BOOKS ===");
        double totalFines = 0;
        for (int i = 0; i < overdue.size(); i++) {
            Book book = overdue.get(i);
            double fine = book.calculateFine();
            totalFines += fine;
            System.out.printf("%d. %s | Fine: $%.2f%n", i + 1, book, fine);
        }
        System.out.printf("Total outstanding fines: $%.2f%n", totalFines);
    }

    public void displayStatistics() {
        long availableBooks = books.stream().filter(Book::isAvailable).count();
        long borrowedBooks = books.size() - availableBooks;
        long overdueBooks = books.stream().filter(Book::isOverdue).count();
        long reservedBooks = books.stream()
                .filter(book -> !book.getReservationQueue().isEmpty())
                .count();
        double totalFines = books.stream().mapToDouble(Book::calculateFine).sum();

        System.out.println("\n=== LIBRARY STATISTICS ===");
        System.out.println("Total Books: " + books.size());
        System.out.println("Available Books: " + availableBooks);
        System.out.println("Borrowed Books: " + borrowedBooks);
        System.out.println("Overdue Books: " + overdueBooks);
        System.out.println("Books with Reservations: " + reservedBooks);
        System.out.println("Registered Members: " + members.size());
        System.out.printf("Total Outstanding Fines: $%.2f%n", totalFines);
    }

    public boolean exportToCsv() {
        boolean booksOk = fileHandler.exportBooksToCsv(books, "books_export.csv");
        boolean membersOk = fileHandler.exportMembersToCsv(members, "members_export.csv");
        if (booksOk && membersOk) {
            System.out.println("Exported to data/books_export.csv and data/members_export.csv");
            return true;
        }
        System.out.println("Export completed with errors. Check console messages.");
        return false;
    }

    public List<Book> getBooks() {
        return books;
    }

    public List<Member> getMembers() {
        return members;
    }
}
