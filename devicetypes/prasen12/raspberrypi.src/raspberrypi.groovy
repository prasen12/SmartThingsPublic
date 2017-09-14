/**
 *  RaspberryPi
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
metadata {
    definition (name: "RaspberryPi", namespace: "prasen12", author: "Prasen Palvankar") {
        capability "Actuator"		
        capability "Switch"
        capability "Refresh"
        
        attribute "cpuTemp", "number"
        attribute "cpuLoad", "number"
        attribute "freeMem", "number"       
        attribute "totalMem", "number"
        attribute "connectStatus", "string"
        
        command	"reboot"
    }


    tiles(scale:2){
    
        /**
        standardTile("switchTile", "device.switch", width: 3, height: 4,
        canChangeIcon: true) {
        state "off", label: '${name}', action: "switch.on",
        icon: "st.Outdoor.outdoor13", backgroundColor: "#ffffff"
        state "on", label: '${name}', action: "switch.off",
        icon: "st.Outdoor.outdoor13", backgroundColor: "#E60000"
        }
         **/
        standardTile("title1", "device.connectStatus", width: 2, height: 2, decoration: "flat") {
            state "ONLINE", label: "Online",  icon: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png"
            state "OFFLINE", label: "Offline", backgroundColor: "#FF0000",  icon: "https://www.raspberrypi.org/app/themes/mind-control/images/logo-white.png"
        }
        standardTile("reboot", "command.reboot", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: "Reboot", action:"reboot", icon:"st.secondary.activity", backgroundColor: "#FF0000"
        }
        standardTile("refresh", "command.refresh", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh", backgroundColor: "#42BF34"
        }
        valueTile("cpuTemp", "device.cpuTemp", width: 2, height: 2, decoration: "flat") {
            state "val", label:'${currentValue}\nF', defaultState: true, backgroundColor: "#00a0dc"
        }
        valueTile("totalMem", "device.totalMem", width: 2, height: 2, decoration: "flat") {
            state "val", label:'${currentValue}', defaultState: true, backgroundColor: "#A55A5A"
        }
        valueTile("freeMem", "device.freeMem", width: 2, height: 2, decoration: "flat") {
            state "val", label:'${currentValue}', defaultState: true, backgroundColor: "#A55A5A"
        }
        /**
        valueTile("cpuLoad", "device.text", width: 6, height: 2) {
        state "val", label:'Services', defaultState: true
        }
         */
   
          
          
        childDeviceTile("irrigationStation1", "irrigationStation1", height: 4, width: 6, childTileName: "irrigationStation")
        childDeviceTile("irrigationStation2", "irrigationStation2", height: 4, width: 6, childTileName: "irrigationStation")
          
    }
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
	
    def msg = parseLanMessage(description)
    log.debug("${msg}")
    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps

    def cpuEvent;
    def loadEvent;
    def freeMemEvent;
    def totalMemEvent;
    
    if (json) {
    	
    
        log.debug("Temp = ${json}")
      
        switch (json.type) {
        case ('pi.health'):
            state.refreshed = true;
            log.debug("Creating cpu data events, temp = ${json.data.cpuTemp}");
            cpuEvent = createEvent(name: "cpuTemp", value: json.data.cpuTemp);
            loadEvent = createEvent(name: "cpuLoad", value: (String.format("Load Avg\n\n%1.4f\nLast 1 Minute", json.data.load.last1)));
            freeMemEvent = createEvent(name: "freeMem", value: (String.format("%4.0f MB\nFree", (json.data.freeMem/1024/1024))));                
            totalMemEvent = createEvent(name: "totalMem", value: (String.format("%4.0f MB\nTotal", (json.data.totalMem/1024/1024))));
            connectStatusEvent = createEvent (name: "connectStatus", value: "ONLINE");
            break;
            /**
            case ('controller.irrigation.stations'):
            log.debug "Received list of irrigation stations ${json.data}, child devices created = ${state.childDevicesCreated}"
            if (getChildDevices().size() == 0){
            state.createdDevices = [:];
            createChildDevices(json.data.stations, state.createdDevices);
            state.childDevicesCreated = true;
            }
            break;
             **/
        case ('controller.irrigation.state'):
            log.debug "Received state for irrigation station ${json.data}"
            log.debug ("Child devices = ${state.childDevices}, ${state.createdDevices}")

            def cd = getChildDevices();

            for (d in cd) {
                if (d.currentValue("stationId") == json.data.id) {
                    d.sendEvent(name: "switch", value: json.data.status);
                }
            }
            break;
        }
    
    }
    return [cpuEvent, loadEvent, freeMemEvent, totalMemEvent, connectStatusEvent]
}

/**
def createChildDevices(stations, createdDevices) {
log.debug("createChildDevices()");
for (station in stations) {
log.debug "Creating child device for irrigation station ${station}"
def childDevice = addChildDevice(
"Raspberry Pi Irrigation Station",
"${device.deviceNetworkId}.${station.id}",
null,
[completedSetup: true, label: "${device.label} (Irrigation Station)", componentName: station.id, componentLabel: "Irrigiation Station"])
childDevice.sendEvent(name: "stationId", value: station.id);
childDevice.sendEvent(name: "stationName", value: station.name)
}
}
 */


// handle commands

def on() {
    log.debug "Executing 'on' , device.switch = ${state['switch']}"
    def ip = getDataValue("ip");
    sendEvent(name: "switch", value: "on")   
    
    return executeActions("on")
}

def off() {
    log.debug "Executing 'off', device.switch = "	
    sendEvent(name: "switch", value: "off")
    return executeActions("off")
}

def refresh() {
    log.debug "Executing refresh()"
    if (!state.refreshed) {
        sendEvent(name: "connectStatus", value: "OFFLINE")
    }
    def children = getChildDevices()
    children.each { child -> 
    	log.debug "Initiaing refresh on -- ${child.displayName}"
        child.refresh()
    }
    state.refreshed = false;
    getRPiHealth()
    // Auto-refresh every 5 minutes    
    runIn(300, "refresh")
}

def reboot() {
    log.debug "Excuting reboot()"
    
}
// Setup child devices on being installed
def installed() {
    log.debug "Setting up child devices"
    log.debug "Device is ${device.displayName}"
    state.childDevicesCreated = false;
    log.debug("Selected services = ${parent.getSelectedServices()}")
    
    getRPiHealth()
    createChildDevices1()
    state.refreshed = true;
}

def createChildDevices1() {	
    log.debug("createChildDevices()")
    def selectedServices = parent.getSelectedServices()
    log.debug "createChidDevices1(${selectedServices})"
    def services = parent.getServices();
    log.debug "All services from parent = ${services}"
    log.debug "selected service 0 = ${selectedServices[0]}"
    selectedServices.each { selectedService ->
     	log.debug "Finding service for ${selectedService}"
     	def service = services.find { service -> 
            service.id == selectedService
     	}
        if (service != null) {
            def deviceType = getDeviceType(service.type)
            if (deviceType == null) {
            	log.warn("Service type ${service.type} not yet supported");
            } else {
            	log.debug "Creating device of type - ${deviceType}"
            	def childDevice = addChildDevice(
                    deviceType,
							"${device.deviceNetworkId}.${service.id}",
                    device.getHub().getId(),
                    [completedSetup: true, label: "${device.label} (${service.name})", componentName: service.id, componentLabel: "${service.name}"])
                childDevice.sendEvent(name: "serviceId", value: service.id);
                childDevice.sendEvent(name: "serviceName", value: service.name)
                childDevice.updateStatus(service.id);
                parent.setServiceDevice(childDevice);
            }
        
        	
        }
    }
}


def getDeviceType(String serviceType) {
    def deviceTypes = [:];
    deviceTypes << ['irrigation': 'Raspberry Pi Irrigation Station']
    //deviceTypes << ['switch': 'Raspberry Pi Switch Control']
    return deviceTypes[serviceType]
}

def updated() {
    log.debug "Updated"
    if (getChildDevices().size() == 0){
     	// If there are no child devices on update, for some reason they
        // weren't created on install. Try again.
        //TODO: Add new, delete devices without corresponding station in the RPi response
     	getStations();
    }
    
}

def getRPiHealth() {
    def hubAction = new physicalgraph.device.HubAction(
    	[
            method: "GET",      
            path: "/api/pi/health",
            headers: [
                HOST: getHost()
            ]
        ]
    )
    log.debug("Action ${hubAction}");
    sendHubCommand(hubAction)

}

def getHost() {
    log.debug "Device ${device.displayName}, ${device.label} ${device.deviceNetworkId}, hub=${device.getHub()}"
    if (device.displayName == 'Virtual') 
    return "10.0.0.36:9000"
    else
    return(getDataValue("ip") + ":" + getDataValue("port"));
}

def getServiceDevice(objId) {
    def devices = getChildDevices()
    return devices.find( {it.id == objId} )
}