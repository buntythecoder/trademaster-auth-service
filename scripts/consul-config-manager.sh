#!/bin/bash

# TradeMaster Consul Configuration Manager
# Advanced script for managing configurations in Consul KV

CONSUL_HOST=${CONSUL_HOST:-localhost}
CONSUL_PORT=${CONSUL_PORT:-8500}
CONSUL_URL="http://$CONSUL_HOST:$CONSUL_PORT"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }

# Function to check if Consul is available
check_consul() {
    print_info "Checking Consul availability at $CONSUL_URL..."
    if curl -s "$CONSUL_URL/v1/status/leader" > /dev/null; then
        print_success "Consul is available"
        return 0
    else
        print_error "Consul is not available at $CONSUL_URL"
        return 1
    fi
}

# Function to list all configurations
list_configs() {
    print_info "Listing all configurations in Consul KV..."
    echo
    curl -s "$CONSUL_URL/v1/kv/config/?recurse=true" | jq -r '.[].Key' | sort
    echo
}

# Function to get configuration value
get_config() {
    local key=$1
    if [ -z "$key" ]; then
        print_error "Key parameter is required"
        return 1
    fi
    
    print_info "Getting configuration for key: $key"
    result=$(curl -s "$CONSUL_URL/v1/kv/config/$key")
    
    if [ "$result" = "null" ] || [ -z "$result" ]; then
        print_warning "Key not found: config/$key"
        return 1
    fi
    
    echo "$result" | jq -r '.[0].Value' | base64 -d
}

# Function to set configuration value
set_config() {
    local key=$1
    local value=$2
    
    if [ -z "$key" ] || [ -z "$value" ]; then
        print_error "Both key and value parameters are required"
        return 1
    fi
    
    print_info "Setting configuration for key: config/$key"
    if curl -s -X PUT "$CONSUL_URL/v1/kv/config/$key" -d "$value" > /dev/null; then
        print_success "Successfully set config/$key"
    else
        print_error "Failed to set config/$key"
        return 1
    fi
}

# Function to delete configuration
delete_config() {
    local key=$1
    if [ -z "$key" ]; then
        print_error "Key parameter is required"
        return 1
    fi
    
    print_warning "Deleting configuration for key: config/$key"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if curl -s -X DELETE "$CONSUL_URL/v1/kv/config/$key" > /dev/null; then
            print_success "Successfully deleted config/$key"
        else
            print_error "Failed to delete config/$key"
            return 1
        fi
    else
        print_info "Deletion cancelled"
    fi
}

# Function to backup configurations
backup_configs() {
    local backup_file=${1:-"consul-config-backup-$(date +%Y%m%d-%H%M%S).json"}
    
    print_info "Backing up configurations to: $backup_file"
    curl -s "$CONSUL_URL/v1/kv/config/?recurse=true" > "$backup_file"
    
    if [ $? -eq 0 ] && [ -s "$backup_file" ]; then
        print_success "Backup created successfully: $backup_file"
    else
        print_error "Backup failed"
        return 1
    fi
}

# Function to restore configurations
restore_configs() {
    local backup_file=$1
    if [ -z "$backup_file" ] || [ ! -f "$backup_file" ]; then
        print_error "Backup file is required and must exist"
        return 1
    fi
    
    print_warning "Restoring configurations from: $backup_file"
    print_warning "This will overwrite existing configurations!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        while IFS= read -r line; do
            key=$(echo "$line" | jq -r '.Key')
            value=$(echo "$line" | jq -r '.Value' | base64 -d)
            
            if [ "$key" != "null" ]; then
                curl -s -X PUT "$CONSUL_URL/v1/kv/$key" -d "$value" > /dev/null
                echo "Restored: $key"
            fi
        done < <(jq -c '.[]' "$backup_file")
        
        print_success "Configuration restore completed"
    else
        print_info "Restore cancelled"
    fi
}

# Function to watch for configuration changes
watch_configs() {
    local key=${1:-"config/"}
    print_info "Watching for changes in: $key"
    print_info "Press Ctrl+C to stop watching"
    
    # Get initial index
    local index=$(curl -s "$CONSUL_URL/v1/kv/$key?recurse=true" -I | grep -i "x-consul-index" | cut -d' ' -f2 | tr -d '\r')
    
    while true; do
        # Wait for changes
        result=$(curl -s "$CONSUL_URL/v1/kv/$key?recurse=true&index=$index&wait=30s")
        
        if [ "$?" -eq 0 ] && [ "$result" != "null" ]; then
            new_index=$(curl -s "$CONSUL_URL/v1/kv/$key?recurse=true" -I | grep -i "x-consul-index" | cut -d' ' -f2 | tr -d '\r')
            
            if [ "$new_index" != "$index" ]; then
                print_info "Configuration change detected at $(date)"
                echo "$result" | jq -r '.[].Key'
                index=$new_index
            fi
        fi
        
        sleep 1
    done
}

# Function to validate YAML configuration
validate_yaml() {
    local key=$1
    if [ -z "$key" ]; then
        print_error "Key parameter is required"
        return 1
    fi
    
    print_info "Validating YAML for key: config/$key"
    value=$(get_config "$key")
    
    if [ $? -eq 0 ]; then
        echo "$value" | python3 -c "
import sys
import yaml
try:
    yaml.safe_load(sys.stdin.read())
    print('✅ YAML is valid')
    sys.exit(0)
except yaml.YAMLError as e:
    print(f'❌ YAML is invalid: {e}')
    sys.exit(1)
" 2>/dev/null || echo "⚠️  Python3 with PyYAML required for validation"
    fi
}

# Main function
main() {
    case "${1}" in
        "list"|"ls")
            check_consul && list_configs
            ;;
        "get")
            check_consul && get_config "$2"
            ;;
        "set")
            check_consul && set_config "$2" "$3"
            ;;
        "delete"|"del"|"rm")
            check_consul && delete_config "$2"
            ;;
        "backup")
            check_consul && backup_configs "$2"
            ;;
        "restore")
            check_consul && restore_configs "$2"
            ;;
        "watch")
            check_consul && watch_configs "$2"
            ;;
        "validate")
            check_consul && validate_yaml "$2"
            ;;
        "help"|"-h"|"--help"|"")
            echo "TradeMaster Consul Configuration Manager"
            echo
            echo "Usage: $0 <command> [options]"
            echo
            echo "Commands:"
            echo "  list                    List all configuration keys"
            echo "  get <key>              Get configuration value"
            echo "  set <key> <value>      Set configuration value"
            echo "  delete <key>           Delete configuration key"
            echo "  backup [file]          Backup all configurations"
            echo "  restore <file>         Restore configurations from backup"
            echo "  watch [key]            Watch for configuration changes"
            echo "  validate <key>         Validate YAML configuration"
            echo "  help                   Show this help message"
            echo
            echo "Examples:"
            echo "  $0 list"
            echo "  $0 get application/data"
            echo "  $0 set trading-service/data 'server:\n  port: 8083'"
            echo "  $0 backup my-backup.json"
            echo "  $0 watch config/trading-service/"
            echo
            ;;
        *)
            print_error "Unknown command: $1"
            echo "Use '$0 help' for available commands"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"