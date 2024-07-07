package usonia.rules.locks

import usonia.foundation.Device

/**
 * This uses a parameter field on the lock device define allowed codes:
 *
 *     "parameters": {
 *         "ownerCodes": "1,2,3"
 *     }
 */
internal val Device.ownerCodes: List<String> get(){
    return parameters.get("ownerCodes")
        ?.split(',')
        ?.map { it.trim() }
        .orEmpty()
}
