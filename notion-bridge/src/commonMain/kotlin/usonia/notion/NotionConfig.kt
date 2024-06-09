package usonia.notion

import usonia.notion.api.structures.property.PropertyName

internal object NotionConfig {
    const val SERVICE = "notion"
    const val TOKEN = "token"
    const val DATABASE = "database"

    object Properties {
        val REF = PropertyName("Reference ID")
        val TITLE = PropertyName("Name")
        val TAGS = PropertyName("Tags")
        val STATUS = PropertyName("Complete")
    }

    object PropertyValues {
        val STATUS_DONE = "Done"
    }

    object Tags {
        const val LOW_BATTERY = "Low Battery"
        const val DEAD_BATTERY = "Dead Battery"
    }
}
