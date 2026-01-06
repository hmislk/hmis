#!/bin/bash

# Ensure script is executed with one argument
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <excludedServers>"
    exit 1
fi

# Check if logged into Azure
if ! az account show &> /dev/null; then
    printf "Not logged into Azure. Attempting to login...\n"
    if ! az login --identity &> /dev/null; then
        printf "Azure login failed. Please login manually and retry.\n"
        exit 1
    fi
    printf "Azure login successful.\n"
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
INCLUDED_ARRAY=("Development(4.240.39.63)" "QA(4.240.43.211)" "Shared01(52.172.158.159)" "Shared02(20.204.129.229)" "D01(4.213.180.217)")

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

SUCCESS_FILE=$(mktemp)
FAILED_FILE=$(mktemp)

restart_vm() {
    local SERVER_NAME=$1

    local VM_NAME
    VM_NAME=$(jq -r ".vm_names[\"$SERVER_NAME\"]" "$CONFIG_FILE")

    local RESOURCE_GROUP
    RESOURCE_GROUP=$(jq -r ".vm_resource_groups[\"$SERVER_NAME\"]" "$CONFIG_FILE")

    if [[ -z "$VM_NAME" || "$VM_NAME" == "null" ]]; then
        echo "Error: Could not retrieve VM name for $SERVER_NAME."
        echo "$SERVER_NAME" >> "$FAILED_FILE"
        return 1
    fi

    if [[ -z "$RESOURCE_GROUP" || "$RESOURCE_GROUP" == "null" ]]; then
        echo "Error: Could not retrieve resource group for $SERVER_NAME."
        echo "$SERVER_NAME" >> "$FAILED_FILE"
        return 1
    fi

    echo "Restarting VM $VM_NAME in resource group $RESOURCE_GROUP..."
    if az vm restart --name "$VM_NAME" --resource-group "$RESOURCE_GROUP" --no-wait; then
        echo "$SERVER_NAME" >> "$SUCCESS_FILE"
    else
        echo "$SERVER_NAME" >> "$FAILED_FILE"
    fi &
}

# Restart each VM in the included servers list in parallel
for server in "${INCLUDED_ARRAY[@]}"; do
    restart_vm "$server"
done

wait

printf "\nRestarted VMs:\n"
cat "$SUCCESS_FILE"

printf "\nFailed to restart VMs:\n"
cat "$FAILED_FILE"

# Clean up temporary files
rm -f "$SUCCESS_FILE" "$FAILED_FILE"

printf "\nOperation completed successfully."
