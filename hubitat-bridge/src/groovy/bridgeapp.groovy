import groovy.json.JsonSlurper

import java.security.MessageDigest

definition(
        name: "Usonia Bridge",
        namespace: "usonia.hubitat",
        author: "Renee Vandervelde",
        description: "Bridge devices to a Usonia Application",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        oauth: true
)

preferences {
    section("Devices") {
        input "motion", "capability.motionSensor", title: "Select motion sensors", multiple: true, required: false
        input "temp", "capability.temperatureMeasurement", title: "Select temperature sensors", multiple: true, required: false
        input "locks", "capability.lock", title: "Select Locks", multiple: true, required: false
        input "switches", "capability.switch", title: "Choose Switches", multiple: true, required: false
        input "water", "capability.waterSensor", title: "Choose Water Sensors", multiple: true, required: false
        input "doors", "capability.contactSensor", title: "Choose Door Sensors", multiple: true, required: false
        input "power", "capability.powerMeter", title: "Power Meters", multiple: true, required: false
    }
    section("Config") {
        input "bridgeUrl", "text", title: "Bridge URL"
        input "bridgeId", "text", title: "Bridge ID"
        input "psk", "text", title: "Bridge PSK"
    }
}

def initToken() {
    if (!state.accessToken) {
        try {
            createAccessToken()
            log.debug "Creating new Access Token: $state.accessToken"
        } catch (ex) {
            log.error "Did you forget to enable OAuth in SmartApp IDE settings for SmartTiles?"
            log.error ex
        }
    }

    log.info "Cloud URL's are: ${fullApiServerUrl("app-id")}?access_token=${state.accessToken}"
    log.info "Local URL's are: http://<IP>/apps/api/<app-id>/<function>?access_token=${state.accessToken}"

}

mappings {
    path("/telegram") {
        action: [POST: "telegram"]
    }
    path("/actions") {
        action: [POST: "actions"]
    }
}

def installed()
{
    log.trace("Installing")
    initToken()
}

def updated()
{
    unsubscribe()
    log.trace("Updating");
    subscribeToAllEvents(motion, onEvent)
    subscribeToAllEvents(switches, onEvent)
    subscribeToAllEvents(temp, onEvent)
    subscribeToAllEvents(locks, onEvent)
    subscribeToAllEvents(water, onEvent)
    subscribeToAllEvents(doors, onEvent)

    log.info "Cloud URL's are: ${fullApiServerUrl("app-id")}?access_token=${state.accessToken}"
    log.info "Local URL's are: http://<IP>/apps/api/<app-id>/<function>?access_token=${state.accessToken}"
}

def subscribeToAllEvents(sensors, callback) {
    sensors.each { sen ->
        sen.capabilities.each { cap ->
            cap.attributes.each { attr ->
                subscribe(sen, attr.name, callback)
            }
        }
    }
}

def cannonicalType(event) {
    switch (event.name) {
        case "motion":
            return "Motion"
        case "switch":
            return "Switch"
        case "temperature":
            return "Temperature"
        case "humidity":
            return "Humidity"
        case "lock":
            return "Lock"
        case "battery":
            return "Battery"
        case "valve":
            return "Valve"
        case "contact":
            return "Latch"
        case "acceleration":
            return "Movement"
        case "threeAxis":
            return "Tilt"
        case "water":
            return "Water"
        case "pressure":
            return "Pressure"
        case "power":
            return "Power"
        default:
            log.error "Unknown Type ${event.value}"
            return event.name
    }
}

def onEvent(event) {
    def timestamp = event.getDate().getTime()
    def eventJson = [
        "type": cannonicalType(event),
        "timestamp": timestamp,
        "source": event.getDevice().id
    ]

    switch (event.name) {
        case "motion":
            if (event.value == "active") eventJson.motionState = "MOTION"
            else if (event.value == "inactive") eventJson.motionState = "IDLE"
            else log.error "Unknown State ${event.value}"
            break;
        case "switch":
            if (event.value == "on") eventJson.switchState = "ON"
            else if (event.value == "off") eventJson.switchState = "OFF"
            else log.error "Unknown State ${event.value}"
            break;
        case "temperature":
            eventJson.temperature = event.value
            break;
        case "humidity":
            eventJson.humidity = event.value
            break;
        case "lock":
            log.info "Lock Event: " + event.data
            log.info "Event Type: " + event.type
            eventJson.lockState = event.value.toUpperCase()
            def slurper = new JsonSlurper()
            if (event.data != null) {
                eventJson.lockMethod = "KEYPAD"
            } else if (event.type == "physical") {
                eventJson.lockMethod = "MANUAL"
            } else {
                eventJson.lockMethod = "COMMAND"
            }
            eventJson.lockCode = slurper.parseText(event.data ?: "null")?.keySet()?.first()
            break;
        case "contact":
            eventJson.latchState = event.value.toUpperCase()
            break;
        case "water":
            eventJson.waterState = event.value.toUpperCase()
            break;
        case "battery":
            eventJson.battery = event.value
            break;
        case "threeAxis":
            eventJson.x = 0
            eventJson.y = 0
            eventJson.z = 0
            break;
        case "pressure":
            eventJson.pressure = event.value;
            break;
        case "acceleration":
            eventJson.movementState = event.value == "active" ? "MOVING" : "IDLE"
            break;
        case "power":
            eventJson.power = event.value
            break;
        case "valve":
            eventJson.valveState = event.value.toUpperCase()
        case "lastCodeName":
            log.debug "Ignoring event ${event.name}"
            return;
    }

    postAuthorized("$bridgeUrl/bridges/$bridgeId/events", timestamp, eventJson)
}

def postAuthorized(uri, timestamp, json) {
    def jsonString = new groovy.json.JsonBuilder(json).toString()
    def nonce = new Random().nextLong().toString()
    def hash = MessageDigest.getInstance("SHA-256").digest((jsonString + timestamp + psk + nonce).getBytes("UTF-8")).encodeHex().toString()
    def shortHash = hash.substring(0, 8)

    def requestParams = [
        "uri": uri,
        "query": null,
        "requestContentType": "application/json",
        "body": jsonString,
        "headers": [
            "X-Signature": "$hash",
            "X-Timestamp": timestamp,
            "X-Bridge-Id": bridgeId,
            "X-Nonce": nonce
        ]
    ]

    log.trace "--> POST[$shortHash]: $requestParams"
    httpPost(requestParams) { resp ->
        log.trace "<-- POST[$shortHash]: ${resp?.status}"
    }
}

def devices() {
    return motion + temp + locks + switches + water + doors + power
}

def telegram() {
    def data = request.JSON
    def timestamp = now()

    postAuthorized("$bridgeUrl/telegram-bridge", timestamp, data)

    return [success: true]
}

def actions() {
    def action = request.JSON
    def device = devices().find { it.id == action.target }

    switch (action.type) {
        case "Switch":
            if (action.switchState == "ON") {
                logger.debug("Turning device on: $device")
                device.on()
            } else if (action.switchState == "OFF") {
                logger.debug("Turning device off: $device")
                device.off()
            } else {
                log.error "Unknown State ${action.type}"
            }
            break
        case "ColorTemperatureChange":
            if (action.switchState == "ON") {
                logger.debug("Turning device on: $device")
                device.on()
            } else if (action.switchState == "OFF") {
                logger.debug("Turning device off: $device")
                device.off()
            } else {
                log.debug "Ignoring CT State ${action.type}"
            }
            break
        case "Valve":
            if (action.valveState == "OPEN") {
                logger.debug("Opening valve: $device")
                device.open()
            } else if (action.valveState == "CLOSED") {
                logger.debug("Closing valve: $device")
                device.close()
            } else {
                log.error "Unknown Valve Action: ${action.type}"
            }
            break
        case "Lock":
            if (action.lockState == "LOCKED") device.lock()
            else if (action.lockState == "UNLOCKED") device.unlock()
            else log.error "Unknown State ${action.type}"
            break
        case "Intent":
            sendBridgeAction([
                    "type": "Intent",
                    "target": action.target,
                    "intentAction": action.action,
            ])
            break
    }
    return [success: true]
}

def sendBridgeAction(actionJson) {
    def timestamp = now()
    postAuthorized("$bridgeUrl/actions", timestamp, actionJson)
}
