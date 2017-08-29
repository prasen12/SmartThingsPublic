/**
 *  Raspberry Pi Automations
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
    name: "Raspberry Pi Automations",
    namespace: "prasen12",
    parent: "prasen.palvankar@gmail.com:Raspberry Pi (Connect)",
    author: "Prasen Palvankar",
    description: "Automations for services provided by the Raspberry Pi",
    category: "My Apps",
    iconUrl: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png",
    iconX2Url: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png",
    iconX3Url: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png")


preferences {
    section("Which Station to Automate?") {
        input (name: "station", title: "Station", type: "device.raspberryPiIrrigationStation");
    }    
    section("At what time?") {
        input (name: "startTime", title: "Start Time", type:"time");
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"        
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
        state.t ="D";
}

/**
 * Pages
 */

def mainPage() {
    // Make sure this is not directly installed
//    if (parent) {
//        if (atomicState?.isInstalled && parent?.state?.okToInstallAutomation == true){
//            atomicState?.isParent = false;
//        }
//    }

}

// TODO: implement event handlers