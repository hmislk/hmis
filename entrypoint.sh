#!/bin/bash

# Create a temp password file for asadmin
echo "AS_ADMIN_PASSWORD=$ADMIN_PASSWORD" > /tmp/password.txt

# Start Payara domain
/opt/payara/appserver/bin/asadmin start-domain

# Wait for it to come up
echo "Waiting for domain startup..." && sleep 20

# Deploy WAR
/opt/payara/appserver/bin/asadmin --user admin --passwordfile /tmp/password.txt \
  deploy --contextroot=${CONTEXT_PATH} --force=true /opt/payara/app.war

# Follow server log
tail -f /opt/payara/appserver/glassfish/domains/domain1/logs/server.log