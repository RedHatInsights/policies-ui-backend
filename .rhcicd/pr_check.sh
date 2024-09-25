#!/bin/bash

set -exv

# Clowder config
export APP_NAME="policies"
export COMPONENT_NAME="policies-ui-backend"
export IMAGE="quay.io/cloudservices/policies-ui-backend"
export DEPLOY_TIMEOUT="600"

# IQE plugin config
export IQE_PLUGINS="policies"
export IQE_MARKER_EXPRESSION="policies_api_smoke"
export IQE_FILTER_EXPRESSION=""
export IQE_CJI_TIMEOUT="30m"

# Bonfire init
CICD_URL=https://raw.githubusercontent.com/RedHatInsights/bonfire/master/cicd
curl -s $CICD_URL/bootstrap.sh > .cicd_bootstrap.sh && source .cicd_bootstrap.sh

# Build the image and push to Quay
export DOCKERFILE=src/main/docker/Dockerfile.jvm
source $CICD_ROOT/build.sh

# Deploy on ephemeral
export COMPONENTS_W_RESOURCES="policies-engine"
source $CICD_ROOT/deploy_ephemeral_env.sh

# Run smoke tests with ClowdJobInvocation
source $CICD_ROOT/cji_smoke_test.sh

mkdir -p $WORKSPACE/artifacts

# Update IQE plugin config to run floorist plugin tests.
export COMPONENT_NAME="policies-ui-backend"
export IQE_CJI_NAME="floorist"
# Pass in FLOORPLANS_2_TEST.
export IQE_ENV_VARS="FLOORPLANS_TO_TEST=policies-backend-hms"
export IQE_PLUGINS="floorist"
export IQE_MARKER_EXPRESSION="floorist_smoke"
export IQE_IMAGE_TAG="floorist"

# Run smoke tests with ClowdJobInvocation
source $CICD_ROOT/cji_smoke_test.sh
