/**
 *  My Raspberry Pi Handler
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
	definition (name: "RaspberryPi", namespace: "prasen.palvankar@gmail.com", author: "Prasen Palvankar") {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"
        attribute "cpuTemp", "number"
       
	}

	

	tiles {
    		standardTile("switchTile", "device.switch", width: 2, height: 2,
                 canChangeIcon: true) {
                state "off", label: '${name}', action: "switch.on",
                      icon: "st.Outdoor.outdoor12", backgroundColor: "#ffffff"
                state "on", label: '${name}', action: "switch.off",
                      icon: "st.Outdoor.outdoor12", backgroundColor: "#E60000"
    		}
            valueTile("powerTile", "device.cpuTemp", decoration: "flat") {
                      state "power", label:'${currentValue} F'
            }
            standardTile("refreshTile", "device.power", decoration: "ring") {
                state "default", label:'', action:"refresh.refresh",
                      icon:"st.secondary.refresh"
    }

    main "switchTile"
    details(["switchTile","powerTile","refreshTile"])
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

    log.debug("Temp = ${json.data.value}")
    def cpuEvent = createEvent(name: "cpuTemp", value: json.data.value);
    def switchEvent = createEvent(name: "switch", value: "off");
	
    return [cpuEvent, switchEvent]
}

// handle commands
def on() {
	log.debug "Executing 'on' , device.switch = ${state['switch']}"
	def ip = getDataValue("ip");
    log.debug "${ip}"
    executeActions("on")
    
	// TODO: handle 'on' command
}

def off() {
	log.debug "Executing 'off', device.switch = "	   
	executeActions("off")
}

def executeActions(action) {
	def result = new physicalgraph.device.HubAction(
    	method: "GET",      
        path: "/cpuTemp",
        headers: [
        	HOST: getDataValue("ip") + ":" + getDataValue("port")
        ]
    )
    result
    
}

