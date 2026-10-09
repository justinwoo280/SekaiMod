package io.nekohasekai.sagernet.fmt

fun resolveXHTTPMode(mode: String?, browser: Boolean, grpcFraming: Boolean): String {
    if (grpcFraming) return mode?.takeIf { it == "stream-up" || it == "stream-one" } ?: "stream-one"
    if (browser) return mode?.takeIf { it == "packet-up" || it == "stream-up" || it == "stream-one" } ?: "packet-up"
    return mode?.takeIf { it.isNotBlank() } ?: "auto"
}
