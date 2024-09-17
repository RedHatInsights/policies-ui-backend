#!/bin/bash

set -exv

# Clowder config
export APP_NAME="policies"
export COMPONENT_NAME="policies-ui-backend"
export IMAGE="quay.io/cloudservices/policies-ui-backend"
export DEPLOY_TIMEOUT="600"

# IQE plugin config
export IQE_PLUGINS="policies, floorist"
export IQE_MARKER_EXPRESSION="policies_api_smoke or floorist_smoke"
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

# Until test results produce a junit XML file, create a dummy result file so Jenkins will pass
mkdir -p $WORKSPACE/artifacts
cat << EOF > ${WORKSPACE}/artifacts/junit-dummy.xml
<testsuite tests="1">
    <testcase classname="dummy" name="dummytest"/>
</testsuite>
EOF
