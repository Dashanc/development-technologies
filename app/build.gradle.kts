import java.util.Properties
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

val localConfiguration = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val apiUrl = providers.gradleProperty("murpassport.apiUrl")
    .orElse(providers.environmentVariable("MURPASSPORT_API_URL"))
    .orElse(localConfiguration.getProperty("murpassport.apiUrl", ""))
    .get()
val quotedApiUrl = "\"" + apiUrl.replace("\\", "\\\\").replace("\"", "\\\"")
    .replace("\n", "\\n").replace("\r", "\\r") + "\""
val releaseApiUrl = providers.gradleProperty("murpassport.releaseApiUrl")
    .orElse(providers.environmentVariable("MURPASSPORT_RELEASE_API_URL"))
    .orElse(localConfiguration.getProperty("murpassport.releaseApiUrl", ""))
    .get()
if (releaseApiUrl.isNotEmpty()) {
    val address = runCatching { URI(releaseApiUrl) }.getOrNull()
    require(address != null && address.scheme == "https" && address.host != null &&
        address.userInfo == null && address.query == null && address.fragment == null) {
        "murpassport.releaseApiUrl must be an HTTPS URL without credentials, query or fragment."
    }
}
val quotedReleaseApiUrl = "\"" + releaseApiUrl.replace("\\", "\\\\").replace("\"", "\\\"")
    .replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "com.example.mobile"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.mobile"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", quotedApiUrl)
        }
        release {
            buildConfigField("String", "API_BASE_URL", quotedReleaseApiUrl)
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}