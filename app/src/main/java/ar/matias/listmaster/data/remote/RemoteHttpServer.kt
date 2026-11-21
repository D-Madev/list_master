package ar.matias.listmaster.data.remote

import android.content.Context
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * Servidor HTTP chico para la sesión web. Sirve la misma PWA de la carpeta
 * `pwa/` (copiada a assets al compilar) y una API de dos rutas:
 *
 * - `GET /api/state?code=...` devuelve el respaldo JSON y su versión.
 * - `POST /api/state?code=...` con `{ "version": n, "datos": {...} }` guarda
 *   todo en el celular. Si la versión no coincide responde 409 y el navegador
 *   vuelve a leer, así nadie pisa cambios ajenos.
 *
 * No hay base de datos ni cuenta: el celular sigue siendo el único lugar donde
 * viven los datos.
 */
internal class RemoteHttpServer(
    private val context: Context,
    private val code: String,
    private val currentJson: suspend () -> String,
    private val importJson: suspend (String) -> Unit,
) {

    private val versionCounter = AtomicLong(1L)

    val version: Long
        get() = versionCounter.get()

    var port: Int = 0
        private set

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private val files = ConcurrentHashMap<String, ByteArray>()

    /** Abre el puerto y empieza a atender. Devuelve el puerto elegido. */
    fun start(scope: CoroutineScope): Int {
        val socket = runCatching { ServerSocket(DEFAULT_PORT) }
            .getOrElse { ServerSocket(0) }
        serverSocket = socket
        port = socket.localPort
        acceptJob = scope.launch(Dispatchers.IO) { acceptLoop(socket, scope) }
        return port
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    /** Marca que el celular cambió algo, para que el navegador se entere. */
    fun bumpVersion() {
        versionCounter.incrementAndGet()
    }

    private fun acceptLoop(socket: ServerSocket, scope: CoroutineScope) {
        while (!socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (closed: IOException) {
                return
            } catch (error: Exception) {
                continue
            }
            // Cada conexión vive poco: se atiende y se cierra.
            scope.launch { handle(client) }
        }
    }

    private suspend fun handle(client: Socket) = withContext(Dispatchers.IO) {
        client.use { socket ->
            runCatching {
                socket.soTimeout = READ_TIMEOUT_MS
                val input = BufferedInputStream(socket.getInputStream())
                val head = readHead(input) ?: return@use
                val lines = head.split("\r\n")
                val request = lines.firstOrNull()?.split(" ") ?: return@use
                if (request.size < 2) return@use
                val method = request[0]
                val target = request[1]
                val path = target.substringBefore('?')
                val query = parseQuery(target.substringAfter('?', ""))
                val length = lines
                    .firstOrNull { it.startsWith("content-length:", ignoreCase = true) }
                    ?.substringAfter(':')
                    ?.trim()
                    ?.toIntOrNull()
                    ?: 0
                val body = if (length in 1..MAX_BODY_BYTES) readBody(input, length) else ""
                respond(socket.getOutputStream(), method, path, query, body)
            }.onFailure { error ->
                if (error !is IOException) error.printStackTrace()
            }
        }
    }

    private suspend fun respond(
        output: OutputStream,
        method: String,
        path: String,
        query: Map<String, String>,
        body: String,
    ) {
        if (path.startsWith("/api/")) {
            api(output, method, path, query, body)
            return
        }
        val asset = when (path) {
            "/", "/index.html", "" -> INDEX
            else -> path.removePrefix("/")
        }
        val bytes = if (asset == INDEX) indexHtml() else readAsset(asset)
        if (bytes == null) {
            writeText(output, 404, "application/json; charset=utf-8", """{"error":"No encontrado"}""")
            return
        }
        write(output, 200, contentType(asset), bytes, cache = !asset.endsWith(".html"))
    }

    private suspend fun api(
        output: OutputStream,
        method: String,
        path: String,
        query: Map<String, String>,
        body: String,
    ) {
        if (query["code"] != code) {
            writeText(output, 403, JSON, """{"error":"Código inválido o sesión cerrada."}""")
            return
        }
        when {
            path == "/api/session" && method == "GET" -> {
                writeText(output, 200, JSON, """{"ok":true,"version":$version}""")
            }

            path == "/api/state" && method == "GET" -> {
                val json = currentJson()
                writeText(output, 200, JSON, """{"version":$version,"datos":$json}""", cache = false)
            }

            path == "/api/state" && method == "POST" -> save(output, body)

            else -> writeText(output, 404, JSON, """{"error":"No encontrado"}""")
        }
    }

    private suspend fun save(output: OutputStream, body: String) {
        val payload = try {
            JSONObject(body)
        } catch (error: JSONException) {
            writeText(output, 400, JSON, """{"error":"Pedido inválido."}""")
            return
        }
        val sent = payload.optLong("version", -1L)
        if (sent != version) {
            writeText(output, 409, JSON, """{"version":$version}""")
            return
        }
        val datos = payload.optJSONObject("datos")
        if (datos == null) {
            writeText(output, 400, JSON, """{"error":"Faltan los datos."}""")
            return
        }
        try {
            importJson(datos.toString())
        } catch (error: Exception) {
            val message = error.message ?: "No se pudo guardar."
            writeText(output, 400, JSON, JSONObject.quote(message).let { """{"error":$it}""" })
            return
        }
        versionCounter.incrementAndGet()
        writeText(output, 200, JSON, """{"version":$version}""")
    }

    private fun indexHtml(): ByteArray {
        files[INDEX]?.let { return it }
        val raw = context.assets.open("pwa/index.html").bufferedReader().use { it.readText() }
        // CSP: script-src 'self', así que el modo remoto entra como archivo.
        val patched = if (raw.contains("remote.js")) {
            raw
        } else {
            raw.replace("</head>", "    <script type=\"module\" src=\"/js/remote.js\"></script>\n  </head>")
        }
        val bytes = patched.toByteArray(Charsets.UTF_8)
        files[INDEX] = bytes
        return bytes
    }

    private fun readAsset(path: String): ByteArray? {
        files[path]?.let { return it }
        val safe = path.replace("\\", "/")
        if (safe.contains("..")) return null
        val bytes = runCatching {
            context.assets.open("pwa/$safe").use { it.readBytes() }
        }.getOrNull() ?: return null
        files[safe] = bytes
        return bytes
    }

    private fun readHead(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        var matched = 0
        while (buffer.size() < MAX_HEAD_BYTES) {
            val byte = input.read()
            if (byte == -1) return null
            buffer.write(byte)
            matched = when {
                matched == 0 && byte == '\r'.code -> 1
                matched == 1 && byte == '\n'.code -> 2
                matched == 2 && byte == '\r'.code -> 3
                matched == 3 && byte == '\n'.code -> 4
                byte == '\r'.code -> 1
                else -> 0
            }
            if (matched == 4) return buffer.toString("UTF-8")
        }
        return null
    }

    private fun readBody(input: InputStream, length: Int): String {
        val buffer = ByteArray(length)
        var read = 0
        while (read < length) {
            val chunk = input.read(buffer, read, length - read)
            if (chunk == -1) break
            read += chunk
        }
        return String(buffer, 0, read, Charsets.UTF_8)
    }

    private fun parseQuery(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split('&').mapNotNull { pair ->
            val index = pair.indexOf('=')
            if (index <= 0) return@mapNotNull null
            val key = decodeQuery(pair.substring(0, index))
            val value = decodeQuery(pair.substring(index + 1))
            key to value
        }.toMap()
    }

    private fun decodeQuery(value: String): String =
        runCatching { java.net.URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    private fun writeText(
        output: OutputStream,
        status: Int,
        type: String,
        body: String,
        cache: Boolean = false,
    ) {
        write(output, status, type, body.toByteArray(Charsets.UTF_8), cache)
    }

    private fun write(
        output: OutputStream,
        status: Int,
        type: String,
        body: ByteArray,
        cache: Boolean,
    ) {
        val reason = when (status) {
            200 -> "OK"
            400 -> "Bad Request"
            403 -> "Forbidden"
            404 -> "Not Found"
            409 -> "Conflict"
            else -> "Error"
        }
        val headers = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Type: $type\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Cache-Control: ${if (cache) "public, max-age=3600" else "no-store"}\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(headers.toByteArray(Charsets.US_ASCII))
        output.write(body)
        output.flush()
    }

    private fun contentType(path: String): String = when {
        path.endsWith(".html") -> "text/html; charset=utf-8"
        path.endsWith(".css") -> "text/css; charset=utf-8"
        path.endsWith(".js") -> "text/javascript; charset=utf-8"
        path.endsWith(".json") || path.endsWith(".webmanifest") -> JSON
        path.endsWith(".svg") -> "image/svg+xml"
        path.endsWith(".png") -> "image/png"
        path.endsWith(".ttf") -> "font/ttf"
        else -> "application/octet-stream"
    }

    private companion object {
        const val DEFAULT_PORT = 8080
        const val READ_TIMEOUT_MS = 10_000
        const val MAX_HEAD_BYTES = 8_192
        const val MAX_BODY_BYTES = 16 * 1024 * 1024
        const val INDEX = "index.html"
        const val JSON = "application/json; charset=utf-8"
    }
}

/** Primera IPv4 de la red local, que es la que alcanza el navegador. */
internal fun localIpv4(): String? = runCatching {
    NetworkInterface.getNetworkInterfaces()
        .asSequence()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { network -> network.inetAddresses.asSequence() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { it.isSiteLocalAddress }
        ?.hostAddress
}.getOrNull()
