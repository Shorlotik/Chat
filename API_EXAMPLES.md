# Примеры использования Chat API

## 🚀 Запуск приложения

```bash
# 1. Запустить PostgreSQL (если используется локальная установка)
# PostgreSQL должен быть запущен на localhost:5432

# 2. Создать базу данных (если еще не создана)
psql postgres -c "CREATE DATABASE chatdb;"

# 3. Запустить приложение
mvn spring-boot:run
```

Приложение запустится на `http://localhost:8080`

## 📝 Примеры запросов

### 1. Регистрация пользователя

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "pass123",
    "email": "alice@test.com",
    "displayName": "Alice"
  }'
```

**Ответ:**
```json
{
  "token": "eyJhbGci...",
  "userId": 1,
  "username": "alice"
}
```

### 2. Вход в систему

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "pass123"
  }'
```

### 3. Создание личного чата

```bash
TOKEN="your_jwt_token_here"

curl -X POST http://localhost:8080/api/chats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Alice & Bob",
    "type": "PRIVATE",
    "memberIds": [2]
  }'
```

### 4. Создание группового чата

```bash
curl -X POST http://localhost:8080/api/chats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Команда разработчиков",
    "type": "GROUP",
    "memberIds": [2, 3, 4]
  }'
```

### 5. Отправка текстового сообщения со смайликами

```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "chatId": 1,
    "content": "Привет! 👋 Как дела? 😊",
    "type": "TEXT"
  }'
```

### 6. Отправка ссылки

```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "chatId": 1,
    "content": "Посмотри: https://github.com/spring-projects/spring-boot 🔗",
    "type": "LINK"
  }'
```

### 7. Получение сообщений чата

```bash
curl -X GET http://localhost:8080/api/messages/chat/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 8. Получение всех чатов пользователя

```bash
curl -X GET http://localhost:8080/api/chats \
  -H "Authorization: Bearer $TOKEN"
```

### 9. Загрузка файла (картинка, гифка, голосовое)

```bash
# Сначала отправить сообщение, получить его ID
# Затем загрузить файл

curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/image.jpg" \
  -F "messageId=1"
```

### 10. Скачивание файла

```bash
curl -X GET http://localhost:8080/api/files/1 \
  -H "Authorization: Bearer $TOKEN" \
  -o downloaded_file.jpg
```

## ✅ Реализованные функции

- ✅ Регистрация и аутентификация (JWT)
- ✅ Шифрование сообщений (AES-256)
- ✅ Личные чаты (PRIVATE)
- ✅ Групповые чаты (GROUP)
- ✅ Текстовые сообщения
- ✅ Смайлики и эмодзи
- ✅ Ссылки
- ✅ Загрузка файлов (картинки, GIF, голосовые)
- ✅ WebSocket для real-time (настроено)
- ✅ PostgreSQL база данных

## 🔒 Безопасность

1. **JWT токены** - для аутентификации
2. **AES-256 шифрование** - все сообщения хранятся в зашифрованном виде
3. **BCrypt** - для хеширования паролей
4. **Spring Security** - для защиты endpoints

## 📊 Типы сообщений

- `TEXT` - обычный текст, смайлики
- `LINK` - ссылки
- `IMAGE` - картинки
- `GIF` - гифки
- `VOICE` - голосовые сообщения

## ⚠️ Важно

Перед использованием в production обязательно замените:
1. `jwt.secret` в `application.properties` (минимум 256 бит)
2. `encryption.key` в `application.properties` (ровно 32 байта)
3. Пароль базы данных




