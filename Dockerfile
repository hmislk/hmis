FROM payara/server-full:5.2022.5

# Optional: JVM tuning
ENV JVM_ARGS="-Xms6g -Xmx6g"

# Copy WAR to a neutral location (not autodeploy!)
COPY target/*.war /opt/payara/app.war

# Start domain and deploy manually using context path
CMD ["sh", "-c", "\
  ${PAYARA_PATH}/bin/asadmin start-domain && \
  echo 'Waiting for domain startup...' && sleep 20 && \
  ${PAYARA_PATH}/bin/asadmin deploy --contextroot=${CONTEXT_PATH} --force=true /opt/payara/app.war && \
  tail -f ${PAYARA_PATH}/glassfish/domains/domain1/logs/server.log"]