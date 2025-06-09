package li.cil.oc.server.component

import com.google.common.net.InetAddresses

import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util
import java.util.UUID
import java.util.concurrent._
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import javax.net.ssl.SSLException
import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.util.ThreadPoolFactory
import net.minecraft.server.MinecraftServer

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class InternetCard extends prefab.ManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("internet", Visibility.Neighbors).
    create()

  protected var owner: Option[Context] = None

  protected val connections = mutable.Set.empty[InternetCard.Closable]

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Communication,
    DeviceAttribute.Description -> "Internet modem",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "SuperLink X-D4NK"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():boolean -- Returns whether HTTP requests can be made (config setting).""")
  def isHttpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @Callback(doc = """function(url:string[, postData:string[, headers:table[, method:string]]]):userdata -- Starts an HTTP request. If this returns true, further results will be pushed using `http_response` signals.""")
  def request(context: Context, args: Arguments): Array[AnyRef] = {
    checkOwner(context)
    val address = args.checkString(0)
    if (!Settings.get.internetAccessAllowed()) {
      return result(Unit, "internet access is unavailable")
    }
    if (!Settings.get.httpEnabled) {
      return result(Unit, "http requests are unavailable")
    }

    // Thread-safe connection count check
    val currentConnections = connections.synchronized(connections.size)
    if (currentConnections >= Settings.get.maxConnections) {
      throw new IOException("too many open connections")
    }

    val post = if (args.isString(1)) Option(args.checkString(1)) else None
    val headers = if (args.isTable(2)) args.checkTable(2).collect {
      case (key: String, value: AnyRef) => (key, value.toString)
    }.toMap
    else Map.empty[String, String]
    if (!Settings.get.httpHeadersEnabled && headers.nonEmpty) {
      return result(Unit, "http request headers are unavailable")
    }
    val method = if (args.isString(3)) Option(args.checkString(3)) else None
    val request = new InternetCard.HTTPRequest(this, checkAddress(address), post, headers, method)

    // Thread-safe connection addition
    connections.synchronized(connections += request)
    result(request)
  }

  @Callback(direct = true, doc = """function():boolean -- Returns whether TCP connections can be made (config setting).""")
  def isTcpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.tcpEnabled)

  @Callback(direct = true, doc = """function():boolean -- Returns whether WebSocket connections can be made (config setting).""")
  def isWebSocketEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.webSocketEnabled)

  @Callback(doc = """function(address:string[, port:number]):userdata -- Opens a new TCP connection. Returns the handle of the connection.""")
  def connect(context: Context, args: Arguments): Array[AnyRef] = {
    checkOwner(context)
    val address = args.checkString(0)
    val port = args.optInteger(1, -1)
    if (!Settings.get.internetAccessAllowed()) {
      return result(Unit, "internet access is unavailable")
    }
    if (!Settings.get.tcpEnabled) {
      return result(Unit, "tcp connections are unavailable")
    }

    // Thread-safe connection count check
    val currentConnections = connections.synchronized(connections.size)
    if (currentConnections >= Settings.get.maxConnections) {
      throw new IOException("too many open connections")
    }

    val uri = checkUri(address, port)
    val socket = new InternetCard.TCPSocket(this, uri, port)

    // Thread-safe connection addition
    connections.synchronized(connections += socket)
    result(socket)
  }

  /**
   * Creates a new RFC 6455 compliant WebSocket connection.
   *
   * Supports both ws:// and wss:// protocols with proper SSL/TLS handling.
   * Implements full WebSocket handshake, frame processing, and close handshake.
   * Connection count is limited by OpenComputers settings for server performance.
   *
   * @param url WebSocket URL (ws:// or wss://)
   * @param headers Optional HTTP headers for handshake
   * @param protocols Optional subprotocol list for negotiation
   * @return WebSocketConnection handle for Lua API
   */
  @Callback(doc = """function(url:string[, headers:table[, protocols:table]]):userdata -- Opens a new WebSocket connection. Returns the handle of the connection.""")
  def websocket(context: Context, args: Arguments): Array[AnyRef] = {
    checkOwner(context)
    val url = args.checkString(0)
    if (!Settings.get.internetAccessAllowed()) {
      return result(Unit, "internet access is unavailable")
    }
    if (!Settings.get.webSocketEnabled) {
      return result(Unit, "websocket connections are unavailable")
    }

    // Thread-safe connection count check
    val currentConnections = connections.synchronized(connections.size)
    if (currentConnections >= Settings.get.maxConnections) {
      throw new IOException("too many open connections")
    }

    val headers = if (args.isTable(1)) args.checkTable(1).collect {
      case (key: String, value: AnyRef) => (key, value.toString)
    }.toMap
    else Map.empty[String, String]
    if (!Settings.get.httpHeadersEnabled && headers.nonEmpty) {
      return result(Unit, "websocket request headers are unavailable")
    }
    val protocols = if (args.isTable(2)) args.checkTable(2).values.map(_.toString).toList
    else List.empty[String]
    val wsUrl = checkWebSocketAddress(url)

    val websocket = new InternetCard.WebSocketConnection(this, wsUrl, headers, protocols)

    // Thread-safe connection addition
    connections.synchronized(connections += websocket)
    result(websocket)
  }

  private def checkOwner(context: Context) {
    if (owner.isEmpty || context.node != owner.get.node) {
      throw new IllegalArgumentException("can only be used by the owning computer")
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (owner.isEmpty && node.host.isInstanceOf[Context] && node.isNeighborOf(this.node)) {
      owner = Some(node.host.asInstanceOf[Context])
    }
  }

  override def onDisconnect(node: Node) = {
    super.onDisconnect(node)
    if (owner.isDefined && (node == this.node || node.host.isInstanceOf[Context] && (node.host.asInstanceOf[Context] == owner.get))) {
      owner = None
      // Thread-safe connection cleanup - avoid deadlock by copying and releasing lock
      val connectionsToClose = connections.synchronized {
        val toClose = connections.toList
        connections.clear()
        toClose
      }
      // Close connections outside of synchronized block to prevent deadlock
      connectionsToClose.foreach(_.close())
    }
  }

  override def onMessage(message: Message) = {
    super.onMessage(message)
    message.data match {
      case Array() if (message.name == "computer.stopped" || message.name == "computer.started") && owner.isDefined && message.source.address == owner.get.node.address =>
        // Thread-safe connection cleanup - avoid deadlock by copying and releasing lock
        val connectionsToClose = connections.synchronized {
          val toClose = connections.toList
          connections.clear()
          toClose
        }
        // Close connections outside of synchronized block to prevent deadlock
        connectionsToClose.foreach(_.close())
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  private def checkUri(address: String, port: Int): URI = {
    try {
      val parsed = new URI(address)
      if (parsed.getHost != null && (parsed.getPort > 0 || port > 0)) {
        return parsed
      }
    }
    catch {
      case _: Throwable =>
    }

    val simple = new URI("oc://" + address)
    if (simple.getHost != null) {
      if (simple.getPort > 0)
        return simple
      else if (port > 0)
        return new URI(simple.toString + ":" + port)
    }

    throw new IllegalArgumentException("address could not be parsed or no valid port given")
  }

  private def checkAddress(address: String) = {
    val url = try new URL(address)
    catch {
      case e: Throwable => throw new FileNotFoundException("invalid address")
    }
    val protocol = url.getProtocol
    if (!protocol.matches("^https?$")) {
      throw new FileNotFoundException("unsupported protocol")
    }
    url
  }

  private def checkWebSocketAddress(address: String) = {
    val url = try new URL(address.replaceFirst("^ws", "http"))
    catch {
      case e: Throwable => throw new FileNotFoundException("invalid websocket address")
    }
    val originalProtocol = address.split("://", 2)(0).toLowerCase
    if (!originalProtocol.matches("^wss?$")) {
      throw new FileNotFoundException("unsupported websocket protocol")
    }
    (url, originalProtocol == "wss")
  }
}

object InternetCard {
  // For InternetFilteringRuleTest, where Settings.get is not provided.
  private val threadPool: java.util.concurrent.ScheduledExecutorService = ThreadPoolFactory.create("Internet", Option(Settings.get) match {
    case None => 1
    case Some(settings) => settings.internetThreads
  })

  trait Closable {
    def close(): Unit
  }

  /**
   * TCP connection notifier using NIO selector for efficient I/O multiplexing.
   * Handles multiple TCP connections in a single thread to avoid performance issues
   * on multiplayer servers with many concurrent connections.
   */
  object TCPNotifier extends Thread {
    private var selector = Selector.open()
    private val toAccept = new ConcurrentLinkedQueue[(SocketChannel, () => Unit)]
    @volatile private var running = true

    setDaemon(true) // Daemon thread prevents JVM shutdown hanging
    setName("OpenComputers-TCP-Notifier")

    override def run(): Unit = {
      while (running) {
        try {
          Stream.continually(toAccept.poll).takeWhile(_ != null).foreach({
            case (channel: SocketChannel, action: (() => Unit)) =>
              channel.register(selector, SelectionKey.OP_READ, action)
          })

          selector.select()

          import scala.collection.JavaConversions._
          val selectedKeys = selector.selectedKeys
          val readableKeys = mutable.HashSet[SelectionKey]()
          selectedKeys.filter(_.isReadable).foreach(key => {
            key.attachment.asInstanceOf[() => Unit].apply()
            readableKeys += key
          })

          if(readableKeys.nonEmpty) {
            val newSelector = Selector.open()
            selector.keys.filter(!readableKeys.contains(_)).foreach(key => {
              key.channel.register(newSelector, SelectionKey.OP_READ, key.attachment)
            })
            selector.close()
            selector = newSelector
          }
        } catch {
          case e: IOException =>
            OpenComputers.log.error("Error in TCP selector loop.", e)
        }
      }
    }

    def add(e: (SocketChannel, () => Unit)) {
      toAccept.offer(e)
      selector.wakeup()
    }

    def shutdown(): Unit = {
      running = false
      selector.wakeup()
      try {
        selector.close()
      } catch {
        case _: Exception => // Ignore
      }
    }
  }

  TCPNotifier.start()

  /**
   * WebSocket connection notifier implementing RFC 6455 compliant client-only functionality.
   * Uses single NIO selector architecture for optimal performance with multiple concurrent
   * WebSocket connections. Prevents server performance degradation on multiplayer servers.
   */
  object WebSocketNotifier extends Thread {
    private var selector = Selector.open()
    private val toAccept = new ConcurrentLinkedQueue[(SocketChannel, () => Unit)]
    @volatile private var running = true

    setDaemon(true) // Daemon thread prevents JVM shutdown hanging
    setName("OpenComputers-WebSocket-Notifier")

    override def run(): Unit = {
      while (running) {
        try {
          // Register new WebSocket connections
          Stream.continually(toAccept.poll).takeWhile(_ != null).foreach({
            case (channel: SocketChannel, action: (() => Unit)) =>
              channel.register(selector, SelectionKey.OP_READ, action)
          })

          selector.select()

          import scala.collection.JavaConversions._
          val selectedKeys = selector.selectedKeys
          val readableKeys = mutable.HashSet[SelectionKey]()
          selectedKeys.filter(_.isReadable).foreach(key => {
            key.attachment.asInstanceOf[() => Unit].apply()
            readableKeys += key
          })

          // Recreate selector to remove processed keys (same pattern as TCP)
          if(readableKeys.nonEmpty) {
            val newSelector = Selector.open()
            selector.keys.filter(!readableKeys.contains(_)).foreach(key => {
              key.channel.register(newSelector, SelectionKey.OP_READ, key.attachment)
            })
            selector.close()
            selector = newSelector
          }
        } catch {
          case e: IOException =>
            OpenComputers.log.error("Error in WebSocket selector loop.", e)
        }
      }
    }

    def add(e: (SocketChannel, () => Unit)) {
      toAccept.offer(e)
      selector.wakeup()
    }

    def shutdown(): Unit = {
      running = false
      selector.wakeup()
      try {
        selector.close()
      } catch {
        case _: Exception => // Ignore
      }
    }
  }

  WebSocketNotifier.start()

  /**
   * Shutdown hook ensures proper cleanup of notifier threads during JVM shutdown.
   * Prevents resource leaks and ensures graceful termination of network connections.
   * Critical for server environments where clean shutdown is important.
   */
  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    override def run(): Unit = {
      try {
        TCPNotifier.shutdown()
        WebSocketNotifier.shutdown()
      } catch {
        case _: Exception => // Ignore shutdown errors during JVM termination
      }
    }
  }))

  class TCPSocket extends AbstractValue with Closable {
    def this(owner: InternetCard, uri: URI, port: Int) {
      this()
      this.owner = Some(owner)
      channel = SocketChannel.open()
      channel.configureBlocking(false)
      address = threadPool.submit(new AddressResolver(uri, port))
    }

    private var owner: Option[InternetCard] = None
    private var address: Future[InetAddress] = null
    private var channel: SocketChannel = null
    private var isAddressResolved = false
    private val id = UUID.randomUUID()

    private def setupSelector() {
      if (channel == null) return
      TCPNotifier.add((channel, () => {
        owner match {
          case Some(internetCard) =>
            internetCard.node.sendToVisible("computer.signal", "internet_ready", id.toString)
          case _ =>
            channel.close()
        }
      }))
    }

    @Callback(doc = """function():boolean -- Ensures a socket is connected. Errors if the connection failed.""")
    def finishConnect(context: Context, args: Arguments): Array[AnyRef] = {
      val r = this.synchronized(result(checkConnected()))
      setupSelector()
      r
    }

    @Callback(doc = """function([n:number]):string -- Tries to read data from the socket stream. Returns the read byte array.""")
    def read(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      val n = math.min(Settings.get.maxReadBuffer, math.max(0, args.optInteger(0, Int.MaxValue)))
      if (checkConnected()) {
        val buffer = ByteBuffer.allocate(n)
        val read = channel.read(buffer)
        if (read == -1) result(Unit)
        else {
          setupSelector()
          result(buffer.array.view(0, read).toArray)
        }
      }
      else result(Array.empty[Byte])
    }

    @Callback(doc = """function(data:string):number -- Tries to write data to the socket stream. Returns the number of bytes written.""")
    def write(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      if (checkConnected()) {
        val value = args.checkByteArray(0)
        result(channel.write(ByteBuffer.wrap(value)))
      }
      else result(0)
    }

    @Callback(direct = true, doc = """function() -- Closes an open socket stream.""")
    def close(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      close()
      null
    }

    @Callback(direct = true, doc = """function():string -- Returns connection ID.""")
    def id(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      result(id.toString)
    }

    override def dispose(context: Context): Unit = {
      super.dispose(context)
      close()
    }

    override def close(): Unit = {
      owner.foreach(card => {
        card.connections.remove(this)
        address.cancel(true)
        channel.close()
        owner = None
        address = null
        channel = null
      })
    }

    private def checkConnected() = {
      if (owner.isEmpty) throw new IOException("connection lost")
      try {
        if (isAddressResolved) channel.finishConnect()
        else if (address.isCancelled) {
          // I don't think this can ever happen, Justin Case.
          channel.close()
          throw new IOException("bad connection descriptor")
        }
        else if (address.isDone) {
          // Check for errors.
          try address.get catch {
            case e: ExecutionException => throw e.getCause
          }
          isAddressResolved = true
          false
        }
        else false
      }
      catch {
        case t: Throwable =>
          close()
          false
      }
    }

    // This has to be an explicit internal class instead of an anonymous one
    // because the scala compiler breaks otherwise. Yay for compiler bugs.
    private class AddressResolver(val uri: URI, val port: Int) extends Callable[InetAddress] {
      override def call(): InetAddress = {
        val resolved = InetAddress.getByName(uri.getHost)
        checkLists(resolved, uri.getHost)
        val address = new InetSocketAddress(resolved, if (uri.getPort != -1) uri.getPort else port)
        channel.connect(address)
        resolved
      }
    }

  }

  def isRequestAllowed(settings: Settings, inetAddress: InetAddress, host: String): Boolean = {
    if (!settings.internetAccessAllowed()) {
      false
    } else {
      val rules = settings.internetFilteringRules
      inetAddress match {
        // IPv6 handling
        case inet6Address: Inet6Address =>
          // If the IP address is an IPv6 address with an embedded IPv4 address, and the IPv4 address is blocked,
          // block this request.
          if (InetAddresses.hasEmbeddedIPv4ClientAddress(inet6Address)) {
            val inet4in6Address = InetAddresses.getEmbeddedIPv4ClientAddress(inet6Address)
            if (!rules.map(r => r.apply(inet4in6Address, host)).collectFirst({ case Some(r) => r }).getOrElse(true)) {
              return false
            }
          }

          // Process address as an IPv6 address.
          rules.map(r => r.apply(inet6Address, host)).collectFirst({ case Some(r) => r }).getOrElse(false)
        // IPv4 handling
        case inet4Address: Inet4Address =>
          // Process address as an IPv4 address.
          rules.map(r => r.apply(inet4Address, host)).collectFirst({ case Some(r) => r }).getOrElse(false)
        case _ =>
          // Unrecognized address type - block.
          OpenComputers.log.warn("Internet Card blocked unrecognized address type: " + inetAddress.toString)
          false
      }
    }
  }

  def checkLists(inetAddress: InetAddress, host: String): Unit = {
    if (!isRequestAllowed(Settings.get, inetAddress, host)) {
      throw new FileNotFoundException("address is not allowed")
    }
  }

  class HTTPRequest extends AbstractValue with Closable {
    def this(owner: InternetCard, url: URL, post: Option[String], headers: Map[String, String], method: Option[String]) {
      this()
      this.owner = Some(owner)
      this.stream = threadPool.submit(new RequestSender(url, post, headers, method))
    }

    private var owner: Option[InternetCard] = None
    private var response: Option[(Int, String, AnyRef)] = None
    private var stream: Future[InputStream] = null
    private val queue = new ConcurrentLinkedQueue[Byte]()
    private var reader: Future[_] = null
    private var eof = false

    @Callback(doc = """function():boolean -- Ensures a response is available. Errors if the connection failed.""")
    def finishConnect(context: Context, args: Arguments): Array[AnyRef] = this.synchronized(result(checkResponse()))

    @Callback(direct = true, doc = """function():number, string, table -- Get response code, message and headers.""")
    def response(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      response match {
        case Some((code, message, headers)) => result(code, message, headers)
        case _ => result(Unit)
      }
    }

    @Callback(doc = """function([n:number]):string -- Tries to read data from the response. Returns the read byte array.""")
    def read(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      val n = math.min(Settings.get.maxReadBuffer, math.max(0, args.optInteger(0, Int.MaxValue)))
      if (checkResponse()) {
        if (eof && queue.isEmpty) result(Unit)
        else {
          val buffer = ByteBuffer.allocate(n)
          var read = 0
          while (!queue.isEmpty && read < n) {
            buffer.put(queue.poll())
            read += 1
          }
          if (read == 0) {
            readMore()
          }
          result(buffer.array.view(0, read).toArray)
        }
      }
      else result(Array.empty[Byte])
    }

    @Callback(direct = true, doc = """function() -- Closes an open socket stream.""")
    def close(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      close()
      null
    }

    override def dispose(context: Context): Unit = {
      super.dispose(context)
      close()
    }

    override def close(): Unit = {
      owner.foreach(card => {
        card.connections.remove(this)
        stream.cancel(true)
        if (reader != null) {
          reader.cancel(true)
        }
        owner = None
        stream = null
        reader = null
      })
    }

    private def checkResponse() = this.synchronized {
      if (owner.isEmpty) throw new IOException("connection lost")
      if (stream.isDone) {
        if (reader == null) {
          // Check for errors.
          try stream.get catch {
            case e: ExecutionException => throw e.getCause
          }
          readMore()
        }
        true
      }
      else false
    }

    private def readMore(): Unit = {
      if (reader == null || reader.isCancelled || reader.isDone) {
        if (!eof) reader = threadPool.submit(new Runnable {
          override def run(): Unit = {
            val buffer = new Array[Byte](Settings.get.maxReadBuffer)
            val count = stream.get.read(buffer)
            if (count < 0) {
              eof = true
            }
            for (i <- 0 until count) {
              queue.add(buffer(i))
            }
          }
        })
      }
    }

    // This one doesn't (see comment in TCP socket), but I like to keep it consistent.
    private class RequestSender(val url: URL, val post: Option[String], val headers: Map[String, String], val method: Option[String]) extends Callable[InputStream] {
      override def call() = try {
        checkLists(InetAddress.getByName(url.getHost), url.getHost)
        val proxy = Option(MinecraftServer.getServer.getServerProxy).getOrElse(java.net.Proxy.NO_PROXY)
        url.openConnection(proxy) match {
          case http: HttpURLConnection => try {
            http.setDoInput(true)
            http.setDoOutput(post.isDefined)
            http.setRequestMethod(if (method.isDefined) method.get else if (post.isDefined) "POST" else "GET")
            http.setRequestProperty("User-Agent", Settings.get.httpUserAgent.replace("$version", OpenComputers.Version))
            headers.foreach(Function.tupled(http.setRequestProperty))
            if (post.isDefined) {
              http.setReadTimeout(Settings.get.httpTimeout)

              val out = new BufferedWriter(new OutputStreamWriter(http.getOutputStream))
              out.write(post.get)
              out.close()
            }

            // Finish the connection. Call getInputStream a second time below to re-throw any exception.
            // This avoids getResponseCode() waiting for the connection to end in the synchronized block.
            try {
              http.getInputStream
            } catch {
              case _: Exception =>
            }

            HTTPRequest.this.synchronized {
              response = Some((http.getResponseCode, http.getResponseMessage, http.getHeaderFields))
            }

            // TODO: This should allow accessing getErrorStream() for reading unsuccessful HTTP responses' output,
            // but this would be a breaking change for existing OC code.
            http.getInputStream
          }
          catch {
            case t: Throwable =>
              http.disconnect()
              throw t
          }
          case other => throw new IOException("unexpected connection type")
        }
      }
      catch {
        case e: UnknownHostException =>
          throw new IOException("unknown host: " + Option(e.getMessage).getOrElse(e.toString))
        case e: Throwable =>
          throw new IOException(Option(e.getMessage).getOrElse(e.toString))
      }
    }

  }

  /**
   * RFC 6455 compliant WebSocket client implementation for OpenComputers.
   *
   * Features:
   * - Full RFC 6455 compliance (handshake, framing, close handshake)
   * - Client-only implementation (no server functionality)
   * - Proper UTF-8 validation for text frames
   * - Fragmentation support with DoS protection
   * - SSL/TLS support for secure WebSocket connections (wss://)
   * - Ping/Pong frame handling for connection keep-alive
   * - Thread-safe operation with proper resource management
   *
   * Security considerations:
   * - Validates all incoming frames according to RFC 6455
   * - Implements proper close handshake to prevent connection leaks
   * - Limits fragment count and message size to prevent DoS attacks
   * - Uses existing OpenComputers security filtering for connections
   */
  class WebSocketConnection extends AbstractValue with Closable {
    def this(owner: InternetCard, urlAndSecure: (URL, Boolean), headers: Map[String, String], protocols: List[String] = List.empty) {
      this()
      this.owner = Some(owner)
      this.url = urlAndSecure._1
      this.isSecure = urlAndSecure._2
      this.headers = headers
      this.requestedProtocols = protocols
      this.connector = threadPool.submit(new WebSocketConnector())
    }

    private var owner: Option[InternetCard] = None
    private var url: URL = null
    private var isSecure: Boolean = false
    private var headers: Map[String, String] = Map.empty
    private var requestedProtocols: List[String] = List.empty
    private val id = UUID.randomUUID()
    private var connector: Future[SocketChannel] = null
    private var channel: SocketChannel = null
    private var sslEngine: SSLEngine = null
    private var sslInbound: ByteBuffer = null
    private var sslOutbound: ByteBuffer = null
    private var connected = false
    private val messageQueue = new ConcurrentLinkedQueue[String]()
    private val binaryQueue = new ConcurrentLinkedQueue[Array[Byte]]()
    private var handshakeComplete = false

    // RFC 6455 Section 5.4: Fragmentation support with proper state management
    private var fragmentBuffer: Option[java.io.ByteArrayOutputStream] = None
    private var fragmentOpcode: Int = -1
    private val maxMessageSize = Settings.get.maxNetworkPacketSize * 1024 // Use OC setting for max size
    private var fragmentCount: Int = 0 // Track number of fragments to prevent DoS
    private val maxFragmentCount = 1000 // RFC 6455 Section 5.4: Reasonable limit for fragments
    // Note: RFC 6455 does not specify fragmentation timeout - removed for strict compliance
    private var negotiatedExtensions: List[String] = List.empty
    private var negotiatedProtocol: Option[String] = None

    // RFC 6455 Section 7: Close handshake state
    private var closeFrameSent = false
    private var closeFrameReceived = false
    private var closeTimeout: Option[Future[_]] = None

    @Callback(doc = """function():boolean -- Ensures WebSocket connection is established. Errors if the connection failed.""")
    def finishConnect(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      result(checkConnected())
    }

    @Callback(doc = """function():string -- Returns connection ID.""")
    def id(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      result(id.toString)
    }

    @Callback(doc = """function():boolean -- Returns whether the WebSocket is connected.""")
    def isConnected(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      result(connected && handshakeComplete)
    }

    @Callback(doc = """function():string -- Returns the negotiated subprotocol, if any.""")
    def getProtocol(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      result(negotiatedProtocol.orNull)
    }

    @Callback(doc = """function():table -- Returns the negotiated extensions.""")
    def getExtensions(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      val extensionsTable = new java.util.HashMap[String, String]()
      negotiatedExtensions.zipWithIndex.foreach { case (ext, idx) =>
        extensionsTable.put((idx + 1).toString, ext)
      }
      result(extensionsTable)
    }

    @Callback(doc = """function(message:string) -- Sends a text message over the WebSocket.""")
    def send(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      if (!connected || !handshakeComplete) {
        return result(false, "websocket not connected")
      }
      val message = args.checkString(0)
      try {
        sendWebSocketMessage(message.getBytes("UTF-8"), isText = true)
        result(true)
      } catch {
        case e: Exception =>
          result(false, e.getMessage)
      }
    }

    @Callback(doc = """function(data:string) -- Sends binary data over the WebSocket.""")
    def sendBinary(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      if (!connected || !handshakeComplete) {
        return result(false, "websocket not connected")
      }
      val data = args.checkByteArray(0)
      try {
        sendWebSocketMessage(data, isText = false)
        result(true)
      } catch {
        case e: Exception =>
          result(false, e.getMessage)
      }
    }

    @Callback(doc = """function([data:string]) -- Sends a ping frame with optional data.""")
    def ping(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      if (!connected || !handshakeComplete) {
        return result(false, "websocket not connected")
      }
      val data = if (args.count() > 0) args.checkByteArray(0) else Array.empty[Byte]

      // RFC 6455 Section 5.5.2: Control frames payload must be <= 125 bytes
      if (data.length > 125) {
        return result(false, "ping data too large (max 125 bytes)")
      }

      try {
        sendWebSocketFrame(data, isText = false, opcode = 0x9)
        result(true)
      } catch {
        case e: Exception =>
          result(false, e.getMessage)
      }
    }

    @Callback(doc = """function(message:string, fragment_size:number) -- Sends a large text message with fragmentation.""")
    def sendFragmented(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      if (!connected || !handshakeComplete) {
        return result(false, "websocket not connected")
      }
      val message = args.checkString(0)
      val fragmentSize = args.optInteger(1, 1024) // Default 1KB fragments
      try {
        sendFragmentedMessage(message.getBytes("UTF-8"), isText = true, fragmentSize)
        result(true)
      } catch {
        case e: Exception =>
          result(false, e.getMessage)
      }
    }

    @Callback(doc = """function():string -- Receives a text message from the WebSocket.""")
    def receive(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      val message = messageQueue.poll()
      if (message != null) {
        result(message)
      } else {
        result(Unit)
      }
    }

    @Callback(doc = """function():string -- Receives binary data from the WebSocket.""")
    def receiveBinary(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      val data = binaryQueue.poll()
      if (data != null) {
        result(data)
      } else {
        result(Unit)
      }
    }

    @Callback(direct = true, doc = """function() -- Closes the WebSocket connection.""")
    def close(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      close()
      null
    }

    override def dispose(context: Context): Unit = {
      super.dispose(context)
      close()
    }

    override def close(): Unit = {
      owner.foreach(card => {
        card.connections.remove(this)

        // RFC 6455 Section 7.1.2: Proper close handshake with improved timeout handling
        if (connected && handshakeComplete && channel != null && !closeFrameSent) {
          try {
            closeFrameSent = true
            sendCloseFrameOnly(1000, "Normal closure")

            // RFC 6455 Section 7.1.3: Wait for close frame response with proper timeout
            if (!closeFrameReceived) {
              val closeTimeoutMs = Settings.get.httpTimeout.max(3000).min(10000) // Max 10 seconds
              closeTimeout = Some(threadPool.schedule(new Runnable {
                override def run(): Unit = {
                  // Avoid nested synchronized - check state first, then act outside lock
                  val shouldClose = synchronized {
                    !closeFrameReceived && connected
                  }
                  if (shouldClose) {
                    // RFC 6455 Section 7.1.7: Fail the connection if no close response
                    try {
                      owner.foreach(_.node.sendToVisible("computer.signal", "websocket_error", id.toString, "Close handshake timeout"))
                    } catch {
                      case _: Exception => // Ignore signal errors during timeout
                    }
                    forceClose()
                  }
                }
              }, closeTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS))
            } else {
              // Close frame already received, safe to close immediately
              forceClose()
            }
          } catch {
            case _: Exception =>
              // If sending close frame fails, force close immediately
              forceClose()
          }
        } else {
          // Connection not established or already closing, force close
          forceClose()
        }
      })
    }

    private def forceClose(): Unit = {
      if (connector != null) connector.cancel(true)
      closeTimeout.foreach(_.cancel(true))

      // Clean up SSL resources safely
      try {
        if (sslEngine != null) {
          sslEngine.closeOutbound()
        }
      } catch {
        case _: Throwable => // Ignore all cleanup errors
      }

      if (channel != null) channel.close()

      // Reset state variables
      connected = false
      handshakeComplete = false
      closeFrameSent = false
      closeFrameReceived = false
      fragmentCount = 0 // Reset fragment counter
      owner = None
      connector = null
      channel = null
      closeTimeout = None
      // Don't reset SSL variables to avoid setter issues
    }



    private def checkConnected(): Boolean = {
      if (owner.isEmpty) throw new IOException("connection lost")
      if (connector != null && connector.isDone && !connected) {
        try {
          channel = connector.get()
          if (isSecure) {
            initializeSSL()
            // SSL handshake with proper error handling and timeout
            try {
              performSSLHandshake()
              // Check if handshake is complete
              val handshakeStatus = sslEngine.getHandshakeStatus
              if (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED &&
                  handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                // Handshake not complete, will continue on next call
                return false
              }

              // RFC 6455 Section 4.1: Verify SSL session is properly established
              val session = sslEngine.getSession
              if (!session.isValid) {
                throw new SSLException("SSL session is not valid")
              }

            } catch {
              case e: SSLException =>
                // SSL handshake failed or in progress
                if (e.getMessage.contains("timeout") || e.getMessage.contains("failed")) {
                  close()
                  throw e
                }
                // SSL handshake in progress, will retry
                return false
              case e: Exception =>
                close()
                throw new SSLException("SSL handshake failed: " + e.getMessage, e)
            }
          }
          connected = true
          performHandshake()
          startReading()
        } catch {
          case e: ExecutionException =>
            close()
            throw e.getCause
          case e: Exception =>
            close()
            throw e
        }
      }
      connected && handshakeComplete
    }

    private def initializeSSL(): Unit = {
      val sslContext = SSLContext.getDefault
      sslEngine = sslContext.createSSLEngine(url.getHost, if (url.getPort != -1) url.getPort else 443)
      sslEngine.setUseClientMode(true)

      val session = sslEngine.getSession
      val bufferSize = session.getPacketBufferSize
      sslInbound = ByteBuffer.allocate(bufferSize)
      sslOutbound = ByteBuffer.allocate(bufferSize)
    }

    private def performSSLHandshake(): Unit = {
      if (sslEngine == null || sslInbound == null || sslOutbound == null) {
        throw new SSLException("SSL not properly initialized")
      }

      sslEngine.beginHandshake()

      var handshakeStatus = sslEngine.getHandshakeStatus
      val appBuffer = ByteBuffer.allocate(sslEngine.getSession.getApplicationBufferSize)
      var handshakeRounds = 0
      val maxHandshakeRounds = 100 // Prevent infinite loops

      while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED &&
             handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING &&
             handshakeRounds < maxHandshakeRounds) {

        handshakeRounds += 1

        handshakeStatus match {
          case SSLEngineResult.HandshakeStatus.NEED_WRAP =>
            sslOutbound.clear()
            val result = sslEngine.wrap(ByteBuffer.allocate(0), sslOutbound)
            handshakeStatus = result.getHandshakeStatus
            sslOutbound.flip()
            if (sslOutbound.hasRemaining) {
              val written = channel.write(sslOutbound)
              if (written == 0) {
                // Channel not ready for writing, will retry on next call
                return
              }
            }

          case SSLEngineResult.HandshakeStatus.NEED_UNWRAP =>
            val bytesRead = channel.read(sslInbound)
            if (bytesRead < 0) {
              throw new SSLException("SSL handshake failed: connection closed")
            }
            if (bytesRead == 0) {
              // No data available, will retry on next call
              return
            } else {
              sslInbound.flip()
              val result = sslEngine.unwrap(sslInbound, appBuffer)
              handshakeStatus = result.getHandshakeStatus
              sslInbound.compact()
            }

          case SSLEngineResult.HandshakeStatus.NEED_TASK =>
            // RFC 6455 Section 4.1: SSL tasks execution - use thread pool to prevent blocking
            // Execute SSL tasks asynchronously to prevent server thread blocking
            val taskFuture = threadPool.submit(new Callable[Unit] {
              override def call(): Unit = {
                var task = sslEngine.getDelegatedTask
                while (task != null) {
                  task.run()
                  task = sslEngine.getDelegatedTask
                }
              }
            })

            // Wait for tasks completion with timeout to prevent infinite blocking
            try {
              taskFuture.get(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
            } catch {
              case _: java.util.concurrent.TimeoutException =>
                taskFuture.cancel(true)
                throw new SSLException("SSL task execution timeout")
              case e: Exception =>
                throw new SSLException("SSL task execution failed: " + e.getMessage)
            }
            handshakeStatus = sslEngine.getHandshakeStatus

          case _ =>
            throw new SSLException("Unknown handshake status: " + handshakeStatus)
        }
      }

      if (handshakeRounds >= maxHandshakeRounds) {
        throw new SSLException("SSL handshake failed: too many handshake rounds")
      }
    }

    private def sslWrite(data: ByteBuffer): Unit = {
      if (isSecure && sslEngine != null && sslOutbound != null) {
        sslOutbound.clear()
        val result = sslEngine.wrap(data, sslOutbound)
        if (result.getStatus != SSLEngineResult.Status.OK) {
          throw new SSLException("SSL wrap failed: " + result.getStatus)
        }
        sslOutbound.flip()
        writeBufferNonBlocking(sslOutbound)
      } else {
        writeBufferNonBlocking(data)
      }
    }

    private def writeBufferNonBlocking(buffer: ByteBuffer): Unit = {
      val timeout = Settings.get.httpTimeout.max(5000) // 5 second minimum
      val future = new CompletableFuture[Unit]()

      def attemptWrite(): Unit = {
        try {
          val written = channel.write(buffer)
          if (buffer.hasRemaining) {
            // Schedule retry if buffer not fully written
            threadPool.schedule(new Runnable {
              override def run(): Unit = attemptWrite()
            }, 1, TimeUnit.MILLISECONDS)
          } else {
            future.complete(())
          }
        } catch {
          case e: Exception => future.completeExceptionally(e)
        }
      }

      // Start writing
      attemptWrite()

      // Set timeout
      threadPool.schedule(new Runnable {
        override def run(): Unit = {
          if (!future.isDone) {
            future.completeExceptionally(new IOException("Write timeout"))
          }
        }
      }, timeout, TimeUnit.MILLISECONDS)

      // Wait for completion
      try {
        future.get()
      } catch {
        case e: ExecutionException => throw e.getCause
      }
    }

    private def sslRead(buffer: ByteBuffer): Int = {
      if (isSecure && sslEngine != null) {
        val netData = ByteBuffer.allocate(sslEngine.getSession.getPacketBufferSize)
        val bytesRead = channel.read(netData)
        if (bytesRead > 0) {
          netData.flip()
          val result = sslEngine.unwrap(netData, buffer)
          result.getStatus match {
            case SSLEngineResult.Status.OK =>
              result.bytesProduced()
            case SSLEngineResult.Status.BUFFER_UNDERFLOW =>
              // Need more network data, return 0 to indicate no application data available
              0
            case SSLEngineResult.Status.BUFFER_OVERFLOW =>
              throw new SSLException("SSL buffer overflow - application buffer too small")
            case SSLEngineResult.Status.CLOSED =>
              throw new SSLException("SSL connection closed")
            case status =>
              throw new SSLException("SSL unwrap failed: " + status)
          }
        } else {
          bytesRead
        }
      } else {
        channel.read(buffer)
      }
    }

    private def performHandshake(): Unit = {
      // RFC 6455 compliant WebSocket handshake implementation
      // RFC 6455 Section 4.1: Use cryptographically strong random generator
      val keyBytes = new Array[Byte](16)
      new java.security.SecureRandom().nextBytes(keyBytes)
      val key = java.util.Base64.getEncoder.encodeToString(keyBytes)

      val request = new StringBuilder()
      request.append(s"GET ${url.getPath}${if (url.getQuery != null) "?" + url.getQuery else ""} HTTP/1.1\r\n")
      request.append(s"Host: ${url.getHost}${if (url.getPort != -1) ":" + url.getPort else ""}\r\n")

      // RFC 6455 Section 4.2.1: Connection header MUST come before Upgrade
      request.append("Connection: Upgrade\r\n")
      request.append("Upgrade: websocket\r\n")

      request.append(s"Sec-WebSocket-Key: $key\r\n")
      request.append("Sec-WebSocket-Version: 13\r\n")

      // RFC 6455 Section 9: Extensions support - only request if explicitly configured
      // Do not request extensions by default as client-only implementation
      // Extensions would be added here if specifically requested by user

      // RFC 6455 Section 1.9: Subprotocol support
      if (requestedProtocols.nonEmpty) {
        request.append(s"Sec-WebSocket-Protocol: ${requestedProtocols.mkString(", ")}\r\n")
      }

      // RFC 6455 Section 10.2: Security - Origin header for CSRF protection
      // RFC 6455 Section 4.1: Origin MUST use HTTP/HTTPS scheme, not ws/wss
      val originScheme = if (isSecure) "https" else "http"
      val originPort = if (url.getPort != -1) ":" + url.getPort else ""
      request.append(s"Origin: $originScheme://${url.getHost}$originPort\r\n")

      headers.foreach { case (k, v) => request.append(s"$k: $v\r\n") }
      request.append("\r\n")

      sslWrite(ByteBuffer.wrap(request.toString.getBytes("UTF-8")))

      // Non-blocking read response with timeout
      val buffer = ByteBuffer.allocate(4096)
      val startTime = System.currentTimeMillis()
      val timeout = Settings.get.httpTimeout.max(10000) // 10 second minimum for handshake
      var totalBytesRead = 0
      var responseComplete = false

      // NIO-based handshake reading with ScheduledExecutorService
      readHandshakeResponseNonBlocking(buffer, timeout) match {
        case Some(response) =>
          totalBytesRead = response.length
          responseComplete = true
        case None =>
          throw new IOException("WebSocket handshake failed: Response timeout")
      }

      if (!responseComplete) {
        throw new IOException("WebSocket handshake failed: Response timeout")
      }

      if (totalBytesRead <= 0) {
        throw new IOException("WebSocket handshake failed: No response received")
      }

      val response = new String(buffer.array(), 0, buffer.position(), "UTF-8")

      // RFC 6455 Section 4.2.2: Validate handshake response with case-insensitive header matching
      validateHandshakeResponse(response, key)

      // Parse negotiated extensions (RFC 6455 Section 9)
      negotiatedExtensions = parseExtensions(response)

      // Parse negotiated subprotocol (RFC 6455 Section 1.9)
      negotiatedProtocol = parseSubprotocol(response)

      handshakeComplete = true
      owner.foreach(_.node.sendToVisible("computer.signal", "websocket_success", id.toString))
    }

    private def readHandshakeResponseNonBlocking(buffer: ByteBuffer, timeoutMs: Long): Option[String] = {
      val future = new CompletableFuture[String]()
      val responseBuilder = new StringBuilder()

      def attemptRead(): Unit = {
        try {
          val bytesRead = sslRead(buffer)
          if (bytesRead > 0) {
            val newData = new String(buffer.array(), buffer.position() - bytesRead, bytesRead, "UTF-8")
            responseBuilder.append(newData)

            // Check if we have complete HTTP response (ends with \r\n\r\n)
            val currentResponse = responseBuilder.toString()
            if (currentResponse.contains("\r\n\r\n")) {
              future.complete(currentResponse)
            } else {
              // Schedule next read attempt
              threadPool.schedule(new Runnable {
                override def run(): Unit = attemptRead()
              }, 1, TimeUnit.MILLISECONDS)
            }
          } else if (bytesRead == 0) {
            // No data available, schedule retry
            threadPool.schedule(new Runnable {
              override def run(): Unit = attemptRead()
            }, 10, TimeUnit.MILLISECONDS)
          } else {
            future.completeExceptionally(new IOException("Connection closed during handshake"))
          }
        } catch {
          case e: Exception => future.completeExceptionally(e)
        }
      }

      // Start reading
      attemptRead()

      // Set timeout
      threadPool.schedule(new Runnable {
        override def run(): Unit = {
          if (!future.isDone) {
            future.completeExceptionally(new IOException("Handshake timeout"))
          }
        }
      }, timeoutMs, TimeUnit.MILLISECONDS)

      // Wait for completion
      try {
        Some(future.get())
      } catch {
        case _: Exception => None
      }
    }

    private def validateHandshakeResponse(response: String, key: String): Unit = {
      // RFC 6455 Section 4.2.2: Validate handshake response
      if (!response.startsWith("HTTP/1.1 101 Switching Protocols")) {
        throw new IOException("WebSocket handshake failed: Invalid status line")
      }

      // RFC 6455 Section 4.2.2: Response must end with \r\n\r\n
      if (!response.contains("\r\n\r\n")) {
        throw new IOException("WebSocket handshake failed: Invalid response format")
      }

      // Parse headers with case-insensitive matching
      val headers = parseHttpHeaders(response)

      // RFC 6455 Section 4.2.2: Check required headers with strict validation
      headers.get("upgrade") match {
        case Some(upgrade) if upgrade.toLowerCase.trim == "websocket" => // Valid
        case Some(upgrade) => throw new IOException(s"WebSocket handshake failed: Invalid Upgrade header value: $upgrade")
        case None => throw new IOException("WebSocket handshake failed: Missing Upgrade header")
      }

      headers.get("connection") match {
        case Some(connection) if connection.toLowerCase.contains("upgrade") => // Valid
        case Some(connection) => throw new IOException(s"WebSocket handshake failed: Invalid Connection header value: $connection")
        case None => throw new IOException("WebSocket handshake failed: Missing Connection header")
      }

      // RFC 6455 Section 4.2.2: Validate Sec-WebSocket-Accept
      val expectedAccept = calculateWebSocketAccept(key)
      headers.get("sec-websocket-accept") match {
        case Some(accept) if accept == expectedAccept => // Valid
        case Some(accept) => throw new IOException(s"WebSocket handshake failed: Invalid Sec-WebSocket-Accept: expected $expectedAccept, got $accept")
        case None => throw new IOException("WebSocket handshake failed: Missing Sec-WebSocket-Accept header")
      }

      // RFC 6455 Section 1.9: Validate subprotocol if negotiated
      headers.get("sec-websocket-protocol").foreach { negotiated =>
        // RFC 6455 Section 1.9: Server MUST only select from protocols requested by client
        // If client didn't request any protocols, server MUST NOT send any
        if (requestedProtocols.isEmpty) {
          throw new IOException(s"WebSocket handshake failed: Server sent protocol '$negotiated' but client requested none")
        } else if (!requestedProtocols.contains(negotiated.trim)) {
          throw new IOException(s"WebSocket handshake failed: Server selected unsupported protocol: $negotiated")
        }
      }

      // RFC 6455 Section 9: Validate extensions if negotiated
      // Since we are a client-only implementation and don't request any extensions,
      // any extension in response is a protocol violation
      headers.get("sec-websocket-extensions").foreach { extensionHeader =>
        // RFC 6455 Section 9.1: Server MUST NOT use extensions not requested by client
        // For strict RFC 6455 compliance: ANY non-empty extension header is a violation
        // since this client implementation never requests extensions
        val trimmedExtensions = extensionHeader.trim
        if (trimmedExtensions.nonEmpty) {
          throw new IOException(s"WebSocket handshake failed: Server sent unrequested extension: $extensionHeader")
        }
      }
    }

    private def parseHttpHeaders(response: String): Map[String, String] = {
      val lines = response.split("\r\n")
      val headerMap = scala.collection.mutable.Map[String, String]()

      for (line <- lines.drop(1)) { // Skip status line
        if (line.trim.isEmpty) return headerMap.toMap // End of headers

        val colonIndex = line.indexOf(':')
        if (colonIndex > 0) {
          val name = line.substring(0, colonIndex).trim.toLowerCase
          val value = line.substring(colonIndex + 1).trim
          // RFC 6455 Section 4.2.2: Header names and values must not be empty
          if (name.nonEmpty && value.nonEmpty) {
            headerMap(name) = value
          }
        }
      }
      headerMap.toMap
    }

    private def calculateWebSocketAccept(key: String): String = {
      import java.security.MessageDigest
      val websocketMagic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
      val sha1 = MessageDigest.getInstance("SHA-1")
      val hash = sha1.digest((key + websocketMagic).getBytes("UTF-8"))
      java.util.Base64.getEncoder.encodeToString(hash)
    }

    private def parseExtensions(response: String): List[String] = {
      // Parse Sec-WebSocket-Extensions header from response
      val extensionPattern = """(?i)Sec-WebSocket-Extensions:\s*([^\r\n]+)""".r
      extensionPattern.findFirstMatchIn(response) match {
        case Some(m) =>
          m.group(1).split(",").map(_.trim).filter(_.nonEmpty).toList
        case None => List.empty
      }
    }

    private def parseSubprotocol(response: String): Option[String] = {
      // Parse Sec-WebSocket-Protocol header from response
      val protocolPattern = """(?i)Sec-WebSocket-Protocol:\s*([^\r\n,]+)""".r
      protocolPattern.findFirstMatchIn(response).map(_.group(1).trim)
    }

    private def startReading(): Unit = {
      // Use global WebSocket selector instead of individual selectors
      WebSocketNotifier.add((channel, () => {
        try {
          if (connected) {
            readWebSocketFrame()
          }
        } catch {
          case e: Exception =>
            owner.foreach(_.node.sendToVisible("computer.signal", "websocket_error", id.toString, e.getMessage))
            close()
        }
      }))
    }

    private def readWebSocketFrame(): Unit = {
      // RFC 6455 compliant WebSocket frame reading with optimized non-blocking I/O
      val headerBuffer = ByteBuffer.allocate(2)

      // Optimized header reading with single selector reuse
      if (!readBytesWithTimeout(headerBuffer, 2, 5000)) { // 5 second timeout for header
        return // Timeout or connection closed
      }

      headerBuffer.flip()
      val firstByte = headerBuffer.get() & 0xFF
      val secondByte = headerBuffer.get() & 0xFF

      val fin = (firstByte & 0x80) != 0
      val rsv = (firstByte & 0x70) >> 4
      val opcode = firstByte & 0x0F
      val masked = (secondByte & 0x80) != 0
      var payloadLength = (secondByte & 0x7F).toLong

      // RFC 6455 Section 5.2: Validate RSV bits - strict client-only validation
      if (!isValidRsvBits(rsv)) {
        sendCloseFrame(1002, "Protocol error: Invalid RSV bits")
        return
      }

      // RFC 6455 Section 5.4: Validate fragmentation state consistency
      if (opcode >= 0x8) { // Control frame
        // RFC 6455 Section 5.5: Control frames can be injected in the middle of fragmented message
        // This is allowed and should not affect fragmentation state
        // RFC 6455 Section 5.5: Control frames MUST NOT be fragmented
        if (!fin) {
          sendCloseFrame(1002, "Protocol error: Control frames must not be fragmented")
          return
        }
      } else { // Data frame (0x0, 0x1, 0x2)
        if (opcode == 0x0) { // Continuation frame
          if (fragmentBuffer.isEmpty) {
            sendCloseFrame(1002, "Protocol error: Unexpected continuation frame")
            return
          }
        } else { // Start of new message (0x1 or 0x2)
          if (fragmentBuffer.isDefined) {
            sendCloseFrame(1002, "Protocol error: New data frame during fragmentation")
            return
          }
        }
      }

      // Handle extended payload length with optimized non-blocking reads
      if (payloadLength == 126) {
        val lengthBuffer = ByteBuffer.allocate(2)
        if (!readBytesWithTimeout(lengthBuffer, 2, 3000)) { // 3 second timeout
          sendCloseFrame(1002, "Timeout reading 16-bit payload length")
          return
        }

        lengthBuffer.flip()
        payloadLength = lengthBuffer.getShort() & 0xFFFF

        // RFC 6455 Section 5.2: Validate that extended length is actually needed
        if (payloadLength < 126) {
          sendCloseFrame(1002, "Invalid use of 16-bit extended payload length")
          return
        }
      } else if (payloadLength == 127) {
        val lengthBuffer = ByteBuffer.allocate(8)
        if (!readBytesWithTimeout(lengthBuffer, 8, 3000)) { // 3 second timeout
          sendCloseFrame(1002, "Timeout reading 64-bit payload length")
          return
        }

        lengthBuffer.flip()
        payloadLength = lengthBuffer.getLong()

        // RFC 6455 Section 5.2: Check for payload length overflow and validate usage
        if (payloadLength < 0 || payloadLength > Int.MaxValue) {
          sendCloseFrame(1009, "Message too big: payload length overflow")
          return
        }
        if (payloadLength < 65536) {
          sendCloseFrame(1002, "Invalid use of 64-bit extended payload length")
          return
        }
      }

      // Validate control frame constraints (RFC 6455 Section 5.5)
      if (opcode >= 0x8) { // Control frame
        if (!fin) {
          sendCloseFrame(1002, "Protocol error: Control frames must not be fragmented")
          return
        }
        if (payloadLength > 125) {
          sendCloseFrame(1002, "Protocol error: Control frames must have payload <= 125 bytes")
          return
        }
      }

      // RFC 6455 Section 5.1: Server MUST NOT mask frames sent to client
      if (masked) {
        sendCloseFrame(1002, "Protocol error: Server must not mask frames")
        return
      }

      // No masking key needed for server->client frames (RFC 6455 Section 5.1)

      // Read payload with optimized non-blocking I/O
      val payloadSize = payloadLength.toInt
      val payloadBuffer = ByteBuffer.allocate(payloadSize)

      // Calculate timeout based on payload size (minimum 5 seconds, up to 30 seconds for large payloads)
      val payloadTimeoutMs = math.min(30000, math.max(5000, payloadSize / 1024 * 100)) // 100ms per KB

      if (!readBytesWithTimeout(payloadBuffer, payloadSize, payloadTimeoutMs)) {
        sendCloseFrame(1002, "Timeout reading payload")
        return
      }

      payloadBuffer.flip()
      val payload = new Array[Byte](payloadSize)
      payloadBuffer.get(payload)

      // No unmasking needed since server frames are never masked (RFC 6455 Section 5.1)

      // RFC 6455 Section 5.2: Additional security validations
      if (payloadSize > Settings.get.maxNetworkPacketSize * 1024) {
        sendCloseFrame(1009, "Message too big")
        return
      }

      // RFC 6455 Section 5.2: Validate opcode before processing
      if (!isValidOpcode(opcode)) {
        sendCloseFrame(1002, s"Invalid opcode: $opcode")
        return
      }

      // Handle frame based on opcode
      opcode match {
        case 0x1 | 0x2 => // Text or Binary frame
          handleDataFrame(fin, opcode, payload)
        case 0x0 => // Continuation frame
          handleContinuationFrame(fin, payload)
        case 0x8 => // Close frame
          handleCloseFrame(payload)
        case 0x9 => // Ping frame
          // RFC 6455 Section 5.5.2: MUST respond to ping with pong containing same payload
          // Note: Control frame payload size already validated above (must be <= 125 bytes)
          try {
            sendWebSocketFrame(payload, isText = false, opcode = 0xA)
            // Signal to application that ping was received and pong sent
            owner.foreach(_.node.sendToVisible("computer.signal", "websocket_ping", id.toString))
          } catch {
            case e: Exception =>
              // RFC 6455 Section 5.5.2: If we can't send pong, close connection
              sendCloseFrame(1002, "Failed to send pong response")
          }
        case 0xA => // Pong frame
          // RFC 6455 Section 5.5.3: Pong frames MAY be sent unsolicited
          // RFC 6455 Section 5.5.3: Application can use pong for keep-alive detection
          // Note: Control frame payload size already validated above (must be <= 125 bytes)
          // Signal to application that pong was received (for keep-alive tracking)
          owner.foreach(_.node.sendToVisible("computer.signal", "websocket_pong", id.toString))
        case _ => // Unknown frame type
          // RFC 6455 Section 5.2: Unknown opcodes must cause connection failure
          sendCloseFrame(1002, s"Unknown opcode: $opcode")
      }
    }

    // NIO-based helper function for reading bytes with timeout
    private def readBytesWithTimeout(buffer: ByteBuffer, requiredBytes: Int, timeoutMs: Long): Boolean = {
      val future = new CompletableFuture[Boolean]()
      var totalRead = 0

      def attemptRead(): Unit = {
        try {
          if (totalRead >= requiredBytes) {
            future.complete(true)
            return
          }

          val bytesRead = sslRead(buffer)
          if (bytesRead > 0) {
            totalRead += bytesRead
            if (totalRead >= requiredBytes) {
              future.complete(true)
            } else {
              // Schedule next read attempt
              threadPool.schedule(new Runnable {
                override def run(): Unit = attemptRead()
              }, 1, TimeUnit.MILLISECONDS)
            }
          } else if (bytesRead == 0) {
            // Check if connection is still alive
            if (!connected || Thread.currentThread().isInterrupted) {
              future.complete(false)
            } else {
              // Schedule retry
              threadPool.schedule(new Runnable {
                override def run(): Unit = attemptRead()
              }, 10, TimeUnit.MILLISECONDS)
            }
          } else {
            // Connection closed
            future.complete(false)
          }
        } catch {
          case e: Exception => future.complete(false)
        }
      }

      // Start reading
      attemptRead()

      // Set timeout
      threadPool.schedule(new Runnable {
        override def run(): Unit = {
          if (!future.isDone) {
            future.complete(false) // Timeout
          }
        }
      }, timeoutMs, TimeUnit.MILLISECONDS)

      // Wait for completion
      try {
        future.get()
      } catch {
        case _: Exception => false
      }
    }

    private def sendWebSocketFrame(data: Any, isText: Boolean, opcode: Int = -1, fin: Boolean = true): Unit = {
      val payload = data match {
        case s: String => s.getBytes("UTF-8")
        case b: Array[Byte] => b
        case _ => throw new IllegalArgumentException("Invalid data type")
      }

      val actualOpcode = if (opcode != -1) opcode else if (isText) 0x1 else 0x2
      val frame = ByteBuffer.allocate(payload.length + 14) // Max header size + mask key

      // First byte: FIN + RSV(000) + opcode
      // RFC 6455: RSV bits must be 0 unless extension is negotiated
      val finBit = if (fin) 0x80 else 0x00
      val cleanOpcode = actualOpcode & 0x0F // Ensure opcode is only 4 bits
      val firstByte = (finBit | cleanOpcode).toByte // RSV bits are implicitly 0
      frame.put(firstByte)

      // Second byte: MASK + payload length
      if (payload.length < 126) {
        frame.put((0x80 | payload.length).toByte) // Set MASK bit
      } else if (payload.length < 65536) {
        frame.put((0x80 | 126).toByte) // Set MASK bit
        frame.putShort(payload.length.toShort)
      } else {
        frame.put((0x80 | 127).toByte) // Set MASK bit
        frame.putLong(payload.length.toLong)
      }

      // Generate masking key (4 bytes) - RFC 6455 Section 5.3: MUST use cryptographically strong random
      val maskingKey = Array.ofDim[Byte](4)
      new java.security.SecureRandom().nextBytes(maskingKey)
      frame.put(maskingKey)

      // Mask and add payload
      val maskedPayload = payload.zipWithIndex.map { case (byte, index) =>
        (byte ^ maskingKey(index % 4)).toByte
      }
      frame.put(maskedPayload)
      frame.flip()

      sslWrite(frame)
    }

    private def sendWebSocketMessage(data: Array[Byte], isText: Boolean): Unit = {
      val maxFrameSize = 32768 // 32KB max frame size

      if (data.length <= maxFrameSize) {
        // Send as single frame
        sendWebSocketFrame(data, isText)
      } else {
        // Send as fragmented message
        sendFragmentedMessage(data, isText, maxFrameSize)
      }
    }

    private def sendFragmentedMessage(data: Array[Byte], isText: Boolean, fragmentSize: Int): Unit = {
      // RFC 6455 Section 5.4: Validate fragment size
      if (fragmentSize <= 0) {
        throw new IllegalArgumentException("Fragment size must be positive")
      }

      if (data.length == 0) {
        // Send empty frame
        sendWebSocketFrame(data, isText)
        return
      }

      val totalFragments = (data.length + fragmentSize - 1) / fragmentSize

      // RFC 6455 Section 5.4: Prevent excessive fragmentation
      // Limit fragments to prevent DoS attacks and memory exhaustion
      val maxFragments = math.min(1000, Settings.get.maxNetworkPacketSize / 10) // Dynamic limit based on settings
      if (totalFragments > maxFragments) {
        throw new IllegalArgumentException(s"Too many fragments would be created: $totalFragments > $maxFragments")
      }

      for (i <- 0 until totalFragments) {
        val start = i * fragmentSize
        val end = math.min(start + fragmentSize, data.length)
        val fragment = data.slice(start, end)
        val isFirst = i == 0
        val isLast = i == totalFragments - 1

        if (isFirst) {
          // RFC 6455 Section 5.4: First fragment uses original opcode, FIN = false unless single fragment
          val opcodeToUse = if (isText) 0x1 else 0x2
          sendWebSocketFrame(fragment, isText, opcode = opcodeToUse, fin = isLast)
        } else {
          // RFC 6455 Section 5.4: Continuation fragments use opcode 0x0, preserve message type for masking
          sendWebSocketFrame(fragment, isText, opcode = 0x0, fin = isLast)
        }
      }
    }

    private def handleDataFrame(fin: Boolean, opcode: Int, payload: Array[Byte]): Unit = {
      if (fragmentBuffer.isDefined) {
        sendCloseFrame(1002, "Unexpected data frame during fragmentation")
        return
      }

      if (fin) {
        // Complete message in single frame
        // RFC 6455 Section 8.1: UTF-8 validation is done in handleCompleteMessage
        handleCompleteMessage(opcode, payload)
      } else {
        // Start fragmented message
        if (payload.length > maxMessageSize) {
          sendCloseFrame(1009, "Message too big")
          return
        }

        // RFC 6455 Section 5.4: Initialize fragmentation state
        fragmentBuffer = Some(new java.io.ByteArrayOutputStream())
        fragmentOpcode = opcode
        fragmentCount = 1 // Start counting fragments
        fragmentBuffer.get.write(payload)

        // RFC 6455 Section 5.4: Do NOT validate UTF-8 for individual text fragments
        // UTF-8 validation is only done for the complete reassembled message
      }
    }

    private def handleContinuationFrame(fin: Boolean, payload: Array[Byte]): Unit = {
      fragmentBuffer match {
        case Some(buffer) =>
          // RFC 6455 Section 5.4: Check fragment count limit to prevent DoS
          fragmentCount += 1
          if (fragmentCount > maxFragmentCount) {
            sendCloseFrame(1009, "Too many fragments")
            fragmentBuffer = None
            fragmentOpcode = -1
            fragmentCount = 0
            return
          }

          // RFC 6455 Section 5.4: Check message size limit
          if (buffer.size() + payload.length > maxMessageSize) {
            sendCloseFrame(1009, "Message too big")
            fragmentBuffer = None
            fragmentOpcode = -1
            fragmentCount = 0
            return
          }

          buffer.write(payload)

          if (fin) {
            // Complete fragmented message
            val completePayload = buffer.toByteArray

            // RFC 6455 Section 8.1: UTF-8 validation is done in handleCompleteMessage
            handleCompleteMessage(fragmentOpcode, completePayload)

            // Reset fragmentation state
            fragmentBuffer = None
            fragmentOpcode = -1
            fragmentCount = 0
          }

          // RFC 6455 Section 5.4: Do NOT validate UTF-8 for individual continuation frames
          // Only validate the complete reassembled message

        case None =>
          sendCloseFrame(1002, "Unexpected continuation frame")
      }
    }

    private def handleCompleteMessage(opcode: Int, payload: Array[Byte]): Unit = {
      opcode match {
        case 0x1 => // Text message
          try {
            // RFC 6455 Section 8.1: UTF-8 validation - must be strict and complete
            // Use CharsetDecoder for proper UTF-8 validation with strict error handling
            val decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
            decoder.onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            decoder.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)

            // RFC 6455 Section 8.1: Additional validation for complete UTF-8 sequences
            val inputBuffer = java.nio.ByteBuffer.wrap(payload)
            val outputBuffer = java.nio.CharBuffer.allocate(payload.length * 2) // Generous allocation

            val result = decoder.decode(inputBuffer, outputBuffer, true)
            if (result.isError) {
              sendCloseFrame(1007, "Invalid UTF-8 in text frame: " + result.toString)
              return
            }

            // Flush any remaining state
            val flushResult = decoder.flush(outputBuffer)
            if (flushResult.isError) {
              sendCloseFrame(1007, "Invalid UTF-8 in text frame: incomplete sequence")
              return
            }

            // Ensure all bytes were consumed (no incomplete sequences)
            if (inputBuffer.hasRemaining) {
              sendCloseFrame(1007, "Incomplete UTF-8 sequence in text frame")
              return
            }

            outputBuffer.flip()
            val message = outputBuffer.toString

            messageQueue.offer(message)
            owner.foreach(_.node.sendToVisible("computer.signal", "websocket_message", id.toString))
          } catch {
            case _: java.nio.charset.MalformedInputException =>
              sendCloseFrame(1007, "Malformed UTF-8 in text frame")
            case _: java.nio.charset.UnmappableCharacterException =>
              sendCloseFrame(1007, "Unmappable UTF-8 character in text frame")
            case _: java.nio.charset.CharacterCodingException =>
              sendCloseFrame(1007, "Character coding error in text frame")
            case _: Exception =>
              sendCloseFrame(1007, "Invalid UTF-8 in text frame")
          }

        case 0x2 => // Binary message
          binaryQueue.offer(payload)
          owner.foreach(_.node.sendToVisible("computer.signal", "websocket_binary", id.toString))

        case _ =>
          sendCloseFrame(1002, s"Invalid opcode for complete message: $opcode")
      }
    }



    private def isValidRsvBits(rsv: Int): Boolean = {
      // RFC 6455 Section 5.2: RSV bits MUST be 0 unless extension is negotiated
      // For client-only implementation without extension support, ALL RSV bits must be 0
      if (rsv == 0) return true // Always valid

      // RFC 6455 Section 5.2: Since we don't negotiate any extensions in client-only mode,
      // ANY non-zero RSV bits are a protocol violation
      // This is the correct behavior for a strict RFC 6455 client implementation
      false
    }

    private def isValidOpcode(opcode: Int): Boolean = {
      // RFC 6455 Section 5.2: Valid opcodes - strict validation
      if (opcode < 0 || opcode > 15) return false // Must be 4-bit value

      opcode match {
        // Data frames
        case 0x0 | 0x1 | 0x2 => true // Continuation, Text, Binary
        // Control frames
        case 0x8 | 0x9 | 0xA => true // Close, Ping, Pong
        // Reserved opcodes - RFC 6455 Section 5.2: MUST cause connection failure
        case 0x3 | 0x4 | 0x5 | 0x6 | 0x7 => false // Reserved for future non-control frames
        case 0xB | 0xC | 0xD | 0xE | 0xF => false // Reserved for future control frames
        // Invalid range
        case _ => false
      }
    }

    private def isValidCloseCode(code: Int): Boolean = {
      // RFC 6455 Section 7.4.1: Valid close codes - strict RFC 6455 compliance
      if (code < 0 || code > 65535) return false // Must be 16-bit value

      code match {
        // RFC 6455 Section 7.4.1: Standard close codes defined in RFC 6455
        case 1000 | 1001 | 1002 | 1003 | 1007 | 1008 | 1009 | 1010 | 1011 => true
        // RFC 6455 Section 7.4.1: Reserved codes that MUST NOT be used
        case 1004 | 1005 | 1006 => false
        // RFC 6455 Section 7.4.1: 1015 is reserved and MUST NOT be sent in close frame
        case 1015 => false // TLS handshake failure (reserved, MUST NOT be sent)
        // RFC 6455 Section 7.4.1: Codes 1012-1014 are NOT defined in RFC 6455
        // These codes were defined in later specifications (RFC 7692, etc.)
        // For strict RFC 6455 compliance, these codes must be rejected
        case c if c >= 1012 && c <= 1014 => false // Strict RFC 6455 compliance
        case c if c >= 1016 && c <= 2999 => false // Reserved for future WebSocket standard
        // RFC 6455 Section 7.4.2: Application-specific codes
        case c if c >= 3000 && c <= 3999 => true
        // RFC 6455 Section 7.4.2: Private use codes
        case c if c >= 4000 && c <= 4999 => true
        // RFC 6455 Section 7.4.1: Invalid range
        case c if c >= 0 && c <= 999 => false // Invalid range
        case _ => false
      }
    }

    private def failWebSocketConnection(reason: String): Unit = {
      // RFC 6455 Section 7.1.7: Fail the WebSocket Connection
      try {
        owner.foreach(_.node.sendToVisible("computer.signal", "websocket_error", id.toString, reason))
      } catch {
        case _: Exception => // Ignore signal errors during failure
      }
      close()
    }

    private def sendCloseFrame(statusCode: Int, reason: String = ""): Unit = {
      sendCloseFrameOnly(statusCode, reason)
      close()
    }

    private def sendCloseFrameOnly(statusCode: Int, reason: String = ""): Unit = {
      try {
        // RFC 6455 Section 7.4.1: Validate close code before sending
        if (!isValidCloseCode(statusCode)) {
          // Use 1002 (protocol error) for invalid codes
          val payload = ByteBuffer.allocate(2)
          payload.putShort(1002.toShort)
          sendWebSocketFrame(payload.array(), isText = false, opcode = 0x8)
          return
        }

        // RFC 6455 Section 7.4.1: Validate UTF-8 encoding of reason before sending
        val reasonBytes = try {
          val bytes = reason.getBytes("UTF-8")
          // Validate that the bytes can be decoded back to the same string
          val decoded = new String(bytes, "UTF-8")
          if (decoded != reason) {
            // UTF-8 encoding/decoding mismatch, send without reason
            Array.empty[Byte]
          } else {
            bytes
          }
        } catch {
          case _: Exception =>
            // UTF-8 encoding failed, send without reason
            Array.empty[Byte]
        }

        // RFC 6455 Section 7.4.1: Close frame payload must be <= 125 bytes
        if (2 + reasonBytes.length > 125) {
          // Send close frame without reason if too long
          val payload = ByteBuffer.allocate(2)
          payload.putShort(statusCode.toShort)
          sendWebSocketFrame(payload.array(), isText = false, opcode = 0x8)
          return
        }

        val payload = ByteBuffer.allocate(2 + reasonBytes.length)
        payload.putShort(statusCode.toShort)
        payload.put(reasonBytes)
        sendWebSocketFrame(payload.array(), isText = false, opcode = 0x8)
      } catch {
        case _: Exception => // Ignore errors when sending close frame
      }
    }

    private def handleCloseFrame(payload: Array[Byte]): Unit = {
      var statusCode = 1000 // Normal closure
      var reason = ""

      if (payload.length >= 2) {
        val buffer = ByteBuffer.wrap(payload)
        statusCode = buffer.getShort() & 0xFFFF

        // RFC 6455 Section 7.4.1: Validate close code
        if (!isValidCloseCode(statusCode)) {
          // Invalid close code - fail the connection
          failWebSocketConnection("Invalid close code")
          return
        }

        if (payload.length > 2) {
          val reasonBytes = new Array[Byte](payload.length - 2)
          buffer.get(reasonBytes)

          // RFC 6455 Section 7.4.1: Reason must be valid UTF-8 - strict validation
          try {
            val decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
            decoder.onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            decoder.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)

            val charBuffer = decoder.decode(java.nio.ByteBuffer.wrap(reasonBytes))
            reason = charBuffer.toString
          } catch {
            case _: Exception =>
              failWebSocketConnection("Invalid UTF-8 in close reason")
              return
          }
        }
      } else if (payload.length == 1) {
        // RFC 6455 Section 7.4.1: Close frame with 1 byte payload is invalid
        failWebSocketConnection("Invalid close frame payload length")
        return
      }

      // RFC 6455 Section 7.1.2: Close handshake handling
      closeFrameReceived = true

      if (!closeFrameSent) {
        // RFC 6455 Section 7.1.2: Send close frame in response if we haven't sent one yet
        try {
          closeFrameSent = true
          sendCloseFrameOnly(statusCode, "")
        } catch {
          case _: Exception => // Ignore errors when sending close response
        }
      }

      // RFC 6455 Section 7.1.3: Close the connection after close handshake
      // Cancel any pending close timeout since we received the close frame
      closeTimeout.foreach(_.cancel(true))
      forceClose()
    }

    private class WebSocketConnector extends Callable[SocketChannel] {
      override def call(): SocketChannel = {
        checkLists(InetAddress.getByName(url.getHost), url.getHost)
        val port = if (url.getPort != -1) url.getPort else if (isSecure) 443 else 80
        val address = new InetSocketAddress(url.getHost, port)

        val socketChannel = SocketChannel.open()
        // RFC 6455 compliance: Use non-blocking sockets to prevent server thread blocking
        socketChannel.configureBlocking(false)

        // Non-blocking connect with timeout
        val connected = socketChannel.connect(address)
        if (!connected) {
          // NIO-based connection completion with timeout
          finishConnectionNonBlocking(socketChannel, Settings.get.httpTimeout.max(5000))
        }

        socketChannel
      }
    }

    private def finishConnectionNonBlocking(socketChannel: SocketChannel, timeoutMs: Long): Unit = {
      val future = new CompletableFuture[Unit]()

      def attemptFinish(): Unit = {
        try {
          if (socketChannel.finishConnect()) {
            future.complete(())
          } else {
            // Schedule retry
            threadPool.schedule(new Runnable {
              override def run(): Unit = attemptFinish()
            }, 10, TimeUnit.MILLISECONDS)
          }
        } catch {
          case e: Exception => future.completeExceptionally(e)
        }
      }

      // Start connection attempt
      attemptFinish()

      // Set timeout
      threadPool.schedule(new Runnable {
        override def run(): Unit = {
          if (!future.isDone) {
            future.completeExceptionally(new IOException("WebSocket connection timeout"))
          }
        }
      }, timeoutMs, TimeUnit.MILLISECONDS)

      // Wait for completion
      try {
        future.get()
      } catch {
        case e: ExecutionException => throw e.getCause
      }
    }
  }

}
