package dev.pierrot.listeners

import com.microsoft.signalr.*
import dev.pierrot.config
import dev.pierrot.getLogger
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

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

    private var hubConnection: HubConnection
    private var reconnectDisposable: Disposable? = null
    private val retryAttempt = AtomicInteger(0)
    private val isReconnecting = AtomicBoolean(false)

    private val subscriptions = mutableMapOf<String, MutableList<Subscription>>()

    init {
        hubConnection = buildConnection()
        setupBaseEventListeners()
        connect()
    }

    private fun buildConnection(): HubConnection {
        val headers = hashMapOf("Secret" to "123")
        return HubConnectionBuilder.create("$HUB_URL?ClientId=$clientId")
            .withHeaders(headers)
            .withKeepAliveInterval(-1)
            .build()
    }

    fun <T> on(eventName: String, handler: (T) -> Unit, clazz: Class<T>): Subscription {
        val subscription = hubConnection.on(eventName, handler, clazz)
        subscriptions.getOrPut(eventName) { mutableListOf() }.add(subscription)
        return subscription
    }

    fun onString(eventName: String, handler: (String) -> Unit): Subscription {
        return on(eventName, handler, String::class.java)
    }

    fun onMap(eventName: String, handler: (Map<String, Any>) -> Unit): Subscription {
        @Suppress("UNCHECKED_CAST")
        return on(eventName, handler, Map::class.java as Class<Map<String, Any>>)
    }

    fun onAny(eventName: String, handler: (Any) -> Unit): Subscription {
        return on(eventName, handler, Any::class.java)
    }

    fun off(eventName: String, subscription: Subscription) {
        subscription.unsubscribe()
        subscriptions[eventName]?.remove(subscription)
    }

    fun off(eventName: String) {
        subscriptions[eventName]?.forEach { it.unsubscribe() }
        subscriptions.remove(eventName)
    }

    fun offAll() {
        subscriptions.forEach { (_, subs) ->
            subs.forEach { it.unsubscribe() }
        }
        subscriptions.clear()
    }

    private fun setupBaseEventListeners() {
        onString("connection") { message ->
            logger.info("Connection message: $message")
            retryAttempt.set(0)
            isReconnecting.set(false)
        }

        onAny("error") { error ->
            logger.error("Error received: $error")
            checkConnectionAndReconnect()
        }

        onString("disconnect") { reason ->
            logger.warn("Disconnected: $reason")
            checkConnectionAndReconnect()
        }

        hubConnection.onClosed { exception ->
            exception?.let {
                logger.error("Connection closed with error: ${it.message}")
            } ?: logger.warn("Connection closed")

            checkConnectionAndReconnect()
        }
    }

    private fun checkConnectionAndReconnect() {
        if (hubConnection.connectionState != HubConnectionState.CONNECTED &&
            !isReconnecting.get()) {
            startReconnecting()
        }
    }

    private fun connect() {
        if (hubConnection.connectionState != HubConnectionState.CONNECTED) {
            hubConnection.start()
                .doOnSubscribe {
                    logger.info("Attempting to connect...")
                    isReconnecting.set(true)
                }
                .subscribe(
                    {
                        logger.info("Connected successfully")
                        isReconnecting.set(false)
                        retryAttempt.set(0)
                        reconnectDisposable?.dispose()
                    },
                    { error ->
                        logger.error("Failed to connect: ${error.message}")
                        startReconnecting()
                    }
                )
        }
    }

    private fun startReconnecting() {
        if (isReconnecting.compareAndSet(false, true)) {
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (MAX_RETRY_ATTEMPTS != -1 && retryAttempt.get() >= MAX_RETRY_ATTEMPTS) {
            logger.error("Maximum retry attempts reached")
            isReconnecting.set(false)
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
                    if (hubConnection.connectionState == HubConnectionState.DISCONNECTED) {
                        hubConnection = buildConnection()
                        setupBaseEventListeners()
                    }
                    connect()
                },
                { error ->
                    logger.error("Error during reconnect attempt #${currentAttempt + 1}: ${error.message}")
                    isReconnecting.set(false)
                    scheduleReconnect()
                }
            )
    }

    fun send(method: String, vararg args: Any) {
        try {
            if (hubConnection.connectionState == HubConnectionState.CONNECTED) {
                hubConnection.send(method, *args)
//                logger.info("Sent method: $method with args: ${args.joinToString()}")
            } else {
                logger.warn("Cannot send message - connection is not established")
                checkConnectionAndReconnect()
            }
        } catch (e: Exception) {
            logger.error("Error sending message: ${e.message}")
            checkConnectionAndReconnect()
        }
    }

    fun <T : Any> invoke(method: String, returnType: Class<T>, vararg args: Any): Single<T> {
        return if (hubConnection.connectionState == HubConnectionState.CONNECTED) {
            hubConnection.invoke(returnType, method, *args)
        } else {
            Single.error(IllegalStateException("Connection is not established"))
        }
    }

    fun dispose() {
        offAll()
        isReconnecting.set(false)
        reconnectDisposable?.dispose()
        hubConnection.stop()
        instance = null
    }
}