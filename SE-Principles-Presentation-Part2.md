# Software Engineering Principles - Part 2
## Continuation: Additional Principles & Practices

*This document continues from SE-Principles-Presentation.md*

**Previously covered:**
1. KISS - Keep It Simple, Stupid
2. YAGNI - You Aren't Gonna Need It
3. DRY - Don't Repeat Yourself
4. Single Responsibility Principle
5. Open/Closed Principle
6. Liskov Substitution Principle
7. Interface Segregation Principle
8. Dependency Inversion Principle
9. Write Clean Code
10. Fail Fast

---

## 11. Boy Scout Rule

### The Concept

**Leave the codebase cleaner than you found it.** Every time you touch code, make a small improvement. These incremental improvements compound over time, preventing technical debt accumulation.

**Why it matters:**
- Prevents gradual code degradation
- Distributes refactoring effort over time
- No need for massive "cleanup" projects
- Code quality improves continuously

**The principle:** Always leave code better than you found it, even if just slightly.

### Real-World Analogy

**Hospital Cleanliness:**

**Without Boy Scout Rule:**
```
Nurse spills medication → leaves it
Doctor notices dirty equipment → ignores it
Technician sees expired supplies → walks past
Everyone: "Not my job" or "I'll do it later"

Result after 6 months:
- Hospital filthy
- Safety hazards everywhere
- Requires 2-week shutdown for deep cleaning
- Expensive, disruptive
```

**With Boy Scout Rule:**
```
Nurse spills medication → cleans it immediately + nearby area
Doctor notices dirty equipment → cleans it + checks others
Technician sees expired supplies → removes them + checks shelf

Result after 6 months:
- Hospital consistently clean
- No safety hazards
- No disruption needed
- Minimal effort, maximum benefit
```

**This is Boy Scout Rule:** Small, continuous improvements prevent major problems.

### Good Example: Industry Standard

**Linux Kernel Development:**

```c
// Developer fixing a memory leak
// Before (existing code):
void process_data(char *buffer) {
    char *temp = malloc(1024);
    // ... process data ...
    // Missing free(temp) - memory leak!
}

// Developer's change:
void process_data(char *buffer) {
    char *temp = malloc(1024);
    if (temp == NULL) {
        return;  // Added null check (Boy Scout improvement #1)
    }

    // ... process data ...

    free(temp);  // Fixed original memory leak

    // While here, also improved variable naming (Boy Scout #2)
    // Was: char *temp
    // Now: char *processing_buffer (in actual implementation)
}

// Boy Scout Rule applied:
// 1. Fixed the original bug (memory leak)
// 2. Added defensive null check
// 3. Improved variable naming
// Codebase is now better than before
```

### Good Example: HMIS - Incremental Refactoring

**What we did right:**

```java
// Task: Fix bug in date filtering
// Found this code:

public void searchBills() {
    List<Bill> allBills = billFacade.findAll();  // Loads ALL bills
    List<Bill> filtered = new ArrayList<>();

    for (Bill bill : allBills) {
        Date billDate = bill.getBillDate();

        // BUG: Comparison doesn't account for time component
        if (billDate.after(fromDate) && billDate.before(toDate)) {
            filtered.add(bill);
        }
    }

    this.bills = filtered;
}

// Could have just fixed the bug:
// if (billDate.compareTo(fromDate) >= 0 && billDate.compareTo(toDate) <= 0)

// But applied Boy Scout Rule:
public void searchBills() {
    // 1. Fixed bug: Use proper date range query
    // 2. Improvement: Query database instead of loading all
    // 3. Improvement: Moved to service layer
    this.bills = billService.findByDateRange(fromDate, toDate);
}

// New service method (Boy Scout improvement):
@ApplicationScoped
public class BillService {
    public List<Bill> findByDateRange(Date from, Date to) {
        // Proper date handling with time normalization
        Date startOfDay = CommonFunctions.getStartOfDay(from);
        Date endOfDay = CommonFunctions.getEndOfDay(to);

        String jpql = "SELECT b FROM Bill b " +
                      "WHERE b.billDate >= :from AND b.billDate <= :to " +
                      "AND b.retired = false " +
                      "ORDER BY b.billDate DESC";

        return billFacade.findByJpql(jpql, Map.of(
            "from", startOfDay,
            "to", endOfDay
        ));
    }
}
```

**Boy Scout improvements made:**
1. ✓ Fixed original bug (date comparison)
2. ✓ Improved performance (database query vs in-memory filter)
3. ✓ Improved architecture (moved to service layer)
4. ✓ Added proper date normalization
5. ✓ Added soft-delete filter
6. ✓ Made code reusable

**Impact:**
- Bug fixed
- Performance: 15 seconds → 0.2 seconds
- Code now reusable across controllers
- Similar searches in other controllers updated to use same pattern

### Good Example: HMIS - Small Daily Improvements

**Every commit, every day:**

```java
// Monday: Fixing validation bug
// Found:
public void saveBill() {
    if (bill.getPatient() == null) {
        JsfUtil.addErrorMessage("Patient required");
        return;
    }
    // ...
}

// Fixed bug AND improved:
public void saveBill() {
    // Boy Scout: Extract validation
    if (!isValidForSave()) {
        return;
    }
    // ...
}

private boolean isValidForSave() {
    if (bill.getPatient() == null) {
        JsfUtil.addErrorMessage("Patient is required");
        return false;
    }
    if (bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
        JsfUtil.addErrorMessage("Bill must have at least one item");
        return false;
    }
    return true;
}

// Tuesday: Adding feature
// Found:
private List filterActiveItems(List items) { }

// Added feature AND improved naming:
private List<Item> filterActiveItems(List<Item> items) { }
// Boy Scout: Added generics for type safety

// Wednesday: Code review
// Found:
double total = bill.getTotal();
if (total > 0) {
    // process
}

// Boy Scout: Replaced magic number
private static final double MIN_BILLABLE_AMOUNT = 0.0;

double total = bill.getTotal();
if (total > MIN_BILLABLE_AMOUNT) {
    // process
}

// Thursday: Bug fix
// Found duplicated code in 3 methods
// Boy Scout: Extracted to common utility
// 3 methods × 10 lines = 30 lines
// Reduced to: 3 method calls + 1 utility (13 lines total)

// Friday: Feature complete
// Boy Scout: Ran code formatter on files touched this week
// Boy Scout: Updated Javadoc
// Boy Scout: Added unit test for edge case discovered
```

**Weekly impact:**
- 5 bugs fixed
- 2 features added
- 17 lines of duplication removed
- 8 methods improved
- 3 tests added
- Code quality steadily improved

### Anti-Pattern: "Not My Problem"

**What we did wrong:**

```java
// Developer Task: Add discount calculation to pharmacy bills

public void calculatePharmacyDiscount() {
    // Added new feature
    if (bill.getCustomerType() == CustomerType.REGULAR) {
        bill.setDiscount(5.0);
    } else if (bill.getCustomerType() == CustomerType.VIP) {
        bill.setDiscount(10.0);
    }

    // IGNORED these problems in same file:
    // - Line 47: Unused import
    // - Line 83: Deprecated method call
    // - Line 125: Magic number "100" used 5 times
    // - Line 200: 150-line method that needs splitting
    // - Line 310: Commented-out code from 2 years ago
    // - Line 450: Copy-pasted code (duplicated 3 times)

    // Developer: "Not my task, not my problem"
}
```

**Result over time (12 months, 20 developers):**
```
Month 1: 50 issues ignored
Month 3: 200 issues ignored
Month 6: 500 issues ignored
Month 12: 1,200 issues ignored

Technical debt accumulated:
- 2,000 lines of commented code
- 300 deprecated method calls
- 150 magic numbers
- 50 methods > 100 lines
- 1,000 lines of duplication

Consequences:
- New features take 3x longer (navigating mess)
- Bug fix time increased 5x (unexpected side effects)
- New developer onboarding: 2 weeks → 2 months
- Team morale: Low (nobody wants to work in mess)

Solution needed:
- 2-month "cleanup sprint"
- Feature development paused
- Cost: $50,000+ in lost productivity
```

**The missed opportunity:**

```java
// If EVERY developer applied Boy Scout Rule:

public void calculatePharmacyDiscount() {
    // Added new feature
    if (bill.getCustomerType() == CustomerType.REGULAR) {
        bill.setDiscount(REGULAR_CUSTOMER_DISCOUNT);  // Boy Scout: constant
    } else if (bill.getCustomerType() == CustomerType.VIP) {
        bill.setDiscount(VIP_CUSTOMER_DISCOUNT);  // Boy Scout: constant
    }

    // Boy Scout: While here, quick fixes (2 minutes each):
    // ✓ Removed unused import (Line 47)
    // ✓ Updated deprecated method (Line 83)
    // ✓ Extracted magic number to constant (Line 125)
    // ✓ Added TODO for long method (Line 200) - will fix next time
    // ✓ Deleted commented code (Line 310)

    // Total extra time: 10 minutes
}

// Result over 12 months:
// 20 developers × 50 commits each × 10 minutes = 1,000 commits × 10 min
// Total: ~167 hours of cleanup distributed over 12 months
// = 1 hour per developer per month
// Prevents: $50,000 cleanup sprint
// ROI: 300x return on investment
```

### Anti-Pattern: "I'll Clean It Up Later"

**The lie we tell ourselves:**

```java
// Developer thinks: "I'll clean this up in a separate PR"

// Commit 1: Add feature (messy)
public void processOrder() {
    // TODO: Clean this up later
    // Nested if statements 5 levels deep
    if (order != null) {
        if (order.getItems() != null) {
            if (!order.getItems().isEmpty()) {
                if (order.getCustomer() != null) {
                    if (order.getCustomer().isActive()) {
                        // actual logic buried here
                    }
                }
            }
        }
    }
}

// Commit 2: Another feature (more mess)
// Commit 3: Bug fix (even more mess)
// Commit 4: Urgent hotfix (chaos)

// "Later" never comes
// Technical debt compounds
// Future developers suffer
```

**Reality check:**

```
Week 1: "I'll clean it up next week"
Week 2: "Too busy with new feature"
Week 3: "Forgot about it"
Week 4: "Someone else touched that file, not my problem now"
Month 6: "What was I supposed to clean up?"
Year 1: Messy code now has 50+ commits on top
Year 2: Rewrite considered because code is unmaintainable

Total cleanup cost: Never happened OR massive rewrite
If Boy Scout Rule applied: 5 minutes per commit = 15 minutes total
```

### How to Apply Boy Scout Rule

**Simple checklist for every commit:**

```
Before committing:
□ Fixed the bug/added the feature (main task)

While you're here (Boy Scout - 2-5 minutes):
□ Improve 1-2 variable/method names
□ Extract 1 magic number to constant
□ Add 1 missing validation
□ Delete 1 block of commented code
□ Fix 1 compiler warning
□ Add 1 Javadoc comment
□ Extract 1 duplicated block
□ Format the file

Don't:
□ Try to fix everything (scope creep)
□ Spend hours refactoring (not Boy Scout, that's renovation)
□ Make unrelated changes (confuses git blame)

Golden rule: 5-10 minutes of improvements
If it takes longer, create a ticket for later
```

### Real Impact at HMIS

**Metrics after adopting Boy Scout Rule:**

```
Before Boy Scout Rule (Jan-Jun):
- Code quality score: 62/100
- Average method length: 47 lines
- Code duplication: 18%
- Technical debt: 180 days
- Bug fix time: Average 4 hours
- New feature time: Average 3 days

After Boy Scout Rule (Jul-Dec):
- Code quality score: 81/100 (+19)
- Average method length: 28 lines (-19)
- Code duplication: 9% (-9%)
- Technical debt: 85 days (-95 days)
- Bug fix time: Average 1.5 hours (-62%)
- New feature time: Average 1.8 days (-40%)

Developer satisfaction:
- Before: 6.2/10
- After: 8.7/10
- Reason: "Codebase improving instead of degrading"
```

### Key Takeaways

**Do:**
- Make small improvements every commit
- Focus on code you're already touching
- Limit Boy Scout time to 5-10 minutes
- Leave clear, obvious improvements
- Share Boy Scout improvements in code reviews

**Don't:**
- Ignore problems you encounter
- Promise "I'll fix it later" (do it now)
- Over-refactor (stay focused)
- Make massive unrelated changes
- Skip Boy Scout because you're "too busy"

**The Test:** After your commit, is the code better than before you started?

**Remember:** "Always leave the campground cleaner than you found it." — Boy Scouts of America. Applied to code, this prevents technical debt from accumulating and keeps the codebase healthy.

---

## 12. Defensive Programming

### The Concept

**Never trust external input. Validate everything. Handle all errors gracefully. Assume things will go wrong and plan for it.**

**Why it matters:**
- External systems fail
- Users provide invalid input
- Networks are unreliable
- Disks fill up
- Graceful degradation beats crashes

**The principle:** Write code that is resilient to misuse and hostile environments.

### Real-World Analogy

**Hospital Emergency Preparedness:**

**Not defensive:**
```
Hospital assumes:
- Power never fails
- Suppliers always deliver on time
- All staff always available
- Equipment never breaks
- Internet always works

Reality check:
- Typhoon → power outage → no backup
- Emergency surgery → no power → patient risk
- Supplier delayed → no critical medication
- Disaster
```

**Defensive approach:**
```
Hospital prepares:
- Backup generators (power failure)
- Stock buffer (supplier delays)
- Cross-trained staff (absences)
- Redundant equipment (breakdowns)
- Offline procedures (network down)

Power fails:
- Generator activates in 10 seconds
- Surgery continues safely
- No patient impact
```

**This is defensive programming:** Plan for failure, handle gracefully.

### Good Example: Industry Standard

**Apache Tomcat Server:**

```java
// Defensive programming in server code
public class ConnectionHandler {

    private static final int MAX_CONNECTIONS = 200;
    private static final int SOCKET_TIMEOUT = 30000; // 30 seconds
    private static final int MAX_REQUEST_SIZE = 10_485_760; // 10MB

    public void handleConnection(Socket socket) {
        // Defensive: Set timeout (don't hang forever)
        try {
            socket.setSoTimeout(SOCKET_TIMEOUT);
        } catch (SocketException e) {
            logger.warn("Could not set socket timeout", e);
            closeQuietly(socket);
            return;
        }

        // Defensive: Limit concurrent connections
        if (activeConnections.get() >= MAX_CONNECTIONS) {
            logger.warn("Max connections reached, rejecting connection");
            sendServiceUnavailable(socket);
            closeQuietly(socket);
            return;
        }

        activeConnections.incrementAndGet();

        try {
            InputStream input = socket.getInputStream();

            // Defensive: Limit request size (prevent memory exhaustion)
            input = new BoundedInputStream(input, MAX_REQUEST_SIZE);

            // Defensive: Wrap in BufferedInputStream (performance + safety)
            input = new BufferedInputStream(input);

            processRequest(input);

        } catch (IOException e) {
            // Defensive: Handle I/O errors gracefully
            logger.error("Error processing request", e);
            sendInternalServerError(socket);

        } catch (OutOfMemoryError e) {
            // Defensive: Handle OOM gracefully
            logger.fatal("Out of memory", e);
            System.gc(); // Desperate attempt
            sendServiceUnavailable(socket);

        } finally {
            // Defensive: Always cleanup resources
            activeConnections.decrementAndGet();
            closeQuietly(socket);
        }
    }

    private void closeQuietly(Closeable closeable) {
        // Defensive: Closing can fail, don't let it crash cleanup
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            logger.trace("Error closing resource", e);
            // Swallow - we're cleaning up anyway
        }
    }
}
```

**Defensive techniques:**
- Timeouts prevent hanging
- Connection limits prevent exhaustion
- Size limits prevent memory attacks
- Multiple catch blocks for different failures
- Finally block ensures cleanup
- Quiet close prevents cascade failures

### Good Example: HMIS - External System Integration

**What we did right:**

```java
@ApplicationScoped
public class SmsGatewayService {

    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 15000; // 15 seconds
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY = 2000; // 2 seconds

    @Inject
    private SmsGatewayConfig config;

    public SmsResult sendSms(String phoneNumber, String message) {
        // Defensive: Validate inputs
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            logger.error("Cannot send SMS: phone number is null or empty");
            return SmsResult.failed("Invalid phone number");
        }

        if (message == null || message.trim().isEmpty()) {
            logger.error("Cannot send SMS: message is null or empty");
            return SmsResult.failed("Invalid message");
        }

        // Defensive: Validate message length (SMS limit)
        if (message.length() > 160) {
            logger.warn("Message exceeds 160 characters, truncating");
            message = message.substring(0, 157) + "...";
        }

        // Defensive: Sanitize phone number
        phoneNumber = sanitizePhoneNumber(phoneNumber);

        // Defensive: Check if service is available
        if (!isServiceAvailable()) {
            logger.error("SMS gateway not configured or unavailable");
            return SmsResult.failed("SMS service unavailable");
        }

        // Defensive: Retry logic for transient failures
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return sendSmsWithTimeout(phoneNumber, message);

            } catch (SocketTimeoutException e) {
                // Defensive: Timeout - might succeed on retry
                lastException = e;
                logger.warn("SMS send timeout (attempt {}/{})", attempt, MAX_RETRIES);

                if (attempt < MAX_RETRIES) {
                    sleep(RETRY_DELAY);
                }

            } catch (IOException e) {
                // Defensive: Network error - might succeed on retry
                lastException = e;
                logger.warn("SMS send network error (attempt {}/{}): {}",
                    attempt, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    sleep(RETRY_DELAY);
                }

            } catch (SmsGatewayException e) {
                // Defensive: Gateway rejected - don't retry
                logger.error("SMS gateway error: {}", e.getMessage());
                return SmsResult.failed("SMS gateway error: " + e.getMessage());
            }
        }

        // Defensive: All retries exhausted
        logger.error("SMS send failed after {} attempts", MAX_RETRIES, lastException);
        return SmsResult.failed("SMS send failed after " + MAX_RETRIES + " attempts");
    }

    private SmsResult sendSmsWithTimeout(String phoneNumber, String message) throws IOException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(config.getGatewayUrl());
            connection = (HttpURLConnection) url.openConnection();

            // Defensive: Set timeouts
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestMethod("POST");

            // Defensive: Set required headers
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + config.getApiKey());

            // Send request
            String requestBody = buildRequestBody(phoneNumber, message);
            connection.setDoOutput(true);

            try (OutputStream out = connection.getOutputStream()) {
                out.write(requestBody.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

            // Defensive: Check response code
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                String response = readResponse(connection);
                logger.info("SMS sent successfully to {}", phoneNumber);
                return SmsResult.success(response);

            } else if (responseCode == 429) {
                // Defensive: Rate limited
                throw new SmsGatewayException("Rate limit exceeded");

            } else if (responseCode >= 500) {
                // Defensive: Server error - retriable
                throw new IOException("Gateway server error: " + responseCode);

            } else {
                // Defensive: Client error - not retriable
                String error = readErrorResponse(connection);
                throw new SmsGatewayException("Gateway error: " + error);
            }

        } finally {
            // Defensive: Always cleanup connection
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean isServiceAvailable() {
        // Defensive: Check configuration
        if (config == null || config.getGatewayUrl() == null || config.getApiKey() == null) {
            return false;
        }

        // Defensive: Check circuit breaker (if too many recent failures, don't try)
        if (circuitBreaker.isOpen()) {
            logger.warn("SMS circuit breaker is open, service unavailable");
            return false;
        }

        return true;
    }

    private String sanitizePhoneNumber(String phone) {
        // Defensive: Remove all non-digit characters
        String cleaned = phone.replaceAll("[^0-9+]", "");

        // Defensive: Add country code if missing
        if (!cleaned.startsWith("+")) {
            cleaned = "+94" + cleaned; // Sri Lanka
        }

        return cleaned;
    }

    private void sleep(int millis) {
        // Defensive: Sleep can be interrupted
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Sleep interrupted");
        }
    }

    private String readResponse(HttpURLConnection connection) {
        // Defensive: Reading can fail
        try (InputStream in = connection.getInputStream();
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(in, StandardCharsets.UTF_8))) {

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();

        } catch (IOException e) {
            logger.error("Error reading response", e);
            return "";
        }
    }
}
```

**Defensive layers:**
1. Input validation (null, empty, length)
2. Input sanitization (phone format)
3. Service availability check
4. Timeout configuration
5. Retry logic for transient failures
6. Circuit breaker for repeated failures
7. Different handling for different error types
8. Resource cleanup in finally blocks
9. Graceful degradation (return failure, don't crash)

**Real incident this prevented:**

```
Situation: SMS gateway had 30-second outage (maintenance)

Without defensive programming:
- All 50 concurrent bill saves would have frozen
- Each waiting 30+ seconds for timeout
- Users thought system crashed
- Many clicked "Save" multiple times
- Created duplicate bills
- Chaos

With defensive programming:
- 10-second connection timeout triggered
- Retry logic attempted 3 times
- Total time: ~40 seconds per SMS
- SMS failed gracefully with error message
- Bill still saved successfully
- User saw: "Bill saved. SMS notification failed."
- No duplicate bills
- System remained responsive
- Users understood what happened
```

### Anti-Pattern: Optimistic Programming

**What we did wrong:**

```java
// Assuming everything will work perfectly
public void sendBillNotification(Bill bill) {
    // No input validation - assume bill is valid
    String email = bill.getPatient().getEmail(); // NullPointerException if patient is null!

    // No error handling - assume network works
    EmailService emailService = new EmailService();
    emailService.connect("smtp.gmail.com", 587); // What if connection fails?
    emailService.login(username, password); // What if authentication fails?
    emailService.send(email, "Your Bill", buildBody(bill)); // What if send fails?
    emailService.disconnect();

    // Assume SMS works
    SmsService smsService = new SmsService();
    smsService.send(bill.getPatient().getPhone(), "Bill created"); // What if phone is null? Gateway down?

    // No cleanup - assume no exceptions thrown

    logger.info("Notifications sent"); // Will never log if anything fails!
}
```

**Real disaster:**

```
Production incident:
1. Bill created for patient with no email
2. Method called: sendBillNotification(bill)
3. Line executed: String email = bill.getPatient().getEmail();
4. Result: NullPointerException
5. Exception propagates up
6. Transaction rolls back
7. Bill NOT saved (even though bill was valid!)
8. User sees: "Error saving bill"
9. User confused (bill was correct)
10. Reports "system broken"

Impact:
- 47 bills failed to save over 2 hours
- All had valid data
- Only problem: Missing email
- Bill save should succeed, only notification should fail
- Root cause: Not defensive

Cost:
- 47 patients had to return
- 2 hours of lost billing
- User frustration
- Support tickets
- Emergency hotfix required
```

**Defensive fix:**

```java
public void sendBillNotification(Bill bill) {
    // Defensive: Validate input
    if (bill == null) {
        logger.error("Cannot send notification: bill is null");
        return; // Graceful failure
    }

    if (bill.getPatient() == null) {
        logger.error("Cannot send notification: patient is null for bill {}", bill.getId());
        return; // Graceful failure
    }

    // Defensive: Attempt email (don't let it crash everything)
    try {
        String email = bill.getPatient().getEmail();

        if (email != null && !email.trim().isEmpty()) {
            sendEmail(email, bill);
        } else {
            logger.warn("No email for patient {}, skipping email notification",
                bill.getPatient().getId());
        }
    } catch (Exception e) {
        // Defensive: Email failure doesn't crash bill save
        logger.error("Failed to send email for bill {}", bill.getId(), e);
        // Continue - email is nice-to-have, not critical
    }

    // Defensive: Attempt SMS independently
    try {
        String phone = bill.getPatient().getPhone();

        if (phone != null && !phone.trim().isEmpty()) {
            sendSms(phone, bill);
        } else {
            logger.warn("No phone for patient {}, skipping SMS notification",
                bill.getPatient().getId());
        }
    } catch (Exception e) {
        // Defensive: SMS failure doesn't crash anything
        logger.error("Failed to send SMS for bill {}", bill.getId(), e);
        // Continue - SMS is nice-to-have
    }

    logger.info("Notification attempt completed for bill {}", bill.getId());
}

private void sendEmail(String email, Bill bill) throws EmailException {
    EmailService emailService = null;
    try {
        emailService = new EmailService();
        emailService.connect("smtp.gmail.com", 587);
        emailService.login(username, password);
        emailService.send(email, "Your Bill", buildBody(bill));
        logger.info("Email sent to {} for bill {}", email, bill.getId());
    } finally {
        // Defensive: Always cleanup
        if (emailService != null) {
            try {
                emailService.disconnect();
            } catch (Exception e) {
                logger.trace("Error disconnecting email service", e);
            }
        }
    }
}
```

**Result after fix:**
- Email missing? Bill still saves, email skipped
- SMS gateway down? Bill still saves, SMS skipped
- Network error? Bill still saves, notifications fail gracefully
- User sees: "Bill saved. Email notification failed."
- Clear, actionable message
- No confusion
- No data loss

### Defensive Programming Checklist

**Input Validation:**
```java
// Always validate
- null checks
- empty checks
- range checks
- format checks
- business rule checks

// Examples:
Objects.requireNonNull(patient, "Patient cannot be null");
if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
if (email != null && !email.matches(EMAIL_REGEX)) throw new InvalidEmailException();
```

**Error Handling:**
```java
// Specific exceptions first
catch (FileNotFoundException e) {
    // Handle specific case
} catch (IOException e) {
    // Handle general I/O error
} catch (Exception e) {
    // Last resort
} finally {
    // Always cleanup
}
```

**Resource Management:**
```java
// Always use try-with-resources
try (Connection conn = dataSource.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) {
    // Use resources
} // Automatic cleanup, even if exception thrown
```

**Timeouts:**
```java
// Never wait forever
connection.setConnectTimeout(10000);
connection.setReadTimeout(15000);
executorService.shutdown();
executorService.awaitTermination(30, TimeUnit.SECONDS);
```

**Limits:**
```java
// Prevent resource exhaustion
private static final int MAX_ITEMS = 1000;
private static final int MAX_FILE_SIZE = 10_485_760; // 10MB
private static final int MAX_RETRIES = 3;
```

**Fallbacks:**
```java
// Have Plan B
String value = config.getValue();
if (value == null) {
    value = DEFAULT_VALUE; // Fallback
}

try {
    return primaryService.getData();
} catch (ServiceException e) {
    logger.warn("Primary service failed, using cache", e);
    return cache.getData(); // Fallback
}
```

### Key Takeaways

**Do:**
- Validate all external input
- Handle all possible error cases
- Set timeouts on external calls
- Implement retry logic for transient failures
- Clean up resources in finally blocks
- Log errors with context
- Fail gracefully, never crash

**Don't:**
- Assume input is valid
- Assume external systems work
- Wait indefinitely
- Let exceptions propagate uncaught
- Leak resources
- Swallow exceptions silently

**The Test:** What happens if:
- Input is null?
- Network fails?
- Database is down?
- Disk is full?
- API returns 500?

If answer is "system crashes", you're not defensive enough.

**Remember:** "Hope is not a strategy. Plan for failure." — Unknown

---

## 13. Test-Driven Development (TDD)

### The Concept

**Write tests before writing production code.** Tests define behavior, drive design, and serve as living documentation. Red → Green → Refactor cycle.

**Why it matters:**
- Tests drive better design
- Clear requirements from the start
- Immediate feedback
- Regression protection built-in
- Code is testable by design

**The principle:** Tests first, implementation second.

### The TDD Cycle

```
1. RED: Write failing test
   - Defines what code should do
   - Test fails (code doesn't exist yet)

2. GREEN: Write minimal code to pass
   - Just enough to make test green
   - Don't optimize yet

3. REFACTOR: Improve code
   - Clean up duplication
   - Improve names
   - Optimize if needed
   - Tests ensure behavior unchanged

Repeat for each new requirement
```

### Real-World Analogy

**Building Blueprint:**

**Without TDD (build then test):**
```
1. Build entire house
2. Move in furniture
3. Test electrical system
4. Find problem: Outlets in wrong locations
5. Tear down walls
6. Rewire
7. Rebuild walls
8. Repaint
9. Move furniture back
Cost: High, Time: Weeks
```

**With TDD (test first):**
```
1. Draw electrical blueprint (test)
2. Verify blueprint meets requirements
3. Build according to blueprint (code)
4. Test as you go
5. Find problem during construction (test fails)
6. Fix immediately (change blueprint and implementation)
Cost: Low, Time: Hours
```

**This is TDD:** Design/test first prevents expensive rework.

### Good Example: Industry Standard

**JUnit (Testing Framework Itself):**

```java
// TDD Example: Building assertEquals method

// Step 1: RED - Write test first
@Test
public void testAssertEquals_BothNull_Passes() {
    // What should happen when both values are null?
    // Test defines expected behavior
    assertEquals(null, null); // Should pass
    // Test fails - assertEquals doesn't exist yet!
}

@Test
public void testAssertEquals_EqualStrings_Passes() {
    assertEquals("hello", "hello"); // Should pass
}

@Test
public void testAssertEquals_DifferentStrings_Fails() {
    try {
        assertEquals("hello", "world"); // Should fail
        fail("Expected AssertionError");
    } catch (AssertionError e) {
        // Expected
    }
}

// Step 2: GREEN - Write minimal implementation
public static void assertEquals(Object expected, Object actual) {
    if (expected == null && actual == null) {
        return; // Pass
    }
    if (expected != null && expected.equals(actual)) {
        return; // Pass
    }
    throw new AssertionError("Expected: " + expected + ", but was: " + actual);
}

// Tests now pass!

// Step 3: REFACTOR - Improve implementation
public static void assertEquals(Object expected, Object actual) {
    assertEquals(null, expected, actual);
}

public static void assertEquals(String message, Object expected, Object actual) {
    if (Objects.equals(expected, actual)) {
        return;
    }
    failNotEquals(message, expected, actual);
}

private static void failNotEquals(String message, Object expected, Object actual) {
    String formatted = buildFailureMessage(message, expected, actual);
    throw new AssertionError(formatted);
}

// Tests still pass! Refactoring didn't break anything.
```

### Good Example: HMIS - Bill Calculation

**TDD process:**

```java
// Requirement: Calculate bill total with discount and tax
// Total = (Items Total - Discount) + Tax

// Step 1: RED - Test for basic total
@Test
public void testCalculateTotal_NoDiscountNoTax_ReturnsItemsTotal() {
    // Arrange
    Bill bill = new Bill();
    bill.addItem(new BillItem("Item 1", 100.0, 2)); // 200
    bill.addItem(new BillItem("Item 2", 150.0, 1)); // 150
    // Expected total: 350

    BillCalculator calculator = new BillCalculator();

    // Act
    double total = calculator.calculateTotal(bill);

    // Assert
    assertEquals(350.0, total, 0.01);
}
// TEST FAILS: BillCalculator doesn't exist

// Step 2: GREEN - Minimal implementation
public class BillCalculator {
    public double calculateTotal(Bill bill) {
        return bill.getItems().stream()
            .mapToDouble(item -> item.getRate() * item.getQty())
            .sum();
    }
}
// TEST PASSES!

// Step 3: Add test for discount
@Test
public void testCalculateTotal_WithDiscount_AppliesDiscount() {
    // Arrange
    Bill bill = new Bill();
    bill.addItem(new BillItem("Item 1", 100.0, 2)); // 200
    bill.setDiscountPercentage(10.0); // 10% off
    // Expected: 200 - 20 = 180

    BillCalculator calculator = new BillCalculator();

    // Act
    double total = calculator.calculateTotal(bill);

    // Assert
    assertEquals(180.0, total, 0.01);
}
// TEST FAILS: Discount not implemented

// Step 4: GREEN - Add discount
public double calculateTotal(Bill bill) {
    double itemsTotal = bill.getItems().stream()
        .mapToDouble(item -> item.getRate() * item.getQty())
        .sum();

    double discountPercentage = bill.getDiscountPercentage() != null
        ? bill.getDiscountPercentage()
        : 0.0;
    double discountAmount = itemsTotal * (discountPercentage / 100);

    return itemsTotal - discountAmount;
}
// BOTH TESTS PASS!

// Step 5: Add test for tax
@Test
public void testCalculateTotal_WithTax_AddsTax() {
    // Arrange
    Bill bill = new Bill();
    bill.addItem(new BillItem("Item 1", 100.0, 1)); // 100
    bill.setTaxPercentage(15.0); // 15% tax
    // Expected: 100 + 15 = 115

    BillCalculator calculator = new BillCalculator();

    // Act
    double total = calculator.calculateTotal(bill);

    // Assert
    assertEquals(115.0, total, 0.01);
}
// TEST FAILS: Tax not implemented

// Step 6: GREEN - Add tax
public double calculateTotal(Bill bill) {
    double itemsTotal = bill.getItems().stream()
        .mapToDouble(item -> item.getRate() * item.getQty())
        .sum();

    double discountPercentage = bill.getDiscountPercentage() != null
        ? bill.getDiscountPercentage()
        : 0.0;
    double discountAmount = itemsTotal * (discountPercentage / 100);
    double afterDiscount = itemsTotal - discountAmount;

    double taxPercentage = bill.getTaxPercentage() != null
        ? bill.getTaxPercentage()
        : 0.0;
    double taxAmount = afterDiscount * (taxPercentage / 100);

    return afterDiscount + taxAmount;
}
// ALL TESTS PASS!

// Step 7: Test complex scenario
@Test
public void testCalculateTotal_WithDiscountAndTax_AppliesBoth() {
    // Arrange
    Bill bill = new Bill();
    bill.addItem(new BillItem("Item 1", 100.0, 3)); // 300
    bill.setDiscountPercentage(10.0); // 10% discount
    bill.setTaxPercentage(15.0); // 15% tax
    // Expected: (300 - 30) + 40.5 = 310.5

    BillCalculator calculator = new BillCalculator();

    // Act
    double total = calculator.calculateTotal(bill);

    // Assert
    assertEquals(310.5, total, 0.01);
}
// TEST PASSES! (Implementation already handles it)

// Step 8: Test edge cases
@Test
public void testCalculateTotal_EmptyBill_ReturnsZero() {
    Bill bill = new Bill();
    assertEquals(0.0, new BillCalculator().calculateTotal(bill), 0.01);
}

@Test(expected = IllegalArgumentException.class)
public void testCalculateTotal_NullBill_ThrowsException() {
    new BillCalculator().calculateTotal(null);
}

@Test
public void testCalculateTotal_NegativeDiscount_ThrowsException() {
    Bill bill = new Bill();
    bill.addItem(new BillItem("Item", 100.0, 1));
    bill.setDiscountPercentage(-10.0);

    try {
        new BillCalculator().calculateTotal(bill);
        fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
        assertTrue(e.getMessage().contains("Discount cannot be negative"));
    }
}
// TESTS FAIL - Edge cases not handled

// Step 9: GREEN - Handle edge cases
public double calculateTotal(Bill bill) {
    if (bill == null) {
        throw new IllegalArgumentException("Bill cannot be null");
    }

    if (bill.getItems() == null || bill.getItems().isEmpty()) {
        return 0.0;
    }

    double discountPercentage = bill.getDiscountPercentage() != null
        ? bill.getDiscountPercentage()
        : 0.0;

    if (discountPercentage < 0 || discountPercentage > 100) {
        throw new IllegalArgumentException(
            "Discount must be between 0 and 100, got: " + discountPercentage
        );
    }

    double taxPercentage = bill.getTaxPercentage() != null
        ? bill.getTaxPercentage()
        : 0.0;

    if (taxPercentage < 0 || taxPercentage > 100) {
        throw new IllegalArgumentException(
            "Tax must be between 0 and 100, got: " + taxPercentage
        );
    }

    double itemsTotal = bill.getItems().stream()
        .mapToDouble(item -> item.getRate() * item.getQty())
        .sum();

    double discountAmount = itemsTotal * (discountPercentage / 100);
    double afterDiscount = itemsTotal - discountAmount;

    double taxAmount = afterDiscount * (taxPercentage / 100);

    return afterDiscount + taxAmount;
}
// ALL TESTS PASS!

// Step 10: REFACTOR - Extract methods
public double calculateTotal(Bill bill) {
    validateBill(bill);

    if (bill.getItems() == null || bill.getItems().isEmpty()) {
        return 0.0;
    }

    double itemsTotal = calculateItemsTotal(bill);
    double afterDiscount = applyDiscount(itemsTotal, bill.getDiscountPercentage());
    double total = applyTax(afterDiscount, bill.getTaxPercentage());

    return total;
}

private void validateBill(Bill bill) {
    if (bill == null) {
        throw new IllegalArgumentException("Bill cannot be null");
    }

    validatePercentage("Discount", bill.getDiscountPercentage());
    validatePercentage("Tax", bill.getTaxPercentage());
}

private void validatePercentage(String name, Double percentage) {
    if (percentage != null && (percentage < 0 || percentage > 100)) {
        throw new IllegalArgumentException(
            name + " must be between 0 and 100, got: " + percentage
        );
    }
}

private double calculateItemsTotal(Bill bill) {
    return bill.getItems().stream()
        .mapToDouble(item -> item.getRate() * item.getQty())
        .sum();
}

private double applyDiscount(double total, Double discountPercentage) {
    if (discountPercentage == null || discountPercentage == 0) {
        return total;
    }
    double discountAmount = total * (discountPercentage / 100);
    return total - discountAmount;
}

private double applyTax(double total, Double taxPercentage) {
    if (taxPercentage == null || taxPercentage == 0) {
        return total;
    }
    double taxAmount = total * (taxPercentage / 100);
    return total + taxAmount;
}
// ALL TESTS STILL PASS! Refactoring succeeded.
```

**Benefits of TDD process:**
1. Clear requirements from tests
2. Implementation driven by tests
3. Edge cases discovered early
4. Refactoring safe (tests catch breaks)
5. Code is testable by design
6. Living documentation (tests show usage)

### Anti-Pattern: Test After (Not TDD)

**What we did wrong:**

```java
// Step 1: Write all the code first
public class BillCalculator {
    public double calculateTotal(Bill bill) {
        // 150 lines of complex logic
        // Multiple nested ifs
        // Edge cases "handled"
        // No tests guiding design
        double total = 0;
        if (bill != null) {
            if (bill.getItems() != null) {
                for (BillItem item : bill.getItems()) {
                    if (item != null) {
                        double itemTotal = item.getRate() * item.getQty();
                        if (bill.hasDiscount()) {
                            // Complex discount logic
                            if (item.isDiscountable()) {
                                // Item-level discount
                                if (bill.getCustomerType() == VIP) {
                                    // Special VIP logic
                                    // ... 50 more lines
                                }
                            }
                        }
                        total += itemTotal;
                    }
                }
                if (bill.hasTax()) {
                    // Complex tax logic
                    // ... 40 more lines
                }
            }
        }
        return total;
    }
}

// Step 2: Try to write tests (AFTER code is done)
@Test
public void testCalculateTotal() {
    // How do I test this?
    // So many paths...
    // So many edge cases...
    // Need to mock Bill, BillItem, Customer...
    // Tests become complex

    Bill bill = mock(Bill.class);
    BillItem item = mock(BillItem.class);
    Customer customer = mock(Customer.class);
    // ... 50 lines of mocking

    when(bill.getItems()).thenReturn(Arrays.asList(item));
    when(item.getRate()).thenReturn(100.0);
    when(item.getQty()).thenReturn(2.0);
    when(bill.hasDiscount()).thenReturn(true);
    when(item.isDiscountable()).thenReturn(true);
    when(bill.getCustomerType()).thenReturn(VIP);
    // ... 30 more mocks

    double total = calculator.calculateTotal(bill);

    // What should total be? Unclear from complex logic
    assertEquals(???, total); // Don't know expected value!
}

// Problems:
// 1. Code designed without testability in mind
// 2. Complex dependencies make testing hard
// 3. Don't know expected values (no spec from test)
// 4. Many code paths, hard to cover all
// 5. Refactoring risky (no tests to guide)
```

**Real impact:**
```
Time to write code: 4 hours
Time to write tests after: 6 hours (harder than code!)
Test coverage: 45% (too complex to test all paths)
Bugs found in production: 8
Time to fix bugs: 12 hours
Total time: 22 hours

If TDD used:
Time to write tests: 3 hours
Time to write code: 3 hours (guided by tests)
Test coverage: 95% (tests drove design)
Bugs found in production: 1
Time to fix: 0.5 hours
Total time: 6.5 hours

TDD saved: 15.5 hours (70% time savings)
```

### TDD Best Practices

**1. Test one thing at a time:**
```java
// Good - focused test
@Test
public void calculateTotal_withDiscount_appliesDiscountCorrectly() {
    Bill bill = createBillWithItems(100.0);
    bill.setDiscountPercentage(10.0);

    double total = calculator.calculateTotal(bill);

    assertEquals(90.0, total, 0.01);
}

// Bad - testing multiple things
@Test
public void testEverything() {
    // Tests discount, tax, validation, edge cases all in one
    // If it fails, what broke?
}
```

**2. Use descriptive test names:**
```java
// Good - tells you what it tests
testCalculateTotal_withNegativeDiscount_throwsIllegalArgumentException()
testCalculateTotal_withEmptyBill_returnsZero()
testCalculateTotal_withDiscountAndTax_appliesBothInCorrectOrder()

// Bad - unclear
testCalculate1()
testCalculate2()
testEdgeCase()
```

**3. Follow Arrange-Act-Assert:**
```java
@Test
public void testName() {
    // Arrange - Set up test data
    Bill bill = new Bill();
    bill.addItem(new BillItem("Test", 100.0, 1));

    // Act - Execute the code under test
    double total = calculator.calculateTotal(bill);

    // Assert - Verify the result
    assertEquals(100.0, total, 0.01);
}
```

**4. Test behavior, not implementation:**
```java
// Good - tests what it does
@Test
public void calculateTotal_sumsPricesOfAllItems() {
    Bill bill = createBill(
        item(100.0),
        item(200.0),
        item(300.0)
    );

    assertEquals(600.0, calculator.calculateTotal(bill), 0.01);
}

// Bad - tests how it does it
@Test
public void calculateTotal_usesStreamAPI() {
    // Don't test implementation details
    // What if we change from stream to loop?
    // Test would break even though behavior is same
}
```

### Key Takeaways

**Do:**
- Write test first (red)
- Write minimal code to pass (green)
- Refactor with confidence (tests protect)
- Test behavior, not implementation
- Keep tests simple and focused
- Use descriptive test names

**Don't:**
- Write code before tests
- Skip tests "because I'm in a hurry"
- Test implementation details
- Write complex, fragile tests
- Skip refactoring step

**The Test:** Can you understand requirements by reading tests?

**Remember:** "Code without tests is bad code. It doesn't matter how well written it is; it doesn't matter how pretty or object-oriented or well-encapsulated it is. With tests, we can change the behavior of our code quickly and verifiably. Without them, we really don't know if our code is getting better or worse." — Michael Feathers

---

*[Continuing with remaining principles in next section...]*