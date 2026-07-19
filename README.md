# Console-Based Library Management System

**Week 3 Project: Java Programming Basics**

A Java console application for managing library operations including book tracking, member management, borrowing with due dates, overdue fines, reservations, and file-based data persistence.

---

## Project Overview

This project practices core Java and OOP skills:

- Java syntax, data types, control structures, and methods
- Classes, objects, encapsulation, and constructors
- `ArrayList` collections for books and members
- Console I/O with input validation
- File read/write persistence (`books.txt`, `members.txt`)
- Exception handling for I/O and invalid input
- Search, filter, statistics, CSV export, and reservations

## Features

| Feature | Description |
|---|---|
| Add / remove books | Manage the catalog by ISBN with validation |
| Search & filter | Find by title, author, or ISBN; filter available vs borrowed |
| Register members | Store member ID, name, and email |
| Borrow / return | 2-week loans with due-date tracking |
| Overdue fines | $1.00 per day late |
| Reservations | Waitlist when a book is unavailable |
| Statistics | Totals for books, members, overdue items, and fines |
| CSV export | Export books and members to `data/*.csv` |
| Persistence | Auto load/save via `data/books.txt` and `data/members.txt` |

## How to Run

### Option A — `javac` / `java` (no Maven required)

```bash
cd week3-library-system

# Compile
javac -d bin src/main/java/library/*.java

# Run (must start from project root so data/ is found)
java -cp bin library.Main
```

### Option B — Maven

```bash
cd week3-library-system
mvn -q compile exec:java -Dexec.mainClass="library.Main"
# or
mvn -q package
java -jar target/week3-library-system-1.0.0.jar
```

**Requirements:** JDK 17 or later.

## Sample Menu

```
=== LIBRARY MANAGEMENT SYSTEM ===
1.  Add New Book
2.  View All Books
3.  Search Books
4.  Filter Books (Available / Borrowed)
5.  Remove Book
6.  Register Member
7.  View All Members
8.  Borrow Book
9.  Return Book
10. Reserve Book
11. View Overdue Books & Fines
12. View Library Statistics
13. Export Data to CSV
14. Exit

Enter your choice:
```

## Code Structure

```
week3-library-system/
├── src/main/java/library/
│   ├── Main.java          # Console menu & input validation
│   ├── Book.java          # Book model (due dates, fines, reservations)
│   ├── Member.java        # Member model & borrowed list
│   ├── Library.java       # Business logic / operations
│   └── FileHandler.java   # Load/save text files + CSV export
├── src/main/resources/
├── data/
│   ├── books.txt          # Persisted book catalog
│   └── members.txt        # Persisted members
├── docs/                  # Screenshots / evidence
├── README.md
├── .gitignore
└── pom.xml
```

## Technical Details

### Architecture

- **`Main`** — presentation layer (menu loop, prompts, validation)
- **`Library`** — service layer (borrow, return, search, stats, reservations)
- **`Book` / `Member`** — domain models with encapsulation
- **`FileHandler`** — persistence layer (pipe-delimited text + CSV)

### Data formats

**Book line:** `ISBN|title|author|year|available|borrowedBy|dueDate|reservations`

**Member line:** `id|name|email|borrowedIsbn1,borrowedIsbn2`

Use `-` for empty optional fields.

### Algorithms & structures

- In-memory storage: `ArrayList<Book>` and `ArrayList<Member>`
- Lookup/search: Java Streams (`filter`, `findFirst`, `collect`)
- Loan period: `LocalDate.now().plusWeeks(2)`
- Fine: days overdue × $1.00 (`ChronoUnit.DAYS`)
- Reservations: FIFO queue (`List` on each `Book`)

## Setup Instructions

1. Clone or download the repository:

   ```bash
   git clone https://github.com/goldenmermaid2004/week3-library-system.git
   cd week3-library-system
   ```

2. Ensure JDK 17+ is installed (`java -version`, `javac -version`).

3. Compile and run using the commands in **How to Run**.

4. Sample data loads automatically from `data/books.txt` and `data/members.txt`.

## Testing Evidence

Suggested manual test cases:

| # | Scenario | Expected |
|---|---|---|
| 1 | View all books on start | Sample catalog prints (5 books) |
| 2 | Search "Java" | Matches *Effective Java* |
| 3 | Register new member `MEM006` | Success message; saved to `members.txt` |
| 4 | Borrow available book | Due date ~2 weeks ahead; status becomes borrowed |
| 5 | Return on-time book | "No fine" message |
| 6 | Return overdue book (`9781617294945`) | Fine printed ($1/day) |
| 7 | Reserve borrowed book | Queue position shown |
| 8 | Remove borrowed book | Rejected with clear error |
| 9 | Export CSV | Files created under `data/` |
| 10 | Invalid | Counts match catalog/members |

A scripted smoke test is included:

```bash
# Windows PowerShell (from project root)
Get-Content docs/smoke_input.txt | java -cp bin library.Main
```

## Visual Documentation

Place screenshots of the menu, book list, borrow/return flow, and statistics in the `docs/` folder (for example `docs/menu.png`, `docs/statistics.png`).

## Quality Checklist

- [x] Project overview and goals
- [x] Setup / run instructions
- [x] Clear code structure
- [x] OOP with encapsulation
- [x] File I/O persistence
- [x] ArrayLists for collections
- [x] Console menu + validation
- [x] Exception handling
- [x] Search / filter
- [x] Overdue fines, stats, CSV, reservations
- [x] Sample data and test guidance
