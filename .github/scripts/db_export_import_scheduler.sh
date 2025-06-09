#!/bin/bash

# Ensure script is executed with two arguments
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <fromEnv> <toEnv>"
    exit 1
fi

FROM_ENV=$1
TO_ENV=$2

CONFIG_FILE="/home/azureuser/utils/secrets/server_config.json"

# Ensure the JSON file exists
if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "Error: Configuration file $CONFIG_FILE not found."
    exit 1
fi

# Ensure TO_ENV is either QA1 or QA2
if [[ "$TO_ENV" != "QA1" && "$TO_ENV" != "QA2" ]]; then
    echo "Error: TO_ENV must be QA1 or QA2 for now."
    exit 1
fi

FROM_SSH_KEY=$(jq -r ".server_ssh_keys[\"$FROM_ENV\"]" "$CONFIG_FILE")
TO_SSH_KEY=$(jq -r ".server_ssh_keys[\"$TO_ENV\"]" "$CONFIG_FILE")
FROM_SERVER_IP=$(jq -r ".server_ips[\"$FROM_ENV\"]" "$CONFIG_FILE")
TO_SERVER_IP=$(jq -r ".server_ips[\"$TO_ENV\"]" "$CONFIG_FILE")
FROM_DB_IP=$(jq -r ".db_ips[\"$FROM_ENV\"]" "$CONFIG_FILE")
TO_DB_IP=$(jq -r ".db_ips[\"$TO_ENV\"]" "$CONFIG_FILE")
FROM_DB_USERNAME=$(jq -r ".db_usernames[\"$FROM_ENV\"]" "$CONFIG_FILE")
TO_DB_USERNAME=$(jq -r ".db_usernames[\"$TO_ENV\"]" "$CONFIG_FILE")
FROM_DB_PASSWORD=$(jq -r ".db_passwords[\"$FROM_ENV\"]" "$CONFIG_FILE")
TO_DB_PASSWORD=$(jq -r ".db_passwords[\"$TO_ENV\"]" "$CONFIG_FILE")
FROM_DB_NAME=$(jq -r ".db_names[\"$FROM_ENV\"]" "$CONFIG_FILE")
TO_DB_NAME=$(jq -r ".db_names[\"$TO_ENV\"]" "$CONFIG_FILE")

# Ensure SSH key files exist
if [[ ! -f "$FROM_SSH_KEY" || ! -f "$TO_SSH_KEY" ]]; then
    echo "Error: One or more SSH key files are missing."
    exit 1
fi

echo "Dumping database from $FROM_ENV and replacing in $TO_ENV on date $DATE"

# Function to manage backup files on a given server
manage_backup() {
    local SERVER_IP=$1
    local SSH_KEY=$2
    local DB_IP=$3
    local DB_USERNAME=$4
    local DB_PASSWORD=$5
    local DB_NAME=$6

    ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" azureuser@"$SERVER_IP" \
        DB_IP="$DB_IP" DB_USERNAME="$DB_USERNAME" DB_PASSWORD="$DB_PASSWORD" DB_NAME="$DB_NAME" 'bash -s' << 'EOF'

        if [ ! -d /opt/db_export_import_backups ]; then
            sudo mkdir -p /opt/db_export_import_backups
            sudo chown azureuser:azureuser /opt/db_export_import_backups
        fi
        mkdir -p /opt/db_export_import_backups/myBackup /opt/db_export_import_backups/importedBackup
        if [ -f /opt/db_export_import_backups/myBackup/backup.sql ]; then
            mv /opt/db_export_import_backups/myBackup/backup.sql /opt/db_export_import_backups/myBackup/backup-old.sql
        fi

        # DB dump command
        mysqldump -h "$DB_IP" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" > /opt/db_export_import_backups/myBackup/backup.sql

        sudo chown azureuser:azureuser /opt/db_export_import_backups/myBackup/backup.sql
        sudo chmod 644 /opt/db_export_import_backups/myBackup/backup.sql
        sudo chown -R azureuser:azureuser /opt/db_export_import_backups
EOF
}
# Log into QA and Dev servers to rename /opt/db_export_import_backups contents
restore_database() {
    local SERVER_IP=$1
    local SSH_KEY=$2
    local DB_IP=$3
    local DB_USERNAME=$4
    local DB_PASSWORD=$5
    local DB_NAME=$6

    echo "Restoring database $DB_NAME on $SERVER_IP..."

    ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" azureuser@"$SERVER_IP" \
        DB_IP="$DB_IP" DB_USERNAME="$DB_USERNAME" DB_PASSWORD="$DB_PASSWORD" DB_NAME="$DB_NAME" 'bash -s' << 'EOF'

        # Drop and recreate the database
        echo "Dropping existing database if it exists..."
        mysql -h "$DB_IP" -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "DROP DATABASE IF EXISTS \`$DB_NAME\`;"

        echo "Creating new database..."
        mysql -h "$DB_IP" -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "CREATE DATABASE \`$DB_NAME\`;"

        echo "Importing backup into $DB_NAME..."
        mysql -h "$DB_IP" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" < /opt/db_export_import_backups/importedBackup/backup.sql

        echo "Database $DB_NAME restored successfully!"
EOF
}

# Function to send an email
send_email() {
    local SUBJECT=$1
    local BODY=$2
    shift 2  # Remove first two arguments and keep the rest as recipients
    local RECIPIENTS=("$@")
    local EMAIL_API_URL="http://localhost:8081/messenger/api/email/send"

    # Convert recipients array to JSON format
    local EMAIL_JSON
    EMAIL_JSON=$(printf '[%s]' "$(printf '"%s",' "${RECIPIENTS[@]}" | sed 's/,$//')")

    echo "Sending email notification..."
    curl -X POST "$EMAIL_API_URL" \
         -H "Content-Type: application/json" \
         -d '{
                "subject": "'"$SUBJECT"'",
                "body": "'"$BODY"'",
                "recipients": '"$EMAIL_JSON"',
                "isHtml": false
             }'

    echo "Email sent successfully!"
}

TRANSFER_SUCCESS=false
RESTORE_SUCCESS=false

# Manage backups on source and target servers
if manage_backup "$FROM_SERVER_IP" "$FROM_SSH_KEY" "$FROM_DB_IP" "$FROM_DB_USERNAME" "$FROM_DB_PASSWORD" "$FROM_DB_NAME" &&
   manage_backup "$TO_SERVER_IP" "$TO_SSH_KEY" "$TO_DB_IP" "$TO_DB_USERNAME" "$TO_DB_PASSWORD" "$TO_DB_NAME"; then
    echo "Backup management successful."
else
    echo "Backup management failed."
    send_email "Database Restore Failed" "Backup management failed while transferring from $FROM_ENV to $TO_ENV." "imeshranawella00@gmail.com" "geeth.gsm@gmail.com" "deshanipubudu0415@gmail.com" "iranimadushika28@gmail.com"
    exit 1
fi

# Copy backup file from source to target
echo "Transferring backup file..."
if scp -o StrictHostKeyChecking=no -i "$FROM_SSH_KEY" azureuser@"$FROM_SERVER_IP":/opt/db_export_import_backups/myBackup/backup.sql /home/azureuser/backup.sql &&
   scp -o StrictHostKeyChecking=no -i "$TO_SSH_KEY" /home/azureuser/backup.sql azureuser@"$TO_SERVER_IP":/opt/db_export_import_backups/importedBackup/backup.sql; then
    TRANSFER_SUCCESS=true
else
    send_email "Database Restore Failed" "Backup file transfer from $FROM_ENV to $TO_ENV failed." "imeshranawella00@gmail.com" "geeth.gsm@gmail.com" "deshanipubudu0415@gmail.com" "iranimadushika28@gmail.com"
    exit 1
fi

# Restore database on target server
if restore_database "$TO_SERVER_IP" "$TO_SSH_KEY" "$TO_DB_IP" "$TO_DB_USERNAME" "$TO_DB_PASSWORD" "$TO_DB_NAME"; then
    RESTORE_SUCCESS=true
else
    send_email "Database Restore Failed" "Database restoration on $TO_ENV failed." "imeshranawella00@gmail.com" "geeth.gsm@gmail.com" "deshanipubudu0415@gmail.com" "iranimadushika28@gmail.com"
    exit 1
fi

# Cleanup
rm -f /home/azureuser/backup.sql

if $TRANSFER_SUCCESS && $RESTORE_SUCCESS; then
    send_email "Database Restore Completed" \
        "Database backup successfully transferred from $FROM_ENV to $TO_ENV." \
        "imeshranawella00@gmail.com" "geeth.gsm@gmail.com" "deshanipubudu0415@gmail.com" "iranimadushika28@gmail.com"
fi

echo "Database backup successfully transferred from $FROM_ENV to $TO_ENV."
