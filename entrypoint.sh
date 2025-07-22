#!/bin/bash
set -e

echo "Setting up Payara admin and deploying application..."

DOMAIN_NAME="domain1"
DOMAIN_DIR="/opt/payara/appserver/glassfish/domains/$DOMAIN_NAME"
ADMIN_KEYFILE="${DOMAIN_DIR}/config/admin-keyfile"
WAR_PATH="/opt/payara/app.war"
LOG_PATH="${DOMAIN_DIR}/logs/server.log"

# Prepare password files
echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" > /tmp/password.txt
echo "AS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" > /tmp/newpass.txt
chmod 600 /tmp/password.txt /tmp/newpass.txt

# Start the domain (non-secure) if no admin-keyfile exists
if [ ! -f "$ADMIN_KEYFILE" ]; then
  echo "🟡 No admin-keyfile found. Starting domain to initialize..."
  /opt/payara/appserver/bin/asadmin start-domain
  sleep 15

  echo "🔐 Setting new admin password..."
  echo "yes" | /opt/payara/appserver/bin/asadmin change-admin-password --user admin --passwordfile /tmp/newpass.txt

  echo "🛡️ Enabling secure admin..."
  /opt/payara/appserver/bin/asadmin enable-secure-admin --passwordfile /tmp/password.txt

  echo "♻️ Restarting domain..."
  /opt/payara/appserver/bin/asadmin stop-domain
  /opt/payara/appserver/bin/asadmin start-domain
  sleep 15
else
  echo "✔️ Admin user already exists. Verifying password..."

  # Try logging in with passwordfile
  if ! /opt/payara/appserver/bin/asadmin list-applications --user admin --passwordfile /tmp/password.txt > /dev/null 2>&1; then
    echo "❌ Password mismatch or invalid admin-keyfile. Resetting..."

    echo "🧹 Stopping domain..."
    /opt/payara/appserver/bin/asadmin stop-domain || true

    echo "🗑️ Deleting admin-keyfile..."
    rm -f "$ADMIN_KEYFILE"

    echo "🔁 Restarting domain to reconfigure admin..."
    /opt/payara/appserver/bin/asadmin start-domain
    sleep 15

    echo "🔐 Setting new admin password..."
    echo "yes" | /opt/payara/appserver/bin/asadmin change-admin-password --user admin --passwordfile /tmp/newpass.txt

    echo "🛡️ Enabling secure admin..."
    /opt/payara/appserver/bin/asadmin enable-secure-admin --passwordfile /tmp/password.txt

    echo "♻️ Restarting domain..."
    /opt/payara/appserver/bin/asadmin stop-domain
    /opt/payara/appserver/bin/asadmin start-domain
    sleep 15
  fi
fi

echo "🚀 Deploying application..."
/opt/payara/appserver/bin/asadmin --user admin --passwordfile /tmp/password.txt \
  deploy --contextroot="${CONTEXT_PATH}" --force=true "${WAR_PATH}"

echo "✅ Deployment complete. Streaming logs..."
tail -f "${LOG_PATH}"