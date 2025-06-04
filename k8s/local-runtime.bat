@echo off
cd ..\

call gradlew :agent:build

docker build -f docker/Dockerfile -t polocloud-agent:latest .

pause