#!/bin/bash
set -x
kustomize build . | kubectl apply -f -
kubectl get deployments
kubectl get pods
kubectl get services
curl $(minikube ip):30199/hello