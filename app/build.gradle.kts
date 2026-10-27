import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// Secrets that must not be committed live in the (git-ignored) local.properties:
//   AGORA_APP_ID=<your Agora project App ID>
//   TEST_EMAIL=<email of a test account used by the Espresso tests>
//   TEST_PASSWORD=<its password>
//   TEST_FRIEND_UID=<uid of a second test account that is friends with the first>
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(name: String): String = (localProps.getProperty(name) ?: "").replace("\"", "")

android {
    namespace = "com.zainabfatima.i221064"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.zainabfatima.i221064"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "AGORA_APP_ID", "\"${prop("AGORA_APP_ID")}\"")
        buildConfigField("String", "TEST_EMAIL", "\"${prop("TEST_EMAIL")}\"")
        buildConfigField("String", "TEST_PASSWORD", "\"${prop("TEST_PASSWORD")}\"")
        buildConfigField("String", "TEST_FRIEND_UID", "\"${prop("TEST_FRIEND_UID")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)

    // Firebase: Authentication, Realtime Database, Cloud Messaging
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)

    // Agora voice & video calls
    implementation(libs.agora.full.sdk)

    // Image loading (profile pictures, post images stored as Base64 strings)
    implementation(libs.glide)

    // In-app camera for stories (09)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
}
