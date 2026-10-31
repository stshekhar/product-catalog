# Product Catalog Service

A small REST service for managing an e-commerce product catalog: create, read, update and delete products, and list them with filtering (category, price range), sorting and pagination.

Java 17, Spring Boot 4, Spring Data JPA.

## Running it


.\mvnw spring-boot:run

This starts with an in-memory H2 database, so nothing else is needed. Swagger UI is at http://localhost:8080/swagger-ui.html

To run against PostgreSQL:

docker compose up -d
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=postgres"
Tests:

.\mvnw test

## API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/products` | Create a product, returns `201` with a `Location` header |
| GET | `/api/v1/products/{id}` | Get one product |
| GET | `/api/v1/products` | List / filter products (paginated) |
| PUT | `/api/v1/products/{id}` | Replace a product |
| DELETE | `/api/v1/products/{id}` | Delete a product, returns `204` |

List parameters (all optional):

| Param | Default | Notes |
|-------|---------|-------|
| `category` | – | exact match |
| `minPrice`, `maxPrice` | – | inclusive |
| `page` | `0` | zero based |
| `size` | `20` | max `100` |
| `sortBy` | `name` | `name`, `price` or `createdAt` |
| `direction` | `asc` | `asc` or `desc` |

Example:


curl -X POST localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Air Max 90","description":"Classic running shoe","category":"shoes","price":129.99}'

curl "localhost:8080/api/v1/products?category=shoes&minPrice=50&maxPrice=150&page=0&size=20&sortBy=price"


List response:

```json
{
  "content": [ { "id": "...", "name": "Air Max 90", "category": "shoes", "price": 129.99 } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```


## Design decisions

**Data store: relational (PostgreSQL).**
Products here have a fixed, well known shape (name, category, price), and the main query is "filter by category and price range, sort, page". That is exactly what a relational database with the right index does well. A few hundred thousand rows is small for PostgreSQL. A document store would make sense if products had very different attributes per category, and a key-value store can't do range filtering on price by itself. H2 is only used so the project runs without any setup.

**Indexes.**
A composite index on `(category, price)` covers the most common query (category filter plus price range or price sort) in one index scan. A separate index on `price` covers price filtering without a category.

**Read-heavy traffic.**
- Single product reads (`GET /{id}`) are cached with Caffeine (bounded, 10 000 entries, 10 min expiry). Update and delete evict the entry, so clients don't see stale data from this instance.
- Read methods use read-only transactions, and `open-in-view` is disabled so DB connections aren't held during JSON rendering.
- List results are not cached: filter combinations are too many and the index makes them cheap anyway.

**Pagination.**
Offset pagination (`page`/`size`) because it's simple for clients and fine for this data size. Page size is capped at 100. `id` is added as a second sort key so the order is stable between pages when prices or names are equal.


