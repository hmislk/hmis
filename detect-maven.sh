#!/bin/bash
# Auto-detect Maven location based on machine
# Usage: ./detect-maven.sh [maven-args]
# Example: ./detect-maven.sh test -Dtest="*BigDecimal*Test"

echo "Detecting machine configuration..."
echo "Computer: $(hostname)"
echo "User: $(whoami)"
echo ""

# Machine-specific Maven paths (prioritized)
HOSTNAME=$(hostname)
case "$HOSTNAME" in
    "CARECODE-LAP"|"carecode-lap")
        echo "Using NetBeans bundled Maven for cclap machine"
        "/c/Program Files/NetBeans-16/netbeans/java/maven/bin/mvn.cmd" "$@"
        exit $?
        ;;
    "BuddhikaDesktop"|"buddhikadesktop")
        echo "Using NetBeans bundled Maven for BuddhikaDesktop machine"
        export JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
        export PATH="$JAVA_HOME/bin:$PATH"
        "/d/Program Files/NetBeans-18/netbeans/java/maven/bin/mvn.cmd" "$@"
        exit $?
        ;;
    "hiu-laptop"|"HIU-LAPTOP")
        echo "Using NetBeans bundled Maven for hiu-laptop Linux machine"
        export JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
        export PATH="$JAVA_HOME/bin:$PATH"
        /usr/lib/apache-netbeans/java/maven/bin/mvn "$@"
        exit $?
        ;;
    # Add other machines here as they are documented
    # "OTHER-MACHINE"|"other-machine")
    #     echo "Using Maven for other machine"
    #     "/path/to/maven/bin/mvn" "$@"
    #     exit $?
    #     ;;
esac

# Fallback to standard Maven if available
if command -v mvn >/dev/null 2>&1; then
    echo "Using standard Maven from PATH as fallback"
    mvn "$@"
    exit $?
fi

echo "ERROR: Maven not found and no machine-specific configuration available"
echo "Please update detect-maven.sh with your Maven location"
echo "Current machine: $HOSTNAME"
echo "Current user: $(whoami)"
exit 1