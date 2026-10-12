# SmartChat Memory Manager — Forest edition

A Spring Boot + MySQL mini-project with a minimal light-forest landing page, memory-aware chat demo and live backend explainer.

## Requirements
- Java 17 or newer
- Maven 3.8+ (`mvn -version`)
- MySQL 8 running locally
- A modern browser

## Quick start (VS Code terminal)
1. Unzip this folder, open **smartchat-memory-manager** in VS Code.
2. Start your MySQL server. By default, the application connects to `localhost:3306`, user `root`, password `root`. The JDBC connection includes `createDatabaseIfNotExist=true` (user needs database creation permission). Alternatively, run `sql/schema.sql` in MySQL Workbench before starting.
3. Edit `src/main/resources/application.properties` for your username/password, **or** use environment variables `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`.
4. In the project root, run:

   ```bash
   mvn spring-boot:run
   ```

5. Open:
   - `http://localhost:8080/` — landing page
   - `http://localhost:8080/chat/` — working chat + memory management
   - `http://localhost:8080/backend-explainer/` — live relevance trace

## What actually works
- Persist and retrieve saved memories in MySQL through Spring Data JPA.
- Add/delete memories; filter them by memory type.
- Extract keywords, match synonyms, score each stored memory, select up to five relevant memories, build a context prompt.
- Generate a **mock/demo AI** answer, not a live AI API response.
- Inspect the latest scored memories, prompt, and response in the separate explainer (auto-updates approximately every 1.5 seconds).

## APIs
- `GET /api/memories`
- `POST /api/memories` (JSON with `type`, `content`, `importance`)
- `DELETE /api/memories/{id}`
- `POST /api/chat` (`{ "message": "How should I implement authentication?" }`)
- `GET /api/trace/latest` (returns 204 before the first chat)

## Frontend files
- `src/main/resources/static/index.html` + `style.css`: landing page
- `src/main/resources/static/chat/index.html` + `chat/style.css` + `chat/script.js`: real chat UI
- `src/main/resources/static/backend-explainer/index.html` + `style.css` + `script.js`: live explainer
- `src/main/resources/static/assets/forest-landscape.jpg`: original local forest-themed illustration
- `src/main/resources/static/assets/story-photo.jpg`: bundled editorial-style photo

## Backend files
The Spring Boot main class is `SmartChatApplication.java`. Controllers handle HTTP requests, `ChatService` coordinates the workflow, `MemoryService` and `MemoryRepository` use MySQL, `RelevanceService` selects memories, `MockAiService` responds, and `TraceService` exposes the latest processing trace.

**Note:** The forest-style illustration and editorial imagery are part of the UI. The memory retrieval backend is real; the model output is deliberately simulated for a self-contained college demo. A real AI API requires a separate implementation and secure server-side credentials.

## Local testing
Try asking **How should I implement authentication?** after starting the server. Then open the explainer to inspect how the stored React/Firebase and login-module memories were scored.

# smart-memory-managment
