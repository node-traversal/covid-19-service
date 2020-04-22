#!/bin/bash
set -x

APP_NAME=covid
SERVICE_NAME=services/$APP_NAME-service
DEPLOYMENT_NAME=deployments/$APP_NAME

#echo
#kubectl delete $DEPLOYMENT_NAME
#kubectl delete $SERVICE_NAME

kustomize build kube/ | kubectl apply -f -
kubectl get deployments
kubectl get pods
kubectl get services

