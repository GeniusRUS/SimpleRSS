buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath(kotlin("gradle-plugin", version = "1.7.21"))
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.3")
    }
}

task<Delete>("clean") {
    delete = setOf(rootProject.buildDir)
}