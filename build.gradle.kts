import java.net.URI

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.2")
        classpath(kotlin("gradle-plugin", version = "1.4.31"))
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.3")
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