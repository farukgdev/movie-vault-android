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

rootProject.name = "MovieVault"

include(":app")

include(":core")

include(":data")

include(":feature:catalog")

include(":feature:favorites")

project(":feature:catalog").projectDir = file("feature/catalog")

project(":feature:favorites").projectDir = file("feature/favorites")
