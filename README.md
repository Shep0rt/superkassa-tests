# Автотесты Superkassa

Внешний проект автотестов для проверки Superkassa Server, интеграции с настоящим тестовым ОФД,
соответствия протоколу CPCR и состояния данных в БД.

## Слои тестирования

- API Contract: REST API, HTTP-статусы, схемы ответов, валидация, авторизация и идемпотентность.
- Functional / Business: бизнес-сценарии кассы: смены, чеки, возвраты, статусы фискализации.
- OFD Protocol / Integration: CPCR-артефакты, доставка в ОФД и протокольные данные, наблюдаемые на реальном тестовом ОФД.
- DB / Persistence: read-only проверки БД: смены, чеки, очереди, статусы и защита от дублей.

## Тест-сьюты

```bash
./gradlew smokeTest
./gradlew regressionTest
./gradlew apiSmokeTest
./gradlew functionalSmokeTest
./gradlew protocolSmokeTest
./gradlew dbSmokeTest
```

Будущие тесты должны использовать составные аннотации: `@ApiSmoke`, `@FunctionalRegression`,
`@ProtocolSmoke`, `@DbRegression` и аналогичные.

Gradle-задачи используют пересечение тегов. Например, `apiSmokeTest` запускает тесты,
помеченные как `api & smoke`.

## Структура проекта

Проект автотестов переведен на Kotlin. Новые тесты и framework-код пишем на Kotlin.

- `src/main/kotlin`: тестовый framework, API-клиенты, конфигурация, DB/protocol/support helpers.
- `src/test/kotlin`: сами тесты, базовый класс тестов и теги `@ApiSmoke`, `@ApiRegression` и аналогичные.
- `src/main/resources`: runtime-конфигурация тестов.
- `src/test/resources`: настройки JUnit Platform.

## Запуск тестов и Allure 3 отчет

Команды ниже повторяют привычный сценарий: сначала запускаем нужный сьют, затем строим
Allure 3 отчет из полученных результатов.

Перед запуском API-тестов в Superkassa должны быть заранее созданы несколько ККМ.
Тесты используют существующие ККМ как тестовые данные для проверок списка касс,
пользователей и операций над конкретной ККМ.

Проект использует Allure Gradle Plugin и скачивает runtime Allure 3 во время сборки.
Устанавливать Node.js локально для генерации отчета не нужно.

### 1. API smoke + HTML-отчет

```bash
./gradlew --continue clean apiSmokeTest allureReport \
  -Dsuperkassa.base-url=http://localhost:8080 \
  -Dsuperkassa.auth-pin=local-token
```

Готовый отчет будет в:

```text
build/reports/allure-report/allureReport/index.html
```

Открыть отчет на macOS:

```bash
open build/reports/allure-report/allureReport/index.html
```

### 2. API smoke + web-сервер Allure

```bash
./gradlew --continue clean apiSmokeTest allureServe \
  -Dsuperkassa.base-url=http://localhost:8080 \
  -Dsuperkassa.auth-pin=local-token
```

`allureServe` поднимает локальный web-сервер и держит процесс Gradle активным,
пока сервер не будет остановлен.

### 3. API regression + HTML-отчет

```bash
./gradlew --continue clean apiRegressionTest allureReport \
  -Dsuperkassa.base-url=http://localhost:8080 \
  -Dsuperkassa.auth-pin=local-token
```

Готовый отчет будет в:

```text
build/reports/allure-report/allureReport/index.html
```

Открыть отчет на macOS:

```bash
open build/reports/allure-report/allureReport/index.html
```

### 4. API regression + web-сервер Allure

```bash
./gradlew --continue clean apiRegressionTest allureServe \
  -Dsuperkassa.base-url=http://localhost:8080 \
  -Dsuperkassa.auth-pin=local-token
```

`allureServe` поднимает локальный web-сервер только по результатам API regression-сьюта.

### 5. API smoke and regression + HTML-отчет

```bash
./gradlew --continue clean apiSmokeTest apiRegressionTest allureReport \
  -Dsuperkassa.base-url=http://localhost:8080 \
  -Dsuperkassa.auth-pin=local-token
```

### 6. API smoke and regression + web-сервер Allure

```bash
./gradlew --continue clean apiSmokeTest apiRegressionTest allureServe \
  -Dsuperkassa.base-url=http://localhost:8080 \
  -Dsuperkassa.auth-pin=local-token
```

Для запуска без очистки достаточно убрать `clean`.

## Конфигурация

Runtime-конфигурация загружается из `application.conf` и переопределяется системными свойствами
или переменными окружения.

Основные параметры:

- `-Dsuperkassa.base-url=http://localhost:8080` или переменная окружения `SUPERKASSA_BASE_URL`.
- `-Dsuperkassa.auth-pin=local-token` или переменная окружения `SUPERKASSA_AUTH_PIN`.

Для сценариев, которым нужны права администратора ККМ, `superkassa.auth-pin` должен быть
валидным текущим PIN администратора. Если у администратора стоит стандартный PIN, тесты
могут подготовить пользователя и заменить PIN на тестовый `0808`, после чего использовать
его для проверок режима программирования.

Секреты нельзя коммитить в репозиторий. Токены, пароли и доступы к стендам должны передаваться
через переменные окружения локально или через secret-переменные CI/CD.
