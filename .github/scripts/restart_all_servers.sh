#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <excludedServers>"
    exit 1
fi

EXCLUDED_SERVERS=$1

CONFIG_FILE="/home/azureuser/utils/secrets/server_config.json"

# Ensure the JSON file exists
if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "Error: Configuration file $CONFIG_FILE not found."
    exit 1
fi

IFS=',' read -r -a EXCLUDED_ARRAY <<< "$EXCLUDED_SERVERS"

# Define the included servers
INCLUDED_ARRAY=("Development(4.240.39.63)" "QA(4.240.43.211)")

# Remove excluded servers from included array
for excluded in "${EXCLUDED_ARRAY[@]}"; do
    for i in "${!INCLUDED_ARRAY[@]}"; do
        if [[ "${INCLUDED_ARRAY[i]}" == *"$excluded"* ]]; then
            unset 'INCLUDED_ARRAY[i]'
        fi
    done
done

echo "Excluded Servers:"
for server in "${EXCLUDED_ARRAY[@]}"; do
    echo "$server"
done

echo "Included Servers:"
for server in "${INCLUDED_ARRAY[@]}"; do
    echo "$server"
done

restart_vm() {
    local SERVER_NAME=$1

    local SERVER_IP
    SERVER_IP=$(jq -r ".vm_ips[\"$SERVER_NAME\"]" "$CONFIG_FILE")

    local SSH_KEY
    SSH_KEY=$(jq -r ".vm_ssh_keys[\"$SERVER_NAME\"]" "$CONFIG_FILE")

    if [[ -z "$SERVER_IP" || "$SERVER_IP" == "null" ]]; then
        echo "Error: Could not retrieve IP for $SERVER_NAME."
        echo "VM $SERVER_NAME failed to restart."
        return 1
    fi

    if [[ -z "$SSH_KEY" || "$SSH_KEY" == "null" ]]; then
        echo "Error: Could not retrieve SSH key for $SERVER_NAME."
        echo "VM $SERVER_NAME failed to restart."
        return 1
    fi

    echo "Restarting VM $SERVER_NAME on $SERVER_IP..."

    if ! ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" azureuser@"$SERVER_IP" <<EOF
        echo 'Restarting VM...'
        sudo reboot
EOF
    then
        echo "VM $SERVER_NAME failed to restart."
        return 1
    fi

    echo "VM $SERVER_NAME restarted successfully."
}

# Restart each VM in the included servers list
for server in "${INCLUDED_ARRAY[@]}"; do
    restart_vm "$server" || continue
done

echo "Operation completed successfully."
