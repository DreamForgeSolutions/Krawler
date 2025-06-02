# Krawler 🕷️

[![Kotlin](https://img.shields.io/badge/kotlin-2.1.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blueviolet)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/solutions.dreamforge.krawler/krawler)](https://search.maven.org/search?q=g:solutions.dreamforge.krawler)

A powerful, modern web crawling and scraping library for Kotlin Multiplatform. Build efficient web crawlers that run on JVM, Android, iOS, JavaScript, and WebAssembly with a beautiful Kotlin DSL.

## ✨ Features

- 🌍 **True Multiplatform**: Single codebase runs on JVM, Android, iOS, JS, and WASM
- 🎯 **Intuitive Kotlin DSL**: Configure crawlers with clean, type-safe syntax
- 🚀 **High Performance**: Concurrent crawling with coroutines and smart rate limiting
- 🔍 **Flexible Extraction**: CSS selectors, XPath, regex, and custom extractors
- 🤖 **Robots.txt Compliance**: Respects website crawling policies automatically
- 📊 **Built-in Analytics**: Track performance metrics and crawl statistics
- 🔌 **Extensible Architecture**: Clean architecture with pluggable components
- 💾 **Smart Caching**: Reduce redundant requests with intelligent caching
- 🎨 **Sample App**: Full-featured Compose Multiplatform demo application

## 📋 Table of Contents

- [Installation](#-installation)
- [Quick Start](#-quick-start)
- [Platform Setup](#-platform-setup)
- [Core Concepts](#-core-concepts)
- [Advanced Usage](#-advanced-usage)
- [Architecture](#-architecture)
- [Sample Application](#-sample-application)
- [API Reference](#-api-reference)
- [Contributing](#-contributing)
- [License](#-license)

## 📦 Installation

### Multiplatform Project

Add Krawler to your `build.gradle.kts`:

```kotlin
kotlin {
    commonMain {
        dependencies {
            implementation("solutions.dreamforge.krawler:krawler:0.0.1")
        }
    }
}
```

### Platform-Specific Projects

<details>
<summary>JVM/Android</summary>

```kotlin
dependencies {
    implementation("solutions.dreamforge.krawler:krawler-jvm:0.0.1")
}
```
</details>

<details>
<summary>iOS</summary>

```kotlin
kotlin {
    ios {
        binaries {
            framework {
                baseName = "krawler"
            }
        }
    }
}
```
</details>

<details>
<summary>JavaScript</summary>

```kotlin
dependencies {
    implementation("solutions.dreamforge.krawler:krawler-js:0.0.1")
}
```
</details>

## 🚀 Quick Start

### Basic Example

```kotlin
import solutions.dreamforge.krawler.*
import solutions.dreamforge.krawler.dsl.*

suspend fun main() {
    // Create a crawler instance
    val crawler = CrawlerSDK.create()
    
    // Define your crawl configuration
    val config = crawler {
        name = "My First Crawler"
        maxConcurrency = 10
        
        source("example") {
            urls("https://example.com")
            depth(2)
            
            extract {
                text("title", "h1")
                text("description", "meta[name=description]")
                links("links", "a[href]") {
                    multiple()
                }
            }
        }
    }
    
    // Start crawling and collect results
    crawler.crawl(config).collect { result ->
        when (result.status) {
            CrawlStatus.SUCCESS -> {
                println("Crawled: ${result.webPage?.url}")
                println("Title: ${result.webPage?.extractedData["title"]}")
            }
            else -> println("Failed: ${result.error}")
        }
    }
}
```

### Advanced Configuration

```kotlin
val advancedConfig = crawler {
    name = "Advanced News Crawler"
    maxConcurrency = 20
    
    // Global extraction rules
    extract {
        text("title", "h1, h2, .headline") {
            required()
            process {
                trim()
                uppercase()
            }
        }
        
        html("content", "article, .post-content") {
            process {
                // Remove ads and scripts
                custom("clean-html")
            }
        }
        
        // Extract structured data
        regex("price", "\\$([0-9,]+\\.?[0-9]*)", group = 1)
    }
    
    // Global crawl policy
    policy {
        respectRobotsTxt = true
        delay(2000) // 2 seconds between requests
        userAgent = "MyNewsBot/1.0"
        maxRetries = 3
        timeout = 15000
        
        allowContentTypes("text/html", "application/xhtml+xml")
        
        headers {
            put("Accept-Language", "en-US,en;q=0.9")
            put("Accept-Encoding", "gzip, deflate")
        }
    }
    
    // Multiple sources with different configurations
    source("tech-news") {
        urls(
            "https://techcrunch.com",
            "https://theverge.com",
            "https://arstechnica.com"
        )
        depth(3)
        priority(CrawlRequest.Priority.HIGH)
        
        // Source-specific rules
        extract {
            text("author", ".author-name, .by-line")
            text("date", "time[datetime]")
        }
    }
    
    source("business-news") {
        urls("https://bloomberg.com", "https://ft.com")
        depth(2)
        priority(CrawlRequest.Priority.NORMAL)
        
        policy {
            delay(5000) // More conservative for premium sites
        }
    }
}
```

## 🔧 Platform Setup

### JVM Configuration

```kotlin
val crawler = CrawlerSDK.create(
    SDKConfiguration(
        userAgent = "MyBot/1.0 (Compatible; JVM)",
        maxConcurrency = 50,
        connectTimeoutSeconds = 10,
        readTimeoutSeconds = 30
    )
)
```

### Android Permissions

Add to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### iOS Configuration

No special configuration required. The library uses native iOS networking APIs.

### JavaScript/Browser

```kotlin
// Runs in browser with CORS limitations
val crawler = CrawlerSDK.create(
    SDKConfiguration(
        userAgent = "MyBot/1.0 (Compatible; Browser)",
        maxConcurrency = 10 // Limited by browser
    )
)
```

## 📚 Core Concepts

### Crawl Request

The fundamental unit of crawling:

```kotlin
val request = CrawlRequest(
    id = "unique-id",
    url = "https://example.com",
    depth = 0,
    maxDepth = 3,
    extractionRules = listOf(/* ... */),
    crawlPolicy = CrawlPolicy(/* ... */),
    priority = CrawlRequest.Priority.HIGH,
    metadata = mapOf("category" to "tech"),
    timestamp = Clock.System.now()
)
```

### Extraction Rules

Define what data to extract:

```kotlin
// CSS Selector
val titleRule = ExtractionRule(
    name = "title",
    selector = Selector.CssSelector("h1.main-title"),
    extractionType = ExtractionType.TEXT,
    required = true
)

// XPath
val priceRule = ExtractionRule(
    name = "price",
    selector = Selector.XPathSelector("//span[@class='price']/text()"),
    extractionType = ExtractionType.TEXT,
    postProcessors = listOf(
        PostProcessor.Extract("([0-9.]+)", 1),
        PostProcessor.Custom("parse-currency")
    )
)

// Regex
val emailRule = ExtractionRule(
    name = "emails",
    selector = Selector.RegexSelector("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
    extractionType = ExtractionType.TEXT,
    multiple = true
)
```

### Post Processors

Transform extracted data:

```kotlin
extract {
    text("price", ".price") {
        process {
            trim()
            replace("$", "")
            replace(",", "")
            custom("to-number")
        }
    }
    
    text("description", ".desc") {
        process {
            trim()
            substring(0, 200)
            custom("remove-html") { 
                // Configuration for custom processor
                put("preserve-links", "true")
            }
        }
    }
}
```

### Crawl Policies

Control crawler behavior:

```kotlin
policy {
    respectRobotsTxt = true
    followRedirects = true
    maxRedirects = 5
    delayBetweenRequests = 1000 // milliseconds
    maxRetries = 3
    timeout = 30000
    maxContentLength = 10 * 1024 * 1024 // 10MB
    
    allowContentTypes(
        "text/html",
        "application/xhtml+xml",
        "application/xml"
    )
    
    headers {
        put("Accept", "text/html,application/xhtml+xml")
        put("Accept-Language", "en-US,en;q=0.9")
        put("Cache-Control", "no-cache")
    }
}
```

## 🔥 Advanced Usage

### Batch Crawling

```kotlin
val requests = (1..100).map { page ->
    CrawlRequest(
        id = "page-$page",
        url = "https://example.com/products?page=$page",
        // ... other configuration
    )
}

crawler.batchCrawl(
    requests = requests,
    maxConcurrency = 20,
    batchId = "products-crawl"
).collect { result ->
    // Process results
}
```

### Custom Extraction Engine

```kotlin
class MyCustomExtractor : ExtractionEngine {
    override suspend fun extract(
        html: String,
        rules: List<ExtractionRule>
    ): Map<String, ExtractedValue> {
        // Custom extraction logic
        return extractedData
    }
}

val crawler = CrawlerSDK.create(
    extractionEngine = MyCustomExtractor(),
    // ... other components
)
```

### Progress Monitoring

```kotlin
val crawler = CrawlerSDK.create()

// Monitor statistics
launch {
    while (true) {
        val stats = crawler.getStats()
        println("""
            Active: ${stats.activeCrawls}
            Completed: ${stats.completedCrawls}
            Failed: ${stats.failedCrawls}
            Queue Size: ${stats.queueSize}
            Avg Response Time: ${stats.averageResponseTime}ms
        """.trimIndent())
        delay(1000)
    }
}

// Start crawling
crawler.crawl(config).collect { /* ... */ }
```

### Error Handling

```kotlin
crawler.crawl(config).collect { result ->
    when (result.status) {
        CrawlStatus.SUCCESS -> handleSuccess(result)
        CrawlStatus.ROBOTS_BLOCKED -> println("Blocked by robots.txt")
        CrawlStatus.TIMEOUT -> println("Request timed out")
        CrawlStatus.NETWORK_ERROR -> println("Network error: ${result.error}")
        CrawlStatus.PARSE_ERROR -> println("Failed to parse: ${result.error}")
        else -> println("Other error: ${result.status}")
    }
}
```

### Custom Post Processors

```kotlin
class CurrencyParser : PostProcessorService {
    override fun register() {
        registerProcessor("parse-currency") { value, config ->
            val currency = config["currency"] ?: "USD"
            val amount = value.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            "$currency $amount"
        }
    }
}
```

## 🏗️ Architecture

Krawler follows Clean Architecture principles:

```
krawler/
├── domain/              # Business logic
│   ├── model/          # Domain models
│   ├── repository/     # Repository interfaces
│   ├── service/        # Domain services
│   └── usecase/        # Use cases
├── infrastructure/      # Implementation details
│   ├── cache/          # Caching implementation
│   ├── extraction/     # HTML parsing
│   ├── repository/     # Repository implementations
│   └── robots/         # Robots.txt handling
├── dsl/                # Kotlin DSL
├── engine/             # Crawling engine
└── http/               # HTTP client abstraction
```

### Key Components

- **CrawlerSDK**: Main entry point and facade
- **CrawlerEngine**: Orchestrates crawling operations
- **ExtractionEngine**: Extracts data from HTML
- **RobotsService**: Handles robots.txt compliance
- **CrawlRepository**: Stores crawl results
- **HttpClient**: Platform-specific HTTP implementation

## 🎮 Sample Application

The project includes a full-featured Compose Multiplatform demo:

### Running the Sample

```bash
# Desktop (JVM)
./gradlew :sample:composeApp:run

# Android
# Open in Android Studio and run

# iOS
# Open sample/iosApp/iosApp.xcodeproj in Xcode

# Web (JS)
./gradlew :sample:composeApp:jsBrowserRun

# Web (WASM)
./gradlew :sample:composeApp:wasmJsBrowserRun
```

### Sample Features

- Real-time crawling visualization
- Performance metrics dashboard
- Category-based crawling
- Source performance tracking
- Recent results display
- Responsive UI for all platforms

## 📖 API Reference

### CrawlerSDK

```kotlin
interface CrawlerSDK {
    // Create crawler instance
    fun create(config: SDKConfiguration = SDKConfiguration()): CrawlerSDK
    
    // Start crawling with DSL configuration
    suspend fun crawl(configuration: CrawlerConfiguration): Flow<CrawlResult>
    
    // Crawl single URL
    suspend fun crawlSingle(request: CrawlRequest): CrawlResult
    
    // Batch crawl multiple URLs
    suspend fun batchCrawl(
        requests: List<CrawlRequest>,
        maxConcurrency: Int = 50,
        batchId: String = "batch_${timestamp}"
    ): Flow<CrawlResult>
    
    // Get statistics
    fun getStats(): CrawlerStats
    
    // Stop crawler
    suspend fun stop()
}
```

### DSL Functions

```kotlin
// Main DSL entry point
fun crawler(block: CrawlerConfiguration.() -> Unit): CrawlerConfiguration

// Source configuration
fun CrawlerConfiguration.source(name: String, block: SourceBuilder.() -> Unit)

// Extraction rules
fun extract(block: ExtractionRulesBuilder.() -> Unit)

// Crawl policy
fun policy(block: CrawlPolicyBuilder.() -> Unit)
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/dreamforge/krawler.git
   ```

2. Open in IntelliJ IDEA or Android Studio

3. Build the project:
   ```bash
   ./gradlew build
   ```

4. Run tests:
   ```bash
   ./gradlew test
   ```

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Write unit tests for new features

## 📄 License

Krawler is released under the Apache License 2.0. See [LICENSE](LICENSE) for details.

```
Copyright 2024 DreamForge Solutions

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🙏 Acknowledgments

- Built with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- HTTP networking by [Ktor](https://ktor.io/)
- HTML parsing by [Ksoup](https://github.com/MohamedRejeb/Ksoup)
- UI powered by [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

## 📬 Contact

- **Issues**: [GitHub Issues](https://github.com/dreamforge/krawler/issues)
- **Discussions**: [GitHub Discussions](https://github.com/dreamforge/krawler/discussions)
- **Email**: hello@dreamforge.solutions

---

<p align="center">
  Made with ❤️ by DreamForge Solutions
</p>
