# Monitoring Service

## Overview

The Monitoring Service allows users to configure between 1 and 5 monitoring jobs. Each job will periodically test a specified URL and record the outcome of each test (e.g., success, failure, response time, and any error messages).  
The system stores these results and expose them via a REST API.

## Documentation

The documentation of this project is available [here](http://0.0.0.0:8000).

## Api First

This project is following APi First design. So for adding/removing/update endpoints to the api, the file `src/main/resources/static/openapi.yaml` should be updated first. Then follow [these](#generate-rest-api)
instructions to generate the code. While editing `openapi.yaml` please don't forget to add examples.

## Running locally

### Generate REST Api

This project was design with Api First approach, therefore in order to compile it's needed to generate the rest api interfaces from the OpenApi spec. For that run the command:

```
./mvnw clean compile
```

### OpenApi Linter

To run openapi linter and have a better report of the errors/warning run:

```
docker run --rm -v $PWD/src/main/resources/static:/work dshanley/vacuum html-report openapi.yaml
```

Notes:

* For `html-report` command there is no option to ignore file. So expect warnings being ignored to be present
* An `report.html` file is created under `src/main/resources/static`, open it with your browser

### Run the app

#### Option 1
Bootstrap the database:
```
docker compose up -d
```

Bootstrap docs:
```
docker run --rm -it -p 8000:8000 -v ${PWD}:/docs --entrypoint sh squidfunk/mkdocs-material:9.5.2 -c "pip install mdx_include && cd src/doc && mkdocs serve --dev-addr=0.0.0.0:8000"
```

Bootstrap the app:
```
./mvnw spring-boot:run
```

App api available at: http://localhost:8080/monitoring-service/swagger-ui/index.html      
Docs available at: http://0.0.0.0:8000

#### Option 2
Use docker compose for all.

Build the docker image:
```
./mvnw jib:dockerBuild 
```

Bootstrap all:
```
docker compose -f docker-compose-full.yml up
```

App api available at: http://localhost:8080/monitoring-service/swagger-ui/index.html       
Docs available at: http://0.0.0.0:8000

