#!/bin/bash

# Ensure script is executed with two arguments
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <selectedAction> <selectedServer>"
    exit 1
fi

SELECTED_ACTION=$1
SELECTED_SERVER=$2

CONFIG_FILE="/home/azureuser/utils/secrets/server_config.json"

# Ensure the JSON file exists
if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "Error: Configuration file $CONFIG_FILE not found."
    exit 1
fi

SERVER_IP=$(jq -r ".vm_ips[\"$SELECTED_SERVER\"]" "$CONFIG_FILE")
SERVER_SSH_KEY=$(jq -r ".vm_ssh_keys[\"$SELECTED_SERVER\"]" "$CONFIG_FILE")
SERVER_NAME=$(jq -r ".vm_names[\"$SELECTED_SERVER\"]" "$CONFIG_FILE")
SERVER_RESOURCE_GROUP=$(jq -r ".vm_resource_groups[\"$SELECTED_SERVER\"]" "$CONFIG_FILE")

# Ensure required values are not empty
if [[ -z "$SERVER_IP" || "$SERVER_IP" == "null" ]]; then
    echo "Error: No IP found for server '$SELECTED_SERVER'. Check configuration."
    exit 1
fi

if [[ -z "$SERVER_SSH_KEY" || "$SERVER_SSH_KEY" == "null" ]]; then
    echo "Error: No SSH key found for server '$SELECTED_SERVER'. Check configuration."
    exit 1
fi

# Ensure SSH key file exists
if [[ ! -f "$SERVER_SSH_KEY" ]]; then
    echo "Error: SSH key file '$SERVER_SSH_KEY' not found."
    exit 1
fi

echo "Executing action '$SELECTED_ACTION' on server '$SELECTED_SERVER' ($SERVER_IP)..."

if [[ "$SELECTED_ACTION" == "NGINX" || "$SELECTED_ACTION" == "PAYARA" ]]; then
    ssh -o StrictHostKeyChecking=no -i "$SERVER_SSH_KEY" azureuser@"$SERVER_IP" "
        if [[ '$SELECTED_ACTION' == 'NGINX' ]]; then
            echo 'Reloading NGINX...'
            sudo systemctl reload nginx && echo 'NGINX reloaded successfully' || echo 'NGINX reload failed'
        elif [[ '$SELECTED_ACTION' == 'PAYARA' ]]; then
            echo 'Restarting Payara...'
            sudo systemctl restart payara_domain1.service && echo 'Payara restarted successfully' || echo 'Payara restart failed'
        else
            echo 'Error: Unknown action.'
            exit 1
        fi
    "
elif [ "$SELECTED_ACTION" == "VM" ]; then
    echo "Restarting VM..."
    # Check if logged into Azure
    if ! az account show &> /dev/null; then
        echo "Not logged into Azure. Attempting to login..."
        if ! az login --identity &> /dev/null; then
            echo "Azure login failed. Please login manually and retry."
            exit 1
        fi
        echo "Azure login successful."
    fi

    # Restart the VM
    if az vm restart --name "$SERVER_NAME" --resource-group "$SERVER_RESOURCE_GROUP" --no-wait; then
        echo "VM restart initiated successfully."
    else
        echo "Error: Failed to restart VM '$SERVER_NAME' in resource group '$SERVER_RESOURCE_GROUP'."
        exit 1
    fi
else
    echo "Error: Unknown action."
    exit 1
fi

echo "Operation completed successfully."
