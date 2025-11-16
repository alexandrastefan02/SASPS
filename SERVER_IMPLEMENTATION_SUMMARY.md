# Team Work Chat Application - Server (Thread-Based)

## ✅ SERVER IMPLEMENTATION COMPLETE!

### What We Built

A **real-time chat server** using:
- ⚡ **WebSockets** for bidirectional communication
- 🧵 **Classic Thread-Based Architecture** (Tomcat thread pool)
- 🔒 **Thread-Safe Collections** (ConcurrentHashMap, CopyOnWriteArrayList)
- 📨 **STOMP Protocol** over WebSocket

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    SERVER (Running on Port 8080)            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  WebSocket Endpoint: ws://localhost:8080/ws                 │
│                            ↓                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Tomcat Thread Pool (200 threads)             │  │
│  │   [T1] [T2] [T3] ... [T200]                         │  │
│  └──────────────────────────────────────────────────────┘  │
│                            ↓                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Message Broker (STOMP)                       │  │
│  │   /app/*   → Routes to controllers                   │  │
│  │   /topic/* → Broadcasts to subscribers               │  │
│  └──────────────────────────────────────────────────────┘  │
│                            ↓                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         ChatController                               │  │
│  │   @MessageMapping("/chat.send")                      │  │
│  │   @MessageMapping("/chat.register")                  │  │
│  │   @MessageMapping("/chat.history")                   │  │
│  └──────────────────────────────────────────────────────┘  │
│                            ↓                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         SessionManager (Thread-Safe!)                │  │
│  │   ConcurrentHashMap<String, ChatUser>               │  │
│  │   CopyOnWriteArrayList<Message>                     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Files Created

### 1. **Configuration**
- `pom.xml` - Added spring-boot-starter-websocket
- `application.properties` - Server port 8080

### 2. **Models**
- `Message.java` - Chat message with content, sender, timestamp, type
- `ChatUser.java` - Connected user info

### 3. **Configuration**
- `WebSocketConfig.java` - WebSocket endpoint and message broker setup

### 4. **Services**
- `SessionManager.java` - Thread-safe session and message management

### 5. **Controllers**
- `ChatController.java` - Handles chat messages via @MessageMapping

### 6. **Listeners**
- `WebSocketEventListener.java` - Handles connect/disconnect events

---

## How It Works (Thread-Based)

### Message Flow Example:

```
3 clients send messages simultaneously:

T=0ms: Client A sends "Hello"
T=2ms: Client B sends "Hi"
T=5ms: Client C sends "Hey"
         ↓
    Thread Pool
         ↓
[Thread-5]  ← processes "Hello"  }
[Thread-12] ← processes "Hi"     } ALL IN PARALLEL!
[Thread-18] ← processes "Hey"    }
         ↓
Each thread calls ChatController.handleChatMessage()
         ↓
Each thread adds to SessionManager (THREAD-SAFE!)
         ↓
Broadcasts to all subscribers
```

### Thread-Safety Mechanisms:

**ConcurrentHashMap** (for users):
```java
// Multiple threads can access simultaneously
activeUsers.put(sessionId, user);  // Thread-safe!
```

**CopyOnWriteArrayList** (for messages):
```java
// Thread-safe: Creates new copy on write
messageHistory.add(message);  // No locks needed for reads!
```

---

## Server Endpoints

### WebSocket Connection
- **URL:** `ws://localhost:8080/ws`
- **Protocol:** STOMP over WebSocket

### Message Destinations

#### Send To (Client → Server):
- `/app/chat.register` - Register username
- `/app/chat.send` - Send chat message
- `/app/chat.history` - Request message history

#### Subscribe To (Server → Client):
- `/topic/messages` - Receive broadcast messages
- `/topic/history` - Receive message history

---

## Running the Server

```bash
# Build
cd demo
mvn clean package -DskipTests

# Run
java -jar target/demo-0.0.1-SNAPSHOT.war
```

**Server will start on:** `http://localhost:8080`  
**WebSocket endpoint:** `ws://localhost:8080/ws`

---

## What's Next?

Now we need to create a **client application** that:
1. Connects to `ws://localhost:8080/ws`
2. Registers with a username
3. Sends and receives messages
4. Shows real-time chat

---

## Thread-Based Architecture Notes

### ✅ Advantages:
- Simple and straightforward
- Well-understood model
- Good for moderate load

### ⚠️ Challenges:
- Lock contention under high load
- Thread context switching overhead
- Need careful synchronization

### 🎯 Performance Characteristics:
- **Good:** 10-100 concurrent users
- **Okay:** 100-1000 concurrent users
- **Challenging:** 1000+ concurrent users

This is why we'll later compare with the **Actor Model** implementation!

---

## Next Steps

1. ✅ Server is running
2. ⏳ Create client application
3. ⏳ Test multi-client communication
4. ⏳ Measure performance
5. ⏳ Compare with Actor Model (later)

---

**Server Status:** ✅ RUNNING on port 8080
**Ready for client connections!** 🚀
