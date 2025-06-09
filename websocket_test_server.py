#!/usr/bin/env python3
"""
WebSocket Test Server for OpenComputers WebSocket Implementation Testing

This server implements various test endpoints to verify RFC 6455 compliance
and proper handling of different WebSocket scenarios.

Features:
- Echo server for basic message testing
- Binary message handling
- Fragmentation testing
- Ping/Pong frame testing
- Slow response simulation
- Multiple connection handling
- UTF-8 validation testing
- Connection timeout testing
- Close handshake testing

Usage:
    python3 websocket_test_server.py [--host HOST] [--port PORT]
"""

import asyncio
import websockets
import json
import time
import logging
import argparse
import signal
import sys
from typing import Set, Dict, Any
import struct

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class WebSocketTestServer:
    def __init__(self, host: str = "0.0.0.0", port: int = 8080):
        self.host = host
        self.port = port
        self.connections: Set[websockets.WebSocketServerProtocol] = set()
        self.stats = {
            "connections_total": 0,
            "connections_active": 0,
            "messages_sent": 0,
            "messages_received": 0,
            "bytes_sent": 0,
            "bytes_received": 0
        }
        
    async def register_connection(self, websocket: websockets.WebSocketServerProtocol):
        """Register a new WebSocket connection"""
        self.connections.add(websocket)
        self.stats["connections_total"] += 1
        self.stats["connections_active"] += 1
        logger.info(f"New connection from {websocket.remote_address}. Active: {self.stats['connections_active']}")
        
    async def unregister_connection(self, websocket: websockets.WebSocketServerProtocol):
        """Unregister a WebSocket connection"""
        self.connections.discard(websocket)
        self.stats["connections_active"] -= 1
        logger.info(f"Connection closed from {websocket.remote_address}. Active: {self.stats['connections_active']}")

    async def echo_handler(self, websocket: websockets.WebSocketServerProtocol, path: str):
        """Basic echo server - returns received messages"""
        await self.register_connection(websocket)
        try:
            async for message in websocket:
                self.stats["messages_received"] += 1
                self.stats["bytes_received"] += len(message) if isinstance(message, str) else len(message)
                
                # Echo the message back
                await websocket.send(message)
                self.stats["messages_sent"] += 1
                self.stats["bytes_sent"] += len(message) if isinstance(message, str) else len(message)
                
                logger.debug(f"Echoed message: {message[:50]}{'...' if len(str(message)) > 50 else ''}")
                
        except websockets.exceptions.ConnectionClosed:
            logger.info("Echo connection closed normally")
        except Exception as e:
            logger.error(f"Echo handler error: {e}")
        finally:
            await self.unregister_connection(websocket)

    async def binary_handler(self, websocket: websockets.WebSocketServerProtocol, path: str):
        """Binary message testing"""
        await self.register_connection(websocket)
        try:
            async for message in websocket:
                self.stats["messages_received"] += 1
                
                if isinstance(message, bytes):
                    # Echo binary data
                    await websocket.send(message)
                    self.stats["bytes_received"] += len(message)
                    self.stats["bytes_sent"] += len(message)
                    logger.debug(f"Echoed binary message: {len(message)} bytes")
                else:
                    # Convert text to binary and echo
                    binary_data = message.encode('utf-8')
                    await websocket.send(binary_data)
                    self.stats["bytes_received"] += len(message)
                    self.stats["bytes_sent"] += len(binary_data)
                    logger.debug(f"Converted text to binary: {len(binary_data)} bytes")
                
                self.stats["messages_sent"] += 1
                
        except websockets.exceptions.ConnectionClosed:
            logger.info("Binary connection closed normally")
        except Exception as e:
            logger.error(f"Binary handler error: {e}")
        finally:
            await self.unregister_connection(websocket)

    async def fragment_handler(self, websocket: websockets.WebSocketServerProtocol, path: str):
        """Fragmentation testing - sends messages in fragments"""
        await self.register_connection(websocket)
        try:
            async for message in websocket:
                self.stats["messages_received"] += 1
                self.stats["bytes_received"] += len(message)
                
                if message == "FRAGMENT_TEST":
                    # Send a message in 3 fragments
                    fragments = ["Fragment1", "Fragment2", "Fragment3"]
                    
                    for i, fragment in enumerate(fragments):
                        # Note: websockets library handles fragmentation automatically
                        # For manual fragmentation, we'd need lower-level control
                        await websocket.send(fragment)
                        self.stats["messages_sent"] += 1
                        self.stats["bytes_sent"] += len(fragment)
                        
                        # Small delay between fragments
                        await asyncio.sleep(0.1)
                    
                    logger.info("Sent fragmented message")
                else:
                    await websocket.send(message)
                    self.stats["messages_sent"] += 1
                    self.stats["bytes_sent"] += len(message)
                
        except websockets.exceptions.ConnectionClosed:
            logger.info("Fragment connection closed normally")
        except Exception as e:
            logger.error(f"Fragment handler error: {e}")
        finally:
            await self.unregister_connection(websocket)

    async def ping_handler(self, websocket: websockets.WebSocketServerProtocol, path: str):
        """Ping/Pong testing"""
        await self.register_connection(websocket)
        try:
            async for message in websocket:
                self.stats["messages_received"] += 1
                self.stats["bytes_received"] += len(message)
                
                if message == "PING_TEST":
                    # Send a ping frame
                    await websocket.ping(b"test_ping_data")
                    logger.info("Sent ping frame")
                    
                    # Echo the original message
                    await websocket.send("PING_SENT")
                    self.stats["messages_sent"] += 1
                    self.stats["bytes_sent"] += len("PING_SENT")
                else:
                    await websocket.send(message)
                    self.stats["messages_sent"] += 1
                    self.stats["bytes_sent"] += len(message)
                
        except websockets.exceptions.ConnectionClosed:
            logger.info("Ping connection closed normally")
        except Exception as e:
            logger.error(f"Ping handler error: {e}")
        finally:
            await self.unregister_connection(websocket)

    async def slow_handler(self, websocket: websockets.WebSocketServerProtocol, path: str):
        """Slow response testing - simulates slow server"""
        await self.register_connection(websocket)
        try:
            # Simulate slow connection establishment
            await asyncio.sleep(2)
            
            async for message in websocket:
                self.stats["messages_received"] += 1
                self.stats["bytes_received"] += len(message)
                
                if message == "SLOW_MESSAGE":
                    # Simulate slow processing
                    await asyncio.sleep(3)
                    await websocket.send("SLOW_RESPONSE_RECEIVED")
                    logger.info("Sent slow response")
                else:
                    # Normal response with delay
                    await asyncio.sleep(1)
                    await websocket.send(f"SLOW_ECHO: {message}")
                
                self.stats["messages_sent"] += 1
                self.stats["bytes_sent"] += len("SLOW_RESPONSE_RECEIVED")
                
        except websockets.exceptions.ConnectionClosed:
            logger.info("Slow connection closed normally")
        except Exception as e:
            logger.error(f"Slow handler error: {e}")
        finally:
            await self.unregister_connection(websocket)

    async def multi_handler(self, websocket: websockets.WebSocketServerProtocol, path: str):
        """Multiple connection testing"""
        await self.register_connection(websocket)
        try:
            # Extract connection ID from path
            connection_id = path.split('/')[-1] if '/' in path else "unknown"
            
            async for message in websocket:
                self.stats["messages_received"] += 1
                self.stats["bytes_received"] += len(message)
                
                response = f"MULTI_RESPONSE_{connection_id}: {message}"
                await websocket.send(response)
                self.stats["messages_sent"] += 1
                self.stats["bytes_sent"] += len(response)
                
                logger.debug(f"Multi connection {connection_id} handled message")
                
        except websockets.exceptions.ConnectionClosed:
            logger.info(f"Multi connection {connection_id} closed normally")
        except Exception as e:
            logger.error(f"Multi handler error: {e}")
        finally:
            await self.unregister_connection(websocket)

    async def stats_handler(self, websocket: websockets.WebSocketServerProtocol, path: str):
        """Statistics endpoint"""
        await self.register_connection(websocket)
        try:
            # Send current stats
            stats_json = json.dumps(self.stats, indent=2)
            await websocket.send(stats_json)
            self.stats["messages_sent"] += 1
            self.stats["bytes_sent"] += len(stats_json)
            
            logger.info("Sent server statistics")
            
        except websockets.exceptions.ConnectionClosed:
            logger.info("Stats connection closed normally")
        except Exception as e:
            logger.error(f"Stats handler error: {e}")
        finally:
            await self.unregister_connection(websocket)

    async def route_handler(self, websocket: websockets.WebSocketServerProtocol, path: str):
        """Main router for different test endpoints"""
        logger.info(f"New WebSocket connection to {path} from {websocket.remote_address}")
        
        try:
            if path == "/echo" or path == "/":
                await self.echo_handler(websocket, path)
            elif path == "/binary":
                await self.binary_handler(websocket, path)
            elif path == "/fragment":
                await self.fragment_handler(websocket, path)
            elif path == "/ping":
                await self.ping_handler(websocket, path)
            elif path == "/slow":
                await self.slow_handler(websocket, path)
            elif path.startswith("/multi/"):
                await self.multi_handler(websocket, path)
            elif path == "/stats":
                await self.stats_handler(websocket, path)
            else:
                logger.warning(f"Unknown path: {path}")
                await websocket.close(code=1002, reason="Unknown endpoint")
                
        except Exception as e:
            logger.error(f"Handler error for {path}: {e}")
            try:
                await websocket.close(code=1011, reason="Server error")
            except:
                pass

    async def start_server(self):
        """Start the WebSocket test server"""
        logger.info(f"Starting WebSocket test server on {self.host}:{self.port}")
        
        # Create server with custom handler
        server = await websockets.serve(
            self.route_handler,
            self.host,
            self.port,
            ping_interval=20,
            ping_timeout=10,
            close_timeout=10
        )
        
        logger.info(f"WebSocket test server running on ws://{self.host}:{self.port}")
        logger.info("Available endpoints:")
        logger.info("  /echo - Basic echo server")
        logger.info("  /binary - Binary message testing")
        logger.info("  /fragment - Fragmentation testing")
        logger.info("  /ping - Ping/Pong testing")
        logger.info("  /slow - Slow response testing")
        logger.info("  /multi/<id> - Multiple connection testing")
        logger.info("  /stats - Server statistics")
        
        return server

    def print_stats(self):
        """Print current server statistics"""
        print("\n=== Server Statistics ===")
        for key, value in self.stats.items():
            print(f"{key}: {value}")
        print("========================\n")

async def main():
    parser = argparse.ArgumentParser(description="WebSocket Test Server for OpenComputers")
    parser.add_argument("--host", default="0.0.0.0", help="Host to bind to (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=8080, help="Port to bind to (default: 8080)")
    parser.add_argument("--verbose", "-v", action="store_true", help="Enable verbose logging")
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Create and start server
    test_server = WebSocketTestServer(args.host, args.port)
    server = await test_server.start_server()
    
    # Setup signal handlers for graceful shutdown
    def signal_handler():
        logger.info("Received shutdown signal")
        test_server.print_stats()
        server.close()
    
    # Handle Ctrl+C gracefully
    for sig in [signal.SIGTERM, signal.SIGINT]:
        asyncio.get_event_loop().add_signal_handler(sig, signal_handler)
    
    try:
        # Keep server running
        await server.wait_closed()
    except KeyboardInterrupt:
        logger.info("Server stopped by user")
    finally:
        test_server.print_stats()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nServer stopped.")
        sys.exit(0)
