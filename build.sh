#!/bin/bash

mvn -Dmaven.test.skip=true clean package

cp -rf ./target/*-make-assembly.tar.gz ../avalon

