#!/bin/bash
set -x

APP_NAME=covid
SERVICE_NAME=services/$APP_NAME-service
DEPLOYMENT_NAME=deployments/$APP_NAME

kubectl get deployments
kubectl get pods
kubectl get services

NODE_PORT=$(kubectl get $SERVICE_NAME -o go-template='{{(index .spec.ports 0).nodePort}}')
MINIKUBE_URL=$(minikube ip)
export POD_NAME=$(kubectl get pods -o go-template --template '{{range .items}}{{.metadata.name}}{{"\n"}}{{end}}')

echo
curl $MINIKUBE_URL:$NODE_PORT/covid-19-jhu-csse-service/ping
echo
kubectl exec -ti $POD_NAME curl localhost:8080/covid-19-jhu-csse-service/ping
echo
