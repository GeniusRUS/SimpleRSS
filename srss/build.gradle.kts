import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    compileSdkVersion(30)
    defaultConfig {
        applicationId = "com.genius.srss"
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
        resConfigs("ru", "en")

        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.incremental", "true")
            }
        }
    }
    signingConfigs {
        getByName("debug") {
            keyAlias = gradleLocalProperties(rootDir).getProperty("key.alias")
            keyPassword = gradleLocalProperties(rootDir).getProperty("key.password")
            storeFile = file("../GeniusKey.jks")
            storePassword = gradleLocalProperties(rootDir).getProperty("key.store.password")
        }

        create("release") {
            keyAlias = gradleLocalProperties(rootDir).getProperty("key.alias")
            keyPassword = gradleLocalProperties(rootDir).getProperty("key.password")
            storeFile = file("../GeniusKey.jks")
            storePassword = gradleLocalProperties(rootDir).getProperty("key.store.password")
        }
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-rules-debug.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    buildFeatures {
        viewBinding = true
    }
}

val daggerVer = "2.33"
val moxyVer = "2.2.1"
val roomVer = "2.2.6"
val coroutineVer = "1.4.2"
val navigationVer = "2.3.3"

dependencies {
    implementation(kotlin("stdlib-jdk7", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.browser:browser:1.3.0")
    implementation("androidx.activity:activity-ktx:1.2.0")
    implementation("androidx.fragment:fragment-ktx:1.3.0")

    implementation("com.google.android.material:material:1.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVer")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutineVer")

    implementation("com.unitbean.core:android:1.7.1")

    implementation("io.coil-kt:coil:1.1.1")

    implementation("androidx.navigation:navigation-fragment-ktx:$navigationVer")
    implementation("androidx.navigation:navigation-ui-ktx:$navigationVer")

    implementation("dev.chrisbanes.insetter:insetter:0.5.0")

    implementation("com.github.moxy-community:moxy:$moxyVer")
    implementation("com.github.moxy-community:moxy-androidx:$moxyVer")
    implementation("com.github.moxy-community:moxy-material:$moxyVer")
    implementation("com.github.moxy-community:moxy-ktx:$moxyVer")
    kapt("com.github.moxy-community:moxy-compiler:$moxyVer")

    implementation("com.github.GeniusRUS:Earl:128065f54c")
    implementation("com.github.kirich1409:viewbindingpropertydelegate-noreflection:1.4.3")
    implementation("com.github.razir.progressbutton:progressbutton:2.1.0")

    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    implementation("androidx.room:room-runtime:$roomVer")
    implementation("androidx.room:room-ktx:$roomVer")
    kapt("androidx.room:room-compiler:$roomVer")

    implementation("com.google.dagger:dagger:$daggerVer")
    implementation("com.google.dagger:dagger-android:$daggerVer")
    kapt("com.google.dagger:dagger-compiler:$daggerVer")
    kapt("com.google.dagger:dagger-android-processor:$daggerVer")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}