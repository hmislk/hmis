# Deployment in a Development Environment

## Overview

HMIS is a Java EE web application that serves as both the web interface and RESTful API for hospital management. This guide will help you set up a complete local development environment.

## Technology Stack

### Required Components

| Component | Version/Details | Notes |
|-----------|----------------|--------|
| **Operating System** | Windows 10/11, Linux (Ubuntu 20.04+/22.04+), macOS | Any modern OS |
| **Java Development Kit (JDK)** | JDK 11 (OpenJDK or Oracle) | **Required** - JDK 8 is outdated |
| **Application Server** | Payara Server 5.2022.5 | Recommended and tested version |
| **Database** | MySQL 8.0+ or MariaDB 10.5+ | MySQL 8.0 recommended |
| **Build Tool** | Maven 3.6+ | Usually bundled with NetBeans |
| **IDE** | NetBeans 16+, IntelliJ IDEA, or Eclipse | NetBeans recommended for beginners |
| **Version Control** | Git 2.30+ | Required for source code management |

### Optional but Recommended

- **GitHub CLI** (`gh`) - For working with pull requests and issues
- **MySQL Workbench** - For database management and visualization
- **Postman or similar** - For API testing

## Platform-Specific Setup

### Windows Setup

#### 1. Install Java Development Kit (JDK 11)

**Option A: Download from Oracle or Adoptium**
1. Download JDK 11 from [Adoptium (Eclipse Temurin)](https://adoptium.net/temurin/releases/?version=11)
2. Run the installer and complete the installation
3. Verify installation:
   ```cmd
   java -version
   ```
   Expected output: `openjdk version "11.x.x"`

**Option B: Using Package Manager (Chocolatey)**
```cmd
choco install temurin11
```

#### 2. Install Git

Download and install from [git-scm.com](https://git-scm.com/download/win) or use Chocolatey:
```cmd
choco install git
```

#### 3. Install MySQL Server

**Option A: Download MySQL Installer**
1. Download [MySQL Community Server 8.0+](https://dev.mysql.com/downloads/mysql/)
2. Run the installer and choose "Developer Default" configuration
3. Set a root password (remember this for later!)
4. Complete the installation wizard

**Option B: Using Chocolatey**
```cmd
choco install mysql
```

**Configure MySQL:**
1. Create database and user:
   ```sql
   CREATE DATABASE hmis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE DATABASE hmisaudit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'hmis'@'localhost' IDENTIFIED BY 'hmis';
   GRANT ALL PRIVILEGES ON hmis.* TO 'hmis'@'localhost';
   GRANT ALL PRIVILEGES ON hmisaudit.* TO 'hmis'@'localhost';
   FLUSH PRIVILEGES;
   ```

#### 4. Install NetBeans IDE

1. Download [NetBeans 16+ with JDK Bundle](https://netbeans.apache.org/download/index.html)
2. Choose "All" or "Java EE" bundle
3. Install to default location (e.g., `C:\Program Files\NetBeans-16`)
4. Maven is bundled with NetBeans - no separate installation needed

#### 5. Install Payara Server

1. Download [Payara Server 5.2022.5](https://nexus.payara.fish/#browse/browse:payara-community:fish%2Fpayara%2Fdistributions%2Fpayara%2F5.2022.5%2Fpayara-5.2022.5.zip)
2. Extract to a permanent location (e.g., `D:\Payara` or `C:\Payara`)
3. Configure Java path in `<PAYARA_HOME>\glassfish\config\asenv.bat`:
   ```batch
   set AS_JAVA=C:\Program Files\Java\jdk-11.0.x
   ```
   Or if using Adoptium:
   ```batch
   set AS_JAVA=C:\Program Files\Eclipse Adoptium\jdk-11.x.x-hotspot
   ```

4. Download [MySQL Connector/J 8.0.30+](https://mvnrepository.com/artifact/mysql/mysql-connector-java/8.0.30)
5. Copy `mysql-connector-java-8.0.30.jar` to `<PAYARA_HOME>\glassfish\lib\`

6. Start Payara:
   ```cmd
   cd D:\Payara\payara5\bin
   asadmin start-domain domain1
   ```

7. Access Admin Console at `http://localhost:4848`

### Linux Setup (Ubuntu/Debian)

#### 1. Install JDK 11

```bash
sudo apt update
sudo apt install openjdk-11-jdk
java -version
```

#### 2. Install Git

```bash
sudo apt install git
git --version
```

#### 3. Install MySQL Server

```bash
sudo apt install mysql-server
sudo mysql_secure_installation
```

**Configure MySQL:**
```bash
sudo mysql -u root -p
```

Then run:
```sql
CREATE DATABASE hmis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE hmisaudit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'hmis'@'localhost' IDENTIFIED BY 'hmis';
GRANT ALL PRIVILEGES ON hmis.* TO 'hmis'@'localhost';
GRANT ALL PRIVILEGES ON hmisaudit.* TO 'hmis'@'localhost';
FLUSH PRIVILEGES;
```

#### 4. Install Maven (if not using NetBeans bundled version)

```bash
sudo apt install maven
mvn -version
```

#### 5. Install NetBeans IDE

```bash
# Download NetBeans from apache.org
wget https://archive.apache.org/dist/netbeans/netbeans/16/netbeans-16-bin.zip
unzip netbeans-16-bin.zip -d ~/
```

Or install via Snap:
```bash
sudo snap install netbeans --classic
```

#### 6. Install Payara Server

```bash
# Download Payara
cd ~
wget https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/5.2022.5/payara-5.2022.5.zip
unzip payara-5.2022.5.zip -d ~/

# Configure Java path
echo 'AS_JAVA="/usr/lib/jvm/java-11-openjdk-amd64"' >> ~/payara5/glassfish/config/asenv.conf

# Download MySQL connector
wget https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.30/mysql-connector-java-8.0.30.jar
cp mysql-connector-java-8.0.30.jar ~/payara5/glassfish/lib/

# Start Payara
cd ~/payara5/bin
./asadmin start-domain domain1
```

### macOS Setup

#### 1. Install Homebrew (if not already installed)

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

#### 2. Install JDK 11

```bash
brew install openjdk@11
sudo ln -sfn $(brew --prefix)/opt/openjdk@11/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-11.jdk
java -version
```

#### 3. Install Git

```bash
brew install git
```

#### 4. Install MySQL

```bash
brew install mysql
brew services start mysql
mysql_secure_installation
```

Then create databases as shown in the Linux section.

#### 5. Install Maven

```bash
brew install maven
```

#### 6. Install NetBeans and Payara

Follow similar steps as Linux, downloading from official websites.

## Getting Started with Development

### Step 1: Clone the Repository

```bash
# Clone from GitHub
git clone https://github.com/hmislk/hmis.git
cd hmis
```

### Step 2: Configure Payara JDBC Resources

1. Open Payara Admin Console: `http://localhost:4848`
2. Navigate to **Resources > JDBC > JDBC Connection Pools**
3. Click **New** and create a connection pool:
   - **Pool Name**: `hmisPool`
   - **Resource Type**: `javax.sql.DataSource`
   - **Database Driver Vendor**: `MySQL`
   - Click **Next**

4. Configure Additional Properties:
   - **ServerName**: `localhost`
   - **Port**: `3306`
   - **DatabaseName**: `hmis`
   - **User**: `hmis`
   - **Password**: `hmis`
   - **URL**: `jdbc:mysql://localhost:3306/hmis?zeroDateTimeBehavior=convertToNull&useSSL=false`

5. Click **Ping** to test the connection - should show success
6. Click **Finish**

7. Create JNDI Resource:
   - Navigate to **Resources > JDBC > JDBC Resources**
   - Click **New**
   - **JNDI Name**: `jdbc/hmis` (or your preferred name like `jdbc/coop`)
   - **Pool Name**: Select `hmisPool`
   - Click **OK**

8. Repeat steps 3-7 to create audit database connection:
   - **Pool Name**: `hmisAuditPool`
   - **DatabaseName**: `hmisaudit`
   - **JNDI Name**: `jdbc/hmisaudit` (or `jdbc/ruhunuAudit`)

### Step 3: Configure persistence.xml

**IMPORTANT**: For local development, you can use hardcoded JNDI names, but **NEVER commit these to GitHub**.

1. Open `src/main/resources/META-INF/persistence.xml`
2. For **local development only**, you can temporarily change:
   ```xml
   <jta-data-source>${JDBC_DATASOURCE}</jta-data-source>
   ```
   to:
   ```xml
   <jta-data-source>jdbc/hmis</jta-data-source>
   ```

   And:
   ```xml
   <jta-data-source>${JDBC_AUDIT_DATASOURCE}</jta-data-source>
   ```
   to:
   ```xml
   <jta-data-source>jdbc/hmisaudit</jta-data-source>
   ```

**WARNING**: Before committing any changes, you MUST revert these to use environment variables (`${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}`), otherwise QA deployments will fail.

See the [Persistence Verification Guide](https://github.com/hmislk/hmis/blob/development/developer_docs/deployment/persistence-verification.md) for detailed instructions.

### Step 4: Build the Project

#### Using the Detection Script (Recommended)

The project includes a Maven auto-detection script:

**Windows (Git Bash):**
```bash
./detect-maven.sh clean package -DskipTests
```

**Windows (Command Prompt):**
```cmd
detect-maven.bat clean package -DskipTests
```

**Linux/macOS:**
```bash
./detect-maven.sh clean package -DskipTests
```

#### Using Maven Directly

```bash
mvn clean package -DskipTests
```

**Build Output**: After successful build, the WAR file will be in `target/rh-3.0.0.war`

### Step 5: Deploy to Payara

#### Option A: Using NetBeans IDE

1. Right-click the project in NetBeans
2. Select **Clean and Build**
3. Right-click again and select **Run** or **Deploy**
4. NetBeans will automatically deploy to the configured server

#### Option B: Using Payara Admin Console

1. Open `http://localhost:4848`
2. Navigate to **Applications**
3. Click **Deploy**
4. Browse to `target/rh-3.0.0.war`
5. Set **Context Root** to `/` or `/hmis`
6. Click **OK**

#### Option C: Using Command Line

```bash
# Windows
D:\Payara\payara5\bin\asadmin deploy --force --contextroot /hmis target/rh-3.0.0.war

# Linux/macOS
~/payara5/bin/asadmin deploy --force --contextroot /hmis target/rh-3.0.0.war
```

### Step 6: Access the Application

Open your browser and navigate to:
```
http://localhost:8080/hmis/faces/index1.xhtml
```

Or if you deployed with root context:
```
http://localhost:8080/faces/index1.xhtml
```

## First-Time Setup

1. The application will automatically create database tables on first deployment
2. Create your first institution and user through the initial setup wizard
3. Login with the created credentials

## Development Workflow

### Running Tests

```bash
# Run all tests
./detect-maven.sh test

# Run specific tests
./detect-maven.sh test -Dtest="*BigDecimal*Test"

# Run tests in quiet mode
./detect-maven.sh test -q
```

### Hot Reload During Development

**NetBeans:**
- Enable "Deploy on Save" in project properties
- Changes to Java files require rebuild, but JSF/XHTML changes are reflected immediately

**Manual Redeploy:**
```bash
./detect-maven.sh clean package -DskipTests
asadmin undeploy rh
asadmin deploy target/rh-3.0.0.war
```

### Viewing Server Logs

**Windows:**
```
D:\Payara\glassfish\domains\domain1\logs\server.log
```

**Linux/macOS:**
```
~/payara5/glassfish/domains/domain1/logs/server.log
```

Tail logs in real-time:
```bash
# Linux/macOS
tail -f ~/payara5/glassfish/domains/domain1/logs/server.log

# Windows (PowerShell)
Get-Content D:\Payara\glassfish\domains\domain1\logs\server.log -Wait -Tail 50
```

### Database Credentials Security

**CRITICAL**: Never commit database credentials to git.

Store credentials in a separate location:
- **Windows**: `C:\Credentials\credentials.txt`
- **Linux/macOS**: `~/.config/hmis/credentials.txt`

See the [MySQL Developer Guide](https://github.com/hmislk/hmis/blob/development/developer_docs/database/mysql-developer-guide.md) for details.

## Working with Git and GitHub

### Daily Workflow

```bash
# Create a feature branch from development
git checkout development
git pull origin development
git checkout -b feature/my-new-feature

# Make changes, then commit
git add .
git commit -m "Add new feature

Closes #123"

# Push to your fork or branch
git push origin feature/my-new-feature
```

### Creating Pull Requests

Use GitHub CLI for easier PR management:

```bash
# Install GitHub CLI
# Windows: winget install --id GitHub.cli
# macOS: brew install gh
# Linux: See https://github.com/cli/cli#installation

# Authenticate
gh auth login

# Create PR
gh pr create --base development --title "Add new feature" --body "Description..."
```

See the [Commit Conventions](https://github.com/hmislk/hmis/blob/development/developer_docs/git/commit-conventions.md) for detailed guidelines.

## CI/CD and QA Deployment

The project uses GitHub Actions for automated deployment to QA environments.

### QA Environments

| Environment | URL | Branch |
|-------------|-----|--------|
| QA1 | https://qa.carecode.org/qa1 | `hims-qa1` |
| QA2 | https://qa.carecode.org/qa2 | `hims-qa2` |
| QA3 | https://qa.carecode.org/qa3 | `hims-qa3` |

See the [QA Deployment Guide](https://github.com/hmislk/hmis/blob/development/developer_docs/deployment/qa-deployment-guide.md) for deployment instructions.

## Troubleshooting

### Port Already in Use

**Error:** `Port 8080 is already in use`

**Solution:**
```bash
# Find the process using port 8080
# Windows
netstat -ano | findstr :8080
taskkill /PID [PID_NUMBER] /F

# Linux/macOS
lsof -i :8080
kill -9 [PID_NUMBER]
```

### Database Connection Failed

**Error:** `Cannot establish connection to jdbc/hmis`

**Solutions:**
1. Verify MySQL is running:
   ```bash
   # Windows
   net start MySQL80

   # Linux/macOS
   sudo systemctl status mysql
   ```

2. Test connection from MySQL client:
   ```bash
   mysql -u hmis -p -h localhost hmis
   ```

3. Verify JDBC connection pool in Payara Admin Console (Ping test)

4. Check MySQL connector JAR is in `<PAYARA_HOME>/glassfish/lib/`

### Build Fails with "Maven Not Found"

**Solution:**
Use the auto-detection script which finds Maven automatically:
```bash
./detect-maven.sh clean package
```

Or add Maven to your PATH manually.

### Application Doesn't Start After Deployment

**Check:**
1. Review server logs: `<PAYARA_HOME>/glassfish/domains/domain1/logs/server.log`
2. Verify persistence.xml uses correct JNDI names
3. Ensure both databases (`hmis` and `hmisaudit`) exist
4. Check JDBC resources are properly configured in Payara

### Persistence.xml Environment Variable Errors

If you see errors about `${JDBC_DATASOURCE}` not being found:

**For local development**, you have two options:

**Option 1 - Use hardcoded JNDI (simpler, but requires vigilance):**
- Change `${JDBC_DATASOURCE}` to `jdbc/hmis` in persistence.xml
- Change `${JDBC_AUDIT_DATASOURCE}` to `jdbc/hmisaudit`
- **NEVER commit these changes** - use `git stash` or scripts to manage

**Option 2 - Configure environment variables (safer for teams):**
- Set system environment variables that Payara will read
- See Payara documentation on how to set domain-level variables

## Additional Resources

### Official Documentation

- **HMIS Developer Documentation**: [developer_docs/](https://github.com/hmislk/hmis/tree/development/developer_docs)
- **Payara Server Documentation**: https://docs.payara.fish/
- **MySQL Documentation**: https://dev.mysql.com/doc/
- **Maven Documentation**: https://maven.apache.org/guides/

### Project-Specific Guides

- [DTO Implementation Guidelines](https://github.com/hmislk/hmis/blob/development/developer_docs/dto/implementation-guidelines.md)
- [UI Development Guidelines](https://github.com/hmislk/hmis/blob/development/developer_docs/ui/comprehensive-ui-guidelines.md)
- [JSF AJAX Update Guidelines](https://github.com/hmislk/hmis/blob/development/developer_docs/jsf/ajax-update-guidelines.md)
- [Testing with Maven](https://github.com/hmislk/hmis/blob/development/developer_docs/testing/maven-commands.md)

### Community Support

- **GitHub Issues**: https://github.com/hmislk/hmis/issues
- **GitHub Discussions**: https://github.com/hmislk/hmis/discussions
- **Project Wiki**: https://github.com/hmislk/hmis/wiki

## Best Practices for Local Development

1. **Never commit credentials** - use separate credential files
2. **Always verify persistence.xml** before pushing - use environment variables
3. **Run tests before committing** - use `./detect-maven.sh test`
4. **Follow commit conventions** - include issue numbers (`Closes #123`)
5. **Keep branches up to date** - regularly merge from `development`
6. **Use feature branches** - never commit directly to `development` or `main`
7. **Test UI changes** in different browsers (Chrome, Firefox, Edge)
8. **Monitor server logs** when debugging issues

## Quick Reference Commands

### Essential Commands

```bash
# Start Payara
asadmin start-domain domain1

# Stop Payara
asadmin stop-domain domain1

# Restart Payara
asadmin restart-domain domain1

# Build project
./detect-maven.sh clean package -DskipTests

# Run tests
./detect-maven.sh test

# Deploy application
asadmin deploy --force target/rh-3.0.0.war

# Undeploy application
asadmin undeploy rh

# View Payara logs
tail -f <PAYARA_HOME>/glassfish/domains/domain1/logs/server.log
```

### Git Commands

```bash
# Switch to development branch
git checkout development

# Pull latest changes
git pull origin development

# Create feature branch
git checkout -b feature/my-feature

# Commit changes
git add .
git commit -m "Description

Closes #123"

# Push changes
git push origin feature/my-feature
```

---

**Last Updated**: November 2025
**Maintained By**: HMIS Development Team
**Questions?** Create an issue on GitHub: https://github.com/hmislk/hmis/issues

## Changelog

### November 2025
- Updated to reflect Payara 5.2022.5 (latest tested version)
- Updated JDK requirement to JDK 11
- Added platform-specific setup instructions (Windows, Linux, macOS)
- Added CI/CD and QA deployment information
- Added persistence.xml security guidelines
- Added troubleshooting section
- Added quick reference commands
- Removed outdated Ubuntu 18.04 and JDK 1.8 references
- Added links to detailed developer documentation
