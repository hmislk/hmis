#!/bin/bash

set -e

echo "Starting Payara domain..."
/opt/payara/appserver/bin/asadmin start-domain

echo "Waiting for Payara to fully start..."
sleep 20

echo "Creating temporary password file..."
PASSWORD_FILE=/opt/payara/passwordfile
echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" > $PASSWORD_FILE

echo "Changing admin password and enabling secure admin..."
/opt/payara/appserver/bin/asadmin change-admin-password --user admin --passwordfile=$PASSWORD_FILE || true
/opt/payara/appserver/bin/asadmin enable-secure-admin --user admin --passwordfile=$PASSWORD_FILE || true

echo "Deploying application to context path: ${CONTEXT_PATH}"
/opt/payara/appserver/bin/asadmin deploy --contextroot=${CONTEXT_PATH} --force=true --user admin --passwordfile=$PASSWORD_FILE /opt/payara/app.war

echo "Tailing server log..."
tail -f /opt/payara/appserver/glassfish/domains/domain1/logs/server.log