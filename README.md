# Recipe Sharing API

Spring Boot midterm project: a REST backend where **chefs** publish **recipes**, organised by **category** and **tags**, with photo upload, validation, and server-side pagination and sorting.

**Stack:** Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA (Hibernate), Bean Validation, H2 (file-based), springdoc (Swagger UI), and a server-rendered web UI (Thymeleaf + Bootstrap 5).

## Run it

Requires a **JDK 21 or newer** (a JRE alone is not enough to compile).

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

| What | Where |
|---|---|
| **Web UI** (browse, search, add/edit recipes with photos) | http://localhost:8080/ |
| Swagger UI (try every endpoint, including uploads) | http://localhost:8080/swagger-ui.html |
| H2 console | http://localhost:8080/h2-console — JDBC URL `jdbc:h2:file:./data/recipesdb`, user `sa`, empty password |
| Uploaded images | `./uploads/` on disk, served at `/uploads/{fileName}` |

On first start an empty database is filled with demo data (3 chefs, 4 categories, 8 recipes). Set `app.seed-data=false` to disable.

Run the tests (they use an in-memory database and never touch `./data`): `./mvnw test`

## Web UI

Thymeleaf pages served by the same app (Bootstrap and Font Awesome come from WebJars, so there is no separate frontend build). They call the same services as the REST API.

| Page | Path | What it does |
|---|---|---|
| Recipe list | `/`, `/recipes` | Photo cards; search by title; filter by category, difficulty, tag, max time; sort dropdown; 9 per page with pager (`?q=&categoryId=&difficulty=&tag=&maxTime=&sort=title,asc&page=1`) |
| Recipe details | `/recipes/{id}` | Photo, chef, tags, instructions, edit / delete |
| New / edit recipe | `/recipes/new`, `/recipes/{id}/edit` | Validated form with photo upload (`multipart/form-data`); errors are shown next to the fields |
| Chefs | `/chefs`, `/chefs/new`, `/chefs/{id}` | List with top chefs, add, profile with their recipes, delete |
| Categories | `/categories` | List, add, delete (a category that still has recipes is refused with a message) |

Controllers live in `controller.web` (`@Controller`, constructor injection); form beans (`RecipeForm`, `ChefForm`, `CategoryForm`) mirror the request DTOs' validation rules. Templates are in `src/main/resources/templates` (layout + fragments), styles in `static/css/style.css`. REST errors stay JSON (`GlobalExceptionHandler` only covers `@RestController`s); the pages have their own `WebExceptionHandler` and an `error.html` page.

## Data model

```
Chef 1 ──── * Recipe * ──── 1 Category
                │
                * ──── * Tag          (join table recipe_tags)
                └─ imageFileName      (name of the uploaded photo)
```

- **One-to-many / many-to-one:** `Chef → Recipe`, `Category → Recipe`
- **Many-to-many:** `Recipe ↔ Tag`

## Endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/api/recipes` | Paged + sorted: `?page=0&size=5&sort=cookTimeMinutes,asc`, optional `?title=` |
| GET | `/api/recipes/search` | Optional filters `tag`, `categoryId`, `difficulty`, `minTime`, `maxTime`; sorting via `sortBy`, `direction`; `page`, `size` |
| GET | `/api/recipes/{id}` | |
| POST | `/api/recipes` | JSON body, validated |
| PUT | `/api/recipes/{id}` | JSON body, validated |
| DELETE | `/api/recipes/{id}` | Also removes the image file |
| POST | `/api/recipes/{id}/image` | `multipart/form-data`, part `file`; replaces any previous image |
| POST | `/api/recipes/with-image` | `multipart/form-data`: recipe fields + optional `file` |
| DELETE | `/api/recipes/{id}/image` | |
| GET/POST/PUT/DELETE | `/api/chefs`, `/api/chefs/{id}` | `GET /api/chefs` is paged |
| GET | `/api/chefs/{id}/recipes`, `/api/chefs/top?limit=5` | |
| GET/POST/PUT/DELETE | `/api/categories`, `/api/categories/{id}` | |
| GET | `/api/categories/{id}/recipes?maxTime=30` | Paged |
| GET/POST/PUT/DELETE | `/api/tags`, `/api/tags/{id}` | |
| GET | `/api/tags/popular?limit=5` | |

Page size is capped at 50. Errors come back as JSON (`400` validation with per-field messages, `404`, `409` duplicates / still in use, `413` file too large).

### Examples

```bash
# create a recipe (unknown tags are created automatically)
curl -X POST localhost:8080/api/recipes -H 'Content-Type: application/json' -d '{
  "title":"Lentil Soup","instructions":"Simmer 20 min.","cookTimeMinutes":30,"servings":4,
  "difficulty":"EASY","chefId":1,"categoryId":2,"tagNames":["vegan","quick"]}'

# validation error -> 400 with fieldErrors
curl -X POST localhost:8080/api/recipes -H 'Content-Type: application/json' -d '{"title":""}'

# pagination + sorting
curl 'localhost:8080/api/recipes?page=0&size=3&sort=cookTimeMinutes,asc'

# combined search
curl 'localhost:8080/api/recipes/search?tag=vegan&difficulty=EASY&maxTime=40&sortBy=title&direction=asc'

# upload a photo to recipe 1, then open the returned imageUrl
curl -F file=@dish.jpg localhost:8080/api/recipes/1/image

# create a recipe and photo in one multipart request
curl -F title='Stir Fry' -F instructions='Fry.' -F cookTimeMinutes=15 -F servings=2 \
     -F difficulty=EASY -F chefId=1 -F categoryId=1 -F tagNames=vegan -F tagNames=wok \
     -F file=@dish.jpg localhost:8080/api/recipes/with-image
```

## Requirements checklist

Base package: `com.kiu.recipes`

| Requirement | Where |
|---|---|
| **Layered packages** | `controller`, `service`, `repository`, `entity` (plus `dto`, `config`, `exception`) |
| **Stereotype annotations** | `@RestController` (controllers), `@Service` (services), `@Repository` (repositories), `@Configuration` (`WebConfig`, `OpenApiConfig`, `DataSeeder`), `@RestControllerAdvice` (`GlobalExceptionHandler`) |
| **Constructor injection, no field `@Autowired`** | Every controller, service and config class takes its dependencies through the constructor into `private final` fields. `@Autowired` appears nowhere in the project (tests included) |
| **≥ 3 entities** | `Chef`, `Category`, `Recipe`, `Tag` |
| **≥ 2 relationship types** | `@OneToMany`/`@ManyToOne` (`Chef`/`Category` ↔ `Recipe`), `@ManyToMany` (`Recipe` ↔ `Tag`, join table `recipe_tags`) |
| **Repositories extend Spring Data** | all four extend `JpaRepository` |
| **≥ 2 method-name queries** | `RecipeRepository`: `findByCategoryIdAndCookTimeMinutesLessThanEqual`, `findByTitleContainingIgnoreCase`, `findByChefIdOrderByCreatedAtDesc`; `ChefRepository.findByEmail`; `TagRepository.findByNameIgnoreCase` |
| **≥ 2 custom `@Query`** | `RecipeRepository.search` (optional filters over a tag join, with count query), `ChefRepository.findTopChefs` (group by + count), `TagRepository.findPopularTags` |
| **`@Transactional` on data-changing logic** | every create/update/delete/upload method in `ChefService`, `CategoryService`, `TagService`, `RecipeService` (reads use `readOnly = true`) |
| **`@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`** | all controllers |
| **`@PathVariable`** | `/{id}` endpoints |
| **`@RequestParam`** | search filters, `title`, `maxTime`, `limit`, upload part `file` |
| **`@RequestBody` / `@ModelAttribute`** | `@RequestBody`: create/update endpoints; `@ModelAttribute`: `RecipeController.createWithImage` |
| **JSR-303 + `@Valid`** | constraints on the `*Request` records in `dto` (`@NotBlank`, `@Size`, `@Email`, `@NotNull`, `@Min`, `@Max`); `@Valid` on every request body / model attribute in the controllers |
| **File upload (`MultipartFile`, `multipart/form-data`)** | `RecipeController.uploadImage` and `createWithImage`; `FileStorageService` writes the file to `./uploads/`; the generated file name is saved in `Recipe.imageFileName` |
| **Pagination & sorting (`Pageable`, `Sort`)** | `RecipeController.list` (`Pageable` + `@PageableDefault`); `RecipeService.search` builds `Sort.by(direction, sortBy)` and a `PageRequest`; results wrapped in `PageResponse` |

### Upload details

- Accepts JPEG, PNG, GIF and WEBP up to 5 MB. The type is checked from the file's **magic bytes**, not the client-supplied name or content type, so a script renamed to `.png` is rejected (SVG is intentionally not allowed).
- The stored name is a server-generated UUID plus extension; the original file name is never used on disk.
- Replacing an image or deleting a recipe removes the old file once the database transaction commits; a rolled-back request removes the newly written file.

## Tests

`./mvnw test` runs 38 tests: repository queries (`RecipeRepositoryTest`), MockMvc integration tests (`RecipeApiIntegrationTest`) for validation, CRUD, error handling, pagination/sorting, search, and the full upload flow, and `WebPagesIntegrationTest` for the HTML pages (rendering, form validation, photo upload, edit/delete, chefs and categories, and that `/api` errors remain JSON).
