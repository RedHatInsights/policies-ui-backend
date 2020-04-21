#!/bin/sh

GIT_SHA=`git rev-parse HEAD`
GIT_BRANCH=`git rev-parse --abbrev-ref HEAD`
BUILD_TIME=`date`
BUILD_HOST=`hostname`

echo "

package com.redhat.cloud.policies.app;

final class Version {

   static final String GIT_SHA = \"$GIT_SHA\";
   static final String GIT_BRANCH = \"$GIT_BRANCH\";
   static final String COMPILE_TIME = \"$BUILD_TIME\";
   static final String BUILD_HOST = \"$BUILD_HOST\";
}
" > src/main/java/com/redhat/cloud/policies/app/Version.java
