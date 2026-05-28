# Инструкция по установке и запуску

## Предварительные требования

1. **Java 17** - [Скачать](https://adoptium.net/)
2. **Docker Desktop** - [Скачать](https://www.docker.com/products/docker-desktop/)
3. **Maven** (опционально, можно использовать встроенный)

## Быстрый запуск

### 1. Запуск базы данных
docker-compose up -d
2. Запуск приложения
Через IDEA:
Открой проект в IntelliJ IDEA

Запусти src/main/java/com/promo/otp/Server.java

Через Maven:
mvn clean compile
mvn exec:java -Dexec.mainClass="com.promo.otp.Server"
3. Проверка работы
Открой новый терминал и выполни:


# Регистрация
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"login":"test","password":"pass123","role":"USER"}'

# Логин
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"test","password":"pass123"}'

# Админ (пароль admin123)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin123"}'

  Остановка
# Остановка базы данных
docker-compose down

# Остановка приложения - Ctrl+C в терминале с сервером
Устранение проблем
Порт 5432 уже занят
Измени порт в docker-compose.yml:
ports:
  - "5433:5432"
Ошибка подключения к БД
Убедись, что PostgreSQL запущен:
docker ps | findstr postgres
