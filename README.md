# Solarsimulator

## Overview

SolarSimulator is a Kotlin-based application designed to simulate solar power generation scenarios. It provides a web service to manage and analyze a network of solar power plants over time.


## Project Structure

The project is organized into several key components:

- **Service Layer** (`com.fastned.solarsimulator.service`): Contains business logic for calculations and operations, implementing the core functionality of the application.
- **Repository Layer** (`com.fastned.solarsimulator.db`): Handles database interactions for storing and retrieving power plant data.
- **Controller Layer** (`com.fastned.solarsimulator.controller`): Defines REST endpoints for managing power plants and simulating power generation.


## API Specification
The API specification is defined in the `resources/specs/specs.yaml` file, which describes the endpoints, request/response bodies, and error codes for the application. The specification is used to generate the controller classes and data models for the application.

## Implementation Details
- Language: Kotlin
- Framework: Spring Boot
- Database: PostgreSQL(Docker)
- Testing: JUnit

## Running the Application
To run the application, you need to have Docker installed on your machine. You can start the application by running the following command:

```./gradlew :bootRun```

This will start the application on port 8080.

## Testing the Application

You can run the unit tests for the application by running the following command:

```./gradlew :test```

