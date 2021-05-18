import java.net.URI

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.1")
        classpath(kotlin("gradle-plugin", version = "1.5.0"))
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.5")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven { url = URI("https://jitpack.io") }
    }
}

task<Delete>("clean") {
    delete = setOf(rootProject.buildDir)
}