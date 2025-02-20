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

# Ensure TO_ENV is QA
if [[ "$TO_ENV" != "QA" ]]; then
    echo "Error: TO_ENV must be QA for now."
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

    ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" azureuser@$SERVER_IP \
        DB_IP="$DB_IP" DB_USERNAME="$DB_USERNAME" DB_PASSWORD="$DB_PASSWORD" DB_NAME="$DB_NAME" 'bash -s' << 'EOF'

        if [ ! -d /opt/swapDatabases ]; then
            sudo mkdir -p /opt/swapDatabases
            sudo chown azureuser:azureuser /opt/swapDatabases
        fi
        mkdir -p /opt/swapDatabases/myBackup /opt/swapDatabases/importedBackup
        if [ -f /opt/swapDatabases/myBackup/backup.sql ]; then
            mv /opt/swapDatabases/myBackup/backup.sql /opt/swapDatabases/myBackup/backup-old.sql
        fi

        # DB dump command
        mysqldump -h "$DB_IP" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" > /opt/swapDatabases/myBackup/backup.sql

        sudo chown azureuser:azureuser /opt/swapDatabases/myBackup/backup.sql
        sudo chmod 644 /opt/swapDatabases/myBackup/backup.sql
        sudo chown -R azureuser:azureuser /opt/swapDatabases
EOF
}
# Log into QA and Dev servers to rename /opt/swapDatabases contents
restore_database() {
    local SERVER_IP=$1
    local SSH_KEY=$2
    local DB_IP=$3
    local DB_USERNAME=$4
    local DB_PASSWORD=$5
    local DB_NAME=$6

    echo "Restoring database $DB_NAME on $SERVER_IP..."

    ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" azureuser@$SERVER_IP \
        DB_IP="$DB_IP" DB_USERNAME="$DB_USERNAME" DB_PASSWORD="$DB_PASSWORD" DB_NAME="$DB_NAME" 'bash -s' << 'EOF'

        # Drop and recreate the database
        echo "Dropping existing database if it exists..."
        mysql -h "$DB_IP" -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "DROP DATABASE IF EXISTS \`$DB_NAME\`;"

        echo "Creating new database..."
        mysql -h "$DB_IP" -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "CREATE DATABASE \`$DB_NAME\`;"

        echo "Importing backup into $DB_NAME..."
        mysql -h "$DB_IP" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" < /opt/swapDatabases/importedBackup/backup.sql

        echo "Database $DB_NAME restored successfully!"
EOF
}

# Manage backups on source and target servers
manage_backup "$FROM_SERVER_IP" "$FROM_SSH_KEY" "$FROM_DB_IP" "$FROM_DB_USERNAME" "$FROM_DB_PASSWORD" "$FROM_DB_NAME"
manage_backup "$TO_SERVER_IP" "$TO_SSH_KEY"  "$TO_DB_IP" "$TO_DB_USERNAME" "$TO_DB_PASSWORD" "$TO_DB_NAME"

# Copy backup file from source to target
echo "Transferring backup file..."
scp -o StrictHostKeyChecking=no -i "$FROM_SSH_KEY" azureuser@$FROM_SERVER_IP:/opt/swapDatabases/myBackup/backup.sql /home/azureuser/backup.sql
scp -o StrictHostKeyChecking=no -i "$TO_SSH_KEY" /home/azureuser/backup.sql azureuser@$TO_SERVER_IP:/opt/swapDatabases/importedBackup/backup.sql

# Restore database on target server
restore_database "$TO_SERVER_IP" "$TO_SSH_KEY" "$TO_DB_IP" "$TO_DB_USERNAME" "$TO_DB_PASSWORD" "$TO_DB_NAME"

# Cleanup
rm -f /home/azureuser/backup.sql

echo "Database backup successfully transferred from $FROM_ENV to $TO_ENV on $DATE."
