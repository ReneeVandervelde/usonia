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
        val URGENCY = PropertyName("Urgency")
        val IMPACT = PropertyName("Impact")
    }

    object UrgencyValues {
        val LOW = "Low"
        val MEDIUM = "Medium"
        val HIGH = "High"
    }

    object ImpactValues {
        val LOW = "Low"
        val MEDIUM = "Medium"
        val HIGH = "High"
    }

    object PropertyValues {
        val STATUS_DONE = "Done"
    }

    object Tags {
        const val LOW_BATTERY = "Low Battery"
        const val DEAD_BATTERY = "Dead Battery"
    }
}
