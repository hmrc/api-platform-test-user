#!/bin/bash

sbt clean compile coverage test it:test coverageOff coverageReport
python dependencyReport.py api-platform-test-user
