# 💬 Secure Chat

Защищенный чат на Java 21 с Spring Boot и PostgreSQL, поддерживающий шифрование сообщений, групповые чаты и отправку медиафайлов.

## 🚀 Технологии

- **Java 21** - современная версия Java
- **Spring Boot 3.2** - фреймворк для backend
- **Spring Security** - защита endpoints
- **Spring WebSocket** - real-time коммуникация
- **PostgreSQL** - надежная база данных
- **JWT** - токены для аутентификации
- **AES-256** - шифрование сообщений
- **Lombok** - упрощение кода
- **Maven** - сборка проекта

## ✨ Функционал

### Аутентификация
- ✅ Регистрация пользователей
- ✅ JWT авторизация
- ✅ BCrypt хеширование паролей

### Чаты
- ✅ Личные чаты (один-на-один)
- ✅ Групповые чаты
- ✅ Управление участниками

### Сообщения
- ✅ Текстовые сообщения
- ✅ Смайлики и эмодзи 😊👋🎉
- ✅ Ссылки
- ✅ Картинки
- ✅ GIF анимации
- ✅ Голосовые сообщения
- ✅ **AES-256 шифрование** всех сообщений

### Real-time
- ✅ WebSocket для мгновенной доставки
- ✅ Уведомления о новых сообщениях

## 📦 Установка и запуск

### Требования
- Java 21+
- Maven 3.8+
- PostgreSQL 15+

### Быстрый старт

1. **Клонируйте репозиторий**
```bash
git clone <repository-url>
cd Chat
```

2. **Настройте базу данных**
```bash
# Создайте БД
psql postgres -c "CREATE DATABASE chatdb;"
```

3. **Настройте application.properties**
```properties
# Обновите настройки подключения к БД
spring.datasource.url=jdbc:postgresql://localhost:5432/chatdb
spring.datasource.username=your_username
spring.datasource.password=your_password

# ⚠️ ВАЖНО: Замените ключи для production
jwt.secret=your-secret-key-min-256-bits
encryption.key=your-32-byte-encryption-key!
```

4. **Соберите и запустите**
```bash
mvn clean install
mvn spring-boot:run
```

Приложение будет доступно на `http://localhost:8080`

## 🌐 Веб-интерфейс

Откройте браузер и перейдите на `http://localhost:8080`

### Возможности веб-интерфейса:
- 🎨 Современный адаптивный дизайн
- 🔐 Регистрация и вход
- 💬 Список всех чатов
- 📝 Отправка сообщений
- ⚡ Real-time обновления (WebSocket)
- 👥 Создание личных и групповых чатов
- 😊 Поддержка смайликов

## 📖 API Использование

См. [API_EXAMPLES.md](API_EXAMPLES.md) для подробных примеров использования REST API.

### Быстрый пример

```bash
# 1. Регистрация
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123","email":"alice@test.com","displayName":"Alice"}'

# 2. Получение токена
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123"}' \
  | jq -r '.token')

# 3. Создание чата
curl -X POST http://localhost:8080/api/chats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Мой чат","type":"PRIVATE","memberIds":[2]}'

# 4. Отправка сообщения
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"chatId":1,"content":"Привет! 👋","type":"TEXT"}'
```

## 🔒 Безопасность

### Шифрование
Все сообщения шифруются с использованием AES-256 перед сохранением в базу данных. Только авторизованные пользователи могут расшифровывать сообщения.

### Аутентификация
- JWT токены с настраиваемым временем жизни
- Защита от несанкционированного доступа
- Проверка членства в чатах перед отправкой сообщений

### Хранение паролей
Пароли хешируются с использованием BCrypt перед сохранением.

## 🗄️ Структура базы данных

```sql
users           - пользователи
chats           - чаты (личные и групповые)
chat_members    - связь чатов и участников
messages        - сообщения (зашифрованные)
attachments     - вложения (файлы)
```

## 🛠️ Разработка

### Структура проекта
```
src/main/java/com/chat/
├── config/          # Конфигурация (Security, WebSocket)
├── controller/      # REST контроллеры
├── dto/            # Data Transfer Objects
├── entity/         # JPA сущности
├── repository/     # Репозитории
├── security/       # JWT фильтры и утилиты
├── service/        # Бизнес-логика
└── util/           # Утилиты (шифрование)
```

## 📝 TODO

- [ ] Добавить тесты
- [ ] Реализовать фронтенд клиент
- [ ] Добавить поиск по сообщениям
- [ ] Реализовать редактирование/удаление сообщений
- [ ] Добавить статусы "прочитано/не прочитано"
- [ ] Реализовать типизацию (пользователь печатает...)
- [ ] Добавить уведомления

## 📄 Лицензия

MIT

## 👥 Автор

Created with ❤️

