FROM payara/server-full:5.2022.5

# Optional JVM tuning
ENV JVM_ARGS="-Xms6g -Xmx6g"

# Deployment context path
ENV CONTEXT_PATH=qa1

# Copy WAR to neutral location
COPY target/*.war /opt/payara/app.war

# Start domain and deploy manually
CMD bash -c "\
  /opt/payara/appserver/bin/asadmin start-domain && \
  echo 'Waiting for domain startup...' && sleep 20 && \
  /opt/payara/appserver/bin/asadmin deploy --contextroot=${CONTEXT_PATH} --force=true /opt/payara/app.war && \
  tail -f /opt/payara/appserver/glassfish/domains/domain1/logs/server.log"