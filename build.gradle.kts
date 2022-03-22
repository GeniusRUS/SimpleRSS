buildscript {
    val compose_version by extra("1.1.1")
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.1.2")
        classpath(kotlin("gradle-plugin", version = "1.6.10"))
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.4.1")
    }
}

task<Delete>("clean") {
    delete = setOf(rootProject.buildDir)
}