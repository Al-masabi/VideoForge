pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VideoForge"

include(":app")
include(":core:designsystem")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:media")
include(":core:subtitle")
include(":core:adaptive")
include(":plugin:api")
include(":engine:ffmpeg-native")
include(":benchmark")