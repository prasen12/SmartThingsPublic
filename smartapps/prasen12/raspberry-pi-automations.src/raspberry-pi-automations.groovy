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
    parent: "prasen12:Raspberry Pi Connect",
    author: "Prasen Palvankar",
    description: "Automations for services provided by the Raspberry Pi",
    category: "My Apps",
    iconUrl: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png",
    iconX2Url: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png",
    iconX3Url: "https://www.raspberrypi.org/app/uploads/2011/10/Raspi-PGB001.png")


preferences {
	page (name: "mainPage")
    page (name: "schedulePage")
    page(name: "notAllowedPage")
    
}

def installed() {
	log.debug "Installed with settings: ${settings}"               
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	 def device = parent.getServiceDevice(station);
    log.debug "Service Device = ${device}"   
	app.updateLabel("Automation for ${device}");
    schedule(startTime, startSprinkler);
    schedule(endTime, stopSprinkler);
}

def startSprinkler() {
	log.debug "Start sprinkler called"
    parent.getServiceDevice(station).on()
}

def stopSprinkler() {
	log.debug "Stop sprinkler called"
    parent.getServiceDevice(station).off()
}
/**
 * Pages
 */

def mainPage() {
    // Make sure this is not directly installed
    if (parent) {
    	schedulePage()
      } 
    else {
    	notAllowedPage()
    }

}

def notAllowedPage () {
	dynamicPage(name: "notAllowedPage", title: "This install Method is Not Allowed", install: false, uninstall: true) {
		section() {
			paragraph "Raspberry Pi Automations can't be directly installed from the Marketplace.\n\nPlease use the Raspberry Pi Connect SmartApp to configure them.", required: true,
			state: null, image: getAppImg("disable_icon2.png")
		}
	}
}

def schedulePage() {
	dynamicPage(name: "schedulePage", title: "Sprinkler Schedule", install: true, uninstall: true) {    	
                section("Which Station to Automate?") {
                	input (name: "station", title: "Station", type: "device.raspberryPiIrrigationStation");
            	}    
            	section("At what time?") {
                	input (name: "startTime", title: "Start Time", type:"time");
            	}
                section("At what time do you want it turned off?") {
                	input (name: "endTime", title: "Turn off at", type: "time");
                }
                section("Which days?") {
                	input "days", "enum", multiple: true, title: "Run on specific day(s)", description: "Choose Days", required: true, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]				
                }
               
		   	
        }
}
// TODO: implement event handlers