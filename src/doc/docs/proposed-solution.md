# Proposed Solution

## The Problem
Develop a monitoring tool using Java and Spring Boot that allows users to configure between 1 to 5 monitoring jobs. Each job will periodically test a specified URL and record the outcome of each test (e.g., success, failure, response time). The system must store these results and expose them via a REST API, which will be consumed by a separate React frontend application.

## Architecture
The REST API it's divided into two types os operations, managing jobs (create, update, delete, etc) and get the results of those jobs executions (listing with filters, delete executions, etc). The API it's based on a openapi file following the concept of api first.

The applications it's divided into several layers that are independent of each other following the concept of loose coupling. Popular Hexagonal architecture was not implemented in this case since could be overkill for the microservice in hands.  
The main application layers includes:

* Persistence layer - responsible to talk with te database
* Service layer - responsible for the business logic, model validations are done in this layer to allow to change the interface if needed
* Rest layer -  application interface, based on a openapi file with generated code

To manually create jobs and schedule them at specify internal the framework chosen was quartz.

The database chosen was PostgreSQL, a popular and widely used database. Although I have worked with several databases, the ones with more experience are PostgreSQL and MongoDB. Since chose to use quartz framework and there is no official support for MongoDB, PostgreSQL was a natural choice.  
The application persistence framework chosen was spring-data-jpa due to its simplicity.

## Application code walkthrough
### Layers
* Configuration
    * Enable auditor aware, a spring feature that allows to out of the box audit fields in the database like createdDate, createdBy, etc
    * ModelMapper to map POJOs between app layers
* Exception
    * Have a common exception that handles ProblemDetail and facilitate error handling
* Persistence
    * Spring data jpa it's simple to use and save a lot of boilerplate code 
    * Listing includes pagination with information about the total elements, total pages and total elements per page. Also have a default limit to avoid out of memory issues. This limit can be configured through properties
* Rest
    * Filter to log all requests and responses
    * Global exception handler to do error handling 
    * Controllers implementing auto generated apis from openapi file
* Service
    * Use data transfer objects following onion principle
    * Use interfaces to be easily replace the implementation if needed
    * Use validation annotations as much as possible to have a more clean code
    * Max allowed jobs in the system can be configured through properties

### Flyway
Using flyway for database migration.

### Swagger
Using Swagger to expose the openapi file and have an easy interface to interact with the application.

### Spotless
To make sure the code it's formatted according to specified rules.

### Testing
Use test containers to get a more real environment when doing unit tests. Use sql scripts to pre-populate the database.

### Actuator
Use spring actuator to expose a management service in a different port. Although was not required, added some production grade features like exporting metric through prometheus and include the database in the readiness probe.

### Docker Compose
One docker compose file with only the database to be used while developing. And another full docker compose file with all dependencies ready to go. It contains the database, documentation and the application.

### GITLAB CI/CD
Create a small CI/CD with openapi linter, maven verify, docker image build and publish project documentation in GitLab Pages.

## Future Improvements
* Generate javadoc and make the static html available through GitLab Pages
* Security scans, like Snyk or use GitLab security scans
* Setup Renovate Bot to keep dependencies up to date
* Add a code quality tool like Sonar
* Include soft delete feature, allows users to delete resources but restore them within a retention period
* Make jobs cluster compatible, using quartz clustered option or a framework like shedlock
* Api versioning, preferable through content negotiation but understandable that it's hard to implement for clients
* Include Spring Security and protect the api with a bearer token
* Improve database performance, job executions it's predictable that will have a huge amount of data. Solutions like read replicas, sharding and partitions should be considered
* Improve logging, and potentially change to json since it's more cloud friendly
* Continuing to evolve the api
    * More crud endpoints
    * More filters
    * Have more sophisticated scheduler options besides a cron expression
    * Create alerts
    * Send notifications
* Implement a release process
* Deploy to kubernetes
    * Deployment
    * ConfigMap
    * HorizontalPodAutoscaler
    * PodDisruptionBudget
    * Service
    * Ingress/VirtualService(Istio)
* Get documentation in its own git repo

## Alternatives
* GCP Monitoring
    * Create an Uptime Check. It's possible to monitor an url and set the frequency. It also allows to validate the response and create alerts and send a message to several notifications channels, like PagerDuty, Slack, Webhooks, Pub/Sub, etc.
* Other Cloud providers should have similar functionality
* Search online for url monitor tools