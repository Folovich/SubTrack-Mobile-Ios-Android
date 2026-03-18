# SubTrack

**SubTrack** — fullstack-приложение для управления подписками.  
Проект позволяет пользователю отслеживать свои подписки, смотреть аналитику расходов, получать уведомления о ближайших списаниях и импортировать данные через CSV.

## Возможности проекта

- регистрация и авторизация пользователей
- JWT-аутентификация
- добавление, редактирование и удаление подписок
- категории подписок
- аналитика расходов
- уведомления о предстоящем списании
- прогноз месячных и годовых расходов
- быстрый flow отмены/паузы подписки через email-черновик в поддержку (`mailto` + copy text)
- синхронизация между Web и Mobile через единый Backend API
- импорт подписок через CSV
- Swagger / OpenAPI
- demo seed-данные для тестирования

---

## Технологический стек

### Backend
- Java
- Spring Boot
- Spring Security
- JWT
- PostgreSQL
- Flyway / Liquibase
- Swagger / OpenAPI

### Web
- React
- TypeScript
- Vite

### Mobile
- React Native
- Expo
- TypeScript

### Infra
- Docker
- Docker Compose

---

## Архитектура проекта

Проект организован в виде **monorepo**.

- **Backend** — один Spring Boot backend-монолит
- **Web** — React-клиент
- **Mobile** — React Native / Expo-клиент
- **Database** — PostgreSQL
- **Docs** — документация, сценарий демо, инструкции запуска
- **Infra / Scripts** — docker-compose, env-примеры, вспомогательные скрипты

Все клиенты работают с одним REST API, поэтому синхронизация между Web и Mobile реализуется через Backend.

---

## Структура проекта

```text
subtrack/
├─ README.md
├─ .gitignore
├─ .editorconfig
├─ .env.example
├─ apps/
│  ├─ backend/
│  ├─ web/
│  └─ mobile/
├─ database/
│  ├─ migrations/
│  ├─ seed/
│  └─ schema/
├─ docs/
│  ├─ api-notes/
│  ├─ demo-script/
│  └─ launch-guide/
├─ infra/
│  ├─ docker-compose.yml
│  ├─ docker/
│  └─ env/
└─ scripts/
   ├─ start/
   ├─ db/
   └─ utils/
```

---

## Запуск Проекта (Backend + Docker + Web + Mobile)

### 1) Системные требования

- Git
- Docker Engine + Docker Compose v2 (`docker compose ...`)
- JDK 21 (в `apps/backend/build.gradle` задан `JavaLanguageVersion.of(21)`)
- Node.js 20 LTS + npm 10+ (lockfileVersion=3, web Dockerfile использует `node:20-alpine`)
- Для Mobile:
  - Android Studio (эмулятор), либо
  - Expo Go на физическом устройстве

Проверка версий:

```bash
java -version
node -v
npm -v
docker --version
docker compose version
```

### 2) Подготовка `.env`

Рекомендуемый способ (создаёт файлы автоматически и фиксирует нужные Gmail feature flags):

```bash
bash scripts/setup/bootstrap-env.sh
```

Для Windows PowerShell:

```bash
powershell -ExecutionPolicy Bypass -File scripts/setup/bootstrap-env.ps1
```

Что делает bootstrap:
- создаёт `.env` файлы из `.env.example`, если их ещё нет;
- синхронизирует `IMPORT_PROVIDERS_GMAIL_ENABLED=true` и `IMPORT_PROVIDERS_GMAIL_MAILBOX_ENABLED=true` в `/.env` и `apps/backend/.env`;
- выставляет `IMPORT_PROVIDERS_GMAIL_MAILBOX_STARTUP_VALIDATION_ENABLED=false` для `dev`-старта без fail-fast;
- показывает `SET/EMPTY` для обязательных OAuth секретов без вывода их значений.

Ручной способ (если нужен полный контроль):

```bash
cp .env.example .env
cp apps/backend/.env.example apps/backend/.env
cp apps/web/.env.example apps/web/.env
cp apps/mobile/.env.example apps/mobile/.env
```

Ключевые значения:

- Backend:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/subtrack`
  - `SPRING_DATASOURCE_USERNAME=subtrack`
  - `SPRING_DATASOURCE_PASSWORD=subtrack`
  - `JWT_SECRET` минимум 32 байта
  - защита логина настраивается через `APP_SECURITY_LOGIN_*` (rate-limit по IP и lockout по `email+IP`)
  - пароль регистрации: 10-72 символов, lowercase+uppercase+digit+special, без пробелов
- Web:
  - `VITE_API_URL=http://localhost:8080/api/v1`
- Mobile:
  - Android эмулятор: `EXPO_PUBLIC_API_URL=http://10.0.2.2:8080`
  - Физическое устройство: `EXPO_PUBLIC_API_URL=http://<LAN_IP>:8080`

Важно: в `apps/mobile/src/constants/api.ts` приложение падает при старте, если `EXPO_PUBLIC_API_URL` не задан.

Обязательные параметры для реального Gmail OAuth (заполняются вручную, безопасно по умолчанию пустые):
- `GMAIL_CLIENT_ID`
- `GMAIL_CLIENT_SECRET`
- при отличающихся адресах также проверить `GMAIL_REDIRECT_URI` и `GMAIL_FRONTEND_REDIRECT_URI`

### 3) Вариант A: запуск через Docker (быстрый старт)

Запуск всего docker-compose стека:

```bash
docker compose -f infra/docker-compose.yml up -d --build
```

Или скриптом:

```bash
powershell -ExecutionPolicy Bypass -File scripts/start/start-all.ps1
```

`scripts/start/start-all.ps1` и `scripts/start/start-all.sh` теперь автоматически запускают bootstrap env перед `docker compose up`.

Проверка backend:

```bash
powershell -ExecutionPolicy Bypass -File scripts/utils/health-check.ps1 -BaseUrl http://localhost:8080 -ProbePath /v3/api-docs -TimeoutSec 60
```

Нюанс: текущий `infra/docker/web.Dockerfile` содержит placeholder-команду (`echo Web image placeholder`), поэтому web-клиент для разработки лучше запускать локально (см. вариант B).

### 4) Вариант B (рекомендуется для разработки): Docker только для PostgreSQL, остальное локально

1. Поднять PostgreSQL:

```bash
docker compose -f apps/backend/docker-compose.yml up -d
```

2. Запустить backend:

```bash
cd apps/backend
./gradlew bootRun
```

Для Windows CMD/PowerShell:

```bash
cd apps/backend
gradlew.bat bootRun
```

3. Запустить web:

```bash
cd apps/web
npm ci
npm run dev
```

Web будет доступен на `http://localhost:5173`.

4. Запустить mobile (Expo):

```bash
cd apps/mobile
npm ci
npm run start
```

Откройте Expo Dev Tools и выберите платформу:

- `a` для Android эмулятора
- `i` для iOS (macOS)
- `w` для web-режима Expo

### 5) Полезные URL после старта

- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Web: `http://localhost:5173`

### 6) Остановка

Остановить локальный PostgreSQL (backend compose):

```bash
docker compose -f apps/backend/docker-compose.yml down
```

Остановить инфраструктуру из `infra/docker-compose.yml`:

```bash
docker compose -f infra/docker-compose.yml down
```

Или скриптом:

```bash
powershell -ExecutionPolicy Bypass -File scripts/start/stop-all.ps1
```

### 7) Частые проблемы и решения

- `Unsupported class file major version` / backend не стартует:
  - убедитесь, что используется JDK 21 (`java -version`).
- `provider GMAIL mailbox import flow is disabled by feature flag`:
  - выполните `bash scripts/setup/bootstrap-env.sh` и перезапустите backend.
- `EXPO_PUBLIC_API_URL is not set` в mobile:
  - задайте `EXPO_PUBLIC_API_URL` в `apps/mobile/.env`.
- Mobile на физическом устройстве не видит backend:
  - используйте `http://<LAN_IP>:8080`, проверьте общий Wi-Fi и firewall.
- Порт занят (`5432`, `8080`, `5173`):
  - освободите порт или измените соответствующие переменные (`SERVER_PORT`, `BACKEND_PORT`, `POSTGRES_PORT`, `WEB_PORT`).
- Ошибки CORS в web:
  - добавьте origin web-клиента в `CORS_ALLOWED_ORIGINS` (по умолчанию уже есть `http://localhost:5173`).

### 8) Gmail Import Quick Start + Validation Checklist

1. Подготовить env:

```bash
bash scripts/setup/bootstrap-env.sh
```

2. Поднять PostgreSQL:

```bash
docker compose -f apps/backend/docker-compose.yml up -d
```

3. Запустить backend:

```bash
cd apps/backend
./gradlew bootRun
```

4. Проверить, что Gmail mailbox flow включён:

```bash
bash scripts/utils/check-gmail-import.sh http://localhost:8080
```

5. Проверить, что OAuth реально работает:
- В `apps/backend/.env` заполнены `GMAIL_CLIENT_ID` и `GMAIL_CLIENT_SECRET`.
- Redirect URI в Google Cloud OAuth client совпадает с `GMAIL_REDIRECT_URI`.
- Frontend доступен по адресу из `GMAIL_FRONTEND_REDIRECT_URI`.
- Выполнить login, затем `POST /api/v1/integrations/GMAIL/oauth/start` и пройти consent.
- После callback `GET /api/v1/integrations/GMAIL` должен вернуть статус подключения не `NOT_CONNECTED`.

Техническое ограничение OAuth redirect:
- Для стабильного локального сценария используйте `http://localhost:8080/api/v1/integrations/GMAIL/oauth/callback`.
- Для mobile на физическом устройстве backend может быть доступен по LAN IP, но redirect URI для Google OAuth должен быть заранее whitelisted в Google Cloud и совпадать 1:1 со значением в backend env.

---

## API Contract Policy

API-контракт backend считается стабильным и self-serve.  
Детальные правила и handoff: [BACKEND_HANDOFF.md](./BACKEND_HANDOFF.md).

Обязательные требования для любого PR с изменением API:
- обновить OpenAPI (`docs/api-notes/subtrack-openapi.yaml`) в том же PR;
- обновить/добавить backend тесты под изменённое поведение;
- обновить docs/demo сценарии при изменении внешнего поведения API;
- не допускать “тихих” breaking changes (только с явной депрекацией/миграционными заметками).

Автоматическая проверка контракта:
- локальный запуск полного набора проверок:
  - `powershell -ExecutionPolicy Bypass -File scripts/contracts/run-contract-checks.ps1`
- обновление baseline только при осознанном контрактном изменении:
  - `powershell -ExecutionPolicy Bypass -File scripts/contracts/update-openapi-baseline.ps1 -ConfirmContractChange`
- детали: `docs/api-notes/contract-guard.md`
