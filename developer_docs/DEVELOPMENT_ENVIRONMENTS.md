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
- **Payara Domain**: _[To be documented]_
- **Payara Admin Port**: _[To be documented]_
- **Payara HTTP Port**: _[To be documented]_
- **Deployed App Name / Context Root**: _[To be documented]_

#### hiud (Reference Name)
- **Actual Computer Name**: _[To be documented - run `hostname` command]_
- **User**: _[To be documented - run `whoami` command]_
- **Maven Location**: _[To be documented]_
- **IDE**: _[To be documented]_
- **OS**: _[To be documented]_
- **Payara Domain / Admin Port / HTTP Port**: _[To be documented]_

#### ccd (Reference Name)
- **Actual Computer Name**: _[To be documented - run `hostname` command]_
- **User**: _[To be documented - run `whoami` command]_
- **Maven Location**: _[To be documented]_
- **IDE**: _[To be documented]_
- **OS**: _[To be documented]_
- **Payara Domain / Admin Port / HTTP Port**: _[To be documented]_

#### BuddhikaDesktop (Reference Name)
- **Actual Computer Name**: _[Do not document actual hostname per security guidelines]_
- **User**: _[Do not document actual username per security guidelines]_
- **Maven Location**: `D:\Program Files\NetBeans-18\netbeans\java\maven`
- **IDE**: NetBeans 18 (Maven bundled)
- **OS**: Windows 10
- **JDK for Payara/build**: JDK 11 (Eclipse Adoptium), `C:\Program Files\Eclipse Adoptium\jdk-11.0.23.9-hotspot` - **note**: `where java`/`JAVA_HOME` on this machine can resolve to a JDK 17 install instead; always verify the resolved `JAVA_HOME` against `pom.xml`'s `<release>` value (11 as of this writing) before building, since JDK 17 has been observed to crash under sandboxed execution here.
- **Payara Server**: `D:\Payara`
- **Payara Domain**: `domain1`
- **Payara Admin Port**: `4848` (default)
- **Payara HTTP Port**: `8080` (default)
- **Deployed App Name / Context Root**: `rh-3.0.0` (deployed by an earlier bare `asadmin deploy` without `--name`, so the name was derived from the WAR filename - this is a live, real-world instance of the name/context-root collision bug described in [local-sync-redeploy-script-guide.md](deployment/local-sync-redeploy-script-guide.md#app-name--context-root-collision-on-deploy))

#### carecode (Reference Name)
- **Actual Computer Name**: _[Do not document actual hostname per security guidelines]_
- **User**: _[Do not document actual username per security guidelines]_
- **OS**: Linux Ubuntu
- **Payara Server**: `/home/carecode/payara`
- **Payara Domain**: `rh`
- **Payara Admin Port**: `9048` (not the default 4848)
- **Payara HTTP Port**: `9080` (not the default 8080)
- **Deployed App Name / Context Root**: `rh` / `/rh` - see the collision bug in
  [local-sync-redeploy-script-guide.md](deployment/local-sync-redeploy-script-guide.md#app-name--context-root-collision-on-deploy)
  for what happens if a redeploy omits `--name`/`--contextroot` on this machine

## Local Sync-and-Redeploy Script

For a personal script that fetches `origin/development`, restores your local
JNDI settings, builds, and redeploys to your own Payara domain in one
command, see
[Local Sync-and-Redeploy Script Guide](deployment/local-sync-redeploy-script-guide.md)
and run `scripts/generate-sync-redeploy-script.sh` (Linux) or
`scripts\generate-sync-redeploy-script.bat` (Windows) once per machine.

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