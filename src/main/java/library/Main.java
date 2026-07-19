package library;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 * Console menu for the Library Management System.
 */
public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static Library library;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   LIBRARY MANAGEMENT SYSTEM");
        System.out.println("========================================");
        library = new Library();
        boolean running = true;

        while (running) {
            printMenu();
            int choice = readChoice(1, 14);
            System.out.println();

            switch (choice) {
                case 1 -> addBook();
                case 2 -> library.displayAllBooks();
                case 3 -> searchBooks();
                case 4 -> filterBooks();
                case 5 -> removeBook();
                case 6 -> registerMember();
                case 7 -> library.displayAllMembers();
                case 8 -> borrowBook();
                case 9 -> returnBook();
                case 10 -> reserveBook();
                case 11 -> library.displayOverdueBooks();
                case 12 -> library.displayStatistics();
                case 13 -> library.exportToCsv();
                case 14 -> {
                    System.out.println("Thank you for using the Library Management System. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== LIBRARY MANAGEMENT SYSTEM ===");
        System.out.println("1.  Add New Book");
        System.out.println("2.  View All Books");
        System.out.println("3.  Search Books");
        System.out.println("4.  Filter Books (Available / Borrowed)");
        System.out.println("5.  Remove Book");
        System.out.println("6.  Register Member");
        System.out.println("7.  View All Members");
        System.out.println("8.  Borrow Book");
        System.out.println("9.  Return Book");
        System.out.println("10. Reserve Book");
        System.out.println("11. View Overdue Books & Fines");
        System.out.println("12. View Library Statistics");
        System.out.println("13. Export Data to CSV");
        System.out.println("14. Exit");
        System.out.print("\nEnter your choice: ");
    }

    private static void addBook() {
        System.out.println("--- Add New Book ---");
        String isbn = readNonEmpty("ISBN: ");
        if (library.findBookByIsbn(isbn) != null) {
            System.out.println("A book with that ISBN already exists.");
            return;
        }
        String title = readNonEmpty("Title: ");
        String author = readNonEmpty("Author: ");
        int year = readYear("Publication year: ");

        library.addBook(new Book(isbn, title, author, year));
    }

    private static void searchBooks() {
        String keyword = readNonEmpty("Enter search keyword (title, author, or ISBN): ");
        List<Book> results = library.searchBooks(keyword);
        library.displayBooks(results, "SEARCH RESULTS");
    }

    private static void filterBooks() {
        System.out.println("1. Available only");
        System.out.println("2. Borrowed only");
        System.out.print("Choice: ");
        int choice = readChoice(1, 2);
        if (choice == 1) {
            library.displayBooks(library.filterBooksByAvailability(true), "AVAILABLE BOOKS");
        } else {
            library.displayBooks(library.filterBooksByAvailability(false), "BORROWED BOOKS");
        }
    }

    private static void removeBook() {
        String isbn = readNonEmpty("Enter ISBN to remove: ");
        library.removeBook(isbn);
    }

    private static void registerMember() {
        System.out.println("--- Register Member ---");
        String id = readNonEmpty("Member ID (e.g. MEM001): ");
        if (library.findMemberById(id) != null) {
            System.out.println("A member with that ID already exists.");
            return;
        }
        String name = readNonEmpty("Full name: ");
        String email = readEmail("Email: ");
        library.registerMember(new Member(id, name, email));
    }

    private static void borrowBook() {
        System.out.println("--- Borrow Book ---");
        String isbn = readNonEmpty("ISBN: ");
        String memberId = readNonEmpty("Member ID: ");
        library.borrowBook(isbn, memberId);
    }

    private static void returnBook() {
        System.out.println("--- Return Book ---");
        String isbn = readNonEmpty("ISBN: ");
        String memberId = readNonEmpty("Member ID: ");
        library.returnBook(isbn, memberId);
    }

    private static void reserveBook() {
        System.out.println("--- Reserve Book ---");
        System.out.println("1. Add reservation");
        System.out.println("2. Cancel reservation");
        System.out.print("Choice: ");
        int choice = readChoice(1, 2);
        String isbn = readNonEmpty("ISBN: ");
        String memberId = readNonEmpty("Member ID: ");
        if (choice == 1) {
            library.reserveBook(isbn, memberId);
        } else {
            library.cancelReservation(isbn, memberId);
        }
    }

    // --- Input helpers ---

    private static int readChoice(int min, int max) {
        while (true) {
            try {
                int value = scanner.nextInt();
                scanner.nextLine();
                if (value < min || value > max) {
                    System.out.print("Please enter a number between " + min + " and " + max + ": ");
                    continue;
                }
                return value;
            } catch (InputMismatchException e) {
                scanner.nextLine();
                System.out.print("Invalid input. Enter a number: ");
            }
        }
    }

    private static String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Input cannot be empty. Please try again.");
        }
    }

    private static int readYear(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                int year = Integer.parseInt(raw);
                int current = java.time.Year.now().getValue();
                if (year < 1000 || year > current + 1) {
                    System.out.println("Please enter a realistic year between 1000 and " + (current + 1) + ".");
                    continue;
                }
                return year;
            } catch (NumberFormatException e) {
                System.out.println("Invalid year. Enter digits only.");
            }
        }
    }

    private static String readEmail(String prompt) {
        while (true) {
            String email = readNonEmpty(prompt);
            if (email.contains("@") && email.contains(".") && email.length() >= 5) {
                return email;
            }
            System.out.println("Please enter a valid email address (must contain @ and .).");
        }
    }
}
