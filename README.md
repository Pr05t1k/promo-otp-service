# Promo OTP Service

Сервис для генерации и верификации одноразовых паролей (OTP) с поддержкой нескольких каналов доставки.

## Требования

- Java 17 или выше
- Docker и Docker Compose (для PostgreSQL и SMPPsim)
- Maven 3.8+

## Быстрый старт

### 1. Запуск базы данных и эмулятора SMS
docker-compose up -d
2. Настройка окружения
Скопируйте .env.example в .env и заполните необходимые параметры:
cp .env.example .env
3. Сборка проекта
mvn clean compile
4. Запуск сервера
mvn exec:java -Dexec.mainClass="com.promo.otp.Server"
Или
mvn package
java -jar target/otp-service-1.0-SNAPSHOT.jar

### API Endpoints
Аутентификация
Метод	Endpoint	Описание
POST	/api/auth/register	Регистрация пользователя
POST	/api/auth/login	Вход в систему
Пользовательские endpoints (требуют токен)
Метод	Endpoint	Описание
POST	/api/user/otp/generate	Генерация OTP кода
POST	/api/user/otp/validate	Проверка OTP кода
Административные endpoints (требуют токен и роль ADMIN)
Метод	Endpoint	Описание
GET	/api/admin/users	Получение списка пользователей
DELETE	/api/admin/users/{id}	Удаление пользователя
PUT	/api/admin/config	Изменение конфигурации OTP

### Примеры запросов

Регистрация
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "login": "john_doe",
    "password": "securePass123",
    "role": "USER",
    "email": "john@example.com",
    "phone": "+1234567890"
  }'

Логин
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login": "john_doe", "password": "securePass123"}'

Генерация OTP (с токеном)
curl -X POST http://localhost:8080/api/user/otp/generate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "operationId": "transfer_12345",
    "deliveryMethod": "EMAIL"
  }'

Проверка OTP
curl -X POST http://localhost:8080/api/user/otp/validate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "operationId": "transfer_12345",
    "code": "123456"
  }'

### Структура проекта
promo-otp-service/
├── src/
│   ├── main/
│   │   ├── java/com/promo/otp/
│   │   │   ├── Server.java
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── dao/
│   │   │   ├── model/
│   │   │   ├── security/
│   │   │   ├── notification/
│   │   │   └── scheduler/
│   │   └── resources/
│   │       ├── db/migration/schema.sql
│   │       └── logback.xml
│   └── test/
├── docker-compose.yml
├── pom.xml
└── README.md

### Тестирование
Убедитесь, что PostgreSQL запущен: docker ps
Запустите сервер
Используйте предоставленные curl команды или Postman для тестирования API