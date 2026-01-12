enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "battmon"

// Only include backend and shared modules for Docker builds
// This avoids Android SDK requirements in the Docker builder
include(":backend")
include(":shared")
