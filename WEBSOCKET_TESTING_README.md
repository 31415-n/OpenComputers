# WebSocket Testing Suite for OpenComputers

This comprehensive testing suite verifies the RFC 6455 compliant WebSocket implementation in OpenComputers. It includes both automated Python tests and manual Lua tests to ensure the WebSocket functionality works correctly in all scenarios.

## 🎯 Test Coverage

### Core WebSocket Features
- ✅ **Connection Establishment** - WebSocket handshake according to RFC 6455
- ✅ **Message Transmission** - Text and binary message sending/receiving
- ✅ **Frame Processing** - Proper frame parsing and generation
- ✅ **Fragmentation** - Large message fragmentation support
- ✅ **Control Frames** - Ping/Pong and Close frame handling
- ✅ **UTF-8 Validation** - Proper text frame encoding validation
- ✅ **Close Handshake** - Graceful connection termination
- ✅ **Error Handling** - Timeout and error condition management

### Performance & Reliability
- ✅ **Non-blocking I/O** - Ensures game thread never blocks
- ✅ **Multiple Connections** - Concurrent connection handling
- ✅ **Large Messages** - 10KB+ message processing
- ✅ **Slow Servers** - Handling of slow server responses
- ✅ **Connection Limits** - Proper resource management
- ✅ **Memory Management** - No memory leaks or resource exhaustion

### Security & Compliance
- ✅ **RFC 6455 Compliance** - Full WebSocket standard compliance
- ✅ **Client-Only** - No server functionality (security requirement)
- ✅ **SSL/TLS Support** - Secure WebSocket (wss://) connections
- ✅ **Input Validation** - Proper frame and message validation
- ✅ **DoS Protection** - Limits on fragmentation and message size

## 📁 Test Files

### Lua Tests (OpenComputers)
- **`websocket_comprehensive_test.lua`** - Complete Lua test suite for in-game testing
  - Event-driven testing using computer signals
  - Comprehensive coverage of all WebSocket features
  - Non-blocking test execution
  - Detailed result reporting

### Python Tests (External)
- **`websocket_test_server.py`** - RFC 6455 compliant test server
  - Multiple test endpoints for different scenarios
  - Detailed logging and statistics
  - Configurable host/port binding
  - Graceful shutdown handling

- **`websocket_automated_test.py`** - Automated test client
  - Comprehensive test suite execution
  - Performance benchmarking
  - JSON result output
  - Error condition testing

### Automation
- **`run_websocket_tests.sh`** - Complete test suite runner
  - Automated server startup/shutdown
  - Python test execution
  - Result compilation
  - Lua test instruction generation

## 🚀 Quick Start

### 1. Run Automated Tests
```bash
# Install dependencies
pip3 install websockets

# Run complete test suite
./run_websocket_tests.sh
```

### 2. Manual OpenComputers Testing
```lua
-- In OpenComputers computer
dofile("websocket_comprehensive_test.lua")
```

### 3. Custom Server Testing
```bash
# Start test server manually
python3 websocket_test_server.py --host 0.0.0.0 --port 8080

# Run specific tests
python3 websocket_automated_test.py --server-host localhost --server-port 8080
```

## 📋 Detailed Testing Instructions

### Prerequisites
1. **Python 3.7+** with `websockets` package
2. **OpenComputers** with internet card and WebSocket support enabled
3. **Network connectivity** between test server and OpenComputers

### Python Test Server Setup
```bash
# Basic server startup
python3 websocket_test_server.py

# Custom configuration
python3 websocket_test_server.py --host 20.20.20.100 --port 8080 --verbose
```

**Available Test Endpoints:**
- `/echo` - Basic message echo
- `/binary` - Binary message testing
- `/fragment` - Fragmentation testing
- `/ping` - Ping/Pong frame testing
- `/slow` - Slow response simulation
- `/multi/<id>` - Multiple connection testing
- `/stats` - Server statistics

### OpenComputers Lua Testing

#### Automatic Test Execution
```lua
-- Load and run all tests
dofile("websocket_comprehensive_test.lua")
```

#### Manual Test Execution
```lua
local component = require("component")
local internet = component.internet

-- Check WebSocket availability
if not internet.isWebSocketEnabled() then
    print("WebSocket support is disabled!")
    return
end

-- Basic connection test
local ws = internet.websocket("ws://20.20.20.100:8080/echo")

-- Wait for connection event
local event = require("event")
local _, handle = event.pull("websocket_connect")

-- Send test message
ws.send("Hello WebSocket!")

-- Wait for response
local _, _, message = event.pull("websocket_message")
print("Received:", message)

-- Close connection
ws.close()
```

#### Event-Driven Testing Pattern
```lua
local event = require("event")
local computer = require("computer")

local function waitForEvent(eventType, timeout, filter)
    local deadline = computer.uptime() + timeout
    
    while computer.uptime() < deadline do
        local eventData = {computer.pullSignal(0.1)}
        if eventData[1] == eventType then
            if not filter or filter(eventData) then
                return eventData
            end
        end
    end
    return nil -- timeout
end

-- Example usage
local connectEvent = waitForEvent("websocket_connect", 10)
if connectEvent then
    print("Connected successfully!")
else
    print("Connection timeout!")
end
```

## 🔍 Test Scenarios

### 1. Basic Functionality Tests
- Connection establishment and termination
- Text message echo
- Binary message echo
- Large message handling (10KB)
- UTF-8 character support

### 2. Protocol Compliance Tests
- WebSocket handshake validation
- Frame masking verification
- Close code validation
- Ping/Pong frame handling
- Fragmentation support

### 3. Error Condition Tests
- Connection timeout handling
- Invalid URL rejection
- Server unavailability
- Network interruption recovery
- Malformed frame handling

### 4. Performance Tests
- Multiple concurrent connections
- High-frequency message sending
- Large payload transmission
- Slow server response handling
- Memory usage monitoring

### 5. Integration Tests
- OpenComputers event system integration
- Component lifecycle management
- Settings and configuration respect
- Resource cleanup verification

## 📊 Expected Results

### Python Tests
- **All tests should pass** (100% success rate)
- **No connection failures** for valid endpoints
- **Proper timeout handling** for invalid endpoints
- **Correct message echoing** for all message types
- **Performance within acceptable limits**

### Lua Tests
- **No game thread blocking** during any operation
- **Proper event generation** for all WebSocket operations
- **Correct message handling** in OpenComputers environment
- **Graceful error handling** for all failure scenarios
- **Resource cleanup** after test completion

## 🐛 Troubleshooting

### Common Issues

#### "WebSocket not enabled"
```lua
-- Check OpenComputers config
-- Set: internet { enableWebSocket = true }
```

#### "Connection refused"
```bash
# Check if server is running
netstat -tlnp | grep 8080

# Check firewall settings
sudo ufw allow 8080
```

#### "localhost not accessible"
```lua
-- Use IP address instead of localhost in OpenComputers
local ws = internet.websocket("ws://20.20.20.100:8080/echo")
```

#### "Tests hanging"
```lua
-- Check for infinite loops in event handling
-- Use timeout parameters in all event.pull calls
```

### Debug Mode
```bash
# Enable verbose logging
python3 websocket_test_server.py --verbose
python3 websocket_automated_test.py --verbose
```

```lua
-- Enable debug output in Lua
local DEBUG = true
local function debug(msg)
    if DEBUG then print("[DEBUG]", msg) end
end
```

## 📈 Performance Benchmarks

### Expected Performance Metrics
- **Connection Time:** < 2 seconds
- **Message Latency:** < 100ms for small messages
- **Throughput:** > 1MB/s for large messages
- **Concurrent Connections:** 10+ simultaneous connections
- **Memory Usage:** < 10MB per connection

### Monitoring Commands
```bash
# Monitor server performance
top -p $(pgrep -f websocket_test_server)

# Check network connections
ss -tuln | grep 8080

# Monitor memory usage
ps aux | grep python3
```

## 🔒 Security Considerations

### Client-Only Implementation
- ✅ No server functionality exposed
- ✅ Only outbound connections allowed
- ✅ Proper SSL/TLS certificate validation
- ✅ Input validation on all received data

### DoS Protection
- ✅ Connection count limits
- ✅ Message size limits
- ✅ Fragmentation limits
- ✅ Timeout enforcement

### Data Validation
- ✅ UTF-8 validation for text frames
- ✅ Frame format validation
- ✅ Close code validation
- ✅ URL format validation

## 📝 Test Result Analysis

### Success Criteria
1. **100% Python test pass rate**
2. **All Lua tests complete without errors**
3. **No game thread blocking observed**
4. **Proper event generation in all scenarios**
5. **Graceful handling of all error conditions**

### Failure Investigation
1. Check server logs for connection issues
2. Verify OpenComputers configuration
3. Test network connectivity
4. Review error messages and stack traces
5. Validate test environment setup

---

**Note:** This testing suite ensures the WebSocket implementation meets all requirements for production use in OpenComputers environments, including multiplayer servers with multiple concurrent users.
