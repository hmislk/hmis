#!/bin/bash
set -e

echo "Setting up Payara admin..."

# Create admin password file
echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" > /tmp/password.txt
chmod 600 /tmp/password.txt

ADMIN_KEYFILE="/opt/payara/appserver/glassfish/domains/domain1/config/admin-keyfile"

# If admin-keyfile exists and user is injecting password, reset it
if [ -f "$ADMIN_KEYFILE" ]; then
  echo "Existing admin-keyfile found. Deleting to reset password..."
  rm -f "$ADMIN_KEYFILE"
fi

# Start the domain (fresh, without password)
echo "Starting domain..."
/opt/payara/appserver/bin/asadmin start-domain
echo "Waiting for domain startup..."
sleep 15

# Set new admin password
echo "AS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" > /tmp/newpass.txt
chmod 600 /tmp/newpass.txt

echo "Setting admin password..."
echo "yes" | /opt/payara/appserver/bin/asadmin change-admin-password --user admin --passwordfile /tmp/newpass.txt

# Enable secure admin
echo "Enabling secure admin..."
/opt/payara/appserver/bin/asadmin enable-secure-admin --passwordfile /tmp/password.txt

# Restart domain
echo "Restarting domain..."
/opt/payara/appserver/bin/asadmin stop-domain
/opt/payara/appserver/bin/asadmin start-domain
sleep 15

# Deploy
echo "Deploying application..."
/opt/payara/appserver/bin/asadmin --user admin --passwordfile /tmp/password.txt \
  deploy --contextroot="${CONTEXT_PATH}" --force=true /opt/payara/app.war

# Log tailing
tail -f /opt/payara/appserver/glassfish/domains/domain1/logs/server.log