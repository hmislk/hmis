#!/bin/bash
set -e

echo "Setting up Payara admin..."

# Create admin password file
echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" > /tmp/password.txt
chmod 600 /tmp/password.txt

# Start the domain
/opt/payara/appserver/bin/asadmin start-domain
echo "Waiting for domain startup..."
sleep 15

ADMIN_KEYFILE="/opt/payara/appserver/glassfish/domains/domain1/config/admin-keyfile"

# If no admin password is set (first-time setup), do it
if [ ! -f "$ADMIN_KEYFILE" ]; then
  echo "AS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" > /tmp/newpass.txt
  chmod 600 /tmp/newpass.txt

  echo "Setting admin password..."
  echo "yes" | /opt/payara/appserver/bin/asadmin change-admin-password --user admin --passwordfile /tmp/newpass.txt

  echo "Enabling secure admin..."
  /opt/payara/appserver/bin/asadmin enable-secure-admin --passwordfile /tmp/password.txt

  echo "Restarting domain to apply secure admin..."
  /opt/payara/appserver/bin/asadmin stop-domain
  /opt/payara/appserver/bin/asadmin start-domain
  echo "Waiting after restart..."
  sleep 15
fi

# Now deploy using the correct password
echo "Deploying application..."
/opt/payara/appserver/bin/asadmin --user admin --passwordfile /tmp/password.txt \
  deploy --contextroot="${CONTEXT_PATH}" --force=true /opt/payara/app.war

# Tail server logs
tail -f /opt/payara/appserver/glassfish/domains/domain1/logs/server.log