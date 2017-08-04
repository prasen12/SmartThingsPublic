/**
 *  RaspberryPi (Connect)
 *
 *  Copyright 2017 Prasen Palvankar
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "RaspberryPi (Connect)",
    namespace: "prasen12",
    author: "Prasen Palvankar",
    description: "Control Raspberry Pi and components connected to it",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


/**
 * Adapted from bose-soundtouch-connect
 * */
definition(
    name: "Raspberry Pi (Connect)",
    namespace: "prasen.palvankar@gmail.com",
    author: "Prasen Palvankar",
    description: "Control your Raspberry Pi services",
    category: "My Apps",
    iconUrl: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png",
    iconX2Url: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png",
    iconX3Url: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png",
    singleInstance: true

)

preferences {
  page(name: "servicesSelection")
  page(name:"deviceDiscovery")
   
    
  
}

/**
 * Get the urn that for our RaspberryPi
 *
 */
def getDeviceTypeURN() {
    return "urn:schemas-upnp-org:device:RaspberryPiDevice:1"
}

/**
 * Get the name of the new device to instantiate in the user's smartapps
 * This must be an app owned by the namespace (see #getNameSpace).
 *
 * @return name
 */
def getDeviceName() {
    return "RaspberryPi"
}

/**
 * Returns the namespace this app and siblings use
 *
 * @return namespace
 */
def getNameSpace() {
    return "prasen12"
}

/**
 * The deviceDiscovery page used by preferences. Will automatically
 * make calls to the underlying discovery mechanisms as well as update
 * whenever new devices are discovered AND verified.
 *
 * @return a dynamicPage() object
 */
def deviceDiscovery()
{
	log.debug "deviceDiscovery()"
    def refreshInterval = 3 // Number of seconds between refresh
    int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
    state.deviceRefreshCount = deviceRefreshCount + refreshInterval

    def devices = getSelectableDevice()
    def numFound = devices.size() ?: 0

    // Make sure we get location updates (contains LAN data such as SSDP results, etc)
    // cribeNetworkEvents()
	ssdpSubscribe()
    
    //device discovery request every 15s
    if((deviceRefreshCount % 15) == 0) {
        discoverDevices()
    }

    // Verify request every 3 seconds except on discoveries
    if(((deviceRefreshCount % 3) == 0) && ((deviceRefreshCount % 15) != 0)) {
        verifyDevices()
    }

    log.trace "Discovered devices: ${devices}"

    return dynamicPage(name:"deviceDiscovery", title:"Discovery Started!", nextPage: "", refreshInterval: refreshInterval, install:true, uninstall: true) {
        section("Please wait while we discover your ${getDeviceName()}. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
            input "selecteddevice", "enum", required:true, title:"Select ${getDeviceName()} (${numFound} found)", multiple:false, options:devices
        }
        
    }
    
}

/**
 * Called by SmartThings Cloud when user has selected device(s) and
 * pressed "Install".
 */
def installed() {
    log.trace "Installed with settings: ${settings}"
    initialize()
}

/**
 * Called by SmartThings Cloud when app has been updated
 */
def updated() {
    log.trace "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

/**
 * Called by SmartThings Cloud when user uninstalls the app
 *
 * We don't need to manually do anything here because any children
 * are automatically removed upon the removal of the parent.
 *
 * Only time to do anything here is when you need to notify
 * the remote end. And even then you're discouraged from removing
 * the children manually.
 */
def uninstalled() {
}

/**
 * If user has selected devices, will start monitoring devices
 * for changes (new address, port, etc...)
 */
def initialize() {
    log.trace "initialize()"
    state.subscribe = false
    if (selecteddevice) {
        addDevice()
        refreshDevices()
        //subcribeNetworkEvents(true)
    }
}

/**
 * Adds the child devices based on the user's selection
 *
 * Uses selecteddevice defined in the deviceDiscovery() page
 */
def addDevice(){
    def devices = getVerifiedDevices()
    def devlist
    log.trace "Adding children"

    // If only one device is selected, we don't get a list (when using simulator)
    if (!(selecteddevice instanceof List)) {
        devlist = [selecteddevice]
    } else {
        devlist = selecteddevice
    }

    log.trace "These are being installed: ${devlist}"

    devlist.each { dni ->
        def d = getChildDevice(dni)
        if(!d) {
            def newDevice = devices.find { (it.value.mac) == dni }
            def deviceName = newDevice?.value.name
            if (!deviceName){ 
                deviceName = getDeviceName() + "[${newDevice?.value.name}]"
            }
            d = addChildDevice(getNameSpace(), getDeviceName(),
                dni, newDevice?.value.hub, 
                [label:"${deviceName}",
                 data:[
                 	mac: newDevice?.value?.mac,
            		ip: convertHexToIP(newDevice?.value?.networkAddress),
            		port: convertHexToInt(newDevice?.value?.deviceAddress)                
                 ]
                
                
                ]
            )
            // Save the ip:port to the state -- we will need it to configure the services provided by the device just added
           def selectedDeviceHost = convertHexToIP(newDevice?.value?.networkAddress) + ":" + convertHexToInt(newDevice?.value?.deviceAddress)
            log.trace "Created ${d.displayName} with id $dni"
            // sync DTH with device, done here as it currently don't work from the DTH's installed() method
            //d.refresh()
            log.debug "Calling get services..."
           requestDeviceServices(selectedDeviceHost)
        } else {
            log.trace "${d.displayName} with id $dni already exists"
        }
    }
}

/**
 * Resolves a DeviceNetworkId to an address. Primarily used by children
 *
 * @param dni Device Network id
 * @return address or null
 */
def resolveDNI2Address(dni) {
    def device = getVerifiedDevices().find { (it.value.mac) == dni }
    if (device) {
        return convertHexToIP(device.value.networkAddress)
    }
    return null
}




/**
 * Handles SSDP discovery messages and adds them to the list
 * of discovered devices. If it already exists, it will update
 * the port and location (in case it was moved).
 *
 * @param lanEvent
 */
def ssdpHandler(evt) {
    //SSDP DISCOVERY EVENTS
    def lanEvent = parseLanMessage(evt.description)
    lanEvent << ["hub":evt?.hubId]

    log.debug "parseSSDP ${lanEvent}"
    def USN = lanEvent.ssdpUSN.toString()
    def devices = getDevices()

    if (!(devices."${USN}")) {
        //device does not exist
        log.trace "parseSDDP() Adding Device \"${USN}\" to known list"
        devices << ["${USN}":lanEvent]
    } else {
        // update the values
        def d = devices."${USN}"
        if (d.networkAddress != lanEvent.networkAddress || d.deviceAddress != lanEvent.deviceAddress) {
            log.trace "parseSSDP() Updating device location (ip & port)"
            d.networkAddress = lanEvent.networkAddress
            d.deviceAddress = lanEvent.deviceAddress
        }
    }
}

/**
 * Generates a Map object which can be used with a preference page
 * to represent a list of devices detected and verified.
 *
 * @return Map with zero or more devices
 */
Map getSelectableDevice() {
    def devices = getVerifiedDevices()
    def map = [:]
    devices.each {
        def value = "${it.value.name}"
        def key = it.value.mac
        map["${key}"] = value
    }
    map
}

/**
 * Starts the refresh loop, making sure to keep us up-to-date with changes
 *
 */
private refreshDevices() {
    discoverDevices()
    verifyDevices()
    runIn(300, "refreshDevices")
}

/**
 * Starts a subscription for network events
 *
 * @param force If true, will unsubscribe and subscribe if necessary (Optional, default false)
 *
private subscribeNetworkEvents(force=false) {
    if (force) {
        unsubscribe()
        state.subscribe = false
    }

    if(!state.subscribe) {
        subscribe(location, null, onLocation, [filterEvents:false])
        state.subscribe = true
    }
}
*/


void ssdpSubscribe() {
	subscribe(location, "ssdpTerm.${getDeviceTypeURN()}", ssdpHandler)
}



/**
 * Issues a SSDP M-SEARCH over the LAN for a specific type (see getDeviceType())
 */
private discoverDevices() {
    log.trace "discoverDevice() Issuing SSDP request"
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${getDeviceTypeURN()}", physicalgraph.device.Protocol.LAN))
}

/**
 * Walks through the list of unverified devices and issues a verification
 * request for each of them (basically calling verifyDevice() per unverified)
 */
private verifyDevices() {
	log.debug "verifyDevices() - devices = ${getDevices()}"
    def devices = getDevices().findAll { it?.value?.verified != true }

    devices.each {
        verifyDevice(
            it?.value?.mac,
            convertHexToIP(it?.value?.networkAddress),
            convertHexToInt(it?.value?.deviceAddress),
            it?.value?.ssdpPath
        )
    }
}

/**
 * Verify the device, in this case, we need to obtain the info block which
 * holds information such as the actual mac to use in certain scenarios.
 *
 * Without this mac (henceforth referred to as deviceID), we can't do multi-speaker
 * functions.
 *
 * @param deviceNetworkId The DNI of the device
 * @param ip The address of the device on the network (not the same as DNI)
 * @param port The port to use (0 will be treated as invalid and will use 80)
 * @param devicessdpPath The URL path (for example, /desc)
 *
 * @note Result is captured in locationHandler()
 */
private verifyDevice(String deviceNetworkId, String ip, int port, String devicessdpPath) {
	log.debug("Verify device = ${deviceNetworkId}, ${ip}:${port}, ${devicessdpPath}")
    
    if(ip) {
        def address = ip + ":" + port
        sendHubCommand(new physicalgraph.device.HubAction([
                    method: "GET",
                    path: devicessdpPath,
                    headers: [
                        HOST: address,
                    ]],null, [callback: deviceDescriptionHandler]))
    } else {
        log.warn("verifyDevice() IP address was empty")
    }
}


void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "deviceDescriptionHandler()"
	def xmlData = hubResponse.xml
	
    log.debug "UDN = ${xmlData.device.UDN.text()}, name=${xmlData.device.friendlyName}"
    
    def devices = getDevices()
    def device = devices.find {it?.key?.contains(xmlData?.device?.UDN?.text())}
    if (device && !device.value?.verified) {
        	device.value << [name:xmlData?.device?.friendlyName?.text(),
            				model:xmlData?.device?.modelName?.text(),
                            serialNumber:xmlData?.device?.serialNum?.text(),
                            verified: true
                            ]
                            
        } else {
            log.error "deviceDescriptionHandler(): The xml file returned a device that didn't exist"
        }
    }
   
/**
 * Returns an array of devices which have been verified
 *
 * @return array of verified devices
 */
def getVerifiedDevices() {
	log.debug "getVerifiedDevices()"
    getDevices().findAll{ it?.value?.verified == true }
}

/**
 * Returns all discovered devices or an empty array if none
 *
 * @return array of devices
 */
def getDevices() {
	log.debug "getDevices() - ${state.devices}"
    state.devices = state.devices ?: [:]
}

def getServices() {
	log.debug "getServices() - ${state.deviceServices}"
    state.deviceServices = state.deviceServices ?: []
}
/**
 * Calls the RPi for a list of services provided by the Pi
 * Services can be irrigation stations and switches for light control
 */
def requestDeviceServices(String host) {
	log.debug "requestDeviceServices(${host})"
    sendHubCommand(new physicalgraph.device.HubAction([
                    method: "GET",
                    path: "/api/services",
                    headers: [
                        HOST: host,
                    ]],null, [callback: servicesResponseHandler]))
}

/**
 */
void servicesResponseHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "servicesResponseHandler()"
	def response = hubResponse.json
	log.debug "Service response - ${response}" 
    state.deviceServices = response.data.services;
    log.debug "Service response - ${state.deviceServices}" 
}
   
def getSelectableServices() {
	def services = getServices();
    def map = [:]
    services.each {
    log.debug ">>>> ${it}"
    
    	if ( it.enabled == true ) {
            def value = "${it.name}"
            def key = it.definition
            map["${key}"] = value
        }
       
    }
    map
    
}
   
def servicesSelection()   {
	log.debug "servicesSelection() page"
	def refreshInterval = 3;
    //requestDeviceServices()
    def services = getSelectableServices()
    def numFound = services.size() ?: 0
	dynamicPage(name:"servicesSelection", title:"Select services", install:false, uninstall: true) {
        section ("Device Selection") {
        	href "deviceDiscovery",  title: "Select device to use", description: "Tap to open" 
        }
        
        section("Select services to use") {
            input "selectedServices", "enum", required:true, title:"Select services -  (${numFound} found)", multiple:true, options:services, submitOnChange: true
        }
    }
    
}
/**
 * Converts a hexadecimal string to an integer
 *
 * @param hex The string with a hexadecimal value
 * @return An integer
 */
private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

/**
 * Converts an IP address represented as 0xAABBCCDD to AAA.BBB.CCC.DDD
 *
 * @param hex Address represented in hex
 * @return String containing normal IPv4 dot notation
 */
private String convertHexToIP(hex) {
    if (hex)
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
    else
    hex
}

/**
 * Tests if this setup can support SmarthThing Labs items
 *
 * @return true if it supports it.
 */
private Boolean canInstallLabs()
{
    return hasAllHubsOver("000.011.00603")
}

/**
 * Tests if the firmwares on all hubs owned by user match or exceed the
 * provided version number.
 *
 * @param desiredFirmware The version that must match or exceed
 * @return true if hub has same or newer
 */
private Boolean hasAllHubsOver(String desiredFirmware)
{
    return realHubFirmwareVersions.every { fw -> fw >= desiredFirmware }
}

/**
 * Creates a list of firmware version for every hub the user has
 *
 * @return List of firmwares
 */
private List getRealHubFirmwareVersions()
{
    return location.hubs*.firmwareVersionString.findAll { it }
}
