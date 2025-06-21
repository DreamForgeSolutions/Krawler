import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import com.vanniktech.maven.publish.SonatypeHost
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.atomicfu)
    id("com.vanniktech.maven.publish") version "0.31.0"
}

group = "solutions.dreamforge.krawler"
version = "0.0.1-SNAPSHOT"

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    jvmToolchain(17)

    androidTarget { publishLibraryVariants("release") }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kermit)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ksoup)
            implementation(libs.cache4k)
            implementation(libs.atomicfu)
            implementation(libs.stately.concurrent.collections)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.logback.classic)
            implementation(libs.crawler.commons)
            implementation(libs.json.path)
            implementation(libs.config)
        }

        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.mockk)
            }
        }
    }

    //https://kotlinlang.org/docs/native-objc-interop.html#export-of-kdoc-comments-to-generated-objective-c-headers
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compileTaskProvider.configure {
            compilerOptions {
                freeCompilerArgs.add("-Xexport-kdoc")
            }
        }
    }

}

android {
    namespace = "solutions.dreamforge.krawler"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }
}

buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
}

mavenPublishing {
    coordinates(
        groupId = "solutions.dreamforge",
        artifactId = "krawler",
        version = project.version.toString()
    )



    pom {
        name.set("Krawler")
        description.set("Krawl the web with Kotlin Multiplatform")
        inceptionYear.set("2024")
        url.set("https://github.com/DreamForgeSolutions/Krawler")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("dreamforge")
                name.set("dreamforge")
                email.set("hello@dreamforge.solutions")
            }
        }

        scm {
            url.set("https://github.com/DreamForgeSolutions/")
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}