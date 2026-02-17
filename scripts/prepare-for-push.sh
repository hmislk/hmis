#!/bin/bash

# Script to prepare persistence.xml for GitHub push
# Run this before pushing to ensure QA deployment compatibility

PERSISTENCE_FILE="src/main/resources/META-INF/persistence.xml"

if [ ! -f "$PERSISTENCE_FILE" ]; then
    echo "❌ persistence.xml not found at $PERSISTENCE_FILE"
    exit 1
fi

# Store current JNDI names for later restoration
MAIN_JNDI=$(grep -oP '<jta-data-source>\K[^<]*' "$PERSISTENCE_FILE" | head -1)
AUDIT_JNDI=$(grep -oP '<jta-data-source>\K[^<]*' "$PERSISTENCE_FILE" | tail -1)

echo "📝 Current JNDI configuration:"
echo "   Main: $MAIN_JNDI"
echo "   Audit: $AUDIT_JNDI"

# Store for restoration
echo "$MAIN_JNDI" > .jndi-backup-main
echo "$AUDIT_JNDI" > .jndi-backup-audit

# Replace with environment variables
sed -i "s|<jta-data-source>$MAIN_JNDI</jta-data-source>|<jta-data-source>\${JDBC_DATASOURCE}</jta-data-source>|g" "$PERSISTENCE_FILE"
sed -i "s|<jta-data-source>$AUDIT_JNDI</jta-data-source>|<jta-data-source>\${JDBC_AUDIT_DATASOURCE}</jta-data-source>|g" "$PERSISTENCE_FILE"

echo "✅ Replaced JNDI names with environment variables"
echo "🚀 Ready for GitHub push!"
echo ""
echo "After pushing, run: ./scripts/restore-local-jndi.sh"