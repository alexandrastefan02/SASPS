# How The Application Works

## 📋 Overview

This is a **private 1-to-1 messaging application** built with:
- **Backend**: Java Spring Boot (with WebSocket support)
- **Frontend**: HTML/CSS/JavaScript (with SockJS and STOMP)
- **Database**: H2 in-memory database
- **Security**: Spring Security with BCrypt password encoding
- **Architecture**: Thread-based with thread-safe collections (ConcurrentHashMap)

---

## 🏗️ Architecture

### Backend Stack
- **Spring Boot 3.5.7** - Main framework
- **Java 21** - Programming language
- **Spring WebSocket** - Real-time bidirectional communication
- **STOMP Protocol** - Simple Text Oriented Messaging Protocol over WebSocket
- **SockJS** - Fallback for browsers that don't support WebSocket
- **Spring Data JPA** - Database ORM
- **H2 Database** - In-memory database (resets on restart)
- **Spring Security** - Authentication & password encryption

### Frontend Stack
- **SockJS Client** - WebSocket connection with fallback
- **STOMP.js** - STOMP protocol client
- **Vanilla JavaScript** - No frameworks
- **CSS3** - Modern styling with gradients

---

## 📊 Data Model

### User Entity (Database)
```java
@Entity
@Table(name = "users")
public class User {
    Long id;                    // Primary key
    String username;            // Unique username
    String password;            // BCrypt encoded password
    LocalDateTime createdAt;    // Registration timestamp
    LocalDateTime lastSeen;     // Last activity timestamp
    boolean online;             // Current online status
}
```

### Message (In-Memory)
```java
public class Message {
    String content;           // Message text
    String sender;            // Sender's username
    String recipient;         // Recipient's username
    LocalDateTime timestamp;  // When sent
    MessageType type;         // PRIVATE, JOIN, LEAVE, TYPING
    String conversationId;    // "alice_bob" (alphabetically sorted)
}
```

### Contact (DTO)
```java
public class Contact {
    String username;      // Unique identifier
    String displayName;   // Display name (capitalized)
    boolean online;       // Is user currently online?
    int unreadCount;      // Unread message count
}
```

---

## 🔄 Application Flow

### 1. Application Startup
1. **Spring Boot starts** on port 8080
2. **H2 Database** is created in-memory (`jdbc:h2:mem:chatdb`)
3. **DataInitializer** creates 5 demo users:
   - alice / password123
   - bob / password123
   - charlie / password123
   - diana / password123
   - eve / password123
4. **ContactService** initializes hardcoded contact relationships:
   - Alice ↔ Bob, Charlie, Diana
   - Bob ↔ Alice, Charlie, Eve
   - Charlie ↔ Alice, Bob, Diana, Eve
   - Diana ↔ Alice, Charlie
   - Eve ↔ Bob, Charlie
5. **WebSocket endpoint** registered at `ws://localhost:8080/ws`

### 2. User Registration (Optional)
**Flow:**
```
Client → POST /api/auth/register
      → UserService.registerUser()
      → BCrypt encodes password
      → Save to database
      → Return success
```

**REST Endpoint:**
- `POST /api/auth/register`
- Body: `{ "username": "john", "password": "secret" }`
- Response: `{ "success": true, "message": "User registered successfully" }`

### 3. User Login (REST)
**Flow:**
```
Client → POST /api/auth/login
      → UserService.authenticateUser()
      → BCrypt compares passwords
      → If valid: Set user.online = true
      → Return success
```

**REST Endpoint:**
- `POST /api/auth/login`
- Body: `{ "username": "alice", "password": "password123" }`
- Response: `{ "success": true, "message": "Login successful", "username": "alice" }`

### 4. WebSocket Connection
**Flow:**
```
Client → Connect to ws://localhost:8080/ws (SockJS)
      → STOMP handshake
      → Client sends /app/private.register
      → PrivateMessageController.registerUser()
      → PrivateMessageService stores (username → sessionId)
      → User is now registered for private messaging
```

**Key Points:**
- Each user gets a unique session ID (e.g., `lyarbbo4`)
- Session ID is used to route messages to specific user queues
- User status updated to ONLINE in database

### 5. Loading Contacts
**Flow:**
```
Client → GET /api/auth/contacts/alice
      → ContactService.getContactsForUser("alice")
      → Returns list of contacts with online status
      → Client displays in sidebar with green/gray indicators
```

**REST Endpoint:**
- `GET /api/auth/contacts/{username}`
- Response: 
```json
{
  "contacts": [
    { "username": "bob", "displayName": "Bob", "online": true, "unreadCount": 0 },
    { "username": "charlie", "displayName": "Charlie", "online": false, "unreadCount": 0 }
  ]
}
```

### 6. Opening a Chat
**Client-Side Only:**
- User clicks on a contact
- UI switches active chat to that user
- Message input becomes enabled
- Previous messages cleared (conversation history not implemented yet)

### 7. Sending a Private Message
**Flow:**
```
Client → stompClient.send("/app/private.send", {}, message)
      → PrivateMessageController.sendPrivateMessage()
      → PrivateMessageService.sendPrivateMessage(sender, recipient, content)
      → Validates users are contacts
      → Checks if recipient is online (has session)
      → Creates Message object with timestamp
      → Generates conversationId (e.g., "alice_bob" - alphabetically sorted)
      → Stores message in conversationHistory map
      → Sends to recipient via /queue/messages/bob
      → Also sends copy to sender via /queue/messages/alice
```

**WebSocket Message:**
```javascript
{
  "sender": "alice",
  "recipient": "bob",
  "content": "Hello Bob!",
  "type": "PRIVATE"
}
```

**Message Routing:**
- Sent to: `/queue/messages/bob` (recipient's private queue)
- Sent to: `/queue/messages/alice` (sender's copy)

### 8. Receiving Messages
**Flow:**
```
Server → Sends message to /queue/messages/{username}
      → Client's subscription receives it
      → JavaScript displayMessage() function renders it
      → Own messages aligned right (blue)
      → Other's messages aligned left (white)
      → Auto-scroll to bottom
```

**Client Subscription:**
```javascript
stompClient.subscribe('/queue/messages/' + currentUser, function (message) {
    const messageData = JSON.parse(message.body);
    displayMessage(messageData);
});
```

### 9. Typing Indicators (Not Yet Fully Implemented)
**Planned Flow:**
```
Client → User types in input
      → Send /app/private.typing
      → PrivateMessageService.handleTypingIndicator()
      → Send to recipient's /queue/typing/{username}
      → Recipient sees "Bob is typing..."
```

### 10. Logout
**Flow:**
```
Client → POST /api/auth/logout
      → UserService.setUserOnline(username, false)
      → User marked as offline in database
      → stompClient.disconnect()
      → WebSocketEventListener.handleWebSocketDisconnectListener()
      → PrivateMessageService.removeUserSession(username)
      → Session removed from active sessions
```

---

## 🧵 Threading Model

### Tomcat Thread Pool
- **Default**: 200 max threads
- **Min Spare**: 10 threads
- **Request Handling**: Each WebSocket frame processed by a thread from pool
- **Thread Names**: 
  - `http-nio-8080-exec-*` - HTTP requests (REST API)
  - `clientInboundChannel-*` - Incoming WebSocket messages
  - `clientOutboundChannel-*` - Outgoing WebSocket messages

### Thread-Safe Collections
```java
// In PrivateMessageService
private final Map<String, String> userSessions = new ConcurrentHashMap<>();
private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();
private final Map<String, Set<String>> typingIndicators = new ConcurrentHashMap<>();
```

**Why Thread-Safe?**
- Multiple users can send messages concurrently
- ConcurrentHashMap allows:
  - Multiple concurrent reads
  - Atomic put/get operations
  - No external synchronization needed

### Concurrent Message Flow Example
```
Thread-1 (Alice sends to Bob)  →  PrivateMessageService.sendPrivateMessage()
Thread-2 (Bob sends to Alice)  →  PrivateMessageService.sendPrivateMessage()
Thread-3 (Charlie sends to Bob) → PrivateMessageService.sendPrivateMessage()

All threads operate on the same ConcurrentHashMap safely!
```

---

## 📁 Project Structure

```
demo/
├── src/main/java/com/actormodelsasps/demo/
│   ├── DemoApplication.java                    # Main entry point
│   │
│   ├── config/
│   │   ├── WebSocketConfig.java               # WebSocket & STOMP setup
│   │   ├── SecurityConfig.java                # Spring Security config
│   │   └── DataInitializer.java               # Creates demo users on startup
│   │
│   ├── controller/
│   │   ├── AuthController.java                # REST: login, register, logout
│   │   ├── PrivateMessageController.java      # WebSocket: private messaging
│   │   └── ChatController.java                # (Old - not used)
│   │
│   ├── service/
│   │   ├── UserService.java                   # User authentication & management
│   │   ├── PrivateMessageService.java         # Private messaging logic
│   │   ├── ContactService.java                # Contact relationships
│   │   └── SessionManager.java                # (Old - not used)
│   │
│   ├── model/
│   │   ├── User.java                          # JPA Entity
│   │   ├── Message.java                       # Message DTO
│   │   ├── Contact.java                       # Contact DTO
│   │   └── ChatUser.java                      # (Old - not used)
│   │
│   ├── repository/
│   │   └── UserRepository.java                # JPA Repository
│   │
│   └── listener/
│       └── WebSocketEventListener.java        # Connection/Disconnection events
│
├── src/main/resources/
│   └── application.properties                 # Database, logging, JPA config
│
└── pom.xml                                    # Maven dependencies

private-chat-client.html                       # Frontend client
```

---

## 🔌 WebSocket Configuration

### Endpoints
- **Connection**: `ws://localhost:8080/ws` (with SockJS fallback)
- **Message Prefixes**:
  - `/app/*` → Routes to `@MessageMapping` in controllers
  - `/queue/*` → Private user queues (1-to-1 messaging)
  - `/topic/*` → Broadcast topics (not used in current version)
  - `/user/*` → User-specific destinations

### STOMP Routes
| Route | Type | Purpose |
|-------|------|---------|
| `/app/private.register` | Client → Server | Register user session for messaging |
| `/app/private.send` | Client → Server | Send private message |
| `/app/private.history` | Client → Server | Request conversation history |
| `/app/private.typing` | Client → Server | Send typing indicator |
| `/queue/messages/{username}` | Server → Client | Receive private messages |
| `/queue/typing/{username}` | Server → Client | Receive typing indicators |

---

## 🔐 Security

### Password Encoding
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```
- Passwords hashed with BCrypt (cost factor 10)
- Salted automatically (prevents rainbow table attacks)
- One-way encryption (cannot be decrypted)

### Authentication Flow
1. User enters username + password
2. `UserService.authenticateUser()` called
3. Fetch user from database by username
4. Compare: `passwordEncoder.matches(plainPassword, user.getPassword())`
5. Return true/false

### Current Limitations
- No JWT tokens (sessions not persisted)
- No HTTPS (dev environment only)
- CORS enabled for all origins (`@CrossOrigin(origins = "*")`)
- Passwords stored in application.properties (demo only)

---

## 💾 Database Schema

### H2 In-Memory Database
- **URL**: `jdbc:h2:mem:chatdb`
- **Console**: `http://localhost:8080/h2-console`
- **Username**: `sa`
- **Password**: (empty)

### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    last_seen TIMESTAMP,
    is_online BOOLEAN DEFAULT FALSE
);
```

### Data Persistence
⚠️ **Important**: H2 is configured with `spring.jpa.hibernate.ddl-auto=create-drop`
- Database is **recreated** on every restart
- All messages are **lost** when server stops
- Users are **recreated** by DataInitializer on startup

For production:
- Change to `spring.jpa.hibernate.ddl-auto=update`
- Use persistent database (PostgreSQL, MySQL)
- Add Message entity with JPA

---

## 🧪 Testing the Application

### Starting the Server
```bash
cd /Users/alexandrastefan/Desktop/MASTER/AN1/SEM1/SASPS/app/demo
mvn clean package -DskipTests
java -jar target/demo-0.0.1-SNAPSHOT.war
```

Server starts on: **http://localhost:8080**

### Opening the Client
1. Open `private-chat-client.html` in a browser
2. Click on a demo user button (e.g., "alice")
3. Click "Login"
4. Open another browser tab (or incognito window)
5. Login as a different user (e.g., "bob")
6. Click on each other's names in contacts list
7. Send messages!

### Demo Users
| Username | Password | Contacts |
|----------|----------|----------|
| alice | password123 | Bob, Charlie, Diana |
| bob | password123 | Alice, Charlie, Eve |
| charlie | password123 | Alice, Bob, Diana, Eve |
| diana | password123 | Alice, Charlie |
| eve | password123 | Bob, Charlie |

### Expected Behavior
✅ **Working:**
- User login/registration
- Contact list with online/offline status
- Opening 1-to-1 conversations
- Sending/receiving private messages in real-time
- Messages displayed with timestamps
- Auto-scroll to newest message
- User logout

⚠️ **Not Yet Implemented:**
- Conversation history persistence
- Typing indicators (backend ready, frontend incomplete)
- Unread message counts
- Message delivery receipts
- Message read receipts
- Offline message queuing
- File/image sharing
- Group chats

---

## 🐛 Common Issues

### 1. Users Can't See Each Other Online
**Cause**: Frontend subscription happens after other users already logged in

**Solution**: Subscribe to global topics immediately after WebSocket connection, before login

### 2. Messages Not Received
**Checks**:
- Are users contacts? (Check `ContactService.areUsersContacts()`)
- Is recipient online? (Check `PrivateMessageService.isUserOnline()`)
- Is WebSocket connected? (Check browser console)
- Correct subscription? (Should be `/queue/messages/{username}`)

### 3. Session ID Issues
**Cause**: Session ID extraction from WebSocket URL fails

**Solution**: Use reliable extraction:
```javascript
const wsUrl = stompClient.ws._transport.url;
const urlParts = wsUrl.split('/');
const sessionId = urlParts[urlParts.length - 2];
```

### 4. Database Connection Failed
**Cause**: H2 console trying to connect with wrong URL

**Solution**: Use `jdbc:h2:mem:chatdb` (not `jdbc:h2:~/chatdb`)

---

## 🚀 Future Improvements

### 1. Persistence
- Add `Message` entity with JPA
- Store messages in database
- Implement conversation history loading
- Paginated message loading

### 2. Security
- Implement JWT tokens
- Add HTTPS support
- Secure WebSocket connections (wss://)
- Rate limiting for messages
- Input sanitization

### 3. Features
- Complete typing indicators
- Unread message counts
- Message read receipts
- User status messages ("Away", "Busy")
- Group chats
- File sharing
- Emoji reactions
- Message editing/deletion

### 4. Performance
- Implement message pagination
- Add Redis for session management
- Database connection pooling
- WebSocket compression

### 5. Actor Model Implementation
**Next Phase**: Rebuild using Actor Model pattern
- Each user = Actor
- Each conversation = Actor
- Message passing instead of shared state
- Compare performance with thread-based approach

---

## 📊 Logging & Debugging

### Server Logs
Look for these indicators:
```
✅ Demo data initialized successfully
✅ WebSocket endpoint registered: ws://localhost:8080/ws
👤 USER CONNECTING: Username: alice, Session ID: abc123
💬 Private message sent: alice -> bob: "Hello!"
📇 Retrieved 3 contacts for user: alice
```

### Logging Levels (application.properties)
```properties
logging.level.com.actormodelsasps=DEBUG          # Your application
logging.level.org.springframework.web.socket=DEBUG  # WebSocket details
logging.level.org.hibernate.SQL=DEBUG            # SQL queries
```

### Browser Console
Check for:
- WebSocket connection status
- STOMP frame messages
- Subscription confirmations
- Message send/receive events

---

## 📖 Key Technologies Explained

### STOMP (Simple Text Oriented Messaging Protocol)
- Text-based protocol over WebSocket
- Frame-based (like HTTP)
- Commands: CONNECT, SEND, SUBSCRIBE, DISCONNECT
- Destinations: /app/*, /queue/*, /topic/*

### SockJS
- WebSocket emulation library
- Provides fallbacks when WebSocket unavailable:
  1. WebSocket (preferred)
  2. HTTP Streaming
  3. HTTP Long Polling
  4. HTTP POST chunked

### Spring WebSocket Architecture
```
Client
  ↓ (WebSocket/SockJS)
Spring WebSocket Handler
  ↓ (STOMP)
Message Broker (SimpleBroker)
  ↓ (Routing)
@MessageMapping Controllers
  ↓ (Business Logic)
Services
  ↓ (Response)
SimpMessagingTemplate
  ↓ (Broker)
Client's Subscriptions
```

### Thread Safety
- **ConcurrentHashMap**: Lock-free reads, segmented writes
- **CopyOnWriteArrayList**: Snapshot iteration, expensive writes
- **Atomic Operations**: `computeIfAbsent()`, `putIfAbsent()`

---

## 🎯 Summary

Your application is a **private 1-to-1 messaging platform** with:

**✅ What's Working:**
- REST API for authentication (login/register/logout)
- WebSocket real-time messaging with STOMP protocol
- Thread-safe concurrent message handling
- Contact list with online/offline status
- Private message routing to specific users
- In-memory message history per conversation
- H2 database with user persistence

**🏗️ Architecture:**
- Thread-based concurrency (Tomcat thread pool)
- Pub/Sub pattern with private queues
- Service layer for business logic
- Repository pattern for data access
- DTO pattern for data transfer

**🔜 Next Steps:**
- Complete typing indicators
- Add message persistence
- Implement conversation history loading
- Build Actor Model version for comparison

This is a solid foundation for comparing thread-based vs actor-based messaging systems! 🚀
