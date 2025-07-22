FROM payara/server-full:5.2022.5

ENV JVM_ARGS="-Xms6g -Xmx6g"

# Copy WAR file to a neutral location (not auto-deployed)
COPY target/*.war /opt/payara/app.war

# Copy the entrypoint script
COPY entrypoint.sh /opt/payara/entrypoint.sh
RUN chmod +x /opt/payara/entrypoint.sh

# Use the script as the container entrypoint
ENTRYPOINT ["/opt/payara/entrypoint.sh"]