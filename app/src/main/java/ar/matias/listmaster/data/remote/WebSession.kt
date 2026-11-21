package ar.matias.listmaster.data.remote

import android.content.Context
import ar.matias.listmaster.data.BackupCodec
import ar.matias.listmaster.data.ListMasterRepository
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sesión web desde el celular: abre un enlace por un rato y desde el navegador
 * se ve y se edita lo mismo que en la app. El celular sigue siendo la base; no
 * hay servidor ni cuenta en internet.
 */
class WebSession(
    private val context: Context,
    private val repository: ListMasterRepository,
) {

    /** Lo que la pantalla de Ajustes necesita mostrar. */
    data class Snapshot(
        val url: String,
        val code: String,
        val expiresAt: Long,
    ) {
        fun remainsMillis(now: Long = System.currentTimeMillis()): Long = expiresAt - now
    }

    sealed interface Result {
        data class Started(val snapshot: Snapshot) : Result
        data class Failed(val reason: Reason) : Result
    }

    enum class Reason { NO_NETWORK, UNAVAILABLE }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _snapshot = MutableStateFlow<Snapshot?>(null)
    val snapshot: StateFlow<Snapshot?> = _snapshot.asStateFlow()

    private var server: RemoteHttpServer? = null
    private var expiryJob: Job? = null
    private val pendingEcho = AtomicInteger(0)

    /**
     * Abre la sesión por [durationMillis]. Devuelve el enlace para compartir o
     * el motivo por el que no se pudo.
     */
    suspend fun start(durationMillis: Long): Result {
        stop()
        val address = localIpv4() ?: return Result.Failed(Reason.NO_NETWORK)
        val code = "%06d".format(Random.nextInt(0, 1_000_000))
        val current = RemoteHttpServer(
            context = context,
            code = code,
            currentJson = { BackupCodec.encode(repository.snapshot()) },
            importJson = { json ->
                // Lo que llega del navegador ya está guardado: la próxima
                // emisión de datos es el eco de esta escritura, no un cambio
                // hecho en el celular, así que no hay que avisar de nuevo.
                // Se marca antes de escribir para que el aviso llegue a tiempo
                // aunque Room emita mientras la transacción todavía corre.
                pendingEcho.incrementAndGet()
                try {
                    repository.replaceAll(BackupCodec.decode(json))
                } catch (error: Exception) {
                    pendingEcho.decrementAndGet()
                    throw error
                }
            },
        )
        val port = runCatching { current.start(scope) }.getOrElse {
            current.stop()
            return Result.Failed(Reason.UNAVAILABLE)
        }
        server = current
        val snapshot = Snapshot(
            url = "http://$address:$port",
            code = code,
            expiresAt = System.currentTimeMillis() + durationMillis,
        )
        _snapshot.value = snapshot
        expiryJob = scope.launch {
            delay(durationMillis)
            stop()
        }
        return Result.Started(snapshot)
    }

    fun stop() {
        expiryJob?.cancel()
        expiryJob = null
        server?.stop()
        server = null
        _snapshot.value = null
    }

    /** El celular cambió algo: el navegador tiene que volver a leer. */
    fun notifyLocalChange() {
        if (pendingEcho.getAndUpdate { if (it > 0) it - 1 else 0 } > 0) return
        server?.bumpVersion()
    }
}
