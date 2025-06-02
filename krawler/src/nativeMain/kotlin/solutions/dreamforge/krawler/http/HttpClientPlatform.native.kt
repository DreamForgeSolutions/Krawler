package solutions.dreamforge.krawler.http

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun httpClientEngine(): HttpClientEngine = Darwin.create {
    configureRequest {
        setAllowsCellularAccess(true)
        setAllowsConstrainedNetworkAccess(true)
        setAllowsExpensiveNetworkAccess(true)
    }

    configureSession {
        setTimeoutIntervalForRequest(30.0)
        setTimeoutIntervalForResource(60.0)
        setWaitsForConnectivity(true)
    }
}
