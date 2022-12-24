package usonia.foundation.unit

import inkapplications.spondee.scalar.Percentage
import inkapplications.spondee.scalar.WholePercentage

operator fun Percentage.compareTo(percent: WholePercentage): Int = toDecimal().value.toDouble().compareTo(percent.toDecimal().value.toDouble())
