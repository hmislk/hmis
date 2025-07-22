FROM payara/server-full:5.2022.5

ENV JVM_ARGS="-Xms6g -Xmx6g"

# Copy WAR file
COPY target/*.war /opt/payara/app.war

# Copy entrypoint script
COPY entrypoint.sh /opt/scripts/entrypoint.sh

# Use the script as the container entrypoint
ENTRYPOINT ["/opt/scripts/entrypoint.sh"]