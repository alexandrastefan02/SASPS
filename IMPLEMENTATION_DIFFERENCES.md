# Implementation Differences: Thread-Based vs Actor Model

## Side-by-Side Code Comparison

This document shows the actual implementation differences between Variant 1 and Variant 2.

---

## 1. Message Processing

### Variant 1 (Thread-Based): ChatController.java

```java
@Controller
public class ChatController {
    
    @Autowired
    private SessionManager sessionManager;
    
    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public Message handleChatMessage(Message message, 
                                    SimpMessageHeaderAccessor headerAccessor) {
        
        String threadName = Thread.currentThread().getName();
        System.out.println("Thread: " + threadName);
        
        // Set server timestamp
        message.setTimestamp(LocalDateTime.now());
        
        // ⚠️ CRITICAL SECTION: Multiple threads call this simultaneously!
        // SessionManager uses CopyOnWriteArrayList (thread-safe but expensive)
        sessionManager.addMessage(message);
        
        // Return message - Spring broadcasts it
        return message;
    }
}
```

**Characteristics:**
- ✅ Simple and direct
- ⚠️ Multiple threads execute this method **simultaneously**
- ⚠️ Requires thread-safe collections
- ⚠️ Lock contention under high load

---

### Variant 2 (Actor Model): ChatController.java

```java
@Controller
public class ChatController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private ActorSystem<ChatActor.Command> actorSystem;

    @MessageMapping("/chat.send")
    public void handleChatMessage(Message message,
                                     SimpMessageHeaderAccessor headerAccessor) {
        
        String threadName = Thread.currentThread().getName();
        System.out.println("Thread: " + threadName);
        System.out.println("Forwarding to ChatActor...");

        // ✅ NO PROCESSING HERE - Just forward to actor
        // The actor will process messages sequentially
        actorSystem.tell(new ChatActor.HandleMessage(message));
    }
}
```

**Characteristics:**
- ✅ Controller is stateless - just forwards messages
- ✅ No thread-safety concerns here
- ✅ Asynchronous processing
- ✅ Natural backpressure (actor mailbox queues messages)

---

## 2. State Management

### Variant 1: SessionManager.java

```java
@Service
public class SessionManager {
    
    // ⚠️ MUST BE THREAD-SAFE - Accessed by multiple threads
    private final ConcurrentHashMap<String, ChatUser> activeUsers 
        = new ConcurrentHashMap<>();
    
    // ⚠️ EXPENSIVE: Creates full copy on every write!
    private final CopyOnWriteArrayList<Message> messageHistory 
        = new CopyOnWriteArrayList<>();
    
    public void addMessage(Message message) {
        // Multiple threads can call this simultaneously
        // CopyOnWriteArrayList handles synchronization by:
        // 1. Creating a FULL COPY of the array
        // 2. Adding the message to the copy
        // 3. Atomically replacing the old array
        // This is EXPENSIVE for writes!
        messageHistory.add(message);
    }
}
```

**Performance Impact:**
```
Scenario: 1000 messages/second

CopyOnWriteArrayList behavior:
- Message 1: Copy array of 0 elements → add → replace (fast)
- Message 2: Copy array of 1 element → add → replace (fast)
- Message 100: Copy array of 99 elements → add → replace (slow)
- Message 1000: Copy array of 999 elements → add → replace (VERY slow!)

Cost increases linearly with array size!
```

---

### Variant 2: ChatActor.java

```java
public class ChatActor extends AbstractBehavior<ChatActor.Command> {
    
    // ✅ PLAIN ArrayList - NO thread-safety needed!
    // Only the actor accesses this - no other threads
    private final List<Message> messageHistory = new ArrayList<>();
    
    private Behavior<Command> onHandleMessage(HandleMessage command) {
        Message message = command.message;
        
        // ✅ Process sequentially - one message at a time
        // No locks, no synchronization, no race conditions!
        message.setTimestamp(LocalDateTime.now());
        messageHistory.add(message);  // Just add - no copying!
        
        messagingTemplate.convertAndSend("/topic/messages", message);
        return this;
    }
}
```

**Performance Impact:**
```
Scenario: 1000 messages/second

ArrayList behavior:
- Message 1: Just add to array (O(1))
- Message 2: Just add to array (O(1))
- Message 100: Just add to array (O(1))
- Message 1000: Just add to array (O(1))

Cost is constant - much faster!
```

---

## 3. Concurrency Flow Visualization

### Variant 1 (Thread-Based)

```
Time: T+0ms
┌─────────────────────────────────────────────┐
│ Client A sends "Hello"                      │
│ → Tomcat assigns Thread-5                  │
│   Thread-5: handleChatMessage()            │
│   Thread-5: sessionManager.addMessage()    │
│   Thread-5: LOCKS CopyOnWriteArrayList     │ ← CRITICAL SECTION
│   Thread-5: Copy array, add message        │
│   Thread-5: UNLOCKS                        │
└─────────────────────────────────────────────┘

Time: T+1ms (While Thread-5 still working)
┌─────────────────────────────────────────────┐
│ Client B sends "Hi"                         │
│ → Tomcat assigns Thread-12                 │
│   Thread-12: handleChatMessage()           │
│   Thread-12: sessionManager.addMessage()   │
│   Thread-12: WAITS for Thread-5 to finish  │ ← LOCK CONTENTION!
│   Thread-12: (blocked...)                  │
└─────────────────────────────────────────────┘

Time: T+2ms (While Thread-5 and Thread-12 working)
┌─────────────────────────────────────────────┐
│ Client C sends "Hey"                        │
│ → Tomcat assigns Thread-18                 │
│   Thread-18: handleChatMessage()           │
│   Thread-18: sessionManager.addMessage()   │
│   Thread-18: WAITS for Thread-12 to finish │ ← MORE CONTENTION!
│   Thread-18: (blocked...)                  │
└─────────────────────────────────────────────┘

Result: Messages processed sequentially due to lock contention!
Throughput: Limited by lock overhead
```

---

### Variant 2 (Actor Model)

```
Time: T+0ms
┌─────────────────────────────────────────────┐
│ Client A sends "Hello"                      │
│ → Tomcat assigns Thread-5                  │
│   Thread-5: handleChatMessage()            │
│   Thread-5: actorSystem.tell(message)      │ ← NO LOCK - Just enqueue
│   Thread-5: DONE (returns immediately)     │
└─────────────────────────────────────────────┘

Time: T+1ms (Thread-5 already finished)
┌─────────────────────────────────────────────┐
│ Client B sends "Hi"                         │
│ → Tomcat assigns Thread-12                 │
│   Thread-12: handleChatMessage()           │
│   Thread-12: actorSystem.tell(message)     │ ← NO LOCK - Just enqueue
│   Thread-12: DONE (returns immediately)    │
└─────────────────────────────────────────────┘

Time: T+2ms (Both threads finished)
┌─────────────────────────────────────────────┐
│ Client C sends "Hey"                        │
│ → Tomcat assigns Thread-18                 │
│   Thread-18: handleChatMessage()           │
│   Thread-18: actorSystem.tell(message)     │ ← NO LOCK - Just enqueue
│   Thread-18: DONE (returns immediately)    │
└─────────────────────────────────────────────┘

Meanwhile, in Actor System:
┌─────────────────────────────────────────────┐
│ ChatActor Mailbox: ["Hello", "Hi", "Hey"]  │
│ ChatActor Thread: Processing sequentially  │
│   Process "Hello" → broadcast              │
│   Process "Hi" → broadcast                 │
│   Process "Hey" → broadcast                │
└─────────────────────────────────────────────┘

Result: Messages processed efficiently in pipeline!
Throughput: High - no lock contention in controller
```

---

## 4. Thread Allocation

### Variant 1

```
Tomcat Thread Pool (200 threads)
[Thread-1] → Blocked waiting for lock
[Thread-2] → Processing request
[Thread-3] → Blocked waiting for lock
[Thread-4] → Blocked waiting for lock
[Thread-5] → Processing request
...
[Thread-200] → Blocked waiting for lock

Under high load:
- Most threads BLOCKED waiting for locks
- Only 1-2 threads actually doing work
- Poor CPU utilization
- Thread pool exhaustion
```

### Variant 2

```
Tomcat Thread Pool (200 threads)
[Thread-1] → Quickly forwards to actor, FREE
[Thread-2] → Quickly forwards to actor, FREE
[Thread-3] → Quickly forwards to actor, FREE
[Thread-4] → Quickly forwards to actor, FREE
[Thread-5] → Quickly forwards to actor, FREE
...
[Thread-200] → Available

Actor Thread Pool (configurable)
[Actor-Thread-1] → Processing messages from mailbox
[Actor-Thread-2] → Processing messages from mailbox
[Actor-Thread-3] → Processing messages from mailbox
...

Under high load:
- Tomcat threads quickly return (no blocking)
- Actor threads efficiently process messages
- Excellent CPU utilization
- No thread pool exhaustion
```

---

## 5. Scalability Patterns

### Variant 1: Vertical Scaling Only

```
Single Server:
  200 threads × 1 request/thread = 200 concurrent requests MAX
  
  Want 1000 users? 
  → Need 5 servers with load balancer
  → Shared session state required (Redis/etc)
  → Complex deployment
```

### Variant 2: Vertical + Horizontal Scaling

```
Single Server:
  Actors can handle 1000+ concurrent users
  (Not limited by thread pool)
  
  Want 10,000 users?
  → Akka Cluster: Distribute actors across nodes
  → No external session store needed
  → Simple deployment
```

---

## 6. Error Handling

### Variant 1

```java
@MessageMapping("/chat.send")
public Message handleChatMessage(Message message) {
    try {
        sessionManager.addMessage(message);
        return message;
    } catch (Exception e) {
        // What happens to the message?
        // Who gets notified?
        // How to retry?
        log.error("Failed to process message", e);
        return null; // Lost message!
    }
}
```

**Issues:**
- ❌ No built-in retry mechanism
- ❌ Lost messages on failure
- ❌ Hard to implement fault tolerance

---

### Variant 2

```java
public class ChatActor extends AbstractBehavior<ChatActor.Command> {
    
    // Supervisor strategy (built-in fault tolerance)
    public static Behavior<Command> create(SimpMessagingTemplate template) {
        return Behaviors.supervise(
            Behaviors.setup(ctx -> new ChatActor(ctx, template))
        )
        .onFailure(SupervisorStrategy.restart());  // Auto-restart on failure
    }
    
    private Behavior<Command> onHandleMessage(HandleMessage command) {
        try {
            // Process message
            messageHistory.add(command.message);
            messagingTemplate.convertAndSend("/topic/messages", command.message);
            return this;
        } catch (Exception e) {
            // Actor will be restarted by supervisor
            // Message is not lost (can be replayed)
            throw e;
        }
    }
}
```

**Advantages:**
- ✅ Built-in supervision
- ✅ Automatic restart on failure
- ✅ Message replay capability
- ✅ Fault isolation

---

## 7. Testing Complexity

### Variant 1: Testing Race Conditions

```java
@Test
public void testConcurrentMessageHandling() throws Exception {
    // Very hard to test race conditions reliably
    
    ExecutorService executor = Executors.newFixedThreadPool(100);
    CountDownLatch latch = new CountDownLatch(100);
    
    // Spawn 100 threads
    for (int i = 0; i < 100; i++) {
        final int id = i;
        executor.submit(() -> {
            try {
                Message msg = new Message("Test " + id, "user" + id, Message.MessageType.CHAT);
                controller.handleChatMessage(msg, null);
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await(10, TimeUnit.SECONDS);
    
    // How do we verify correctness?
    // Race conditions are non-deterministic!
    // Test might pass even with bugs!
}
```

---

### Variant 2: Testing with Actor TestKit

```java
@Test
public void testMessageHandling() {
    // Much easier - deterministic behavior
    
    TestProbe<Message> probe = testKit.createTestProbe();
    ActorRef<ChatActor.Command> actor = testKit.spawn(ChatActor.create(template));
    
    // Send messages
    actor.tell(new ChatActor.HandleMessage(msg1));
    actor.tell(new ChatActor.HandleMessage(msg2));
    actor.tell(new ChatActor.HandleMessage(msg3));
    
    // Messages are processed in order, always
    // No race conditions, deterministic testing
    verify(template, times(3)).convertAndSend(anyString(), any());
}
```

---

## 8. Memory Footprint Comparison

### Variant 1: CopyOnWriteArrayList

```
Initial: 1 message  = 1 KB
After 100 messages:
  Array: 100 KB
  
Add message #101:
  1. Allocate new array: 101 KB
  2. Copy 100 messages: 100 KB → 101 KB
  3. Add new message
  4. Replace old array
  5. GC collects old 100 KB array
  
Temporary peak: 100 KB (old) + 101 KB (new) = 201 KB
GC pressure: HIGH
```

### Variant 2: Regular ArrayList

```
Initial: 1 message  = 1 KB
After 100 messages:
  Array: 100 KB
  
Add message #101:
  1. Add to array: 101 KB
  
Temporary peak: 101 KB
GC pressure: LOW
```

---

## 9. Key Differences Summary

| Aspect | Thread-Based (V1) | Actor Model (V2) |
|--------|-------------------|------------------|
| **State** | Shared (needs locks) | Isolated (no locks) |
| **Collections** | ConcurrentHashMap, CopyOnWriteArrayList | Plain ArrayList, HashMap |
| **Lock Contention** | High under load | None |
| **Thread Efficiency** | Poor (blocking) | Excellent (non-blocking) |
| **Memory Copies** | Many (CopyOnWrite) | Few (direct add) |
| **GC Pressure** | High | Low |
| **Fault Tolerance** | Manual | Built-in (supervision) |
| **Testing** | Non-deterministic | Deterministic |
| **Scalability** | Thread-limited | Actor-limited (much higher) |
| **Learning Curve** | Easy | Medium |

---

## 10. When Each Approach Makes Sense

### Use Thread-Based (Variant 1) When:
- ✅ Team is new to concurrency
- ✅ < 100 concurrent users expected
- ✅ Simple CRUD operations
- ✅ Short-term project
- ✅ Simplicity > performance

### Use Actor Model (Variant 2) When:
- ✅ > 200 concurrent users expected
- ✅ High throughput required
- ✅ Long-lived connections (WebSockets)
- ✅ Need fault tolerance
- ✅ Future horizontal scaling planned
- ✅ Real-time messaging critical

---

## Conclusion

The actor model provides:
1. **Better performance** under high load (no lock contention)
2. **Better scalability** (not thread-limited)
3. **Better maintainability** (no race conditions)
4. **Better fault tolerance** (built-in supervision)

At the cost of:
1. **Steeper learning curve** (need to understand actors)
2. **Slightly more complex** (message passing vs direct calls)

For a chat application with potential for growth, the actor model is the better choice.
