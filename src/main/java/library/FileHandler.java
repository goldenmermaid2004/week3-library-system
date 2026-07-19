package library;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles loading and saving books/members to text files, plus CSV export.
 * Book line format (pipe-separated):
 * ISBN|title|author|year|available|borrowedBy|dueDate|reservations
 * Member line format:
 * id|name|email|borrowedIsbn1,borrowedIsbn2
 */
public class FileHandler {
    private static final String DATA_DIR = "data";
    private static final String BOOKS_FILE = "books.txt";
    private static final String MEMBERS_FILE = "members.txt";
    private static final String NULL_TOKEN = "-";

    private final Path booksPath;
    private final Path membersPath;

    public FileHandler() {
        Path dataDir = Paths.get(DATA_DIR);
        this.booksPath = dataDir.resolve(BOOKS_FILE);
        this.membersPath = dataDir.resolve(MEMBERS_FILE);
        ensureDataDirectory(dataDir);
    }

    private void ensureDataDirectory(Path dataDir) {
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            System.err.println("Warning: could not create data directory: " + e.getMessage());
        }
    }

    public List<Book> loadBooks() {
        List<Book> books = new ArrayList<>();
        if (!Files.exists(booksPath)) {
            return books;
        }

        try (BufferedReader reader = Files.newBufferedReader(booksPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                Book book = parseBook(line);
                if (book != null) {
                    books.add(book);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading books: " + e.getMessage());
        }
        return books;
    }

    public List<Member> loadMembers() {
        List<Member> members = new ArrayList<>();
        if (!Files.exists(membersPath)) {
            return members;
        }

        try (BufferedReader reader = Files.newBufferedReader(membersPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                Member member = parseMember(line);
                if (member != null) {
                    members.add(member);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading members: " + e.getMessage());
        }
        return members;
    }

    public void saveBooks(List<Book> books) {
        try {
            ensureDataDirectory(booksPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(booksPath, StandardCharsets.UTF_8)) {
                writer.write("# ISBN|title|author|year|available|borrowedBy|dueDate|reservations");
                writer.newLine();
                for (Book book : books) {
                    writer.write(serializeBook(book));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving books: " + e.getMessage());
        }
    }

    public void saveMembers(List<Member> members) {
        try {
            ensureDataDirectory(membersPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(membersPath, StandardCharsets.UTF_8)) {
                writer.write("# id|name|email|borrowedBooks");
                writer.newLine();
                for (Member member : members) {
                    writer.write(serializeMember(member));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving members: " + e.getMessage());
        }
    }

    public boolean exportBooksToCsv(List<Book> books, String fileName) {
        Path path = Paths.get(DATA_DIR).resolve(fileName);
        try {
            ensureDataDirectory(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("ISBN,Title,Author,Year,Available,BorrowedBy,DueDate,Reservations");
                writer.newLine();
                for (Book book : books) {
                    writer.write(String.format("%s,\"%s\",\"%s\",%d,%s,%s,%s,\"%s\"",
                            escapeCsv(book.getIsbn()),
                            escapeCsv(book.getTitle()),
                            escapeCsv(book.getAuthor()),
                            book.getYear(),
                            book.isAvailable(),
                            book.getBorrowedBy() == null ? "" : book.getBorrowedBy(),
                            book.getDueDate() == null ? "" : book.getDueDate().toString(),
                            String.join(";", book.getReservationQueue())));
                    writer.newLine();
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error exporting books CSV: " + e.getMessage());
            return false;
        }
    }

    public boolean exportMembersToCsv(List<Member> members, String fileName) {
        Path path = Paths.get(DATA_DIR).resolve(fileName);
        try {
            ensureDataDirectory(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("ID,Name,Email,BorrowedCount,BorrowedISBNs");
                writer.newLine();
                for (Member member : members) {
                    writer.write(String.format("%s,\"%s\",%s,%d,\"%s\"",
                            escapeCsv(member.getId()),
                            escapeCsv(member.getName()),
                            escapeCsv(member.getEmail()),
                            member.getBorrowedCount(),
                            String.join(";", member.getBorrowedBooks())));
                    writer.newLine();
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error exporting members CSV: " + e.getMessage());
            return false;
        }
    }

    private Book parseBook(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 4) {
                System.err.println("Skipping malformed book line: " + line);
                return null;
            }

            String isbn = parts[0].trim();
            String title = parts[1].trim();
            String author = parts[2].trim();
            int year = Integer.parseInt(parts[3].trim());

            Book book = new Book(isbn, title, author, year);

            if (parts.length >= 5) {
                book.setAvailable(Boolean.parseBoolean(parts[4].trim()));
            }
            if (parts.length >= 6) {
                String borrowedBy = parts[5].trim();
                book.setBorrowedBy(NULL_TOKEN.equals(borrowedBy) || borrowedBy.isEmpty() ? null : borrowedBy);
            }
            if (parts.length >= 7) {
                String due = parts[6].trim();
                book.setDueDate(NULL_TOKEN.equals(due) || due.isEmpty() ? null : LocalDate.parse(due));
            }
            if (parts.length >= 8 && !parts[7].trim().isEmpty() && !NULL_TOKEN.equals(parts[7].trim())) {
                String[] reservations = parts[7].trim().split(",");
                for (String memberId : reservations) {
                    if (!memberId.trim().isEmpty()) {
                        book.getReservationQueue().add(memberId.trim());
                    }
                }
            }
            return book;
        } catch (Exception e) {
            System.err.println("Error parsing book line '" + line + "': " + e.getMessage());
            return null;
        }
    }

    private Member parseMember(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 3) {
                System.err.println("Skipping malformed member line: " + line);
                return null;
            }

            String id = parts[0].trim();
            String name = parts[1].trim();
            String email = parts[2].trim();
            Member member = new Member(id, name, email);

            if (parts.length >= 4 && !parts[3].trim().isEmpty() && !NULL_TOKEN.equals(parts[3].trim())) {
                List<String> books = Arrays.stream(parts[3].split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                for (String isbn : books) {
                    member.borrowBook(isbn);
                }
            }
            return member;
        } catch (Exception e) {
            System.err.println("Error parsing member line '" + line + "': " + e.getMessage());
            return null;
        }
    }

    private String serializeBook(Book book) {
        String borrowedBy = book.getBorrowedBy() == null ? NULL_TOKEN : book.getBorrowedBy();
        String dueDate = book.getDueDate() == null ? NULL_TOKEN : book.getDueDate().toString();
        String reservations = book.getReservationQueue().isEmpty()
                ? NULL_TOKEN
                : String.join(",", book.getReservationQueue());
        return String.join("|",
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                String.valueOf(book.getYear()),
                String.valueOf(book.isAvailable()),
                borrowedBy,
                dueDate,
                reservations);
    }

    private String serializeMember(Member member) {
        String borrowed = member.getBorrowedBooks().isEmpty()
                ? NULL_TOKEN
                : String.join(",", member.getBorrowedBooks());
        return String.join("|",
                member.getId(),
                member.getName(),
                member.getEmail(),
                borrowed);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }
}
