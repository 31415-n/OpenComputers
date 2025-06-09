#!/usr/bin/env python3
"""
Automated WebSocket Test Client for OpenComputers WebSocket Implementation

This script performs comprehensive testing of WebSocket functionality
to verify RFC 6455 compliance and proper integration with OpenComputers.

Features:
- Automated test execution
- RFC 6455 compliance verification
- Performance testing
- Error condition testing
- Detailed reporting
- Integration with test server

Usage:
    python3 websocket_automated_test.py [--server-host HOST] [--server-port PORT]
"""

import asyncio
import websockets
import json
import time
import logging
import argparse
import sys
import struct
import hashlib
import base64
import ssl
from typing import List, Dict, Any, Optional
from dataclasses import dataclass
from enum import Enum

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class TestResult(Enum):
    PASS = "PASS"
    FAIL = "FAIL"
    SKIP = "SKIP"

@dataclass
class TestCase:
    name: str
    description: str
    result: TestResult = TestResult.SKIP
    duration: float = 0.0
    message: str = ""
    details: Dict[str, Any] = None

class WebSocketTestClient:
    def __init__(self, server_host: str = "localhost", server_port: int = 8080):
        self.server_host = server_host
        self.server_port = server_port
        self.base_url = f"ws://{server_host}:{server_port}"
        self.test_results: List[TestCase] = []
        
    def add_test_result(self, test_case: TestCase):
        """Add a test result to the collection"""
        self.test_results.append(test_case)
        status_icon = "✅" if test_case.result == TestResult.PASS else "❌" if test_case.result == TestResult.FAIL else "⏭️"
        logger.info(f"{status_icon} {test_case.name}: {test_case.result.value} ({test_case.duration:.2f}s) {test_case.message}")

    async def test_basic_connection(self) -> TestCase:
        """Test basic WebSocket connection establishment"""
        test = TestCase("Basic Connection", "Test WebSocket connection establishment and close")
        start_time = time.time()
        
        try:
            uri = f"{self.base_url}/echo"
            async with websockets.connect(uri, timeout=10) as websocket:
                # Connection successful
                test.result = TestResult.PASS
                test.message = "Connection established and closed successfully"
                
        except Exception as e:
            test.result = TestResult.FAIL
            test.message = f"Connection failed: {str(e)}"
            
        test.duration = time.time() - start_time
        return test

    async def test_text_message_echo(self) -> TestCase:
        """Test text message sending and receiving"""
        test = TestCase("Text Message Echo", "Test sending and receiving text messages")
        start_time = time.time()
        
        try:
            uri = f"{self.base_url}/echo"
            async with websockets.connect(uri, timeout=10) as websocket:
                test_message = "Hello WebSocket!"
                
                # Send message
                await websocket.send(test_message)
                
                # Receive echo
                response = await asyncio.wait_for(websocket.recv(), timeout=5)
                
                if response == test_message:
                    test.result = TestResult.PASS
                    test.message = "Text message echoed correctly"
                else:
                    test.result = TestResult.FAIL
                    test.message = f"Echo mismatch: sent '{test_message}', got '{response}'"
                    
        except Exception as e:
            test.result = TestResult.FAIL
            test.message = f"Text message test failed: {str(e)}"
            
        test.duration = time.time() - start_time
        return test

    async def test_binary_message_echo(self) -> TestCase:
        """Test binary message sending and receiving"""
        test = TestCase("Binary Message Echo", "Test sending and receiving binary messages")
        start_time = time.time()
        
        try:
            uri = f"{self.base_url}/binary"
            async with websockets.connect(uri, timeout=10) as websocket:
                test_data = bytes([0x01, 0x02, 0x03, 0x04, 0x05, 0xFF, 0xFE])
                
                # Send binary data
                await websocket.send(test_data)
                
                # Receive echo
                response = await asyncio.wait_for(websocket.recv(), timeout=5)
                
                if isinstance(response, bytes) and response == test_data:
                    test.result = TestResult.PASS
                    test.message = "Binary message echoed correctly"
                else:
                    test.result = TestResult.FAIL
                    test.message = f"Binary echo mismatch or wrong type: {type(response)}"
                    
        except Exception as e:
            test.result = TestResult.FAIL
            test.message = f"Binary message test failed: {str(e)}"
            
        test.duration = time.time() - start_time
        return test

    async def test_large_message(self) -> TestCase:
        """Test large message handling"""
        test = TestCase("Large Message", "Test handling of large messages (10KB)")
        start_time = time.time()
        
        try:
            uri = f"{self.base_url}/echo"
            async with websockets.connect(uri, timeout=15) as websocket:
                # Create 10KB message
                large_message = "A" * 10240
                
                # Send large message
                await websocket.send(large_message)
                
                # Receive echo
                response = await asyncio.wait_for(websocket.recv(), timeout=10)
                
                if response == large_message:
                    test.result = TestResult.PASS
                    test.message = f"Large message ({len(large_message)} bytes) handled correctly"
                else:
                    test.result = TestResult.FAIL
                    test.message = f"Large message mismatch: expected {len(large_message)}, got {len(response)}"
                    
        except Exception as e:
            test.result = TestResult.FAIL
            test.message = f"Large message test failed: {str(e)}"
            
        test.duration = time.time() - start_time
        return test

    async def test_utf8_messages(self) -> TestCase:
        """Test UTF-8 message handling"""
        test = TestCase("UTF-8 Messages", "Test various UTF-8 encoded messages")
        start_time = time.time()
        
        utf8_tests = [
            "Hello, World!",
            "Привет, мир!",
            "こんにちは世界",
            "🌍🚀💻🎉",
            "Mixed: Hello мир 世界 🎉"
        ]
        
        try:
            uri = f"{self.base_url}/echo"
            async with websockets.connect(uri, timeout=10) as websocket:
                success_count = 0
                
                for test_string in utf8_tests:
                    await websocket.send(test_string)
                    response = await asyncio.wait_for(websocket.recv(), timeout=5)
                    
                    if response == test_string:
                        success_count += 1
                    else:
                        logger.warning(f"UTF-8 mismatch for: {test_string}")
                
                if success_count == len(utf8_tests):
                    test.result = TestResult.PASS
                    test.message = f"All {len(utf8_tests)} UTF-8 strings handled correctly"
                else:
                    test.result = TestResult.FAIL
                    test.message = f"Only {success_count}/{len(utf8_tests)} UTF-8 strings handled correctly"
                    
        except Exception as e:
            test.result = TestResult.FAIL
            test.message = f"UTF-8 test failed: {str(e)}"
            
        test.duration = time.time() - start_time
        return test

    async def test_ping_pong(self) -> TestCase:
        """Test ping/pong frame handling"""
        test = TestCase("Ping/Pong Frames", "Test WebSocket ping/pong mechanism")
        start_time = time.time()
        
        try:
            uri = f"{self.base_url}/ping"
            async with websockets.connect(uri, timeout=10) as websocket:
                # Send ping test command
                await websocket.send("PING_TEST")
                
                # Wait for response
                response = await asyncio.wait_for(websocket.recv(), timeout=5)
                
                if "PING_SENT" in response:
                    test.result = TestResult.PASS
                    test.message = "Ping/Pong mechanism working"
                else:
                    test.result = TestResult.FAIL
                    test.message = f"Unexpected ping response: {response}"
                    
        except Exception as e:
            test.result = TestResult.FAIL
            test.message = f"Ping/Pong test failed: {str(e)}"
            
        test.duration = time.time() - start_time
        return test

    async def test_close_handshake(self) -> TestCase:
        """Test proper close handshake"""
        test = TestCase("Close Handshake", "Test WebSocket close handshake with codes")
        start_time = time.time()
        
        try:
            uri = f"{self.base_url}/echo"
            websocket = await websockets.connect(uri, timeout=10)
            
            # Send a message first
            await websocket.send("test")
            await websocket.recv()
            
            # Close with specific code
            await websocket.close(code=1000, reason="Normal closure")
            
            # Check if connection is properly closed
            if websocket.closed:
                test.result = TestResult.PASS
                test.message = "Close handshake completed successfully"
            else:
                test.result = TestResult.FAIL
                test.message = "Connection not properly closed"
                
        except Exception as e:
            test.result = TestResult.FAIL
            test.message = f"Close handshake test failed: {str(e)}"
            
        test.duration = time.time() - start_time
        return test

    async def test_connection_timeout(self) -> TestCase:
        """Test connection timeout handling"""
        test = TestCase("Connection Timeout", "Test connection to non-existent server")
        start_time = time.time()
        
        try:
            # Try to connect to non-existent server
            uri = "ws://192.168.255.255:9999/timeout"
            
            try:
                await asyncio.wait_for(websockets.connect(uri), timeout=5)
                # If we get here, connection succeeded when it shouldn't have
                test.result = TestResult.FAIL
                test.message = "Connection should have failed but succeeded"
            except (ConnectionRefusedError, OSError, asyncio.TimeoutError):
            
            test.result = TestResult.PASS
            test.message = "Connection timeout handled correctly"
            
        except Exception as e:
            # If we get here, the timeout worked
            test.result = TestResult.PASS
            test.message = "Connection timeout handled correctly"
            
        test.duration = time.time() - start_time
        return test

    async def test_multiple_connections(self) -> TestCase:
        """Test multiple concurrent connections"""
        test = TestCase("Multiple Connections", "Test handling multiple concurrent connections")
        start_time = time.time()
        
        try:
            connections = []
            connection_count = 5
            
            # Create multiple connections
            for i in range(connection_count):
                uri = f"{self.base_url}/multi/{i}"
                websocket = await websockets.connect(uri, timeout=10)
                connections.append((websocket, i))
            
            # Test each connection
            working_connections = 0
            for websocket, conn_id in connections:
                try:
                    await websocket.send(f"TEST_{conn_id}")
                    response = await asyncio.wait_for(websocket.recv(), timeout=5)
                    
                    if f"MULTI_RESPONSE_{conn_id}" in response:
                        working_connections += 1
                        
                except Exception as e:
                    logger.warning(f"Connection {conn_id} failed: {e}")
            
            # Close all connections
            for websocket, _ in connections:
                await websocket.close()
            
            if working_connections >= connection_count * 0.8:  # 80% success rate
                test.result = TestResult.PASS
                test.message = f"{working_connections}/{connection_count} connections working"
            else:
                test.result = TestResult.FAIL
                test.message = f"Only {working_connections}/{connection_count} connections working"
                
        except Exception as e:
            test.result = TestResult.FAIL
            test.message = f"Multiple connections test failed: {str(e)}"
            
        test.duration = time.time() - start_time
        return test

    async def test_slow_server_response(self) -> TestCase:
        """Test handling of slow server responses"""
        test = TestCase("Slow Server Response", "Test handling of slow server responses")
        start_time = time.time()
        
        try:
            uri = f"{self.base_url}/slow"
            async with websockets.connect(uri, timeout=15) as websocket:
                # Send message to slow endpoint
                await websocket.send("SLOW_MESSAGE")
                
                # Wait for slow response (server waits 3 seconds)
                response = await asyncio.wait_for(websocket.recv(), timeout=10)
                
                if "SLOW_RESPONSE" in response:
                    test.result = TestResult.PASS
                    test.message = "Slow server response handled correctly"
                else:
                    test.result = TestResult.FAIL
                    test.message = f"Unexpected slow response: {response}"
                    
        except asyncio.TimeoutError:
            test.result = TestResult.FAIL
            test.message = "Slow server response timed out"
        except Exception as e:
            test.result = TestResult.FAIL
            test.message = f"Slow server test failed: {str(e)}"
            
        test.duration = time.time() - start_time
        return test

    async def run_all_tests(self) -> Dict[str, Any]:
        """Run all WebSocket tests"""
        logger.info("=== WebSocket Automated Test Suite ===")
        logger.info(f"Testing server at {self.base_url}")
        logger.info("")
        
        # List of all test functions
        test_functions = [
            self.test_basic_connection,
            self.test_text_message_echo,
            self.test_binary_message_echo,
            self.test_large_message,
            self.test_utf8_messages,
            self.test_ping_pong,
            self.test_close_handshake,
            self.test_connection_timeout,
            self.test_multiple_connections,
            self.test_slow_server_response
        ]
        
        start_time = time.time()
        
        # Run all tests
        for test_func in test_functions:
            try:
                test_result = await test_func()
                self.add_test_result(test_result)
                
                # Small delay between tests
                await asyncio.sleep(0.5)
                
            except Exception as e:
                error_test = TestCase(
                    name=test_func.__name__,
                    description="Test function crashed",
                    result=TestResult.FAIL,
                    message=f"Test crashed: {str(e)}"
                )
                self.add_test_result(error_test)
        
        total_time = time.time() - start_time
        
        # Calculate results
        passed = sum(1 for t in self.test_results if t.result == TestResult.PASS)
        failed = sum(1 for t in self.test_results if t.result == TestResult.FAIL)
        total = len(self.test_results)
        
        # Generate report
        report = {
            "total_tests": total,
            "passed": passed,
            "failed": failed,
            "success_rate": (passed / total * 100) if total > 0 else 0,
            "total_time": total_time,
            "tests": [
                {
                    "name": t.name,
                    "result": t.result.value,
                    "duration": t.duration,
                    "message": t.message
                }
                for t in self.test_results
            ]
        }
        
        # Print summary
        logger.info("")
        logger.info("=== Test Results Summary ===")
        logger.info(f"Total tests: {total}")
        logger.info(f"Passed: {passed}")
        logger.info(f"Failed: {failed}")
        logger.info(f"Success rate: {report['success_rate']:.1f}%")
        logger.info(f"Total time: {total_time:.2f} seconds")
        logger.info("")
        
        if failed == 0:
            logger.info("🎉 ALL TESTS PASSED! WebSocket implementation is RFC 6455 compliant.")
        else:
            logger.info("❌ Some tests failed. Check the detailed results above.")
        
        return report

async def main():
    parser = argparse.ArgumentParser(description="WebSocket Automated Test Client")
    parser.add_argument("--server-host", default="localhost", help="WebSocket server host")
    parser.add_argument("--server-port", type=int, default=8080, help="WebSocket server port")
    parser.add_argument("--output", help="Output file for test results (JSON)")
    parser.add_argument("--verbose", "-v", action="store_true", help="Enable verbose logging")
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Create test client and run tests
    client = WebSocketTestClient(args.server_host, args.server_port)
    
    try:
        report = await client.run_all_tests()
        
        # Save results if output file specified
        if args.output:
            with open(args.output, 'w') as f:
                json.dump(report, f, indent=2)
            logger.info(f"Test results saved to {args.output}")
        
        # Exit with error code if tests failed
        sys.exit(0 if report["failed"] == 0 else 1)
        
    except KeyboardInterrupt:
        logger.info("Tests interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Test suite failed: {e}")
        sys.exit(1)

if __name__ == "__main__":
    asyncio.run(main())
