# Internet Card

![Cat videos in 3, 2, ...](oredict:oc:internetCard)

The internet card grants [computers](../general/computer.md) access to the internet. It provides ways to perform simple HTTP requests, open plain TCP client sockets, and establish WebSocket connections for real-time communication.

## Features

- **HTTP Requests**: Perform GET, POST, and other HTTP requests to web servers
- **TCP Sockets**: Open raw TCP connections for custom protocols
- **WebSocket Support**: Establish WebSocket connections (ws:// and wss://) for real-time bidirectional communication with full SSL/TLS encryption support
- **Error Handling**: Proper handling of HTTP error responses and connection failures

## WebSocket Usage

WebSockets provide a persistent, full-duplex communication channel between the computer and a WebSocket server. This implementation fully complies with RFC 6455 WebSocket Protocol standard.

## Features

- **Full RFC 6455 Compliance**: Complete implementation of the WebSocket protocol
- **Fragmentation Support**: Automatic handling of large messages
- **Subprotocol Negotiation**: Support for WebSocket subprotocols
- **Extensions Support**: Built-in support for WebSocket extensions
- **UTF-8 Validation**: Proper validation of text frames
- **Binary Data**: Full support for binary WebSocket frames
- **Security Features**: Origin validation, rate limiting, and CSRF protection

## Basic Usage

```lua
local internet = require("internet")

-- Connect to a WebSocket server (unencrypted)
local ws = internet.websocket("ws://echo.websocket.org/")

-- Or connect to a secure WebSocket server (encrypted)
local wss = internet.websocket("wss://echo.websocket.org/")

-- Send a message
ws.send("Hello, WebSocket!")

-- Receive messages
local message = ws.receive()
if message then
  print("Received:", message)
end
```

## Advanced Usage

```lua
-- Connect with custom headers and subprotocols
local headers = {["User-Agent"] = "OpenComputers/1.0"}
local protocols = {"chat", "echo"}
local ws = internet.websocket("ws://example.com/", headers, protocols)

-- Check negotiated protocol
local protocol = ws.getProtocol()
print("Using protocol:", protocol)

-- Check negotiated extensions
local extensions = ws.getExtensions()
for i, ext in pairs(extensions) do
  print("Extension:", ext)
end

-- Send large messages with automatic fragmentation
local large_data = string.rep("data", 10000)
ws.send(large_data)

-- Send with explicit fragmentation
ws.sendFragmented("Large message", 1024) -- 1KB fragments

-- Send binary data
local binary = {0x48, 0x65, 0x6C, 0x6C, 0x6F} -- "Hello"
ws.sendBinary(binary)

-- Receive binary data
local binary_data = ws.receiveBinary()
```

-- Send binary data
ws.sendBinary("Binary data here")

-- Receive binary data
local binaryData = ws.receiveBinary()

-- Close the connection
ws.close()
```

Installing an internet card in a [computers](../general/computer.md) will also attach a custom file system that contains a few internet related applications, such as one for downloading and uploading snippets from/to pastebin as well as a `wget` clone that allows downloading data from arbitrary HTTP URLs.
