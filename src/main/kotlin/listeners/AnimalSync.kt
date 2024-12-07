package dev.pierrot.listeners

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.microsoft.signalr.Subscription
import dev.pierrot.config
import dev.pierrot.getLogger
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AnimalSync private constructor(val clientId: Int) {
    lateinit var clientConnectionId: String

    companion object {
        private val logger = getLogger("AnimalSync")
        private val HUB_URL = config.app.websocket
        private val RETRY_DELAYS = listOf(0L, 2000L, 5000L, 10000L, 30000L)
        private const val MAX_RETRY_ATTEMPTS = -1

        @Volatile
        private var instance: AnimalSync? = null
        private val LOCK = Any()

        fun initialize(clientId: Int) {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = AnimalSync(clientId)
                }
            }
        }

        fun getInstance(): AnimalSync {
            return instance ?: throw IllegalStateException(
                "AnimalSync must be initialized with clientId before using"
            )
        }
    }

    private val pendingEvents = ConcurrentLinkedQueue<PendingEvent>()
    private val pendingSubscriptions = ConcurrentLinkedQueue<PendingSubscription>()

    data class PendingEvent(
        val method: String,
        val args: Array<out Any?>,
        val type: EventType,
        val returnType: Class<*>? = null,
        val continuation: ((Any?) -> Unit)? = null
    ) {
        enum class EventType {
            SEND, INVOKE
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PendingEvent

            if (method != other.method) return false
            if (!args.contentEquals(other.args)) return false
            if (type != other.type) return false
            if (returnType != other.returnType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = method.hashCode()
            result = 31 * result + args.contentHashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + (returnType?.hashCode() ?: 0)
            return result
        }
    }

    data class PendingSubscription(
        val eventName: String,
        val handler: Any,
        val clazz: Class<*>
    )

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
            .build()
    }

    private fun processPendingEvents() {
        val iterator = pendingEvents.iterator()
        while (iterator.hasNext()) {
            val pendingEvent = iterator.next()
            try {
                when (pendingEvent.type) {
                    PendingEvent.EventType.SEND -> {
                        hubConnection.send(pendingEvent.method, *pendingEvent.args)
                        logger.info("Resent queued send event: ${pendingEvent.method}")
                    }

                    PendingEvent.EventType.INVOKE -> {
                        if (pendingEvent.returnType == null) {
                            logger.warn("Invoke event missing return type: ${pendingEvent.method}")
                            continue
                        }

                        hubConnection.invoke(pendingEvent.returnType, pendingEvent.method, *pendingEvent.args)
                            .subscribe(
                                { result ->
                                    logger.info("Successfully invoked queued event: ${pendingEvent.method}")
                                    pendingEvent.continuation?.invoke(result)
                                },
                                { error ->
                                    logger.error("Failed to invoke queued event: ${pendingEvent.method}, error: ${error.message}")
                                    pendingEvent.continuation?.invoke(null)
                                }
                            )
                    }
                }
                iterator.remove()
            } catch (e: Exception) {
                logger.error("Error processing pending event: ${e.message}")
            }
        }
    }

    private fun processPendingSubscriptions() {
        val iterator = pendingSubscriptions.iterator()
        while (iterator.hasNext()) {
            val pendingSub = iterator.next()
            try {
                @Suppress("UNCHECKED_CAST")
                when (pendingSub.clazz) {
                    String::class.java -> {
                        val handler = pendingSub.handler as (String) -> Unit
                        hubConnection.on(pendingSub.eventName, handler, String::class.java)
                    }

                    Map::class.java -> {
                        @Suppress("UNCHECKED_CAST")
                        val handler = pendingSub.handler as (Map<String, Any>) -> Unit
                        hubConnection.on(pendingSub.eventName, handler, Map::class.java as Class<Map<String, Any>>)
                    }

                    Any::class.java -> {
                        val handler = pendingSub.handler as (Any) -> Unit
                        hubConnection.on(pendingSub.eventName, handler, Any::class.java)
                    }

                    else -> {
                        logger.warn("Unsupported handler type for event: ${pendingSub.eventName}")
                        continue
                    }
                }
                iterator.remove()
            } catch (e: Exception) {
                logger.error("Error restoring subscription: ${e.message}")
            }
        }
    }

    private fun setupBaseEventListeners() {
        onString("connection") { message ->
            clientConnectionId = message
            retryAttempt.set(0)
            isReconnecting.set(false)
            processPendingSubscriptions()
            processPendingEvents()
        }

        onAny("error") { error ->
            logger.error("Error received: $error")
        }

        onString("disconnect") { reason ->
            logger.warn("Disconnected: $reason")
            startReconnecting()
        }

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
                        processPendingSubscriptions()
                        processPendingEvents()
                    },
                    { error ->
                        logger.warn("Failed to connect: ${error.message}")
                        startReconnecting()
                    }
                )
        }
    }

    private fun startReconnecting() {
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (MAX_RETRY_ATTEMPTS != -1 && retryAttempt.get() >= MAX_RETRY_ATTEMPTS) {
            logger.warn("Maximum retry attempts reached")
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

    fun <T> on(eventName: String, handler: (T) -> Unit, clazz: Class<T>): Subscription {
        return if (hubConnection.connectionState == HubConnectionState.CONNECTED) {
            val subscription = hubConnection.on(eventName, handler, clazz)
            subscriptions.getOrPut(eventName) { mutableListOf() }.add(subscription)
            subscription
        } else {
            logger.warn("Connection not established. Queuing subscription for $eventName")
            pendingSubscriptions.add(PendingSubscription(eventName, handler, clazz))
            startReconnecting()

            hubConnection.on("lmao") {}
        }
    }

    fun onString(eventName: String, handler: (String) -> Unit): Subscription {
        return on(eventName, handler, String::class.java)
    }

    fun onMap(eventName: String, handler: (Map<String, Any>) -> Unit): Subscription? {
        try {
            @Suppress("UNCHECKED_CAST")
            return on(eventName, handler, Map::class.java as Class<Map<String, Any>>)
        } catch (err: Exception) {
            return null
        }
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

    private fun offAll() {
        subscriptions.forEach { (_, subs) ->
            subs.forEach { it.unsubscribe() }
        }
        subscriptions.clear()
    }

    fun send(method: String, vararg args: Any?) {
        try {
            if (hubConnection.connectionState == HubConnectionState.CONNECTED) {
                hubConnection.send(method, *args)
            } else {
                logger.warn("Connection not established. Queuing event.")
                pendingEvents.offer(
                    PendingEvent(
                        method,
                        args,
                        PendingEvent.EventType.SEND
                    )
                )
                startReconnecting()
            }
        } catch (e: Exception) {
            logger.error("Error sending message: ${e.message}")
            pendingEvents.offer(
                PendingEvent(
                    method,
                    args,
                    PendingEvent.EventType.SEND
                )
            )
        }
    }

    fun <T : Any> invoke(method: String, returnType: Class<T>, vararg args: Any): Single<T> {
        return if (hubConnection.connectionState == HubConnectionState.CONNECTED) {
            hubConnection.invoke(returnType, method, *args)
        } else {
            Single.create { emitter ->
                logger.warn("Connection not established. Queuing invoke event.")

                val pendingEvent = PendingEvent(
                    method,
                    args,
                    PendingEvent.EventType.INVOKE,
                    returnType
                ) { result ->
                    if (result != null && returnType.isInstance(result)) {
                        @Suppress("UNCHECKED_CAST")
                        emitter.onSuccess(result as T)
                    } else {
                        emitter.onError(IllegalStateException("Invalid return type or null result"))
                    }
                }

                pendingEvents.offer(pendingEvent)
                startReconnecting()
            }
        }
    }

    fun dispose() {
        pendingEvents.clear()
        pendingSubscriptions.clear()
        offAll()
        isReconnecting.set(false)
        reconnectDisposable?.dispose()
        hubConnection.stop()
        instance = null
    }
}