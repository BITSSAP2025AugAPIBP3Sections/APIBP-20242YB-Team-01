#!/usr/bin/env bash
set -euo pipefail

# Use a unique tag for all images
IMAGE_TAG="dev"
SERVICES=(
  user-service
  auction-service
  analytics-service
  payment-service
  notification-service
)

for SERVICE in "${SERVICES[@]}"; do
  IMAGE="apibp-20242yb-team-01-${SERVICE}:${IMAGE_TAG}"
  DIR="./services/${SERVICE}"
  echo "[K8S] Building $IMAGE from $DIR ..."
  docker build -t "$IMAGE" "$DIR"
  # Robust image existence check (handle both with and without 'library/' prefix)
  IMAGE_ALT="library/apibp-20242yb-team-01-${SERVICE}:${IMAGE_TAG}"
  if ! docker image inspect "$IMAGE" > /dev/null 2>&1 && ! docker image inspect "$IMAGE_ALT" > /dev/null 2>&1; then
    echo "[K8S][ERROR] Image $IMAGE (or $IMAGE_ALT) was not built successfully. Aborting."
    exit 1
  fi
  echo "[K8S] Loading $IMAGE into kind ..."
  kind load docker-image "$IMAGE"
done

echo "[K8S] All images built and loaded into kind with tag: $IMAGE_TAG."
