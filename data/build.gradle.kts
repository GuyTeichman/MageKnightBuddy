import com.android.build.api.variant.HasUnitTest

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.guyteichman.mageknightbuddy.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// Room's BundledSQLiteDriver lets DAO tests run on plain JVM (see docs/adr/0003-room-tests-via-bundled-sqlite-driver.md).
// The Android-flavored sqlite-bundled and room-runtime artifacts don't expose the context-free JVM
// builder APIs (sqlite-bundled also lacks JVM native binaries), so both are substituted with their
// -jvm variants for the unit test compile+runtime classpaths only; production stays untouched.
androidComponents {
    onVariants { variant ->
        (variant as HasUnitTest).unitTest?.let { unitTest ->
            listOf(unitTest.compileConfiguration, unitTest.runtimeConfiguration).forEach { configuration ->
                with(configuration.resolutionStrategy.dependencySubstitution) {
                    substitute(module("androidx.sqlite:sqlite-bundled:${libs.versions.sqlite.get()}"))
                        .using(module("androidx.sqlite:sqlite-bundled-jvm:${libs.versions.sqlite.get()}"))
                    substitute(module("androidx.sqlite:sqlite:${libs.versions.sqlite.get()}"))
                        .using(module("androidx.sqlite:sqlite-jvm:${libs.versions.sqlite.get()}"))
                    substitute(module("androidx.room:room-runtime:${libs.versions.room.get()}"))
                        .using(module("androidx.room:room-runtime-jvm:${libs.versions.room.get()}"))
                }
            }
        }
    }
}

dependencies {
    implementation(project(":domain"))

    api(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.sqlite.bundled)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}
