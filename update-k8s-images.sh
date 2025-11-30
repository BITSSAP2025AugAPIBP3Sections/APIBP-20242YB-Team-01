#!/bin/bash

# Script to update Kubernetes manifests with Docker Hub images
# This updates all image references in k8s/*.yaml files

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
DOCKER_USERNAME="${DOCKER_USERNAME:-}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
K8S_DIR="k8s"

print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

get_docker_username() {
    if [ -z "$DOCKER_USERNAME" ]; then
        print_message "$YELLOW" "Please enter your Docker Hub username:"
        read -r DOCKER_USERNAME
        
        if [ -z "$DOCKER_USERNAME" ]; then
            print_message "$RED" "❌ Error: Docker Hub username is required"
            exit 1
        fi
    fi
    print_message "$GREEN" "✅ Using Docker Hub username: $DOCKER_USERNAME"
}

update_k8s_manifest() {
    local file=$1
    local service_name=$2
    local image_name=$3
    
    if [ -f "$file" ]; then
        print_message "$YELLOW" "📝 Updating $file..."
        
        # Backup original file
        cp "$file" "${file}.backup"
        
        # Update image reference in the YAML file
        # This handles various possible image formats
        sed -i.tmp "s|image: .*${service_name}.*|image: ${DOCKER_USERNAME}/${image_name}:${IMAGE_TAG}|g" "$file"
        rm -f "${file}.tmp"
        
        print_message "$GREEN" "✅ Updated: $file"
    else
        print_message "$RED" "⚠️  File not found: $file"
    fi
}

main() {
    print_message "$GREEN" "╔════════════════════════════════════════════════════╗"
    print_message "$GREEN" "║  Kubernetes Manifest Update Script                ║"
    print_message "$GREEN" "╚════════════════════════════════════════════════════╝\n"
    
    get_docker_username
    
    # Update each service manifest
    update_k8s_manifest "${K8S_DIR}/user-service.yaml" "user-service" "ebaazee-user-service"
    update_k8s_manifest "${K8S_DIR}/auction-service.yaml" "auction-service" "ebaazee-auction-service"
    update_k8s_manifest "${K8S_DIR}/payment-service.yaml" "payment-service" "ebaazee-payment-service"
    update_k8s_manifest "${K8S_DIR}/notification-service.yaml" "notification-service" "ebaazee-notification-service"
    update_k8s_manifest "${K8S_DIR}/analytics-service.yaml" "analytics-service" "ebaazee-analytics-service"
    
    print_message "$GREEN" "\n✨ Kubernetes manifests updated successfully!"
    print_message "$YELLOW" "\n📝 Backup files created with .backup extension"
    print_message "$YELLOW" "📝 You can now deploy using: kubectl apply -f k8s/\n"
}

main
