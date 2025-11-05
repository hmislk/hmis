#!/bin/bash

##########################################################
# 🗓️ CRON SCHEDULING INSTRUCTIONS
#
# ✅ This script is scheduled to restart VMs and verify SSH access.
#
# 📍 Timezone: VM is in UTC, but restart time is in IST.
# 📍 Goal: Restart VM every Tuesday at 3:20 AM IST
#         (equivalent to Monday 21:50 UTC)
#
# ✏️ To schedule this:
# Run `crontab -e` and add this line:
# ---------------------------------------------------------
# 50 21 * * 1 /home/azureuser/utils/server_utils/restart_servers_weekly.sh >> /home/azureuser/utils/logs/weekly_vm_restart.log 2>&1
# ---------------------------------------------------------
# This ensures the script runs at the correct IST time weekly.
##########################################################

CONFIG_FILE="/home/azureuser/utils/secrets/server_config.json"
VM_LIST=("VM_NAME_AS_IN_CONFIG")
RECIPIENTS=("email1@gmail.com" "email2@gmail.com")

TIMESTAMP=$(TZ='Asia/Kolkata' date '+%Y-%m-%d %H:%M:%S IST')

send_email() {
    local SUBJECT=$1
    local BODY=$2
    shift 2
    local JSON
        JSON=$(jq -n \
            --arg subject "$SUBJECT" \
            --arg body "$BODY" \
            --argjson recipients "$(printf '%s\n' "$@" | jq -R . | jq -s .)" \
            --argjson isHtml false \
            '{subject: $subject, body: $body, recipients: $recipients, isHtml: $isHtml}'
        )

        curl -s -X POST "http://localhost:8081/messenger/api/email/send" \
             -H "Content-Type: application/json" \
             -d "$JSON"
}

# -----------------------
# Azure login
# -----------------------
echo "[$TIMESTAMP] Checking Azure login..."
if ! az account show &>/dev/null; then
    echo "Logging in..."
    if ! az login --identity &>/dev/null; then
        echo "Azure login failed."
        exit 1
    fi
fi

# -----------------------
# Send start email
# -----------------------
START_BODY="🔄 Restart process started at $TIMESTAMP for the following VMs:\n\n$(printf '%s\n' "${VM_LIST[@]}")"
send_email "🔄 VM Restart Initiated" "$START_BODY" "${RECIPIENTS[@]}"

# -----------------------
# Restart VMs in parallel
# -----------------------
for SERVER_KEY in "${VM_LIST[@]}"; do
    (
        NAME=$(jq -r ".vm_names[\"$SERVER_KEY\"]" "$CONFIG_FILE")
        GROUP=$(jq -r ".vm_resource_groups[\"$SERVER_KEY\"]" "$CONFIG_FILE")

        if [[ -z "$NAME" || "$NAME" == "null" || -z "$GROUP" || "$GROUP" == "null" ]]; then
            echo "❌ Missing VM config for: $SERVER_KEY"
            exit 1
        fi

        echo "[$SERVER_KEY] Restarting $NAME..."
        az vm restart --name "$NAME" --resource-group "$GROUP" --no-wait
    ) &
done

wait

# -----------------------
# Send completion email
# -----------------------
END_TIMESTAMP=$(TZ='Asia/Kolkata' date '+%Y-%m-%d %H:%M:%S IST')
END_BODY="✅ Restart process completed at $END_TIMESTAMP for the following VMs:\n\n$(printf '%s\n' "${VM_LIST[@]}")\n\nWill check the status in 10 minutes!"
send_email "✅ VM Restart Completed" "$END_BODY" "${RECIPIENTS[@]}"

echo "Restart script completed at $END_TIMESTAMP."

echo "Waiting 10 minutes before checking SSH access..."
sleep 600

# -----------------------
# SSH connectivity check
# -----------------------
PASSED_VMS=()
FAILED_VMS=()

for SERVER_KEY in "${VM_LIST[@]}"; do
    IP=$(jq -r ".vm_ips[\"$SERVER_KEY\"]" "$CONFIG_FILE")
    SSH_KEY=$(jq -r ".vm_ssh_keys[\"$SERVER_KEY\"]" "$CONFIG_FILE")

    if [[ -z "$IP" || "$IP" == "null" || -z "$SSH_KEY" || "$SSH_KEY" == "null" || ! -f "$SSH_KEY" ]]; then
        echo "❌ Invalid IP or SSH key for $SERVER_KEY"
        FAILED_VMS+=("$SERVER_KEY")
        continue
    fi

    echo "🔄 Trying SSH to $SERVER_KEY ($IP)..."
    if ssh -o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=no -i "$SSH_KEY" azureuser@"$IP" "echo ok" &>/dev/null; then
        echo "✅ SSH successful for $SERVER_KEY"
        PASSED_VMS+=("$SERVER_KEY")
    else
        echo "❌ SSH failed for $SERVER_KEY"
        FAILED_VMS+=("$SERVER_KEY")
    fi
done

# -----------------------
# Send final SSH status email
# -----------------------
CHECK_TIMESTAMP=$(TZ='Asia/Kolkata' date '+%Y-%m-%d %H:%M:%S IST')
CHECK_BODY="🔍 SSH Connectivity Check Completed at $CHECK_TIMESTAMP.\n\n"

if [[ ${#PASSED_VMS[@]} -gt 0 ]]; then
    CHECK_BODY+="✅ Successfully reachable via SSH:\n$(printf '%s\n' "${PASSED_VMS[@]}")\n\n"
fi

if [[ ${#FAILED_VMS[@]} -gt 0 ]]; then
    CHECK_BODY+="❌ Unreachable via SSH:\n$(printf '%s\n' "${FAILED_VMS[@]}")\n"
fi

if [[ ${#PASSED_VMS[@]} -eq 0 && ${#FAILED_VMS[@]} -eq 0 ]]; then
    CHECK_BODY+="⚠️ No VM connectivity could be checked. Please verify manually."
fi

send_email "🔍 VM SSH Status After Restart" "$CHECK_BODY" "${RECIPIENTS[@]}"
echo "✅ SSH check completed at $CHECK_TIMESTAMP."