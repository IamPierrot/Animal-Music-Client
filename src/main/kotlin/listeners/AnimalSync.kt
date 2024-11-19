package dev.pierrot.listeners

import com.microsoft.signalr.*
import dev.pierrot.config
import dev.pierrot.getLogger
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.serialization.builtins.serializer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AnimalSync private constructor(val clientId: Int) {
    companion object {
        private val logger = getLogger("AnimalSync")
        private val HUB_URL = config.app.websocket
        private val RETRY_DELAYS = listOf(0L, 2000L, 5000L, 10000L, 30000L)
        private const val MAX_RETRY_ATTEMPTS = -1

        @Volatile
        private var instance: AnimalSync? = null
        private val LOCK = Any()

        fun initialize(clientId: Int) {
            if (instance == null) {
                synchronized(LOCK) {
                    if (instance == null) {
                        instance = AnimalSync(clientId)
                    }
                }
            }
        }

        fun getInstance(): AnimalSync {
            return instance ?: throw IllegalStateException(
                "AnimalSync must be initialized with clientId before using"
            )
        }
    }

    private val hubConnection: HubConnection
    private var reconnectDisposable: Disposable? = null
    private val retryAttempt = AtomicInteger(0)
    private var isReconnecting = false

    // Callback map để xử lý các events
    private val messageHandlers = mutableMapOf<String, (Map<String, Any>) -> Unit>()

    init {
        hubConnection = buildConnection()
        setupEventListeners()
        connect()
    }

    private fun buildConnection(): HubConnection {
        val headers = hashMapOf("Secret" to "123")
        return HubConnectionBuilder.create("$HUB_URL?ClientId=$clientId")
            .withHeaders(headers)
//            .withKeepAliveInterval(100000)
//            .withHandshakeResponseTimeout(30000)
            .build()
    }

    fun registerMessageHandler(messageType: String, handler: (Map<String, Any>) -> Unit) {
        messageHandlers[messageType] = handler
    }

    private fun setupEventListeners() {
        hubConnection.on(
            "connection",
            { message: String ->
                logger.info("Connection message: $message")
                retryAttempt.set(0)
            },
            String::class.java
        )

        hubConnection.on(
            "error",
            { error: Any ->
                logger.error("Error received: $error")
            },
            Any::class.java
        )

        hubConnection.on(
            "disconnect",
            { reason: String ->
                logger.warn("Disconnected: $reason")
                startReconnecting()
            },
            String::class.java
        )

        hubConnection.on(
            "msg",
            { message: Map<String, Any> ->
                messageHandlers["msg"]?.invoke(message)
            },
            Map::class.java
        )

        hubConnection.on(
            "handle_no_client",
            { message: Map<String, Any> ->
                messageHandlers["handle_no_client"]?.invoke(message)
            },
            Map::class.java
        )

        hubConnection.onClosed { exception ->
            exception?.let {
                logger.error("Connection closed with error: ${it.message}")
            } ?: logger.warn("Connection closed")

            startReconnecting()
        }
    }

    private fun connect() {
        if (hubConnection.connectionState != HubConnectionState.CONNECTED) {
            hubConnection.start()
                .doOnSubscribe { logger.info("Attempting to connect...") }
                .subscribe(
                    {
                        logger.info("Connected successfully")
                        isReconnecting = false
                        retryAttempt.set(0)
                    },
                    { error ->
                        logger.error("Failed to connect: ${error.message}")
                        startReconnecting()
                    }
                )
        }
    }

    private fun startReconnecting() {
        if (!isReconnecting && hubConnection.connectionState != HubConnectionState.CONNECTED) {
            isReconnecting = true
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (MAX_RETRY_ATTEMPTS != -1 && retryAttempt.get() >= MAX_RETRY_ATTEMPTS) {
            logger.error("Maximum retry attempts reached")
            isReconnecting = false
            return
        }

        val currentAttempt = retryAttempt.getAndIncrement()
        val delay = if (currentAttempt < RETRY_DELAYS.size) {
            RETRY_DELAYS[currentAttempt]
        } else {
            RETRY_DELAYS.last()
        }

        reconnectDisposable?.dispose()
        reconnectDisposable = Single.timer(delay, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    logger.info("Reconnection attempt #${currentAttempt + 1}")
                    connect()
                },
                { error ->
                    logger.error("Error during reconnect attempt #${currentAttempt + 1}: ${error.message}")
                    scheduleReconnect()
                }
            )
    }

    fun send(method: String, vararg args: Any) {
        try {
            hubConnection.send(method, *args)
            logger.info("Sent method: $method with args: ${args.joinToString()}")
        } catch (e: Exception) {
            logger.error("Error sending message: ${e.message}")
        }
    }

    fun <T : Any> invoke(method: String, returnType: Class<T>, vararg args: Any): Single<T> {
        return hubConnection.invoke(returnType, method, *args)
    }

    fun dispose() {
        isReconnecting = false
        reconnectDisposable?.dispose()
        hubConnection.stop()
        instance = null
    }
}