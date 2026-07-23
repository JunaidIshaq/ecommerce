#!/bin/bash
# Show logs for all services or specific service if provided
SERVICE="$1"
sudo ./deploy.sh logs "$SERVICE"
