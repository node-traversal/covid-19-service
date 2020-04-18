#!/bin/bash
set -x

APP_NAME=covid
SERVICE_NAME=services/$APP_NAME-service
DEPLOYMENT_NAME=deployments/$APP_NAME-deployment

echo
kubectl delete $DEPLOYMENT_NAME
kubectl delete $SERVICE_NAME

kubectl get deployments
kubectl get pods
kubectl get services
