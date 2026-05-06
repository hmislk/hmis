#!/bin/bash

# Script to restore local JNDI names after GitHub push
# This restores your local development configuration

PERSISTENCE_FILE="src/main/resources/META-INF/persistence.xml"

if [ ! -f ".jndi-backup-main" ] || [ ! -f ".jndi-backup-audit" ]; then
    echo "❌ No JNDI backup found. Cannot restore."
    echo "💡 Manually edit $PERSISTENCE_FILE to set your local JNDI names"
    exit 1
fi

# Read backed up JNDI names
MAIN_JNDI=$(cat .jndi-backup-main)
AUDIT_JNDI=$(cat .jndi-backup-audit)

echo "🔄 Restoring local JNDI configuration:"
echo "   Main: $MAIN_JNDI"
echo "   Audit: $AUDIT_JNDI"

# Restore original JNDI names
sed -i "s|<jta-data-source>\${JDBC_DATASOURCE}</jta-data-source>|<jta-data-source>$MAIN_JNDI</jta-data-source>|g" "$PERSISTENCE_FILE"
sed -i "s|<jta-data-source>\${JDBC_AUDIT_DATASOURCE}</jta-data-source>|<jta-data-source>$AUDIT_JNDI</jta-data-source>|g" "$PERSISTENCE_FILE"

# Clean up backup files
rm .jndi-backup-main .jndi-backup-audit

echo "✅ Local JNDI configuration restored"
echo "💻 Ready for local development!"