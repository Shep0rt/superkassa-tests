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

## Конфигурация

Runtime-конфигурация загружается из `application.conf` и переопределяется системными свойствами
или переменными окружения.

Секреты нельзя коммитить в репозиторий. Токены, пароли и доступы к стендам должны передаваться
через переменные окружения локально или через secret-переменные CI/CD.
