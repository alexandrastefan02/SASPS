# Benchmark Testing Guide

## How to Measure Performance Between Variants

This guide explains how to conduct performance benchmarks to compare Variant 1 (Thread-Based) and Variant 2 (Actor Model).

---

## 1. Prerequisites

### Required Tools
```bash
# Install JMeter for load testing
brew install jmeter  # macOS
# or download from: https://jmeter.apache.org/

# Install monitoring tools
brew install htop    # CPU monitoring
```

### JVM Monitoring Setup
```bash
# Enable JMX monitoring in both variants
# Add to run-variant.sh before java command:
JMX_OPTS="-Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9010 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false"

java $JMX_OPTS $jvmArgs -jar "$jarPath"
```

---

## 2. Manual Testing (Simple Approach)

### Test 1: Single Message Latency

**Start Server:**
```bash
./run-variant.sh variant1  # Test variant 1
# OR
./run-variant.sh variant2  # Test variant 2
```

**Test Script (JavaScript):**
```javascript
// Save as: latency-test.html
// Open in browser console

const startTime = Date.now();
stompClient.send("/app/chat.send", {}, JSON.stringify({
    sender: currentUser,
    content: "Test message",
    type: "CHAT"
}));

// In the subscription callback:
stompClient.subscribe('/topic/messages', function (message) {
    const endTime = Date.now();
    const latency = endTime - startTime;
    console.log('Latency:', latency, 'ms');
});
```

**Expected Results:**
- Variant 1: 2-5ms
- Variant 2: 3-6ms

### Test 2: Concurrent Users

**Open Multiple Browser Tabs:**
```bash
# Open 10 tabs, each user sends 10 messages/second
# Monitor server logs for:
# - Thread names
# - Processing time
# - Queue depth (actor variant)
```

**Metrics to Collect:**
- Average latency
- CPU usage (Activity Monitor)
- Memory usage
- Error rate

---

## 3. JMeter Load Testing (Advanced)

### 3.1 JMeter WebSocket Plugin

```bash
# Install WebSocket plugin
# Download from: https://github.com/Blazemeter/jmeter-websocket-samplers
# Place in: $JMETER_HOME/lib/ext/
```

### 3.2 Test Plan Configuration

**Create Test Plan: `chat-load-test.jmx`**

```xml
Thread Group Settings:
- Number of Threads: 50, 100, 200, 500, 1000
- Ramp-up Period: 60 seconds
- Loop Count: 10

WebSocket Sampler:
- Server: localhost
- Port: 8080
- Path: /ws
- Protocol: ws

STOMP Frame:
CONNECT
username:user${__threadNum}


SEND
destination:/app/chat.send
content-length:100

{"sender":"user${__threadNum}","content":"Load test message","type":"CHAT"}
```

### 3.3 Run Tests

```bash
# Variant 1
./run-variant.sh variant1 &
sleep 10  # Wait for startup

jmeter -n -t chat-load-test.jmx -l variant1-results.jtl \
  -Jthreads=100 -Jrampup=60 -Jloops=10

# Stop server
pkill -f demo-0.0.1-SNAPSHOT.war

# Variant 2
./run-variant.sh variant2 &
sleep 10

jmeter -n -t chat-load-test.jmx -l variant2-results.jtl \
  -Jthreads=100 -Jrampup=60 -Jloops=10
```

### 3.4 Analyze Results

```bash
# Generate HTML report
jmeter -g variant1-results.jtl -o variant1-report/
jmeter -g variant2-results.jtl -o variant2-report/

# Open reports
open variant1-report/index.html
open variant2-report/index.html
```

**Compare:**
- Response Time (avg, median, 95th percentile)
- Throughput (requests/second)
- Error Rate
- Active Threads over Time

---

## 4. Memory Profiling

### Using VisualVM

```bash
# Start VisualVM
jvisualvm

# Connect to running application (JMX port 9010)
# Monitor:
# - Heap usage
# - Thread count
# - CPU usage
# - Garbage collection
```

### Using JConsole

```bash
jconsole localhost:9010

# Observe:
# - Memory tab: Heap usage patterns
# - Threads tab: Thread count and states
# - VM Summary: Overall statistics
```

---

## 5. Custom Performance Test

### Create Custom Test Client

**File: `load-test-client.js`**

```javascript
const WebSocket = require('ws');
const Stomp = require('stompjs');

class LoadTestClient {
    constructor(userId, serverUrl) {
        this.userId = userId;
        this.serverUrl = serverUrl;
        this.messagesSent = 0;
        this.messagesReceived = 0;
        this.latencies = [];
    }

    connect() {
        const socket = new WebSocket(this.serverUrl);
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, () => {
            console.log(`User ${this.userId} connected`);
            
            this.stompClient.subscribe('/topic/messages', (message) => {
                const receivedTime = Date.now();
                const msg = JSON.parse(message.body);
                
                if (msg.sender === `user${this.userId}`) {
                    const latency = receivedTime - this.sendTimes.get(msg.content);
                    this.latencies.push(latency);
                    this.messagesReceived++;
                }
            });
            
            this.startSending();
        });
    }

    startSending() {
        this.sendTimes = new Map();
        
        setInterval(() => {
            const content = `Message ${this.messagesSent}`;
            const sendTime = Date.now();
            this.sendTimes.set(content, sendTime);
            
            this.stompClient.send('/app/chat.send', {}, JSON.stringify({
                sender: `user${this.userId}`,
                content: content,
                type: 'CHAT'
            }));
            
            this.messagesSent++;
        }, 100); // 10 messages per second
    }

    getStats() {
        return {
            sent: this.messagesSent,
            received: this.messagesReceived,
            avgLatency: this.latencies.reduce((a, b) => a + b, 0) / this.latencies.length,
            p95Latency: this.percentile(this.latencies, 95),
            p99Latency: this.percentile(this.latencies, 99)
        };
    }

    percentile(arr, p) {
        const sorted = arr.sort((a, b) => a - b);
        const index = Math.ceil(sorted.length * (p / 100)) - 1;
        return sorted[index];
    }
}

// Run test
const clients = [];
const NUM_CLIENTS = 100;

for (let i = 0; i < NUM_CLIENTS; i++) {
    const client = new LoadTestClient(i, 'ws://localhost:8080/ws');
    client.connect();
    clients.push(client);
}

// Collect stats after 60 seconds
setTimeout(() => {
    console.log('\n===== TEST RESULTS =====');
    
    const allStats = clients.map(c => c.getStats());
    
    const totalSent = allStats.reduce((sum, s) => sum + s.sent, 0);
    const totalReceived = allStats.reduce((sum, s) => sum + s.received, 0);
    const avgLatency = allStats.reduce((sum, s) => sum + s.avgLatency, 0) / allStats.length;
    const avgP95 = allStats.reduce((sum, s) => sum + s.p95Latency, 0) / allStats.length;
    
    console.log(`Total Messages Sent: ${totalSent}`);
    console.log(`Total Messages Received: ${totalReceived}`);
    console.log(`Success Rate: ${(totalReceived/totalSent*100).toFixed(2)}%`);
    console.log(`Average Latency: ${avgLatency.toFixed(2)}ms`);
    console.log(`P95 Latency: ${avgP95.toFixed(2)}ms`);
    
    process.exit(0);
}, 60000);
```

**Run:**
```bash
npm install ws stompjs
node load-test-client.js
```

---

## 6. Metrics to Collect

### Performance Metrics

| Metric | How to Measure | Target |
|--------|----------------|--------|
| **Latency (avg)** | End-to-end message time | < 50ms |
| **Latency (P95)** | 95th percentile | < 100ms |
| **Latency (P99)** | 99th percentile | < 200ms |
| **Throughput** | Messages/second | > 10,000 |
| **CPU Usage** | htop or Activity Monitor | < 80% |
| **Memory Usage** | JConsole | < 2GB |
| **Error Rate** | Failed requests % | < 1% |

### Scalability Metrics

| User Count | Target Throughput | Max Latency |
|------------|-------------------|-------------|
| 10 | 100 msg/s | 20ms |
| 50 | 500 msg/s | 30ms |
| 100 | 1,000 msg/s | 50ms |
| 500 | 5,000 msg/s | 100ms |
| 1000 | 10,000 msg/s | 200ms |

---

## 7. Test Scenarios

### Scenario 1: Burst Load
```
Simulate 100 users sending messages simultaneously
Measure: Peak latency, system recovery time
```

### Scenario 2: Sustained Load
```
50 users continuously sending for 10 minutes
Measure: Throughput stability, memory leaks
```

### Scenario 3: Gradual Ramp-Up
```
0 → 500 users over 5 minutes
Measure: Degradation point, scalability limit
```

### Scenario 4: Spike Test
```
Normal load (50 users) → Spike to 500 → Back to 50
Measure: System resilience, recovery time
```

---

## 8. Results Template

### Benchmark Report Format

```markdown
## Test Date: [DATE]
## Variant: [1 or 2]
## Hardware: [SPECS]

### Test Configuration
- Concurrent Users: [NUMBER]
- Messages per User: [NUMBER]
- Duration: [SECONDS]
- Ramp-up: [SECONDS]

### Results
| Metric | Value |
|--------|-------|
| Total Messages | 10,000 |
| Average Latency | 25ms |
| P95 Latency | 50ms |
| P99 Latency | 100ms |
| Throughput | 5,000 msg/s |
| CPU Usage | 45% |
| Memory Usage | 512 MB |
| Error Rate | 0.1% |

### Observations
- [Key findings]
- [Bottlenecks identified]
- [Recommendations]
```

---

## 9. Automated Comparison Script

**File: `compare-variants.sh`**

```bash
#!/bin/bash

echo "=== Performance Comparison ==="
echo ""

# Test configurations
USERS=(10 50 100 500)
DURATION=60

for user_count in "${USERS[@]}"; do
    echo "Testing with $user_count users..."
    
    # Test Variant 1
    echo "  Starting Variant 1..."
    ./run-variant.sh variant1 &
    SERVER_PID=$!
    sleep 10
    
    jmeter -n -t load-test.jmx -l "v1-${user_count}.jtl" \
        -Jthreads=$user_count -Jduration=$DURATION
    
    kill $SERVER_PID
    sleep 5
    
    # Test Variant 2
    echo "  Starting Variant 2..."
    ./run-variant.sh variant2 &
    SERVER_PID=$!
    sleep 10
    
    jmeter -n -t load-test.jmx -l "v2-${user_count}.jtl" \
        -Jthreads=$user_count -Jduration=$DURATION
    
    kill $SERVER_PID
    sleep 5
done

echo ""
echo "Tests complete. Generating reports..."

# Generate comparison report
python3 compare-results.py
```

---

## 10. Tips for Accurate Benchmarking

1. **Warm-up Period**: Run for 30s before measuring
2. **Consistent Environment**: Close other applications
3. **Multiple Runs**: Run each test 3-5 times, take average
4. **Monitor Resources**: Watch CPU, memory, network
5. **JVM Tuning**: Use same JVM settings for both variants
6. **Realistic Data**: Use realistic message sizes
7. **Network Simulation**: Test with network latency if needed

---

## Conclusion

Use this guide to conduct systematic performance testing. The key is to:
- Test multiple load levels
- Measure consistently
- Document everything
- Compare apples to apples

Expected outcome: Actor model (Variant 2) should show better performance under high load (>100 concurrent users).
