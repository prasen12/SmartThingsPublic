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

		attribute "stationId", "string"
		attribute "switchUpdatedOn", "string"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		standardTile("irrigationStation", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.Outdoor.outdoor12", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.Outdoor.outdoor12", backgroundColor: "#00A0DC", nextState: "off"
		}
       
	//main (["irrigationStation", "basicTile"])
	}
}

def installed() {
	log.debug("Irrigation station device handler, installed as ${device.name}")
	sendEvent(name: "switch", value: "off");
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
    parent.turnStationOn(device.currentValue("stationId"));
}

void off() {
	log.debug "Executing 'off'"
	sendEvent(name: "switch", value: "off");
    sendEvent(name: "switchUpdatedOn", value: new Date().format('yyyy-M-d hh:mm:ss'))
    parent.turnStationOff(device.currentValue("stationId"));
}

