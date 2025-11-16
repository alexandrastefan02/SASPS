# 🧪 Testing Guide - WebSocket Chat Server

## Prerequisites

✅ **Server must be running!**
```bash
cd /Users/alexandrastefan/Desktop/MASTER/AN1/SEM1/SASPS/app/demo
java -jar target/demo-0.0.1-SNAPSHOT.war
```

You should see:
```
✅ WebSocket endpoint registered: ws://localhost:8080/ws
Tomcat started on port 8080
```

---

## Method 1: HTML Test Client (Quick & Visual)

### Step 1: Open the HTML file

```bash
# From the app directory
open test-client.html
```

Or simply **double-click** `test-client.html` in Finder

### Step 2: Test with Multiple Users

1. **Open the HTML file in 3 different browser windows/tabs**
2. **Enter different usernames:**
   - Window 1: "Alice"
   - Window 2: "Bob"  
   - Window 3: "Charlie"

3. **Click "Connect to Server"** in each window

### Step 3: Send Messages

- Type messages in any window
- **Watch them appear in ALL windows in real-time!**
- See join/leave notifications when users connect/disconnect

### What You Should See:

**In Browser:**
```
Alice: Hello everyone! 👋
Bob: Hi Alice!
Charlie: Hey team!
```

**In Server Console:**
```
🔗 ══════════ NEW CONNECTION ══════════
   Session ID: abc123
   Status: WebSocket handshake completed ✅

🆕 ══════════ USER JOINING ══════════
   Thread: http-nio-8080-exec-5
   Username: Alice

📨 ══════════ MESSAGE RECEIVED ══════════
   Thread: http-nio-8080-exec-12
   From: Alice
   Content: Hello everyone! 👋
   Broadcasting to all clients...

📊 ═══════ SERVER STATISTICS ═══════
   Active Users: 3
   Total Messages: 5
   Online: 
      - Alice
      - Bob
      - Charlie
```

---

## Method 2: Command Line Test (wscat)

### Install wscat (if needed):
```bash
npm install -g wscat
```

### Connect to server:
```bash
wscat -c ws://localhost:8080/ws
```

### Send STOMP frames manually:
```
Connected (press CTRL+C to quit)

# Subscribe to messages
> SUBSCRIBE
> id:sub-0
> destination:/topic/messages
> 
> 

# Register user
> SEND
> destination:/app/chat.register
> content-type:application/json
> 
> {"sender":"TestUser","type":"JOIN"}
> 

# Send message
> SEND
> destination:/app/chat.send
> content-type:application/json
> 
> {"sender":"TestUser","content":"Hello from wscat!","type":"CHAT"}
> 
```

---

## Method 3: curl Test (HTTP Upgrade)

```bash
curl -i -N \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Host: localhost:8080" \
  -H "Origin: http://localhost:8080" \
  -H "Sec-WebSocket-Key: SGVsbG8sIHdvcmxkIQ==" \
  -H "Sec-WebSocket-Version: 13" \
  http://localhost:8080/ws
```

**Expected Response:**
```
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: qGEgH3En71di5rrssAZTmtRTyFk=
```

---

## Testing Scenarios

### ✅ Scenario 1: Basic Connection
1. Open HTML client
2. Enter username "Alice"
3. Click Connect
4. **Expected:** Green "Connected" status

### ✅ Scenario 2: Message Broadcasting
1. Open 2 HTML clients (Alice & Bob)
2. Alice sends: "Hello Bob!"
3. **Expected:** Bob sees Alice's message instantly

### ✅ Scenario 3: Join Notifications
1. Open HTML client as Alice
2. Open another as Bob
3. **Expected:** Alice sees "Bob joined the chat!"

### ✅ Scenario 4: Leave Notifications
1. Open 2 clients (Alice & Bob)
2. Close Bob's browser window
3. **Expected:** Alice sees "Bob left the chat."

### ✅ Scenario 5: Concurrent Messages (Thread Testing!)
1. Open 5+ HTML clients
2. **Have all clients send messages rapidly**
3. Watch server console for thread activity
4. **Expected:** Multiple threads processing simultaneously

**Server Console Example:**
```
📨 [http-nio-8080-exec-5] Processing: Message from Alice
📨 [http-nio-8080-exec-12] Processing: Message from Bob
📨 [http-nio-8080-exec-18] Processing: Message from Charlie
                ↑ Different threads! ↑
```

---

## Troubleshooting

### ❌ "Connection Failed"
**Problem:** Server not running  
**Solution:** 
```bash
cd demo
java -jar target/demo-0.0.1-SNAPSHOT.war
```

### ❌ "Port 8080 already in use"
**Problem:** Another process using port 8080  
**Solution:** 
```bash
# Find and kill process
lsof -i :8080
kill -9 <PID>
```

### ❌ Messages not appearing
**Problem:** Not subscribed to topic  
**Solution:** Check browser console for errors

### ❌ CORS errors
**Problem:** Origin not allowed  
**Solution:** We set `setAllowedOrigins("*")` so this shouldn't happen

---

## Verification Checklist

✅ Server starts without errors  
✅ Can connect from browser  
✅ Can send messages  
✅ Messages appear in all connected clients  
✅ Join/leave notifications work  
✅ Multiple clients can connect simultaneously  
✅ Server console shows thread activity  
✅ Statistics update correctly  

---

## Performance Testing

### Load Test: Multiple Clients
```bash
# Open 10+ browser tabs simultaneously
# Send messages from all tabs rapidly
# Watch server console for thread pool activity
```

**What to observe:**
- Different thread names handling messages
- Thread-safe collections preventing errors
- No race conditions or crashes

### Expected Thread Activity:
```
📨 [http-nio-8080-exec-1] Processing...
📨 [http-nio-8080-exec-5] Processing...
📨 [http-nio-8080-exec-12] Processing...
📨 [http-nio-8080-exec-18] Processing...
📨 [http-nio-8080-exec-23] Processing...

👆 Multiple threads working in parallel!
```

---

## Next Steps

After testing the HTML client:
1. ✅ Verify server works correctly
2. ✅ Understand the message flow
3. ⏳ Build proper Java client (coming next!)
4. ⏳ Performance benchmarking
5. ⏳ Compare with Actor Model implementation

---

## Quick Start Commands

```bash
# Terminal 1: Start server
cd /Users/alexandrastefan/Desktop/MASTER/AN1/SEM1/SASPS/app/demo
java -jar target/demo-0.0.1-SNAPSHOT.war

# Terminal 2: Open test client
cd /Users/alexandrastefan/Desktop/MASTER/AN1/SEM1/SASPS/app
open test-client.html
```

**🎉 Happy Testing!**
