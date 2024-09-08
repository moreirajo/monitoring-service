# Monitoring Service

The Monitoring Service allows users to configure between 1 and 5 monitoring jobs. Each job will periodically test a specified URL and record the outcome of each test (e.g., success, failure, response time, and any error messages). The system stores these results and expose them via a REST API.

Lastly, the backend was built with [Spring Boot](https://spring.io/projects/spring-boot) framework, and [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html).

## Features

* REST API to manage Jobs
* REST API to retrieve Jobs Executions results
* REST API errors adheres to the [RFC7807](https://datatracker.ietf.org/doc/html/rfc7807) specification
* Jobs Executions fetching with filters and pagination
* Define Jobs scheduling with cron expressions

## Dependencies

* PostgresSQL

## Usage

### API

Monitoring Service offers a REST API to manage Jobs and retrieve execution results.

Detailed documentation of the REST API can be found at <http://localhost:8080/monitoring-service/swagger-ui/index.html>.

#### Jobs Management

A Job is defined by the following properties:

* **name** - the name the will identify the Job
* **description** - helps to identify the purpose of the Job
* **url** - the url to monitor
* **cronExpression** - defines the Job scheduling
* **timezone** - the timezone of the scheduler, defaults to UTC

##### Create new Job

Endpoint: `POST /jobs`

Creates a new Job to monitor some url. Only supports quartz cron expressions to define scheduling, more details [here](https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html).

!!! warning 
    Can only exist one job with the same name.

!!! warning 
    Currently the system only supports up to 5 jobs.

#### Jobs Executions

For each Job Execution it's store the outcome of the call and it's compose by:

* **jobName** - the name of the job associated to the execution
* **url** - the url being tested
* **status** - the status of the execution, it can be `SUCCEEDED` or `FAILED`
* **errorMessage** - in case the status is `FAILED` the error message is stored
* **responseTime** - how long it took the url to respond
* **createdDate** - when the execution run

##### Retrieve Jobs Executions results

Endpoint: `GET /jobs-executions`    

Retrieve all results of Jobs Executions and supports filtering by:

* **jobName** - filter by job name
* **url** - filter by Job url
* **status** - filter by status, allowed values are `SUCCEEDED` and `FAILED`
* **from** - filter results from a specific date
* **to** - filter results to a specific date

This endpoint also supports pagination, making it suitable for frontend applications.
 
There is a limit of 100 in the page size, it is also the default value.

!!! info 
    The maximum page size can be configured differently per environment.

### Errors

Errors format returned by the API are compliant with [RFC7807](https://datatracker.ietf.org/doc/html/rfc7807) specification.  
For technical errors please contact [support](resources.md#support), for functional errors please change the request according to the error.

<table>
  <tr>
    <th>Code</th>
    <th>Type</th>
    <th>Case</th>
    <th>HTTP status code</th>
    <th>Example</th>
  </tr>

  <tr>
    <td>invalid_request_params</td>
    <td>Functional</td>
    <td>When one or multiple fields in the request fail validation.</td>
    <td>400</td>
    <td>
```json
{
  "status": 400,
  "type": "https://www.hansecom.com/errors/invalid_request_params",
  "title": "Your request parameters didn't validate.",
  "code": "invalid_request_params",
  "detail": "prop1 cannot be empty",
  "traceId": "ffb75ca1385ecb0a362ff9cd029d23de",
  "invalidParams": [
    {
      "name": "prop1",
      "reason": "cannot be empty"
    }
  ]
}
```
    </td>
  </tr>

  <tr>
    <td>job_already_exists</td>
    <td>Functional</td>
    <td>When creating a job with a name that already exists.</td>
    <td>409</td>
    <td>
```json
{
  "status": "409,",
  "type": "https://www.hansecom.com/errors/job_already_exists",
  "title": "Job already exists,",
  "code": "job_already_exists,",
  "detail": "Job with name google already exists,",
  "traceId": "ffb75ca1385ecb0a362ff9cd029d23de,",
  "jobName": "google"
}
```
    </td>
  </tr>

  <tr>
    <td>max_jobs_reach</td>
    <td>Functional</td>
    <td>When the system max allowed jobs is reached.</td>
    <td>422</td>
    <td>
```json
{
  "status": 422,
  "type": "https://www.hansecom.com/errors/max_jobs_reach",
  "title": "Max jobs reached",
  "code": "max_jobs_reach",
  "detail": "You have reach the system limit of 5 jobs",
  "traceId": "ffb75ca1385ecb0a362ff9cd029d23de"
}
```
    </td>
  </tr>

  <tr>
    <td>problem_with_request</td>
    <td>Functional</td>
    <td>When there is a problem with the request.</td>
    <td>4xx<sup>1</td>
    <td>
```json
{
  "status": 4xx,
  "type": "https://www.hansecom.com/errors/problem_with_request",
  "title": "There is a problem with the request.",
  "code": "problem_with_request",
  "detail": "404 NOT_FOUND",
  "traceId": "ffb75ca1385ecb0a362ff9cd029d23de"
}
```
    </td>
  </tr>

  <tr>
    <td>internal_server_error</td>
    <td>Technical</td>
    <td>Whenever some unpredictable hazard happens.</td>
    <td>500</td>
    <td>
```json
{
  "status": 500,
  "type": "https://www.hansecom.com/errors/internal_server_error",
  "title": "Internal error on the server",
  "code": "internal_server_error",
  "detail": "Some internal server error happen. Please provide the traceId ffb75ca1385ecb0a362ff9cd029d23de to the support team.",
  "traceId": "ffb75ca1385ecb0a362ff9cd029d23de"
}
```
    </td>
  </tr>

</table>

[^1]: This means that the response is generic and may have multiple possibilities in this field.

## Source code

<https://gitlab.com/moreirajo1/monitoring-service>

## Contribute

Found a bug? [File a bug report!](https://gitlab.com/moreirajo1/monitoring-service/-/issues).

## Releases

GitLab: <https://gitlab.com/moreirajo1/monitoring-service/-/releases>  