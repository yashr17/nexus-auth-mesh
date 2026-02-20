# Define the path to the compose file once
COMPOSE_FILE := docker/docker-compose.yml

# Helper to run docker-compose commands with the correct file path
DC := docker-compose -f $(COMPOSE_FILE)

.PHONY: up down restart logs ps clean build-all

# Start the infrastructure in the background
up:
	$(DC) up -d

# Stop and remove containers
down:
	$(DC) down

# Restart the infrastructure
restart:
	$(DC) restart

# Follow the logs of all containers
logs:
	$(DC) logs -f

# Check status of containers
status:
	$(DC) ps

# Clean up volumes (Warning: This deletes your database data!)
clean-data:
	$(DC) down -v

# Full build of the Java project
build-java:
	./mvnw clean install