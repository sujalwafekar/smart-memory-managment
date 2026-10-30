# MemoryChat — Forest edition

A Spring Boot + MySQL mini-project with a minimal light-forest landing page and memory-aware chat demo.

## Requirements
- Java 17 or newer
- Maven 3.8+ (`mvn -version`)
- MySQL 8 running locally
- A modern browser

## Quick start (VS Code terminal)
1. Open the project folder in VS Code.
2. Start your MySQL server. By default, the application connects to `localhost:3306`, user `root`, password `root`. The JDBC connection includes `createDatabaseIfNotExist=true` (user needs database creation permission). Alternatively, run `sql/schema.sql` in MySQL Workbench before starting.
3. Edit `src/main/resources/application.properties` for your username/password, **or** use environment variables `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`. For deployments, set a strong Base64-encoded `JWT_SECRET`; the checked-in fallback is only for local development.
4. In the project root, run:

   ```bash
   mvn spring-boot:run
   ```

5. Open:
   - `http://localhost:8080/` — landing page
   - `http://localhost:8080/chat/` — working chat + memory management

## What actually works
- Persist and retrieve saved memories in MySQL through Spring Data JPA.
- Add, edit, delete, search, and filter memories by type.
- Extract keywords, match synonyms, score each stored memory, select up to five relevant memories, build a context prompt.
- Generate a contextual answer using NVIDIA AI when it is available, with a fast local demo-AI fallback.
- Create an account or sign in with JWT authentication. Each account can access only its own memories.

## APIs
- `POST /api/auth/register` (`{ "username": "alex", "password": "at-least-8-characters" }`)
- `POST /api/auth/login` (same body; returns a JWT)
- `GET /api/memories`
- `POST /api/memories` (JSON with `type`, `content`, `importance`)
- `PUT /api/memories/{id}` (JSON with `type`, `content`, `importance`)
- `DELETE /api/memories/{id}`
- `POST /api/chat` (`{ "message": "How should I implement authentication?" }`)

All endpoints except `/api/auth/**` require an `Authorization: Bearer <token>` header. The bundled browser UI stores the token locally and redirects to `/auth/` when it is missing or expired.

For local demos, use `demo_user` with password `demo12345`, or choose **Try the demo account** on the sign-in page. It is seeded with the original sample memories.

## Frontend files
- `src/main/resources/static/index.html` + `style.css`: landing page
- `src/main/resources/static/chat/index.html` + `chat/style.css` + `chat/script.js`: real chat UI
- `src/main/resources/static/auth/index.html` + `auth/style.css` + `auth/script.js`: sign-in and registration UI
- `src/main/resources/static/assets/forest-landscape.jpg`: original local forest-themed illustration
- `src/main/resources/static/assets/story-photo.jpg`: bundled editorial-style photo

## Backend files
The MemoryChat Spring Boot application uses controllers for HTTP requests, `ChatService` for workflow coordination, `MemoryService` and `MemoryRepository` for MySQL, `RelevanceService` for memory selection, and an AI service for contextual responses.

**Note:** The memory retrieval backend is real. Memory extraction and the fallback answer are local so the application remains responsive when the cloud AI service is unavailable.

## Local testing
Try asking **How should I implement authentication?** after starting the server. The answer card shows the selected memories that were sent as context.

## Deployment: Render + Vercel

The repository includes `render.yaml` and `vercel.json` for a split deployment:

1. Push this project to GitHub without `.env`.
2. In Render, create a Blueprint from the repository. Supply `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GEMINI_API_KEY`, and `NVIDIA_API_KEY` as environment variables. Render generates `JWT_SECRET`.
3. Copy the Render service URL, for example `https://memorychat-api.onrender.com`.
4. In Vercel, import the same repository and set `MEMORYCHAT_API_URL` to the Render service URL. Deploy the project.

Vercel serves the frontend and provides its Render API URL at runtime. The Spring Boot service accepts requests from Vercel preview and production URLs.
