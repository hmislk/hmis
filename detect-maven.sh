#!/bin/bash
# Auto-detect Maven location based on machine
# Usage: ./detect-maven.sh [maven-args]
# Example: ./detect-maven.sh test -Dtest="*BigDecimal*Test"

echo "Detecting machine configuration..."
echo "Computer: $(hostname)"
echo "User: $(whoami)"
echo ""

# Check if standard Maven is available
if command -v mvn >/dev/null 2>&1; then
    echo "Using standard Maven from PATH"
    mvn "$@"
    exit $?
fi

# Machine-specific Maven paths
HOSTNAME=$(hostname)
case "$HOSTNAME" in
    "CARECODE-LAP"|"carecode-lap")
        echo "Using NetBeans bundled Maven for cclap machine"
        "/c/Program Files/NetBeans-20/netbeans/java/maven/bin/mvn.cmd" "$@"
        exit $?
        ;;
    # Add other machines here as they are documented
    # "OTHER-MACHINE"|"other-machine")
    #     echo "Using Maven for other machine"
    #     "/path/to/maven/bin/mvn" "$@"
    #     exit $?
    #     ;;
esac

echo "ERROR: Maven not found and no machine-specific configuration available"
echo "Please update detect-maven.sh with your Maven location"
echo "Current machine: $HOSTNAME"
echo "Current user: $(whoami)"
exit 1