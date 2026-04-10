apply(from = "$rootDir/ktlint.gradle")
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    implementation(libs.koog.agents)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.slf4j.simple)
}

application {
    mainClass.set("bot.MainKt")
}

kotlin {
    jvmToolchain(
        libs.versions.jdkVersion
            .get()
            .toInt(),
    )
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.jdkVersion.get())
    }
}
