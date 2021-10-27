buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.3")
        classpath(kotlin("gradle-plugin", version = "1.5.31"))
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.5")
    }
}

task<Delete>("clean") {
    delete = setOf(rootProject.buildDir)
}