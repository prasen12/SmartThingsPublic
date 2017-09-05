/**
 *  Raspberry Pi Irrigation Station
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
	definition (name: "Raspberry Pi Irrigation Station", namespace: "prasen12", author: "Prasen Palvankar") {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"
        capability "Refresh"

		attribute "serviceId", "string"
		attribute "switchUpdatedOn", "string"
        attribute "serviceName", "string"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
    /**
		standardTile("irrigationStation1", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", action: "switch.on", icon: "st.Outdoor.outdoor12", backgroundColor: "#ffffff", nextState: "on", label: "${stationName} Off"
			state "on", action: "switch.off", icon: "st.Outdoor.outdoor12", backgroundColor: "#00A0DC", nextState: "off", label: "${stationName} On"
		}
**/
	multiAttributeTile (name: "irrigationStation", type: "generic", width: 4, height: 4) {
        	tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
            	attributeState ("off", action: "switch.on", icon: "st.Outdoor.outdoor12", backgroundColor: "#00BB00", nextState: "on", label: "Off")
				attributeState ("on", action: "switch.off", icon: "st.Outdoor.outdoor12", backgroundColor: "#00A0DC", nextState: "off", label: "ON")
            }
        	tileAttribute("device.serviceName", key: "SECONDARY_CONTROL") {
            	attributeState ("serviceName", label: '${currentValue} ', defaultState: true)
            }
        
        }
       
	//main (["irrigationStation", "basicTile"])
	}
}

def installed() {
	log.debug("Irrigation station device handler, installed as ${device.name}")

	//sendEvent(name: "switch", value: "off");
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	

}

// handle commands
void on() {
	log.debug "Executing 'on'"
	sendEvent(name: "switch", value: "on");
    sendEvent(name: "switchUpdatedOn", value: new Date().format('yyyy-M-d hh:mm:ss'))
    turnStationOn();
}

void off() {
	log.debug "Executing 'off'"
	sendEvent(name: "switch", value: "off");
    sendEvent(name: "switchUpdatedOn", value: new Date().format('yyyy-M-d hh:mm:ss'))
    turnStationOff();
}


void refresh() {
	log.debug "Executing refresh()"
    updateStatus(device.currentValue("serviceId"))
}
/**
 * Update the current status of the service
 * Called by the parent on device creation and refresh
 */
def updateStatus(serviceId) {
	log.debug "Getting station status for  ${serviceId} "
    def hubAction = new physicalgraph.device.HubAction(
    	[
    		method: "GET",      
        	path: "/api/irrigation/stations/${serviceId}",
        	headers: [
        		HOST: parent.getHost()
        	]
        ],
        null,
        [callback: handleStatusResponse]
    )
    log.debug("Action ${hubAction}");
   	sendHubCommand(hubAction)
}

def handleStatusResponse(physicalgraph.device.HubResponse hubResponse) {
	log.debug "handleSatusResponse()"
	def response = hubResponse.json
    
    log.debug("Status response = ${response}")
    if (response.requestStatus == "OK") {
    	sendEvent(name: "switch", value: response.data.status);
		sendEvent(name: "switchUpdatedOn", value: new Date().format('yyyy-M-d hh:mm:ss'))
    } else {
    	log.error("Failed to get status for irrigation station ${device.currentValue('serviceId')}")    	
    }
    

}

def turnStationOn() {
	log.debug "Turning station on "
    def station = device.currentValue('serviceId')
    def hubAction = new physicalgraph.device.HubAction(
    	method: "PUT",      
        path: "/api/irrigation/stations/${station}",
        headers: [
        	HOST: parent.getHost()
        ],
        body: [
        	action: "on"
        ]
    )
    log.debug("Action ${hubAction}");
   	sendHubCommand(hubAction)
}

def turnStationOff() {
	log.debug "Turning station off "
    def station = device.currentValue('serviceId')
    def hubAction = new physicalgraph.device.HubAction(
    	[method: "PUT",      
        path: "/api/irrigation/stations/${station}",
        headers: [
        	HOST: parent.getHost()
        ],
        body: [
        	action: "off"
        ]
        
        ]
    )
    log.debug("Action ${hubAction}");
  	sendHubCommand(hubAction)
}


