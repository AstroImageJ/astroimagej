tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    filePermissions {
        unix("644")
    }

    dirPermissions {
        unix("755")
    }
}
