package dev.pierrot.listeners

import com.microsoft.signalr.*
import dev.pierrot.config
import dev.pierrot.getLogger

class AnimalSync(clientId: Int) {
    companion object {
        val logger = getLogger(this::class.java)
        val HUB_URL = config.app.websocket
        lateinit var hubConnection: HubConnection
    }

    init {
        val headers = hashMapOf(Pair("secret_key", "123"))
        hubConnection = HubConnectionBuilder.create("${HUB_URL}?ClientId=$clientId")
            .withHeaders(headers)
            .withKeepAliveInterval(100000)
            .build()
        listenEvent()
        start()
    }

    private fun listenEvent() {
        hubConnection.on(
            "connection",
            Action1 { s: String? -> logger.info(s) },
            String::class.java
        )
        hubConnection.on("error", Action1<Any> { msg: Any ->
            logger.error(msg.toString())
        }, Any::class.java)
        hubConnection.on("disconnect", { msg: String? ->
            logger.warn(msg)
        }, String::class.java)
        hubConnection.onClosed(OnClosedCallback {
            logger.warn("Connection closed. Attempting to reconnect...")
        })
    }

    private fun start() {
        if (hubConnection.connectionState != HubConnectionState.CONNECTED) {
            hubConnection.start().subscribe(
                {
                    logger.info("Connected successfully")
                },
                { error: Throwable ->
                    logger.error("Error while connecting: {}", error.message)
                }
            ).dispose()
        }
    }

}