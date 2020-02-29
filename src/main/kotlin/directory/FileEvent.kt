package ch.guengel.imageserver.directory

data class FileEvent(val filepath: String, val eventType: EventType) {
    enum class EventType {
        CREATED,
        MODIFIED,
        DELETED
    }
}