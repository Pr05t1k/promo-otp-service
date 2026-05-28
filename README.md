# Promo OTP Service

Сервис для генерации и верификации одноразовых паролей (OTP) с поддержкой JWT аутентификации и разграничением ролей (пользователь/администратор).

## Функциональность

### Пользователи
- Регистрация и аутентификация с JWT токенами
- Генерация OTP кодов для операций
- Валидация OTP кодов
- Отправка кодов через: Email, SMS (эмулятор), Telegram, FILE

### Администраторы
- Просмотр всех пользователей
- Удаление пользователей
- Настройка OTP параметров (длина кода, время жизни)

## 🛠 Технологии

- **Java 17** - основной язык
- **com.sun.net.httpserver** - встроенный HTTP сервер
- **PostgreSQL 17** - база данных
- **JDBC** - работа с БД
- **JWT (JJWT)** - аутентификация
- **Maven** - сборка проекта
- **Docker** - контейнеризация БД
- **Logback** - логирование

## Требования

- Java 17 или выше
- Docker и Docker Compose (для PostgreSQL)
- Maven 3.8+

## Быстрый старт

### 1. Клонирование репозитория

git clone https://github.com/YOUR_USERNAME/promo-otp-service.git
cd promo-otp-service
### 2. Запуск базы данных
docker-compose up -d
### 3. Сборка проекта
mvn clean compile
### 4. Запуск сервера
mvn exec:java -Dexec.mainClass="com.promo.otp.Server"
Сервер запустится на http://localhost:8080

### API Документация
Аутентификация
Регистрация пользователя
POST /api/auth/register
Content-Type: application/json
{
  "login": "username",
  "password": "password123",
  "role": "USER",
  "email": "user@example.com",
  "phone": "+1234567890"
}

Вход в систему
POST /api/auth/login
Content-Type: application/json
{
  "login": "username",
  "password": "password123"
}

# Response:
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "USER",
  "userId": 1,
  "login": "username"
}

Пользовательские API (требуют Bearer токен)
Генерация OTP кода
POST /api/user/otp/generate
Authorization: Bearer <token>
Content-Type: application/json
{
  "operationId": "transfer_12345",
  "deliveryMethod": "FILE"  // EMAIL, SMS, TELEGRAM, FILE
}

# Response:
{
  "message": "OTP code generated and sent successfully",
  "operationId": "transfer_12345",
  "deliveryMethod": "FILE",
  "debug_code": "123456"
}
Валидация OTP кода
POST /api/user/otp/validate
Authorization: Bearer <token>
Content-Type: application/json
{
  "operationId": "transfer_12345",
  "code": "123456"
}

# Response:
{
  "message": "OTP code validated successfully",
  "operationId": "transfer_12345",
  "status": "VERIFIED"
}
Административные API (требуют роль ADMIN)
Получение списка пользователей
GET /api/admin/users
Authorization: Bearer <admin_token>

# Response:
{
  "users": [...],
  "count": 5
}
Обновление конфигурации OTP
PUT /api/admin/config
Authorization: Bearer <admin_token>
Content-Type: application/json
{
  "codeLength": 8,
  "ttlSeconds": 600
}
Удаление пользователя
DELETE /api/admin/users/{userId}
Authorization: Bearer <admin_token>

Тестирование
Пример тестового сценария
# 1. Регистрация пользователя
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"login":"test","password":"pass123","role":"USER"}'

# 2. Логин
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"test","password":"pass123"}'

# 3. Генерация OTP
curl -X POST http://localhost:8080/api/user/otp/generate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"operationId":"test_op","deliveryMethod":"FILE"}'

# 4. Валидация OTP
curl -X POST http://localhost:8080/api/user/otp/validate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"operationId":"test_op","code":"123456"}'

  Структура проекта
text
promo-otp-service/
├── src/main/java/com/promo/otp/
│   ├── Server.java                 # Точка входа
│   ├── controller/                 # HTTP обработчики
│   ├── service/                    # Бизнес-логика
│   ├── dao/                        # Работа с БД
│   ├── model/                      # Сущности
│   ├── security/                   # JWT аутентификация
│   ├── notification/               # Каналы отправки
│   └── scheduler/                  # Планировщик задач
├── src/main/resources/
│   ├── db/migration/schema.sql    # Схема БД
│   └── logback.xml                 # Конфигурация логирования
├── docker-compose.yml              # Docker для PostgreSQL
├── pom.xml                         # Maven конфигурация
└── README.md

Конфигурация
Создай файл .env в корне проекта (опционально для Email/Telegram):
properties
# Email (опционально)
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587

# Telegram (опционально)
TELEGRAM_BOT_TOKEN=your_bot_token
TELEGRAM_CHAT_ID=your_chat_id
 Тестовые аккаунты
Администратор: login: admin, password: admin123
Пользователь: login: test, password: pass123
