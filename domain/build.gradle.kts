plugins {
    alias(libs.plugins.kotlin.jvm)
    // Enemy Picker's token catalogue is JSON in domain/src/main/resources, parsed here with
    // kotlinx-serialization - domain's first-ever dependency. It's pure Kotlin (no Android), so it
    // respects ADR-0001's "zero *Android* dependencies" rule. See ADR-0007 for the full rationale.
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // The token catalogue (ADR-0007) is loaded and parsed at runtime from JSON in this module's
    // resources - see [TokenCatalogue]. kotlinx-serialization is pure Kotlin, keeping domain
    // Android-free per ADR-0001.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
