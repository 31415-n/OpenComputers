local component = require("component")
local computer = require("computer")
local event = require("event")
local internet = component.internet

-- Test configuration
local TEST_SERVER_HOST = "20.20.20.100"  -- localhost disabled in OC by default
local TEST_SERVER_PORT = 8080
local TIMEOUT = 30  -- seconds

-- Test results tracking
local tests = {}
local currentTest = nil
local testResults = {passed = 0, failed = 0, total = 0}

-- Utility functions
local function log(message)
    print("[" .. os.date("%H:%M:%S") .. "] " .. message)
end

local function startTest(name)
    currentTest = {name = name, startTime = computer.uptime()}
    log("Starting test: " .. name)
end

local function endTest(success, message)
    if not currentTest then return end
    
    local duration = computer.uptime() - currentTest.startTime
    local result = success and "PASS" or "FAIL"
    local msg = message or ""
    
    log(string.format("Test '%s' %s (%.2fs) %s", currentTest.name, result, duration, msg))
    
    table.insert(tests, {
        name = currentTest.name,
        success = success,
        duration = duration,
        message = msg
    })
    
    testResults.total = testResults.total + 1
    if success then
        testResults.passed = testResults.passed + 1
    else
        testResults.failed = testResults.failed + 1
    end
    
    currentTest = nil
end

local function waitForEvent(eventType, timeout, filter)
    local deadline = computer.uptime() + (timeout or TIMEOUT)
    
    while computer.uptime() < deadline do
        local eventData = {computer.pullSignal(0.1)}
        if eventData[1] then
            if eventData[1] == eventType then
                if not filter or filter(eventData) then
                    return eventData
                end
            elseif eventData[1] == "interrupted" then
                error("Test interrupted by user")
            end
        end
    end
    
    return nil -- timeout
end

-- WebSocket test functions
local function testWebSocketBasicConnection()
    startTest("WebSocket Basic Connection")
    
    if not internet.isWebSocketEnabled() then
        endTest(false, "WebSocket not enabled in settings")
        return
    end
    
    local url = string.format("ws://%s:%d/echo", TEST_SERVER_HOST, TEST_SERVER_PORT)
    local ws = internet.websocket(url)
    
    if not ws then
        endTest(false, "Failed to create WebSocket connection")
        return
    end
    
    -- Wait for connection event
    local connectEvent = waitForEvent("websocket_connect", 10)
    if not connectEvent then
        endTest(false, "Connection timeout")
        ws.close()
        return
    end
    
    if connectEvent[2] ~= ws then
        endTest(false, "Wrong connection handle in event")
        ws.close()
        return
    end
    
    ws.close()
    
    -- Wait for close event
    local closeEvent = waitForEvent("websocket_close", 5)
    if not closeEvent then
        endTest(false, "Close event timeout")
        return
    end
    
    endTest(true, "Connection established and closed successfully")
end

local function testWebSocketTextMessage()
    startTest("WebSocket Text Message")
    
    local url = string.format("ws://%s:%d/echo", TEST_SERVER_HOST, TEST_SERVER_PORT)
    local ws = internet.websocket(url)
    
    if not ws then
        endTest(false, "Failed to create WebSocket connection")
        return
    end
    
    -- Wait for connection
    local connectEvent = waitForEvent("websocket_connect", 10)
    if not connectEvent then
        endTest(false, "Connection timeout")
        ws.close()
        return
    end
    
    -- Send test message
    local testMessage = "Hello WebSocket!"
    ws.send(testMessage)
    
    -- Wait for message event
    local messageEvent = waitForEvent("websocket_message", 10, function(evt)
        return evt[2] == ws and evt[3] == testMessage
    end)
    
    if not messageEvent then
        endTest(false, "Message echo timeout or wrong content")
        ws.close()
        return
    end
    
    ws.close()
    endTest(true, "Text message sent and received successfully")
end

local function testWebSocketBinaryMessage()
    startTest("WebSocket Binary Message")
    
    local url = string.format("ws://%s:%d/binary", TEST_SERVER_HOST, TEST_SERVER_PORT)
    local ws = internet.websocket(url)
    
    if not ws then
        endTest(false, "Failed to create WebSocket connection")
        return
    end
    
    -- Wait for connection
    local connectEvent = waitForEvent("websocket_connect", 10)
    if not connectEvent then
        endTest(false, "Connection timeout")
        ws.close()
        return
    end
    
    -- Send binary data
    local binaryData = string.char(0x01, 0x02, 0x03, 0x04, 0x05, 0xFF, 0xFE)
    ws.send(binaryData, true) -- true for binary
    
    -- Wait for binary message event
    local messageEvent = waitForEvent("websocket_message", 10, function(evt)
        return evt[2] == ws and evt[3] == binaryData and evt[4] == true
    end)
    
    if not messageEvent then
        endTest(false, "Binary message echo timeout or wrong content")
        ws.close()
        return
    end
    
    ws.close()
    endTest(true, "Binary message sent and received successfully")
end

local function testWebSocketLargeMessage()
    startTest("WebSocket Large Message")
    
    local url = string.format("ws://%s:%d/echo", TEST_SERVER_HOST, TEST_SERVER_PORT)
    local ws = internet.websocket(url)
    
    if not ws then
        endTest(false, "Failed to create WebSocket connection")
        return
    end
    
    -- Wait for connection
    local connectEvent = waitForEvent("websocket_connect", 10)
    if not connectEvent then
        endTest(false, "Connection timeout")
        ws.close()
        return
    end
    
    -- Send large message (10KB)
    local largeMessage = string.rep("A", 10240)
    ws.send(largeMessage)
    
    -- Wait for large message (may take longer)
    local messageEvent = waitForEvent("websocket_message", 20, function(evt)
        return evt[2] == ws and evt[3] == largeMessage
    end)
    
    if not messageEvent then
        endTest(false, "Large message echo timeout or wrong content")
        ws.close()
        return
    end
    
    ws.close()
    endTest(true, "Large message (10KB) sent and received successfully")
end

local function testWebSocketFragmentation()
    startTest("WebSocket Fragmentation")
    
    local url = string.format("ws://%s:%d/fragment", TEST_SERVER_HOST, TEST_SERVER_PORT)
    local ws = internet.websocket(url)
    
    if not ws then
        endTest(false, "Failed to create WebSocket connection")
        return
    end
    
    -- Wait for connection
    local connectEvent = waitForEvent("websocket_connect", 10)
    if not connectEvent then
        endTest(false, "Connection timeout")
        ws.close()
        return
    end
    
    -- Send fragmentation test command
    ws.send("FRAGMENT_TEST")
    
    -- Server should send fragmented message
    local fragments = {}
    local fragmentCount = 0
    local deadline = computer.uptime() + 15
    
    while computer.uptime() < deadline do
        local messageEvent = waitForEvent("websocket_message", 2)
        if messageEvent and messageEvent[2] == ws then
            fragmentCount = fragmentCount + 1
            table.insert(fragments, messageEvent[3])
            
            -- Expect 3 fragments
            if fragmentCount >= 3 then
                break
            end
        end
    end
    
    if fragmentCount < 3 then
        endTest(false, string.format("Expected 3 fragments, got %d", fragmentCount))
        ws.close()
        return
    end
    
    -- Verify fragment content
    local fullMessage = table.concat(fragments)
    local expectedMessage = "Fragment1Fragment2Fragment3"
    
    if fullMessage ~= expectedMessage then
        endTest(false, "Fragmented message content mismatch")
        ws.close()
        return
    end
    
    ws.close()
    endTest(true, "Fragmentation handled correctly")
end

local function testWebSocketPingPong()
    startTest("WebSocket Ping/Pong")
    
    local url = string.format("ws://%s:%d/ping", TEST_SERVER_HOST, TEST_SERVER_PORT)
    local ws = internet.websocket(url)
    
    if not ws then
        endTest(false, "Failed to create WebSocket connection")
        return
    end
    
    -- Wait for connection
    local connectEvent = waitForEvent("websocket_connect", 10)
    if not connectEvent then
        endTest(false, "Connection timeout")
        ws.close()
        return
    end
    
    -- Send ping request
    ws.send("PING_TEST")
    
    -- Wait for ping event
    local pingEvent = waitForEvent("websocket_ping", 10)
    if not pingEvent or pingEvent[2] ~= ws then
        endTest(false, "Ping event timeout")
        ws.close()
        return
    end
    
    -- Pong should be sent automatically, wait for pong event
    local pongEvent = waitForEvent("websocket_pong", 5)
    if not pongEvent or pongEvent[2] ~= ws then
        endTest(false, "Pong event timeout")
        ws.close()
        return
    end
    
    ws.close()
    endTest(true, "Ping/Pong handled correctly")
end

local function testWebSocketCloseHandshake()
    startTest("WebSocket Close Handshake")
    
    local url = string.format("ws://%s:%d/echo", TEST_SERVER_HOST, TEST_SERVER_PORT)
    local ws = internet.websocket(url)
    
    if not ws then
        endTest(false, "Failed to create WebSocket connection")
        return
    end
    
    -- Wait for connection
    local connectEvent = waitForEvent("websocket_connect", 10)
    if not connectEvent then
        endTest(false, "Connection timeout")
        ws.close()
        return
    end
    
    -- Close with specific code and reason
    ws.close(1000, "Normal closure")
    
    -- Wait for close event with proper code
    local closeEvent = waitForEvent("websocket_close", 10, function(evt)
        return evt[2] == ws and evt[3] == 1000 and evt[4] == "Normal closure"
    end)
    
    if not closeEvent then
        endTest(false, "Close handshake failed or wrong parameters")
        return
    end
    
    endTest(true, "Close handshake completed correctly")
end

local function testWebSocketConnectionTimeout()
    startTest("WebSocket Connection Timeout")
    
    -- Try to connect to non-existent server
    local url = "ws://192.168.255.255:9999/timeout"
    local ws = internet.websocket(url)
    
    if not ws then
        endTest(true, "Connection correctly rejected immediately")
        return
    end
    
    -- Wait for timeout or error
    local errorEvent = waitForEvent("websocket_error", 15)
    if not errorEvent or errorEvent[2] ~= ws then
        endTest(false, "Expected timeout error event")
        if ws then ws.close() end
        return
    end
    
    endTest(true, "Connection timeout handled correctly")
end

local function testWebSocketInvalidURL()
    startTest("WebSocket Invalid URL")
    
    local invalidUrls = {
        "http://example.com",  -- Wrong protocol
        "ftp://example.com",   -- Wrong protocol
        "ws://",               -- Incomplete URL
        "invalid-url",         -- Malformed
        ""                     -- Empty
    }
    
    local rejectedCount = 0
    
    for _, url in ipairs(invalidUrls) do
        local success, ws = pcall(internet.websocket, url)
        if not success or not ws then
            rejectedCount = rejectedCount + 1
        else
            ws.close()
        end
    end
    
    if rejectedCount == #invalidUrls then
        endTest(true, "All invalid URLs correctly rejected")
    else
        endTest(false, string.format("Only %d/%d invalid URLs rejected", rejectedCount, #invalidUrls))
    end
end

local function testWebSocketSlowServer()
    startTest("WebSocket Slow Server Response")
    
    local url = string.format("ws://%s:%d/slow", TEST_SERVER_HOST, TEST_SERVER_PORT)
    local ws = internet.websocket(url)
    
    if not ws then
        endTest(false, "Failed to create WebSocket connection")
        return
    end
    
    -- Wait for connection (server responds slowly)
    local connectEvent = waitForEvent("websocket_connect", 20)
    if not connectEvent then
        endTest(false, "Slow connection timeout")
        ws.close()
        return
    end
    
    -- Send message to slow server
    ws.send("SLOW_MESSAGE")
    
    -- Wait for slow response
    local messageEvent = waitForEvent("websocket_message", 25)
    if not messageEvent or messageEvent[2] ~= ws then
        endTest(false, "Slow message response timeout")
        ws.close()
        return
    end
    
    ws.close()
    endTest(true, "Slow server handled without blocking game thread")
end

local function testWebSocketMultipleConnections()
    startTest("WebSocket Multiple Connections")

    local connections = {}
    local maxConnections = 5

    -- Create multiple connections
    for i = 1, maxConnections do
        local url = string.format("ws://%s:%d/multi/%d", TEST_SERVER_HOST, TEST_SERVER_PORT, i)
        local ws = internet.websocket(url)

        if ws then
            table.insert(connections, ws)
        end
    end

    if #connections == 0 then
        endTest(false, "No connections could be established")
        return
    end

    -- Wait for all connections
    local connectedCount = 0
    local deadline = computer.uptime() + 15

    while computer.uptime() < deadline and connectedCount < #connections do
        local connectEvent = waitForEvent("websocket_connect", 1)
        if connectEvent then
            for _, ws in ipairs(connections) do
                if connectEvent[2] == ws then
                    connectedCount = connectedCount + 1
                    break
                end
            end
        end
    end

    -- Test each connection
    local workingConnections = 0
    for i, ws in ipairs(connections) do
        ws.send("TEST_" .. i)

        local messageEvent = waitForEvent("websocket_message", 5, function(evt)
            return evt[2] == ws
        end)

        if messageEvent then
            workingConnections = workingConnections + 1
        end
    end

    -- Close all connections
    for _, ws in ipairs(connections) do
        ws.close()
    end

    if workingConnections >= math.min(3, #connections) then
        endTest(true, string.format("%d/%d connections working", workingConnections, #connections))
    else
        endTest(false, string.format("Only %d/%d connections working", workingConnections, #connections))
    end
end

local function testWebSocketUTF8Handling()
    startTest("WebSocket UTF-8 Handling")

    local url = string.format("ws://%s:%d/echo", TEST_SERVER_HOST, TEST_SERVER_PORT)
    local ws = internet.websocket(url)

    if not ws then
        endTest(false, "Failed to create WebSocket connection")
        return
    end

    -- Wait for connection
    local connectEvent = waitForEvent("websocket_connect", 10)
    if not connectEvent then
        endTest(false, "Connection timeout")
        ws.close()
        return
    end

    -- Test various UTF-8 strings
    local utf8Tests = {
        "Hello, World!",
        "Привет, мир!",
        "こんにちは世界",
        "🌍🚀💻🎉",
        "Mixed: Hello мир 世界 🎉"
    }

    local successCount = 0

    for _, testString in ipairs(utf8Tests) do
        ws.send(testString)

        local messageEvent = waitForEvent("websocket_message", 5, function(evt)
            return evt[2] == ws and evt[3] == testString
        end)

        if messageEvent then
            successCount = successCount + 1
        end
    end

    ws.close()

    if successCount == #utf8Tests then
        endTest(true, "All UTF-8 strings handled correctly")
    else
        endTest(false, string.format("Only %d/%d UTF-8 strings handled", successCount, #utf8Tests))
    end
end

-- Main test runner
local function runAllTests()
    log("=== WebSocket Comprehensive Test Suite ===")
    log("Testing WebSocket RFC 6455 compliance and OpenComputers integration")
    log("")

    -- Check if internet card is available
    if not internet then
        log("ERROR: Internet card not found!")
        return
    end

    -- Run all tests
    local testFunctions = {
        testWebSocketBasicConnection,
        testWebSocketTextMessage,
        testWebSocketBinaryMessage,
        testWebSocketLargeMessage,
        testWebSocketFragmentation,
        testWebSocketPingPong,
        testWebSocketCloseHandshake,
        testWebSocketConnectionTimeout,
        testWebSocketInvalidURL,
        testWebSocketSlowServer,
        testWebSocketMultipleConnections,
        testWebSocketUTF8Handling
    }

    local startTime = computer.uptime()

    for _, testFunc in ipairs(testFunctions) do
        local success, error = pcall(testFunc)
        if not success then
            if currentTest then
                endTest(false, "Test crashed: " .. tostring(error))
            else
                log("ERROR: Test function crashed: " .. tostring(error))
                testResults.total = testResults.total + 1
                testResults.failed = testResults.failed + 1
            end
        end

        -- Small delay between tests
        os.sleep(0.5)
    end

    local totalTime = computer.uptime() - startTime

    -- Print results
    log("")
    log("=== Test Results ===")
    log(string.format("Total tests: %d", testResults.total))
    log(string.format("Passed: %d", testResults.passed))
    log(string.format("Failed: %d", testResults.failed))
    log(string.format("Success rate: %.1f%%", (testResults.passed / testResults.total) * 100))
    log(string.format("Total time: %.2f seconds", totalTime))
    log("")

    -- Detailed results
    for _, test in ipairs(tests) do
        local status = test.success and "PASS" or "FAIL"
        log(string.format("  %s: %s (%.2fs) %s", status, test.name, test.duration, test.message))
    end

    log("")
    if testResults.failed == 0 then
        log("🎉 ALL TESTS PASSED! WebSocket implementation is working correctly.")
    else
        log("❌ Some tests failed. Check the results above.")
    end

    return testResults.failed == 0
end

-- Auto-start if run directly
if not pcall(debug.getlocal, 4, 1) then
    runAllTests()
end
