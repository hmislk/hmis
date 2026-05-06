# Maven Testing Commands

## Environment-Specific Maven Locations
Different development machines have Maven installed in different locations.

**Machine Detection**: Use `hostname` and `whoami` commands to identify the current machine.

### Known Configurations:
- **cclap** (Computer: `<HOSTNAME>`, User: `<USERNAME>`):
  - Maven: `<NB_MAVEN_PATH>`
  - Payara Server: `<PAYARA_SERVER_PATH>` (Domain 1)
- **hiulap** (Computer: `_[TBD]_`, User: `_[TBD]_`): _[To be documented]_
- **hiud** (Computer: `_[TBD]_`, User: `_[TBD]_`): _[To be documented]_ 
- **ccd** (Computer: `_[TBD]_`, User: `_[TBD]_`): _[To be documented]_

## Testing Commands to Try (in order of preference)
When running tests, try these commands in order until one works:

1. **Standard Maven** (if in PATH):
   ```bash
   mvn test
   ```

2. **NetBeans Bundled Maven** (cclap):
   ```bash
   "<NB_MAVEN_PATH>" test
   ```

3. **Maven Wrapper** (if available):
   ```bash
   ./mvnw test        # Linux/Mac
   ./mvnw.cmd test    # Windows
   ```

4. **Specific Test Classes**:
   ```shell
   mvn test -Dtest="*BigDecimal*Test"
   ```

## Note for Claude
**PREFERRED APPROACH**: Use the auto-detection script:
1. **First try**: `./detect-maven.sh test` (automatically detects machine and uses correct Maven)
2. **If script fails**: Fall back to manual detection:
   - Run `hostname` and `whoami` to identify machine
   - Try standard `mvn test`
   - If Maven not found, use machine-specific path based on hostname
   - For `<HOSTNAME>` (cclap): Use `"<NB_MAVEN_PATH>" test`

**The detect-maven.sh script handles all this automatically and should be the first choice.**
