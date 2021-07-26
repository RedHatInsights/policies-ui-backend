#!/bin/bash

set -exv

IMAGE="quay.io/cloudservices/policies-ui-backend"
IMAGE_TAG=$(git rev-parse --short=7 HEAD)

if [[ "$1" != "testrun" ]];
then
    if [[ -z "$QUAY_USER" || -z "$QUAY_TOKEN" ]]; then
        echo "QUAY_USER and QUAY_TOKEN must be set"
        exit 1
    fi
    if [[ -z "$RH_REGISTRY_USER" || -z "$RH_REGISTRY_TOKEN" ]]; then
        echo "RH_REGISTRY_USER and RH_REGISTRY_TOKEN  must be set"
        exit 1
    fi
fi

DOCKER_CONF="$PWD/.docker"
mkdir -p "$DOCKER_CONF"

if [[ "$1" != "testrun" ]];
then
    docker --config="$DOCKER_CONF" login -u="$QUAY_USER" -p="$QUAY_TOKEN" quay.io
    docker --config="$DOCKER_CONF" login -u="$RH_REGISTRY_USER" -p="$RH_REGISTRY_TOKEN" registry.redhat.io
fi

docker --config="$DOCKER_CONF" build -t "${IMAGE}:${IMAGE_TAG}" . -f src/main/docker/Dockerfile-build.jvm

if [[ "$1" != "testrun" ]];
then
    docker --config="$DOCKER_CONF" push "${IMAGE}:${IMAGE_TAG}"
    docker --config="$DOCKER_CONF" tag "${IMAGE}:${IMAGE_TAG}" "${IMAGE}:qa"
    docker --config="$DOCKER_CONF" push "${IMAGE}:qa"
    docker --config="$DOCKER_CONF" tag "${IMAGE}:${IMAGE_TAG}" "${IMAGE}:latest"
    docker --config="$DOCKER_CONF" push "${IMAGE}:latest"
fi

if [ "$1" = "testrun" ];
then
    # Until test results produce a junit XML file, create a dummy result file so Jenkins will pass
    mkdir -p $WORKSPACE/artifacts
    cat << EOF > ${WORKSPACE}/artifacts/junit-dummy.xml
    <testsuite tests="1">
        <testcase classname="dummy" name="dummytest"/>
    </testsuite>
EOF
fi