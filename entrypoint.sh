#!/bin/bash

set -e

# Create the admin password file
echo "AS_ADMIN_PASSWORD=$ADMIN_PASSWORD" > /tmp/password.txt
chmod 600 /tmp/password.txt

# Start domain first
/opt/payara/appserver/bin/asadmin start-domain
echo "Waiting for domain startup..." && sleep 15

# Only if first boot (admin password not set yet)
if [ ! -f /opt/payara/appserver/glassfish/domains/domain1/config/admin-keyfile ]; then
  echo "AS_ADMIN_NEWPASSWORD=$ADMIN_PASSWORD" > /tmp/newpass.txt
  chmod 600 /tmp/newpass.txt

  echo "Setting Payara admin password..."
  /opt/payara/appserver/bin/asadmin change-admin-password --user admin --passwordfile /tmp/newpass.txt <<< "yes"

  echo "Enabling secure admin..."
  /opt/payara/appserver/bin/asadmin enable-secure-admin --passwordfile /tmp/password.txt

  rm -f /tmp/newpass.txt
  echo "Restarting domain for secure admin to take effect..."
  /opt/payara/appserver/bin/asadmin restart-domain
  sleep 15
fi

# Deploy the WAR with the provided context path
echo "Deploying application..."
/opt/payara/appserver/bin/asadmin --user admin --passwordfile /tmp/password.txt \
  deploy --contextroot=${CONTEXT_PATH} --force=true /opt/payara/app.war

# Tail logs
tail -f /opt/payara/appserver/glassfish/domains/domain1/logs/server.log