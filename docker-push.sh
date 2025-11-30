#!/usr/bin/env bash

# Docker Hub Push Script for Ebaazee Microservices
# This script builds and pushes all microservice images to Docker Hub

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DOCKER_USERNAME="${DOCKER_USERNAME:-}"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-docker.io}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

# Service definitions (path|image-name pairs)
SERVICE_PATHS=(
    "services/user-service"
    "services/auction-service"
    "services/payment-service"
    "services/notification-service"
    "services/analytics-service"
    "ebaazee-frontend"
)

SERVICE_NAMES=(
    "ebaazee-user-service"
    "ebaazee-auction-service"
    "ebaazee-payment-service"
    "ebaazee-notification-service"
    "ebaazee-analytics-service"
    "ebaazee-frontend"
)

# Function to print colored messages
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_message "$RED" "❌ Error: Docker is not running. Please start Docker Desktop."
        exit 1
    fi
    print_message "$GREEN" "✅ Docker is running"
}

# Function to prompt for Docker Hub username
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

# Function to login to Docker Hub
docker_login() {
    print_message "$YELLOW" "🔐 Logging in to Docker Hub..."
    
    if docker login; then
        print_message "$GREEN" "✅ Successfully logged in to Docker Hub"
    else
        print_message "$RED" "❌ Failed to login to Docker Hub"
        exit 1
    fi
}

# Function to build and push a service
build_and_push_service() {
    local service_path=$1
    local image_name=$2
    local full_image_name="${DOCKER_USERNAME}/${image_name}:${IMAGE_TAG}"
    
    print_message "$YELLOW" "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_message "$YELLOW" "📦 Building: $image_name"
    print_message "$YELLOW" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
    
    # Build the image
    if docker build -t "$full_image_name" "$service_path"; then
        print_message "$GREEN" "✅ Successfully built: $full_image_name"
    else
        print_message "$RED" "❌ Failed to build: $image_name"
        return 1
    fi
    
    # Push the image
    print_message "$YELLOW" "🚀 Pushing: $full_image_name"
    if docker push "$full_image_name"; then
        print_message "$GREEN" "✅ Successfully pushed: $full_image_name"
    else
        print_message "$RED" "❌ Failed to push: $image_name"
        return 1
    fi
}

# Function to create image list summary
create_summary() {
    print_message "$GREEN" "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_message "$GREEN" "🎉 All images successfully pushed to Docker Hub!"
    print_message "$GREEN" "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
    
    print_message "$YELLOW" "📋 Image List:"
    for image_name in "${SERVICE_NAMES[@]}"; do
        echo "   • ${DOCKER_USERNAME}/${image_name}:${IMAGE_TAG}"
    done
    
    print_message "$YELLOW" "\n📝 To pull these images:"
    print_message "$NC" "   docker pull ${DOCKER_USERNAME}/ebaazee-user-service:${IMAGE_TAG}"
    print_message "$NC" "   docker pull ${DOCKER_USERNAME}/ebaazee-auction-service:${IMAGE_TAG}"
    print_message "$NC" "   docker pull ${DOCKER_USERNAME}/ebaazee-payment-service:${IMAGE_TAG}"
    print_message "$NC" "   docker pull ${DOCKER_USERNAME}/ebaazee-notification-service:${IMAGE_TAG}"
    print_message "$NC" "   docker pull ${DOCKER_USERNAME}/ebaazee-analytics-service:${IMAGE_TAG}"
    print_message "$NC" "   docker pull ${DOCKER_USERNAME}/ebaazee-frontend:${IMAGE_TAG}"
    
    print_message "$YELLOW" "\n📝 Or update your Kubernetes manifests with:"
    print_message "$NC" "   image: ${DOCKER_USERNAME}/ebaazee-<service-name>:${IMAGE_TAG}"
}

# Main execution
main() {
    print_message "$GREEN" "╔════════════════════════════════════════════════════╗"
    print_message "$GREEN" "║  Ebaazee Docker Hub Push Script                   ║"
    print_message "$GREEN" "╚════════════════════════════════════════════════════╝\n"
    
    # Pre-flight checks
    check_docker
    get_docker_username
    docker_login
    
    # Build and push all services
    local failed_services=()
    
    for i in "${!SERVICE_PATHS[@]}"; do
        service_path="${SERVICE_PATHS[$i]}"
        image_name="${SERVICE_NAMES[$i]}"
        if ! build_and_push_service "$service_path" "$image_name"; then
            failed_services+=("$image_name")
        fi
    done
    
    # Report results
    if [ ${#failed_services[@]} -eq 0 ]; then
        create_summary
        print_message "$GREEN" "\n✨ All done! Your images are now available on Docker Hub.\n"
        exit 0
    else
        print_message "$RED" "\n❌ Failed to push the following services:"
        for service in "${failed_services[@]}"; do
            echo "   • $service"
        done
        exit 1
    fi
}

# Run main function
main
