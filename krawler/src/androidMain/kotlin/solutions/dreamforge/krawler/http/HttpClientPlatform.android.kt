package solutions.dreamforge.krawler.http

import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

actual fun httpClientEngine(): io.ktor.client.engine.HttpClientEngine =  OkHttp.create {
    config {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
        retryOnConnectionFailure(true)
    }
}
