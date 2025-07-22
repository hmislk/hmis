FROM payara/server-full:5.2022.5

ENV JVM_ARGS="-Xms6g -Xmx6g"
ENV ADMIN_PASSWORD=""
ENV CONTEXT_PATH=""

# Copy WAR file
COPY target/*.war /opt/payara/app.war

# Copy entrypoint script (already executable from host)
COPY entrypoint.sh /opt/scripts/entrypoint.sh

# Use entrypoint
ENTRYPOINT ["/opt/scripts/entrypoint.sh"]