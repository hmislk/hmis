FROM payara/server-full:5.2022.5

ENV JVM_ARGS="-Xms6g -Xmx6g"
ENV ADMIN_PASSWORD=""
ENV CONTEXT_PATH=""

# Copy WAR to deploy later
COPY target/*.war /opt/payara/app.war

# Use a writable location for the script
COPY entrypoint.sh /opt/scripts/entrypoint.sh
RUN chmod +x /opt/scripts/entrypoint.sh

# Use custom entrypoint
CMD ["/opt/scripts/entrypoint.sh"]