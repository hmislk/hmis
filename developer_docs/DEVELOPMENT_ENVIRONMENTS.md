# Development Environment Configurations

This document records the development environment setups used by team members to help with troubleshooting and consistency.

## Maven Installation Locations

### By Developer/Machine

#### cclap (Reference Name)
- **Actual Computer Name**: `CARECODE-LAP`
- **User**: `buddhika`
- **Maven Location**: `C:\Program Files\NetBeans-16\netbeans\java\maven`
- **IDE**: NetBeans 16 (Maven bundled)
- **OS**: Windows
- **Payara Server**: `C:\Users\buddhika\Payara_Server` (Domain 1)

#### hiulap (Reference Name)
- **Actual Computer Name**: `hiu-laptop`
- **User**: `buddhika`
- **Maven Location**: NetBeans bundled Maven (`/usr/lib/apache-netbeans/java/maven`)
- **IDE**: NetBeans 16
- **OS**: Linux Ubuntu 24.04
- **JDK**: OpenJDK 11 (`/usr/lib/jvm/java-11-openjdk-amd64`)
- **Payara Server**: `/home/buddhika/payara`

#### hiud (Reference Name)
- **Actual Computer Name**: _[To be documented - run `hostname` command]_
- **User**: _[To be documented - run `whoami` command]_
- **Maven Location**: _[To be documented]_
- **IDE**: _[To be documented]_
- **OS**: _[To be documented]_

#### ccd (Reference Name)
- **Actual Computer Name**: _[To be documented - run `hostname` command]_
- **User**: _[To be documented - run `whoami` command]_
- **Maven Location**: _[To be documented]_
- **IDE**: _[To be documented]_
- **OS**: _[To be documented]_

## Auto-Detection Scripts

For convenience, use these scripts that automatically detect your machine and use the correct Maven:

### Windows
```cmd
# Run tests
detect-maven.bat test

# Run specific tests
detect-maven.bat test -Dtest="*BigDecimal*Test"

# Check Maven version
detect-maven.bat --version
```

### Linux/Mac/Git Bash
```bash
# Run tests
./detect-maven.sh test

# Run specific tests  
./detect-maven.sh test -Dtest="*BigDecimal*Test"

# Check Maven version
./detect-maven.sh --version
```

## Testing Commands by Environment

### NetBeans with Bundled Maven (cclap)
```bash
# Use NetBeans Maven
"C:\Program Files\NetBeans-16\netbeans\java\maven\bin\mvn.cmd" test

# Or if NetBeans bin is in PATH
mvn test
```

### Standalone Maven Installation
```bash
# Standard Maven command
mvn test

# Or with wrapper if available
./mvnw test
```

## Environment Variables

### Maven Home Settings
Add these to your system or user environment variables:

```cmd
MAVEN_HOME=C:\Program Files\NetBeans-16\netbeans\java\maven
PATH=%PATH%;%MAVEN_HOME%\bin
```

## IDE-Specific Notes

### NetBeans
- Maven is bundled with NetBeans installation
- No separate Maven installation required
- Maven commands can be run through IDE or command line

### IntelliJ IDEA
- Can use bundled Maven or external installation
- Configure in Settings > Build Tools > Maven

### Eclipse
- Can use bundled Maven (m2e) or external installation
- Configure in Preferences > Maven

## Troubleshooting

### Maven Not Found Error
If you get `mvn: command not found` or `mvn is not recognized`:

1. **Check if Maven is in PATH**:
   ```bash
   echo $PATH  # Linux/Mac
   echo %PATH% # Windows
   ```

2. **Use full path to Maven**:
   ```bash
   "C:\Program Files\NetBeans-16\netbeans\java\maven\bin\mvn.cmd" --version
   ```

3. **Add Maven to PATH** (Windows):
   - Add `C:\Program Files\NetBeans-16\netbeans\java\maven\bin` to your PATH environment variable

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test classes
mvn test -Dtest="*BigDecimal*Test"

# Run tests quietly
mvn test -q

# Skip tests during build
mvn compile -DskipTests
```

## Contributing

When setting up a new development environment, please update this document with:
1. Your machine/developer identifier
2. Maven installation location
3. IDE version and configuration
4. Any specific setup notes

---

*Last updated: July 24, 2025*
*Maintained by: Development Team*