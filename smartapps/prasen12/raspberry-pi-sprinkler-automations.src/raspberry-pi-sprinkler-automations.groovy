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
    name: "Raspberry Pi Sprinkler Automations",
    namespace: "prasen12",
    parent: "prasen12:Raspberry Pi Connect",
    author: "Prasen Palvankar",
    description: "Automations for Sprinklers controlled by the Raspberry Pi",
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
    app.updateLabel("Sprinkler Automation: ${automationName}");
    schedule(buildCrontabEntry(startTime), startSprinkler);
    schedule(buildCrontabEntry(endTime), stopSprinkler);
    def weather = getWeatherFeature("forecast")
    def forecastDay = weather.simpleforecast.forecastday[0]
    log.debug("Weather for ${forecastDay.date.pretty} - High ${forecastDay.high.fahrenheit}")
}

def startSprinkler() {
    log.debug "Start sprinkler called"
    def sprinkler = parent.getServiceDevice(station);
    log.info "Turning on sprinkler ${sprinkler.displayName}"        
    sprinkler.on()
    if (pushNotifyOnStart) {
        sendPush("Turning on sprinkler ${sprinkler.displayName}")
    }
}

def stopSprinkler() {
    log.debug "Stop sprinkler called"
    def sprinkler = parent.getServiceDevice(station);
    log.info "Turning off sprinkler ${sprinkler.displayName}"        
    sprinkler.off()
    if (pushNotifyOnStop) {
        sendPush("Turning off sprinkler ${sprinkler.displayName}")
    }
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
    section("Which Sprinkler to Automate?") {
        input (name: "station", title: "Sprinkler", type: "device.raspberryPiIrrigationStation");
        input (name: "automationName", title: "Automation Name", type: "string")
    }    
    section("Schedule") {
        input (name: "startTime", title: "Start Time", type:"time")
        input (name: "endTime", title: "Turn off at", type: "time");
        input (name: "scheduleDays", type: "enum", multiple: true, title: "Run on specific day(s)", description: "Choose Days", required: true, options: getWeekDays());			
    }
    section("Don't turn on the sprinkler if ... ") {
        input (name: "skipOnRainDay", title: "Rain in forecast for the scheduled day", type: "boolean", required: false)
        input (name: "skipIfRained", title: "It has rained  between 1 and 7 days ago", type: "number", range: "1..7", required: false)                        
    }
    section("Increase the sprinkler time if ...") {
        input (name: "incrementDuration", title: "Temperature forecast is for scheduled day is greater than ", type: "number", required: false)
    }
    section("Notifications") {
        input (name: "pushNotifyOnStart", title: "Send push notification when sprinklers are started?", required: false, type: "bool")
        input (name: "pushNotifyOnStop", title: "Send push notification when sprinklers are stopped?", required: false, type: "bool")
    }
               
		   	
}
}

def buildCrontabEntry(scheduleTime){
def d = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(scheduleTime);

def df = new java.text.SimpleDateFormat("'0 'mm' 'HH' ? * '")
df.setTimeZone(location.timeZone);
def cronEntry = df.format(d)
scheduleDays.eachWithIndex ({ day, idx ->
        def wd = getWeekDays().indexOf(day);
        if (wd != -1) {
            if (idx > 0) {
                cronEntry = cronEntry + ","
            }
            cronEntry = cronEntry + (wd+1)
        }
    })
log.debug "CRON schedule string is ${cronEntry}"
return cronEntry

    
}

def getWeekDays() {
["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
}