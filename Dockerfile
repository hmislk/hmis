FROM payara/server-full:5.2022.5

# JVM tuning (optional)
ENV JVM_ARGS="-Xms6g -Xmx6g"

# Copy WAR to a neutral location
COPY target/*.war /opt/payara/app.war

# Define context root at runtime via ENV
ENV CONTEXT_PATH=qa1

# Entrypoint script
CMD bash -c "\
  /opt/payara/bin/asadmin start-domain && \
  echo 'Waiting for domain startup...' && sleep 20 && \
  /opt/payara/bin/asadmin deploy --contextroot=${CONTEXT_PATH} --force=true /opt/payara/app.war && \
  tail -f /opt/payara/glassfish/domains/domain1/logs/server.log"