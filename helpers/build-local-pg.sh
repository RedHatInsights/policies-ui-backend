#!/bin/sh
cp ../src/test/sql/dbinit.sql .
docker build -t my-postgres -f Dockerfile-postgres .
