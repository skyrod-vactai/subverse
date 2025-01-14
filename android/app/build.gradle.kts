plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.skyrod.subverse"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.skyrod.subverse"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Specify the ABI filters for supported architectures
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Add to your app's build.gradle.kts

val kcatsProjectPath = System.getenv("KCATS_PROJECT_PATH") ?: project.findProperty("kcatsProjectPath")?.toString()
val homeDir = System.getenv("HOME") ?: System.getProperty("user.home")

tasks.register("buildKcatsLib") {
    enabled = kcatsProjectPath != null

    doLast {
        if (kcatsProjectPath == null) {
            println("Skipping Kcats library build - KCATS_PROJECT_PATH not set")
            return@doLast
        }

        // Build for different architectures
        exec {
            workingDir = File(kcatsProjectPath)
            commandLine("cargo", "install", "--path", ".", "--target", "aarch64-linux-android")
        }
        exec {
            workingDir = File(kcatsProjectPath)
            commandLine("cargo", "install", "--path", ".", "--target", "x86_64-linux-android")
        }

        // Create directories if they don't exist
        val jniLibsDir = file("src/main/jniLibs")
        val arm64Dir = file("${jniLibsDir}/arm64-v8a")
        val x86_64Dir = file("${jniLibsDir}/x86_64")
        val assetsDir = file("src/main/assets/cache")

        arm64Dir.mkdirs()
        x86_64Dir.mkdirs()
        assetsDir.mkdirs()

        // Clear and recreate the assets cache directory
        assetsDir.deleteRecursively()
        assetsDir.mkdirs()

        // Copy the compiled libraries
        copy {
            from("${homeDir}/.cargo/target/aarch64-linux-android/release/libkcats.so")
            into(arm64Dir)
        }
        copy {
            from("${homeDir}/.cargo/target/x86_64-linux-android/release/libkcats.so")
            into(x86_64Dir)
        }

        // Copy cache files
        copy {
            from("${homeDir}/.local/share/kcats/cache")
            into(assetsDir)
            include("**/*")
        }
    }
}

// Make the preBuild task depend on buildKcatsLib
tasks.named("preBuild") {
    dependsOn("buildKcatsLib")
}