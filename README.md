# Software Engineer Test Task

As a test task for [Klaus](https://www.klausapp.com) software engineering position we ask our candidates to build a small [gRPC](https://grpc.io) service using language of their choice. Preferred language for new services in Klaus is [Go](https://golang.org).

The service should be using provided sample data from SQLite database (`database.db`).

Please fork this repository and share the link to your solution with us.

# Table of Contents
1. [Overview of the solution](#overview-of-the-solution)
2. [Tasks](#tasks)
    1. [Task 1: Solution](#task-1-solution)
    2. [Task 2.1: Solution](#task-21-solution)
    3. [Task 2.2: Solution](#task-22-solution)
    4. [Task 2.3: Solution](#task-23-solution)
    5. [Task 2.4: Solution](#task-24-solution)
3. [Bonus](#bonus)
4. [Ideas for further improvement](#ideas-for-further-improvement)
5. [Application set-up](#application-set-up)
    1. [Local Environment](#local-environment)
    2. [Docker Environment](#docker-environment)
6. [UI, REST API, gRPC specification](#ui-rest-api-grpc-specification)
    1. [Task 2.1 Aggregated category scores over a period of time](#task-21-aggregated-category-scores-over-a-period-of-time)
        1. [UI](#ui)
        2. [REST API](#rest-api)
        3. [gRPC](#grpc)
    2. [Task 2.2 Scores by ticket](#task-22-scores-by-ticket)
        1. [UI](#ui-1)
        2. [REST API](#rest-api-1)
        3. [gRPC](#grpc-1)
    3. [Task 2.3 Overall quality score](#task-23-overall-quality-score)
        1. [UI](#ui-2)
        2. [REST API](#rest-api-2)
        3. [gRPC](#grpc-2)
    4. [Task 2.4 Overall quality score](#task-24-overall-quality-score)
        1. [UI](#ui-3)
        2. [REST API](#rest-api-3)
        3. [gRPC](#grpc-3)

---
# Overview of the solution

I decided to write this application in Java 17 using Spring Boot 3.3 and Maven as the build system. In addition to implementing the gRPC service, I also opted to create a simple frontend for easier testing and a REST API bridge to access the gRPC service. The frontend is written in Vue 3 using the composition API syntax.\
This readme will first delve into explanation of my approach to solving each of the tasks, will outline the additional functionalities I would implement if this application actually needed to go into production, and is then followed by the application set-up and UI, API, and gRPC documentation to test the application.

---

### Tasks

1. Come up with ticket score algorithm that accounts for rating category weights (available in `rating_categories` table). Ratings are given in a scale of 0 to 5. Score should be representable in percentages from 0 to 100. 

#### Task 1: Solution

To create a ticket score algorithm that accounts for rating category weights and converts the score into a percentage from 0 to 100, we first have to normalize the weights, then calculate the weighted sum, and lastly normalize to percentage.

First, we have to calculate the sum of weights. The weights given are:

Spelling: 1\
Grammar: 0.7\
GDPR: 1.2\
Randomness: 0

So total weight (TW) is: 1 + 0.7 + 1.2 + 0 = 2.9

To normalize the weights (W), we calculate normalized weights (NW) by dividing each weight by the total weight so NW = W / TW.

From this we get:

Spelling: 1 / 2.9 ≈ 0.345\
Grammar: 0.7 / 2.9 ≈ 0.241\
GDPR: 1.2 / 2.9 ≈ 0.414\
Randomness: 0 / 2.9 = 0

To show how to calculate the score, lets assume we have a ticket with the following ratings:

Spelling: 4\
Grammar: 3\
GDPR: 5\
Randomness: 2

Using these ratings and the normalized weights, we can calculate the weighted sum (WS) of it. This is simply done by multiplying the ratings by normalized weights and adding the products together. From that:

WS = (4 × 0.345) + (3 × 0.241) + (5 × 0.414) + (2 × 0)\
WS ≈ 1.38 + 0.723 + 2.07 + 0 = 4.173

To show that this is indeed correct, if we assign each rating value of 5 which is the maximum, we can calculate the max weighted sum (MWS):

MWS = (5 × 0.345) + (5 × 0.241) + (5 × 0.414) + (5 × 0)\
MWS = 1.725 + 1.205 + 2.07 + 0 = 5

So the approach is indeed correct.

Lastly, we have to convert the weighted sum to a score percentage (SP) which can be simply done by:

SP = (WS / MWS) × 100\
SP = (4.173 / 5) × 100 ≈ 83.46%

This algorithm is implemented in the [ScoreServiceImpl](https://github.com/kaarelkaasla/klaus-test-assignment/blob/master/backend/src/main/java/com/kaarelkaasla/klaustestassignment/service/ScoreServiceImpl.java) class and used for percentage score calculations where necessary.

--- 




2. Build a service that can be queried using [gRPC](https://grpc.io/docs/tutorials/basic/go/) calls and can answer following questions:
---

* **Aggregated category scores over a period of time**

  E.g. what have the daily ticket scores been for a past week or what were the scores between 1st and 31st of January.

  For periods longer than one month weekly aggregates should be returned instead of daily values.

  From the response the following UI representation should be possible:

| Category | Ratings | Date 1 | Date 2 | ... | Score |
|----|----|----|----|----|----|
| Tone | 1 | 30% | N/A | N/A | X% |
| Grammar | 2 | N/A | 90% | 100% | X% |
| Random | 6 | 12% | 10% | 10% | X% |


#### **Task 2.1: Solution**

This service uses a query that retrieves aggregated data from the ratings table, filtered by a specific date range (:startDate to :endDate). For each combination of date and rating_category_id, it calculates:

  * The count of ratings (frequency)
  * The average rating (average_rating)

  The results are grouped by date and rating_category_id, meaning we get one row per unique combination of these two fields. The results are then sorted by date and rating_category_id.
        
  The service then creates and returns an array of objects for each category where categories have their category score frequencies, average score of over the entire period, and a sub-array where objects have time period (daily or weekly) averages and the time period length.
      
  If start date or end date is outside the range of the test data, the response starts from the timestamp of the first entry and ends at the timestamp of the last entry. Also if the response is weekly data and the last week has less than 7 days, it averages over that period rather than the entire week.

  The service is defined in [rating_service](https://github.com/kaarelkaasla/klaus-test-assignment/blob/master/backend/src/main/proto/rating_service.proto) Protobuf file and implemented in the [RatingServiceImpl](https://github.com/kaarelkaasla/klaus-test-assignment/blob/master/backend/src/main/java/com/kaarelkaasla/klaustestassignment/service/RatingServiceImpl.java) class.

--- 
  * **Scores by ticket**

      Aggregate scores for categories within defined period by ticket.

      E.g. what aggregate category scores tickets have within defined rating time range have.

      | Ticket ID | Category 1 | Category 2 |
      |----|----|----|
      | 1   |  100%  |  30%  |
      | 2   |  30%  |  80%  |

    #### **Task 2.2: Solution**
    This service queries all tickets and their scores between the given two timestamps, calculates the average rating for each ticket and returns an array of objects where each object has a ticket ID and a sub-object with the averaged category scores.

    The service is defined in [ticket_score_service](https://github.com/kaarelkaasla/klaus-test-assignment/blob/master/backend/src/main/proto/ticket_score_service.proto) Protobuf file and implemented in the [TicketScoreServiceImpl](https://github.com/kaarelkaasla/klaus-test-assignment/blob/master/backend/src/main/java/com/kaarelkaasla/klaustestassignment/service/TicketScoreServiceImpl.java) class.
---

  * **Overall quality score**

      What is the overall aggregate score for a period.

      E.g. the overall score over past week has been 96%.

    #### **Task 2.3: Solution**
    This service takes time period start and end timestamps and whether to include previous period (relevant in the next sub-task), queries all the ticket scores for that time period, calculates the weighted scores for each ticket as described in task 1 and returns the average of all the tickets within the timeframe.

    The service is defined in [ticket_score_weighted_service](https://github.com/kaarelkaasla/klaus-test-assignment/blob/master/backend/src/main/proto/ticket_score_weighted_service.proto) Protobuf file and  implemented in the [TicketWeightedScoreServiceImpl](https://github.com/kaarelkaasla/klaus-test-assignment/blob/master/backend/src/main/java/com/kaarelkaasla/klaustestassignment/service/TicketWeightedScoreServiceImpl.java) class.
---

  * **Period over Period score change**

      What has been the change from selected period over previous period.

      E.g. current week vs. previous week or December vs. January change in percentages.

    #### **Task 2.4: Solution**
      This service is the same as the one described in task 2.3 solution, but simply also takes a boolean value whether to include previous period. If the boolean is "true", it analogously calculates to the current period average weighted score and in addition to that the previous period average weighted score. 
      The previous period is always the same length as the current period and spans the timeframe just before the current period. For example, if the current period is 8th to 14th of January, the previous period would be 1st to 7th of January.

---
### Bonus

* How would you build and deploy the solution?

    At Klaus we make heavy use of containers and [Kubernetes](https://kubernetes.io).
----

#### Bonus Solution:

In the present case, I have opted to use Docker Compose due to its ease of use when it comes to testing containerization. If the application was to be deployed using Kubernetes as the container orchestration tool, I would also locally test the manifests using Minikube or a tool such as [Skaffold](https://skaffold.dev/) before committing and deploying.\
If I had to design the development and deployment pipeline to deploy the application to a live production environment I would first create a building, testing, and deployment pipeline using a tool such as GitHub Actions. This pipeline would enforce that nothing gets merged and deployed before the build and different types of tests (unit, integration, etc) pass. This pipeline would deploy to a Kubernetes cluster such as Google Kubernetes Engine.\
I would also create different environments for pull requests, development master, staging, and production. As an example, the pull request and development master would run on Docker deployed to a development server. Each pull request environment would have a unique URL, for example, `klaus-pr-[pr_number].dev.com`. This would allow developers to test their pull requests in live environments without interfering with other pull requests. The pull requests would be merged to development master which would enforce merged pull requests do not have merge conflicts and everything works together. before moving further in the development pipeline. 
The development master would have an option to be deployed to staging which is deployed to a Kubernetes cluster and configuration-wise would mirror the production environment. This would allow for testing application's configuration and features in an environment that is as close to production as possible without actually being the production environment. The staging environment would similarly have the functionality to push to production which actually deploys the code to the live environment.


---
#### Ideas for further improvement:
Below are some of the ideas I would implement or consider implementing if this was an actual production application and didn't result in over-engineering this simple task.
* Consider writing the project in Go since it has higher performance and efficiency as it compiles to native code which generally results in faster execution times. Also, if the application needs to employ concurrency its goroutines and channels make it better-suited for high-concurrency tasks.
* Use HTTPS for REST APIs and TLS/SSL to encrypt gRPC communications. This ensures data privacy and integrity.
* Greater focus on application's security such as making sure none of the OWASP Top Ten vulnerabilities are present, the application is protected against XSS and CSRF, more specific CORS policy, etc.
* Remove hard-coded environments, ports, and API key from configuration and Docker files. Right now the values are hard-coded in for the ease of use, but should realistically come from environmental variables that aren't publicly exposed.
* Implement pagination. Right now the ticket ID service queries all the tickets over the defined time period in one database query and doesn't really slow down with only 10000 entries and service response sizes don't get big, but if the entries were to scale into millions, the requests would definitely slow down and optimizations would be necessary.
* Use a more efficient relational database such as PostgreSQL since SQLite is rather inefficient and doesn't scale well.
* Better monitoring and health checks such as using the ELK stack and Grafana, Prometheus. A production application would need better tracing, metrics, and structured logging than simply logging into the application's console.
* Use rate limiting to protect the application from denial of service or general abuse.
* Use load balancing to distribute traffic across multiple instances of gRPC service. If there were many clients using the application, a single instance would get overloaded and it failing would mean the whole application goes down.
* Use message compression as enabling gRPC message compression for large payloads reduces bandwidth usage and improve performance.
* Implement retry logic as it enhances application reliability and resilience by automatically handling transient failures, ensuring smooth operation and minimizing user impact during temporary issues or network glitches.
* Use granular caching. It wouldn't make sense to normally cache specific requests in the given application as every request that doesn't query exactly the same data would result in a different cached object. Granular caching would improve efficiency by caching only specific parts of responses (e.g. if the two requests overlap, the overlapping data from the first request could be used in the other) and thus reducing server load.
* Using OpenAPI Docs for automatic REST API and gRPC documentation. Right now the documentation is generated by hand, but if the specification were to change the documentation would also need to change. OpenAPI Docs solves this problem by keeping the documentation automatically up to date. I tried integrating it into this project, but apparently the plugin for Spring Boot and gRPC doesn't really work with Protobuf automatically generated classes.

---

# Application set-up

## Local Environment
1. In the project root ```cd frontend``` and run ```npm run setup:env```. This will create the necessary ```.env``` file required for frontend to function. Alternatively you can use the ```.env.example``` file in the frontend folder to create the ```.env``` file manually.
2. Frontend can be now run from the frontend folder by running ```npm dev run``` which will expose frontend on ```http://localhost:5173/```.
3. In the project root ```cd backend``` and run ```mvn clean install``` (if you have Maven installed locally, ```./mvnw``` for all commands if using the plugin). This will download the necessary dependencies and generate the Java classes based on the Protobuf definitions. If for some reason ```mvn clean install``` does not work, also run ```mvn clean compile```.
4. Backend can now be run from the backend folder by running ```mvn spring-boot:run```. This exposes the backend REST API on ```http://localhost:8080``` and the gRPC service on ```http://localhost:9090```.

## Docker Environment
1. Just navigate to the project root folder and run ```docker-compose up --build```. This will spin up both frontend and backend containers with the database access and expose the same ports as the local development, meaning frontend is available at 5173, REST API 8080, and gRPC 9090.
2. When done, exit the docker process and use ```docker-compose down``` or simply use that command in another terminal window. This will remove now-redundant containers.

# UI, REST API, gRPC specification

**Note**: It's advisable to have the period between ```2019-02-25T13:19:41``` and ```2020-02-25T13:05:41``` at least partially in the requests because all the test data is generated between these two timestamps.\
**Note 2**: If testing using Postman or a similar client, make sure to include ```x-api-key: your-secret-api-key``` (these are the default values) header to the requests as the APIs and gRPC are key-protected.\
**Note 3**: All the Protobuf generated class fields (such as response object fields) are documented in their respective Protobuf files and are omitted here for the brevity's sake.

## Task 2.1 Aggregated category scores over a period of time

### UI
Choose the ```Aggregated Category Scores Over a Period of Time``` dashboard and input the dates. This returns a table as described in the task.

### REST API
Example request:
```
curl -X GET "http://localhost:8080/api/v1/scores/aggregated?startDate=2019-06-01T00:00:00&endDate=2019-06-01T23:59:59" -H "x-api-key: your-secret-api-key"
```

Example successful response:
```
{
    "categoryRatingResults": [
        {
            "categoryName": "Spelling",
            "frequency": 32,
            "overallAverageScorePercentage": 60.62,
            "periodScores": [
                {
                    "period": "2019-06-01",
                    "averageScorePercentage": 60.62,
                    "message": ""
                }
            ]
        },
        {
            "categoryName": "Randomness",
            "frequency": 32,
            "overallAverageScorePercentage": 55.63,
            "periodScores": [
                {
                    "period": "2019-06-01",
                    "averageScorePercentage": 55.63,
                    "message": ""
                }
            ]
        },
        {
            "categoryName": "Grammar",
            "frequency": 32,
            "overallAverageScorePercentage": 49.38,
            "periodScores": [
                {
                    "period": "2019-06-01",
                    "averageScorePercentage": 49.38,
                    "message": ""
                }
            ]
        },
        {
            "categoryName": "GDPR",
            "frequency": 32,
            "overallAverageScorePercentage": 53.13,
            "periodScores": [
                {
                    "period": "2019-06-01",
                    "averageScorePercentage": 53.13,
                    "message": ""
                }
            ]
        }
    ]
}
```

Status codes:
```
200 OK
    Description: Request successful, aggregated scores retrieved.
    When: gRPC service returns scores successfully.
    
400 Bad Request
    Description: Invalid request.
    When: Invalid date format, startDate after endDate, or gRPC INVALID_ARGUMENT.
    
401 Unauthorized
    Description: Authentication required, invalid API key.
    When: Missing or incorrect API key, gRPC UNAUTHENTICATED.
    
404 Not Found
    Description: Resource not found.
    When: No scores found, gRPC NOT_FOUND.
    
500 Internal Server Error
    Description: Server error.
    When: Unexpected errors, gRPC INTERNAL or other unhandled codes.
```

### gRPC
Example request:
```
grpcurl -plaintext -d '{
  "startDate": "2019-06-01T00:00:00",
  "endDate": "2019-06-01T23:59:59"
}' -H 'x-api-key: your-secret-api-key' localhost:9090 com.kaarelkaasla.klaustestassignment.RatingService/GetAggregatedScores

```

Example successful response:
```
{
    "categoryRatingResults": [
        {
            "categoryName": "Spelling",
            "frequency": 32,
            "overallAverageScorePercentage": 60.62,
            "periodScores": [
                {
                    "period": "2019-06-01",
                    "averageScorePercentage": 60.62,
                    "message": ""
                }
            ]
        },
        {
            "categoryName": "Randomness",
            "frequency": 32,
            "overallAverageScorePercentage": 55.63,
            "periodScores": [
                {
                    "period": "2019-06-01",
                    "averageScorePercentage": 55.63,
                    "message": ""
                }
            ]
        },
        {
            "categoryName": "Grammar",
            "frequency": 32,
            "overallAverageScorePercentage": 49.38,
            "periodScores": [
                {
                    "period": "2019-06-01",
                    "averageScorePercentage": 49.38,
                    "message": ""
                }
            ]
        },
        {
            "categoryName": "GDPR",
            "frequency": 32,
            "overallAverageScorePercentage": 53.13,
            "periodScores": [
                {
                    "period": "2019-06-01",
                    "averageScorePercentage": 53.13,
                    "message": ""
                }
            ]
        }
    ]
}
```

Statuses:
```
OK
    Description: Request was successful.
    When: Successfully processed the request.

NOT_FOUND
    Description: No data for given period.
    When: aggregatedRatingsRaw is empty.
    
INVALID_ARGUMENT
    Description: Invalid argument.
    When: Invalid date format, DateTimeParseException.
    
PERMISSION_DENIED
    Description: Permission denied.
    When: Invalid API key provided in the gRPC call.
    
INTERNAL
    Description: Internal server error.
    When: Unexpected exceptions, database query failures.
    
UNKNOWN
    Description: Unknown error.
    When: Any other unhandled exceptions.
```

## Task 2.2 Scores by ticket

### UI
Choose the ```Category Scores by Ticket ID``` dashboard and input the dates. This returns a table as described in the task. Tickets are filtered in ascending order and there is also functionality to filter by ticket ID. 

### REST API
Example request:
```
curl -X GET "http://localhost:8080/api/v1/tickets/category-scores?startDate=2019-06-01T00:00:00&endDate=2019-06-01T12:00:00" -H "x-api-key: your-secret-api-key"
```

Example successful response:
```
[
    {
        "TicketId": 243038,
        "CategoryScores": {
            "Spelling": 100.0,
            "Randomness": 40.0,
            "Grammar": 100.0,
            "GDPR": 60.0
        }
    },
    {
        "TicketId": 248470,
        "CategoryScores": {
            "Spelling": 40.0,
            "Randomness": 80.0,
            "Grammar": 60.0,
            "GDPR": 0.0
        }
    },
    {
        "TicketId": 314637,
        "CategoryScores": {
            "Spelling": 60.0,
            "Randomness": 100.0,
            "Grammar": 0.0,
            "GDPR": 80.0
        }
    },
    {
        "TicketId": 317741,
        "CategoryScores": {
            "Spelling": 80.0,
            "Randomness": 20.0,
            "Grammar": 0.0,
            "GDPR": 0.0
        }
    },
    {
        "TicketId": 328144,
        "CategoryScores": {
            "Spelling": 100.0,
            "Randomness": 80.0,
            "Grammar": 0.0,
            "GDPR": 0.0
        }
    },
    {
        "TicketId": 329025,
        "CategoryScores": {
            "Spelling": 60.0,
            "Randomness": 40.0,
            "Grammar": 60.0,
            "GDPR": 40.0
        }
    },
    {
        "TicketId": 506280,
        "CategoryScores": {
            "Spelling": 40.0,
            "Randomness": 40.0,
            "Grammar": 0.0,
            "GDPR": 20.0
        }
    },
    {
        "TicketId": 509117,
        "CategoryScores": {
            "Spelling": 0.0,
            "Randomness": 80.0,
            "Grammar": 100.0,
            "GDPR": 100.0
        }
    },
    {
        "TicketId": 665670,
        "CategoryScores": {
            "Spelling": 40.0,
            "Randomness": 0.0,
            "Grammar": 100.0,
            "GDPR": 60.0
        }
    },
    {
        "TicketId": 748193,
        "CategoryScores": {
            "Spelling": 80.0,
            "Randomness": 0.0,
            "Grammar": 80.0,
            "GDPR": 80.0
        }
    },
    {
        "TicketId": 768494,
        "CategoryScores": {
            "Spelling": 60.0,
            "Randomness": 60.0,
            "Grammar": 40.0,
            "GDPR": 0.0
        }
    },
    {
        "TicketId": 826710,
        "CategoryScores": {
            "Spelling": 60.0,
            "Randomness": 100.0,
            "Grammar": 60.0,
            "GDPR": 80.0
        }
    },
    {
        "TicketId": 888944,
        "CategoryScores": {
            "Spelling": 80.0,
            "Randomness": 60.0,
            "Grammar": 100.0,
            "GDPR": 100.0
        }
    },
    {
        "TicketId": 902490,
        "CategoryScores": {
            "Spelling": 0.0,
            "Randomness": 40.0,
            "Grammar": 20.0,
            "GDPR": 80.0
        }
    },
    {
        "TicketId": 994247,
        "CategoryScores": {
            "Spelling": 80.0,
            "Randomness": 100.0,
            "Grammar": 80.0,
            "GDPR": 80.0
        }
    }
]
```

Status codes:
```
200 OK
    Description: Request successful, ticket category scores retrieved.
    When: gRPC service returns scores successfully.

400 Bad Request
    Description: Invalid request.
    When: Invalid date format, startDate after endDate, or gRPC INVALID_ARGUMENT.

401 Unauthorized
    Description: Authentication required, invalid API key.
    When: Missing or incorrect API key, gRPC UNAUTHENTICATED.

404 Not Found
    Description: Resource not found.
    When: No scores found, gRPC NOT_FOUND.

500 Internal Server Error
    Description: Server error.
    When: Unexpected errors, gRPC INTERNAL or other unhandled codes.
```

### gRPC
Example request:
```
grpcurl -plaintext -d '{
  "startDate": "2019-06-01T00:00:00",
  "endDate": "2019-06-01T12:00:00"
}' -H 'x-api-key: your-secret-api-key' localhost:9090 com.kaarelkaasla.klaustestassignment.TicketScoreService/GetTicketCategoryScores
```

Example successful response:
```
{
  "ticketCategoryScores": [
    {
      "ticketId": 329025,
      "categoryScores": {
        "GDPR": 40,
        "Grammar": 60,
        "Randomness": 40,
        "Spelling": 60
      }
    },
    {
      "ticketId": 768494,
      "categoryScores": {
        "GDPR": 0,
        "Grammar": 40,
        "Randomness": 60,
        "Spelling": 60
      }
    },
    {
      "ticketId": 994247,
      "categoryScores": {
        "GDPR": 80,
        "Grammar": 80,
        "Randomness": 100,
        "Spelling": 80
      }
    },
    {
      "ticketId": 317741,
      "categoryScores": {
        "GDPR": 0,
        "Grammar": 0,
        "Randomness": 20,
        "Spelling": 80
      }
    },
    {
      "ticketId": 314637,
      "categoryScores": {
        "GDPR": 80,
        "Grammar": 0,
        "Randomness": 100,
        "Spelling": 60
      }
    },
    {
      "ticketId": 748193,
      "categoryScores": {
        "GDPR": 80,
        "Grammar": 80,
        "Randomness": 0,
        "Spelling": 80
      }
    },
    {
      "ticketId": 665670,
      "categoryScores": {
        "GDPR": 60,
        "Grammar": 100,
        "Randomness": 0,
        "Spelling": 40
      }
    },
    {
      "ticketId": 506280,
      "categoryScores": {
        "GDPR": 20,
        "Grammar": 0,
        "Randomness": 40,
        "Spelling": 40
      }
    },
    {
      "ticketId": 248470,
      "categoryScores": {
        "GDPR": 0,
        "Grammar": 60,
        "Randomness": 80,
        "Spelling": 40
      }
    },
    {
      "ticketId": 328144,
      "categoryScores": {
        "GDPR": 0,
        "Grammar": 0,
        "Randomness": 80,
        "Spelling": 100
      }
    },
    {
      "ticketId": 902490,
      "categoryScores": {
        "GDPR": 80,
        "Grammar": 20,
        "Randomness": 40,
        "Spelling": 0
      }
    },
    {
      "ticketId": 826710,
      "categoryScores": {
        "GDPR": 80,
        "Grammar": 60,
        "Randomness": 100,
        "Spelling": 60
      }
    },
    {
      "ticketId": 509117,
      "categoryScores": {
        "GDPR": 100,
        "Grammar": 100,
        "Randomness": 80,
        "Spelling": 0
      }
    },
    {
      "ticketId": 888944,
      "categoryScores": {
        "GDPR": 100,
        "Grammar": 100,
        "Randomness": 60,
        "Spelling": 80
      }
    },
    {
      "ticketId": 243038,
      "categoryScores": {
        "GDPR": 60,
        "Grammar": 100,
        "Randomness": 40,
        "Spelling": 100
      }
    }
  ]
}
```

Statuses:
```
OK
    Description: Request was successful.
    When: Successfully processed the request.
    
NOT_FOUND
    Description: No data for given period.
    When: ratingsRaw is empty.
    
INVALID_ARGUMENT
    Description: Invalid argument.
    When: Invalid date format, ParseException.

PERMISSION_DENIED
    Description: Permission denied.
    When: Invalid API key provided in the gRPC call.
    
INTERNAL
    Description: Internal server error.
    When: Unexpected exceptions, database query failures.
    
UNKNOWN
    Description: Unknown error.
    When: Any other unhandled exceptions.
```

## Task 2.3 Overall quality score

### UI
Choose the ```Overall Quality Score for a Period``` dashboard and input the dates. This returns a color-coded overall score for that period (lower is more red, higher is more green).

### REST API
Example request:
```
curl -X GET "http://localhost:8080/api/v1/tickets/weighted-scores?startDate=2017-03-19T00:00:00&endDate=2019-03-26T23:59:59&includePreviousPeriod=false" -H "x-api-key: your-secret-api-key"
```

Example successful response:
```
{
  "currentPeriodScore": {
    "period": "2017-03-19T00:00:00 to 2019-03-26T23:59:59",
    "averageScorePercentage": 50.67,
    "message": ""
  }
}
```

Status codes:
```
200 OK
    Description: Request successful, weighted scores retrieved.
    When: gRPC service returns scores successfully.

400 Bad Request
    Description: Invalid request.
    When: Invalid date format, startDate after endDate, invalid includePreviousPeriod value, or gRPC INVALID_ARGUMENT.

401 Unauthorized
    Description: Authentication required, invalid API key.
    When: Missing or incorrect API key, gRPC UNAUTHENTICATED.

404 Not Found
    Description: Resource not found.
    When: No scores found, gRPC NOT_FOUND.

500 Internal Server Error
    Description: Server error.
    When: Unexpected errors, gRPC INTERNAL or other unhandled codes.
```

### gRPC
Example request:
```
grpcurl -plaintext -d '{
  "startDate": "2017-03-19T00:00:00",
  "endDate": "2019-03-26T23:59:59",
  "includePreviousPeriod": false
}' -H 'x-api-key: your-secret-api-key' localhost:9090 com.kaarelkaasla.klaustestassignment.TicketWeightedScoreService/GetWeightedScores
```

Example successful response:
```
{
  "currentPeriodScore": {
    "period": "2017-03-19T00:00:00 to 2019-03-26T23:59:59",
    "averageScorePercentage": 50.67
  }
}
```

Statuses:
```
OK
    Description: Request was successful.
    When: Successfully processed the request.
    
NOT_FOUND
    Description: No data for given period.
    When: No ratings found for the specified periods.

INVALID_ARGUMENT
    Description: Invalid argument.
    When: Invalid date format, ParseException.
    
PERMISSION_DENIED
    Description: Permission denied.
    When: Invalid API key provided in the gRPC call.

INTERNAL
    Description: Internal server error.
    When: Unexpected exceptions, database query failures.

UNKNOWN
    Description: Unknown error.
    When: Any other unhandled exceptions.
```

## Task 2.4 Overall quality score

### UI
Choose the ```Overall Quality Score for a Period``` dashboard and input the dates. This returns a color-coded overall score for that period and previous period. Between them the difference from the previous period to the current period is shown.

### REST API
Example request:
```
curl -X GET "http://localhost:8080/api/v1/tickets/weighted-scores?startDate=2017-03-19T00:00:00&endDate=2019-03-26T23:59:59&includePreviousPeriod=false" -H "x-api-key: your-secret-api-key"
```

Example successful response when both periods have scores:
```
{
  "currentPeriodScore": {
    "period": "2019-03-19T00:00:00 to 2019-03-26T23:59:59",
    "averageScorePercentage": 51.61,
    "message": ""
  },
  "previousPeriodScore": {
    "period": "2019-03-11T00:00:00 to 2019-03-18T23:59:59",
    "averageScorePercentage": 48.77,
    "message": ""
  },
  "scoreChange": {
    "value": 2.84,
    "message": ""
  }
}
```

Example successful response when one period has a value:
```
{
  "currentPeriodScore": {
    "period": "2017-03-19T00:00:00 to 2019-03-26T23:59:59",
    "averageScorePercentage": 50.67,
    "message": ""
  },
  "previousPeriodScore": {
    "period": "2015-03-12T00:00:00 to 2017-03-18T23:59:59",
    "averageScorePercentage": 0.0,
    "message": "N/A"
  },
  "scoreChange": {
    "value": 0.0,
    "message": "N/A"
  }
}
```

Status codes:
```
200 OK
    Description: Request successful, weighted scores retrieved.
    When: gRPC service returns scores successfully.

400 Bad Request
    Description: Invalid request.
    When: Invalid date format, startDate after endDate, invalid includePreviousPeriod value, or gRPC INVALID_ARGUMENT.

401 Unauthorized
    Description: Authentication required, invalid API key.
    When: Missing or incorrect API key, gRPC UNAUTHENTICATED.

404 Not Found
    Description: Resource not found.
    When: No scores found, gRPC NOT_FOUND.

500 Internal Server Error
    Description: Server error.
    When: Unexpected errors, gRPC INTERNAL or other unhandled codes.
```

### gRPC
Example request:
```
grpcurl -plaintext -d '{
  "startDate": "2019-03-19T00:00:00",
  "endDate": "2019-03-26T23:59:59",
  "includePreviousPeriod": true
}' -H 'x-api-key: your-secret-api-key' localhost:9090 com.kaarelkaasla.klaustestassignment.TicketWeightedScoreService/GetWeightedScores
```

Example successful response when both periods have scores:
```
{
  "currentPeriodScore": {
    "period": "2019-03-19T00:00:00 to 2019-03-26T23:59:59",
    "averageScorePercentage": 51.61
  },
  "previousPeriodScore": {
    "period": "2019-03-11T00:00:00 to 2019-03-18T23:59:59",
    "averageScorePercentage": 48.77
  },
  "scoreChange": {
    "value": 2.84
  }
}
```

Example successful response when one period has a value:
```
{
  "currentPeriodScore": {
    "period": "2017-03-19T00:00:00 to 2019-03-26T23:59:59",
    "averageScorePercentage": 50.67
  },
  "previousPeriodScore": {
    "period": "2015-03-12T00:00:00 to 2017-03-18T23:59:59",
    "message": "N/A"
  },
  "scoreChange": {
    "message": "N/A"
  }
}
```

Statuses:
```
OK
    Description: Request was successful.
    When: Successfully processed the request.
    
NOT_FOUND
    Description: No data for given period.
    When: No ratings found for the specified periods.

INVALID_ARGUMENT
    Description: Invalid argument.
    When: Invalid date format, ParseException.
    
PERMISSION_DENIED
    Description: Permission denied.
    When: Invalid API key provided in the gRPC call.

INTERNAL
    Description: Internal server error.
    When: Unexpected exceptions, database query failures.

UNKNOWN
    Description: Unknown error.
    When: Any other unhandled exceptions.
```
