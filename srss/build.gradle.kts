import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    compileSdk = 31
    defaultConfig {
        applicationId = "com.genius.srss"
        minSdk = 21
        targetSdk = 31
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
        resourceConfigurations.addAll(listOf("ru", "en"))

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
            applicationIdSuffix = ".develop"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

val daggerVer = "2.40"
val roomVer = "2.3.0"
val coroutineVer = "1.5.2"
val navigationVer = "2.3.5"
val okHttpVer = "4.9.1"
val lifecycleVer = "2.4.0"

dependencies {
    implementation(kotlin("stdlib-jdk7", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("androidx.fragment:fragment-ktx:1.3.6")

    implementation("com.google.android.material:material:1.5.0-alpha05")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVer")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutineVer")

    implementation("io.github.unitbean:androidcore:2.2.0")

    implementation("io.coil-kt:coil:1.4.0")

    implementation("androidx.navigation:navigation-fragment-ktx:$navigationVer")
    implementation("androidx.navigation:navigation-ui-ktx:$navigationVer")

    implementation("dev.chrisbanes.insetter:insetter:0.6.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVer")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVer")

    implementation("com.github.GeniusRUS:Earl:5589667ed6")
    implementation("com.github.kirich1409:viewbindingpropertydelegate-noreflection:1.4.7")
    implementation("com.github.razir.progressbutton:progressbutton:2.1.0")

    implementation("com.squareup.okhttp3:okhttp:$okHttpVer")
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVer")

    implementation("androidx.room:room-runtime:$roomVer")
    implementation("androidx.room:room-ktx:$roomVer")
    kapt("androidx.room:room-compiler:$roomVer")
    kapt("org.xerial:sqlite-jdbc:3.36.0.1")

    implementation("com.google.dagger:dagger:$daggerVer")
    implementation("com.google.dagger:dagger-android:$daggerVer")
    kapt("com.google.dagger:dagger-compiler:$daggerVer")
    kapt("com.google.dagger:dagger-android-processor:$daggerVer")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}