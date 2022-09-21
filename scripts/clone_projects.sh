#!/bin/bash

cd ..
mkdir projects
cd projects


# Spring

mkdir spring
cd spring

git clone https://github.com/apache/rocketmq-dashboard.git
git clone https://github.com/paulhoule/infovore.git
git clone https://github.com/awspring/spring-cloud-aws.git
git clone https://github.com/aks-cykcun/J-MR-Tp.git

cd ..
mkdir guice
cd guice

git clone https://github.com/logzio/apollo.git
git clone https://github.com/wttech/bobcat.git
git clone https://github.com/mgodave/barge.git
git clone https://github.com/ninjaframework/ninja.git
git clone https://github.com/HubSpot/dropwizard-guice.git


cd ..
mkdir cdi
cd cdi

git clone https://github.com/weld/weld-testing.git
git clone https://github.com/smallrye/smallrye-reactive-messaging.git

cd ..