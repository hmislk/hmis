#!/bin/bash
set -e

echo "Setting up Payara admin..."

ADMIN_DIR="/opt/payara/appserver/glassfish/domains/domain1/config"
ADMIN_KEYFILE="${ADMIN_DIR}/admin-keyfile"
WAR_PATH="/opt/payara/app.war"
DOMAIN_NAME="domain1"
LOG_PATH="/opt/payara/appserver/glassfish/domains/${DOMAIN_NAME}/logs/server.log"

# Create temp password files
echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" > /tmp/password.txt
echo "AS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" > /tmp/newpass.txt
chmod 600 /tmp/password.txt /tmp/newpass.txt

# Start domain first
echo "Starting Payara domain..."
/opt/payara/appserver/bin/asadmin start-domain
echo "Waiting for domain startup..."
sleep 20

# If no admin user exists, set the password
if [ ! -f "$ADMIN_KEYFILE" ]; then
  echo "No admin-keyfile found. Setting initial admin password..."
  echo "yes" | /opt/payara/appserver/bin/asadmin change-admin-password --user admin --passwordfile /tmp/newpass.txt
else
  echo "Admin user already exists. Skipping password setup."
fi

# Enable secure admin (only if not already enabled)
echo "Enabling secure admin..."
/opt/payara/appserver/bin/asadmin enable-secure-admin --passwordfile /tmp/password.txt || true

# Restart domain to apply secure admin changes
echo "Restarting domain..."
/opt/payara/appserver/bin/asadmin stop-domain
/opt/payara/appserver/bin/asadmin start-domain
sleep 20

# Deploy WAR
echo "Deploying application..."
/opt/payara/appserver/bin/asadmin --user admin --passwordfile /tmp/password.txt \
  deploy --contextroot="${CONTEXT_PATH}" --force=true "${WAR_PATH}"

# Tail logs
echo "Deployment finished. Tailing logs..."
tail -f "${LOG_PATH}"