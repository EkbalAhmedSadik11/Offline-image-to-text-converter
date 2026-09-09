pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Required for the offline Tesseract OCR engine library (Tesseract4Android)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Image to Text"
include(":app")
