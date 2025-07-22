FROM payara/server-full:5.2022.5

ENV JVM_ARGS="-Xms6g -Xmx6g"
ENV ADMIN_PASSWORD=""
ENV CONTEXT_PATH=""

# Copy WAR to deploy later
COPY target/*.war /opt/payara/app.war

# Use a writable directory like /home/scripts
COPY entrypoint.sh /home/scripts/entrypoint.sh
RUN chmod +x /home/scripts/entrypoint.sh

# Use the custom script to start Payara and deploy
CMD ["/home/scripts/entrypoint.sh"]