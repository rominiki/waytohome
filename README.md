# Way2Home

A backend for a student housing platform, built with Spring Boot. Students can browse and search rental listings, save the ones they like, and message landlords directly. Landlords post listings, and an admin reviews each one before it goes public.

## Background

This started as a university team project. The original version was built with Node.js, React and MySQL, and I worked on it as the team lead. I wanted to implement it also on Spring , so I'm rebuilding the backend on my own in Java which has the same idea, different stack.

## Tech stack

- Java 21
- Spring Boot 3.5
- PostgreSQL 16 (running in Docker)
- Spring Security with JWT for auth
- Flyway for database migrations
- springdoc-openapi (Swagger UI) for API docs
- JUnit 5, Mockito and MockMvc for tests
- Maven

## What's working so far

The project is built in phases. Phases 1 through 7 are done:

- **Project setup** : Spring Boot skeleton, PostgreSQL via Docker, Flyway migrations, a health endpoint, Swagger.
- **Users and registration** : user accounts with roles (student, landlord, admin), passwords hashed with BCrypt, input validation, and a global exception handler that returns proper HTTP status codes instead of stack traces.
- **JWT authentication** : login issues a signed token, and a filter checks that token on every protected request. The whole thing is stateless, so the server keeps no session.
- **Listings** : landlords create listings, anyone can browse them, and only the owner (or an admin) can edit or delete one. New listings start as pending until they're reviewed.
- **Search and filtering** : listings can be filtered by price range, location, number of bedrooms and pet-friendliness, in any combination, with pagination and sorting. Built with JPA Specifications so the query is assembled from whichever filters are actually supplied.
- **Favorites** : students can save and unsave listings. Saving the same one twice does nothing instead of erroring or creating duplicates, with a unique constraint in the database as a backstop.
- **Admin moderation** : admins see pending listings and approve or reject them. Approved listings show up in public search; rejected ones don't.

Every phase has tests covering both the happy paths and the things that should fail (wrong role, missing token, bad input, and so on).

## Still to come

Three phases left:

- **Real-time messaging** : direct chat between students and landlords about a listing, using WebSockets and STOMP so messages arrive instantly instead of needing a refresh.
- **Testing hardening** : switching integration tests over to Testcontainers so they run against a real PostgreSQL instead of assumptions, adding a full end-to-end test that walks the whole user journey, and measuring coverage with JaCoCo.
- **Polish and deployment** : a multi-stage Dockerfile, a CI pipeline on GitHub Actions, secrets moved out of config and into environment variables, and the app deployed somewhere public.

## Running it locally

You'll need Docker and JDK 21.

Start the database:

```bash
docker compose up -d
```

Run the app:

```bash
./mvnw spring-boot:run
```

Then open the API docs at `http://localhost:8080/swagger-ui.html`.

To try the protected endpoints, register a user, log in to get a token, then paste it into the **Authorize** box in Swagger.

## Running the tests

```bash
./mvnw test
```

Make sure the database container is running first, since the integration tests connect to it.

## A note on the API

The whole API is documented in Swagger once the app is running. The short version:

- `POST /api/auth/register` and `POST /api/auth/login` are open to everyone.
- Browsing and searching listings (`GET /api/listings`) is public.
- Creating, editing or deleting a listing needs a landlord token.
- Favorites need a student token.
- The admin endpoints under `/api/admin` need an admin token.
