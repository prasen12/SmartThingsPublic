/**
 *  RaspberryPi Service Manager
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
		name: "RaspberryPi UPnP Service Manager",
		namespace: "prasen.palvankar@gmail.com",
		author: "Prasen Palvankar",
		description: "Raspberry Pi Service Manager based on Smartthings Generic UPNP Service Manager",
		category: "My Apps",
		iconUrl: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png",
		iconX2Url: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png",
		iconX3Url: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png"
        ) {
        	appSetting "rpiURN"
        }

/**
preferences {
	page(name: "searchTargetSelection", title: "UPnP Search Target", nextPage: "deviceDiscovery") {
		section("Search Target") {
			input "searchTarget", "string", title: "Search Target", defaultValue: "urn:schemas-upnp-org:device:RaspberryPiDevice:1", required: true
		}
	}
	page(name: "deviceDiscovery", title: "UPnP Device Setup", content: "deviceDiscovery")
}
*/

def deviceDiscovery() {
	def options = [:]
	def devices = getVerifiedDevices()
	devices.each {
		def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		options["${key}"] = value
	}

	ssdpSubscribe()

	ssdpDiscover()
	verifyDevices()

	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started!", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		section("Please wait while we discover your UPnP Device. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)", multiple: true, options: options
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${appSettings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${appSettings}"

	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "Initializing...";
	unsubscribe()
	unschedule()

	ssdpSubscribe()

//	if (selectedDevices) {
//		addDevices()
//	}
	ssdpDiscover();
	//runEvery5Minutes("ssdpDiscover")
}

void ssdpDiscover() {
	log.debug "Sending discovery command to hub for ${appSettings.rpiURN}"
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${appSettings.rpiURN}", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
	subscribe(location, "ssdpTerm.${appSettings.rpiURN}", ssdpHandler)
}

Map verifiedDevices() {
	
	def devices = getVerifiedDevices()
	def map = [:]
	devices.each {
		def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		map["${key}"] = value
	}
	map
}

void verifyDevices() {
	log.debug "Verify devices"
	def devices = getDevices().findAll { it?.value?.verified != true }
	devices.each {
    	log.debug ">> ${it.value.deviceAddress}"
		int port = convertHexToInt(it.value.deviceAddress)
		String ip = convertHexToIP(it.value.networkAddress)
		String host = "${ip}:${port}"
        log.debug ">> host = ${host}"
		sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
	}
}

def getVerifiedDevices() {
	getDevices().findAll{ it.value.verified == true }
}

def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	state.devices
}

def addDevices() {
	def devices = getDevices()

	selectedDevices.each { dni ->
		def selectedDevice = devices.find { it.value.mac == dni }
		def d
		if (selectedDevice) {
			d = getChildDevices()?.find {
				it.deviceNetworkId == selectedDevice.value.mac
			}
		}

		if (!d) {
			log.debug "Creating Generic UPnP Device with dni: ${selectedDevice.value.mac}"
			addChildDevice("smartthings", "Generic UPnP Device", selectedDevice.value.mac, selectedDevice?.value.hub, [
				"label": selectedDevice?.value?.name ?: "Generic UPnP Device",
				"data": [
					"mac": selectedDevice.value.mac,
					"ip": selectedDevice.value.networkAddress,
					"port": selectedDevice.value.deviceAddress
				]
			])
		}
	}
}

def ssdpHandler(evt) {
	log.debug "SSDP handler got ${evt.description}"
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]

	def devices = getDevices()
	String ssdpUSN = parsedEvent.ssdpUSN.toString()
    log.debug "ssdpUSN=${ssdpUSN}"
	if (devices."${ssdpUSN}") {
		def d = devices."${ssdpUSN}"
		if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
			d.networkAddress = parsedEvent.networkAddress
			d.deviceAddress = parsedEvent.deviceAddress
			def child = getChildDevice(parsedEvent.mac)
			if (child) {
				child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
			}
		}
	} else {
		devices << ["${ssdpUSN}": parsedEvent]
	}
    log.debug "devices = ${devices}"
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "deviceDescriptionHandler"
	def body = hubResponse.xml
	def devices = getDevices()
    log.debug "UDN = ${body.device.UDN.text()}"
	def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
    log.debug "Device = ${device}"
	if (device) {
		device.value << [name: body?.device?.roomName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true]
	}
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}