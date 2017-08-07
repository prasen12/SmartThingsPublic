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
		capability "Beacon"
		capability "Buffered Video Capture"
		capability "Contact Sensor"
		capability "Image Capture"
		capability "Motion Sensor"
		capability "Sensor"
		capability "Switch"
		capability "Valve"
        
        attribute "cpuTemp", "number"
        attribute "cpuLoad", "number"
	}


	tiles(scale:2 ){
    		standardTile("switchTile", "device.switch", width: 2, height: 2,
                 canChangeIcon: true) {
                state "off", label: '${name}', action: "switch.on",
                      icon: "st.Outdoor.outdoor13", backgroundColor: "#ffffff"
                state "on", label: '${name}', action: "switch.off",
                      icon: "st.Outdoor.outdoor13", backgroundColor: "#E60000"
    		}
          
           childDeviceTile("irrigationStation1", "station1", height: 2, width: 2, childTileName: "irrigationStation")
           childDeviceTile("irrigationStation2", "station2", height: 2, width: 2, childTileName: "irrigationStation")
          
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
    
    if (json) {
    	
    
        log.debug("Temp = ${json}")
       /** if (json.type == 'pi.health') {
            log.debug("Creating cpu data events, temp = ${json.data.cpuTemp}");
            cpuEvent = createEvent(name: "cpuTemp", value: json.data.cpuTemp);
            loadEvent = createEvent(name: "cpuLoad", value: json.data.load.last1);
        }**/
        switch (json.type) {
            case ('pi.health'):
                log.debug("Creating cpu data events, temp = ${json.data.cpuTemp}");
                cpuEvent = createEvent(name: "cpuTemp", value: json.data.cpuTemp);
                loadEvent = createEvent(name: "cpuLoad", value: json.data.load.last1);
                break;
            case ('controller.irrigation.stations'):
                log.debug "Received list of irrigation stations ${json.data}, child devices created = ${state.childDevicesCreated}"
                if (getChildDevices().size() == 0){
                    state.createdDevices = [:];
                    createChildDevices(json.data.stations, state.createdDevices);
                    state.childDevicesCreated = true;
                }
                break;
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
    return [cpuEvent, loadEvent]
}

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
    }
}



// handle commands
def capture() {
	log.debug "'capture' not implemented"
	// TODO: handle 'capture' command
}

def take() {
	log.debug "'take' not implemented"
	// TODO: handle 'take' command
}

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

def open() {
	log.debug "'open' not implemented"
	// TODO: handle 'open' command
}

def close() {
	log.debug "'close' not implemented"
	// TODO: handle 'close' command
}

// Setup child devices on being installed
def installed() {
	log.debug "Setting up child devices"
    log.debug "Device is ${device.displayName}"
    state.childDevicesCreated = false;
    
    /**
  	 addChildDevice(
							"Rpi Irrigation Station",
							"${device.deviceNetworkId}.station1",
							null,
							[completedSetup: true, label: "${device.label} (Irrigation Station)", componentName: "irrigationStation", componentLabel: "Irrigiation Station"])
  */
   getStations();
   
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

def turnStationOn(station) {
	log.debug "Turning station on "
    def hubAction = new physicalgraph.device.HubAction(
    	method: "PUT",      
        path: "/api/irrigation/stations/${station}",
        headers: [
        	HOST: getHost()
        ],
        body: [
        	action: "on"
        ]
    )
    log.debug("Action ${hubAction}");
   	sendHubCommand(hubAction)
}

def turnStationOff(station) {
	log.debug "Turning station off "
   def hubAction = new physicalgraph.device.HubAction(
    	[method: "PUT",      
        path: "/api/irrigation/stations/${station}",
        headers: [
        	HOST: getHost()
        ],
        body: [
        	action: "off"
        ]
        
        ]
    )
    log.debug("Action ${hubAction}");
  	sendHubCommand(hubAction)
}

def executeActions(action) {
	def method= "GET"
    def path = "/api/irrigation/stations/station1"
          
    log.debug("Invoking Rpi method = ${method}, path = ${path}")
	def result = new physicalgraph.device.HubAction(
    	method: method,      
        path: path,
        headers: [
        	HOST: getHost()
        ]
    )
    log.debug "executeActions ${result}"
    return result
    
}

def getStations() {
	log.debug "getStations()"
	def hubAction = new physicalgraph.device.HubAction(
    	method: "GET",      
        path: "/api/irrigation/stations",
        headers: [
        	HOST: getHost()
        ]
    )
	log.debug "getStations hubAction = ${hubAction}"
	sendHubCommand(hubAction)
    
}

def getHost() {
    log.debug "Device ${device.displayName}, ${device.label} ${device.deviceNetworkId}, hub=${device.getHub()}"
	if (device.displayName == 'Virtual') 
    	return "10.0.0.36:9000"
    else
		return(getDataValue("ip") + ":" + getDataValue("port"));
}