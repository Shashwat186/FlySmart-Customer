# ✈️ FLYSMART

FLYSMART Customer is the customer-facing flight booking application of FlySmart. Customers can sign up, search flights, book and pay for tickets, manage their bookings and leave feedback.

---

## 🚀 Technologies Used

- Java  
- Spring Boot  
- MySQL  
- JSP (for Frontend)

---

## ⚙️ Project Setup

1. Download the entire project as a `.zip` file and extract it.
2. Import the extracted project into your preferred IDE (e.g., IntelliJ or Eclipse).
3. Navigate to the Spring Boot application and open it in your working directory.
4. A MySQL dump file (`Dump20250508.sql`) is included, containing the database structure (no data).
5. Open **MySQL Workbench**, click on **Server** > **Data Import**.
6. Select the `Dump20250508.sql` file to import the database schema.
7. Edit the `application.properties` file to point to your local MySQL database URL.
8. If you are a beginner, it's recommended to follow each step carefully—ChatGPT can assist if needed.
9. Before using the booking feature, insert sample data (e.g., Airports, Airlines, Flights) into the database.

---

## 🔐 User Access

- **Customer** – Can sign up and log in through the application interface.
- Only customer (`PASSENGER`) accounts can log in to this application. Owner and manager functionality is not part of this application.
- Ensure the required initial data is set up before running the project for a complete experience.

---

## 📌 Prerequisites

- Java 17+  
- Spring Boot  
- MySQL Server  
- MySQL Workbench  
- Maven (for dependency management)
- Docker Desktop (running) – needed only to run the tests

---

## 🧪 Running the Tests

```
./mvnw verify
```

Integration tests start a throwaway MySQL 8.4 in Docker using [Testcontainers](https://testcontainers.com/). They never connect to the real database. The container is set up to behave like production: case-sensitive table names and the same `sql_mode` (ANSI quotes). Its schema comes from `src/test/resources/db/schema.sql`, which was captured from production. Re-capture that file whenever the production schema changes.

---

Feel free to enhance the database with your own flight data and customize the roles to match your use case!



