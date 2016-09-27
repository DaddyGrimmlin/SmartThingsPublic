/**
 *  Government Holiday Scheduler
 *
 *  Copyright 2016 Chris Grimm
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
    name: "Government Holiday Scheduler",
    namespace: "DaddyGrimmlin/schedulers",
    parent: "DaddyGrimmlin/parent:Holiday Scheduler",
    author: "Chris Grimm",
    description: "Schedule Government Holidays that will allow switching modes, execute routines, or turn on/off switches. Child SmartApp of Holiday Scheduler",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
    )


preferences {
    page(name: "page1", title: "Configure Holiday Scheduler", nextPage: "page2") {
    
  		section(title: "What Holiday do you want to schedule?") {
            input("selectedHoliday", "enum", title: "Choose one...", options: [
            "New Year's Day", "MLK Day", "President's Day", "Memorial Day", "Independence Day", "Labor Day", "Columbus Day","Veterans' Day", "Thanksgiving Day", "Day After Thanksgiving", "Christmas Eve", "Christmas Day" ], required: true, multiple: false)
		}
        section(title: "Modes") { 
        	input("modeExecute", "mode",title:"Execute only in specifc mode(s)?", multiple: true, required: false) 
        }
		section("Notifications") {
       		input("recipients", "contact", title: "Send notifications to") {
        		input("sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false)
    			input("phoneNumber", "phone", title: "Send a text message?", required: false)
                //log.debug("sendPushMessage: $sendPushMessage")       
                //log.debug("phoneNumber: $phoneNumber")   
       		}
		}
    }

    page(name: "page2", title: "Define Actions", nextPage: "page3")
	page(name: "page3", title: "Name SmartApp", install: true, uninstall: true)

}

def page2() {
    dynamicPage(name: "page2") {
        //log.debug("Entered Actions Configuration") 

        section(title: "Lights/Virtual Switches") { 
			input("switchesOn", "capability.switch",title: "Turn on which device(s)?", multiple: true, required: false)
			input("switchesOff", "capability.switch",title: "Turn off which device(s)?", multiple: true, required: false)
 			input("invertSwitches", "bool", title: "Invert Switches after Holiday?", required: false)
        }  
        section(title: "Modes") {           
			input("actionMode", "mode",title: "Switch to which mode?", multiple: false, required: false)    
 		}
        section(title: "Routine") {           
			// get the available actions
            def actions = location.helloHome?.getPhrases()*.label
            if (actions) {
           	// sort them alphabetically
            actions.sort()
            log.trace actions
            // use the actions as the options for an enum input
            input("routinesTriggers", "enum", title: "Execute which routines?", options: actions, required: false)
            
            }	   	
 		}   
    } 
}
// page for allowing the user to give the automation a custom name
def page3() {
    if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }
    dynamicPage(name: "page3") {
        if (overrideLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } 
        else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}


def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
    // if the user did not override the label, set the label to the default
    if (!overrideLabel) {
        app.updateLabel(defaultLabel())
    }
    state.pointDay = null
	holidaySchedule()
}

//Main Program
def test(){
	log.info("test")
}
def holidaySchedule(){
	//Schedule New Year's Day
    if(selectedHoliday == "New Year's Day"){
    	//Check if Holiday falls on a Saturday or Sunday
        state.pointDay = true
    	schedule("0 0 0 30 12 ? *", preCheck)
    }
	//Schedule MLK Day
    if(selectedHoliday == "MLK Day"){
    	 state.pointDay = false
		schedule("0 0 0 ? 1 MON#3 *", preCheck)
    }    
	//Schedule President's Day
    if(selectedHoliday == "President's Day"){
     	state.pointDay = false
		schedule("0 0 0 ? 2 MON#3 *", preCheck)
    }
    //Schedule Memorial Day
    if(selectedHoliday == "Memorial Day"){
    	state.pointDay = false
		schedule("0 0 12 ? 5 MON#4 *", preCheck)
    }
	//Schedule Independence Day
    if(selectedHoliday == "Independence Day"){
    	//Check if Holiday falls on a Saturday or Sunday
        state.pointDay = true
    	schedule("0 0 0 2 7 ? *", preCheck)
    }
	//Schedule Labor Day
    if(selectedHoliday == "Labor Day"){
    	state.pointDay = false
		schedule("0 0 0 ? 9 MON#1 *", preCheck)
    }
	//Schedule Columbus Day
    if(selectedHoliday == "Columbus Day"){
     	state.pointDay = false
		schedule("0 0 0 ? 10 MON#2 *", preCheck)
    }
	//Schedule Veterans' Day
    if(selectedHoliday == "Veterans' Day"){
        //Check if Holiday falls on a Saturday or Sunday
        state.pointDay = true
    	schedule("0 0 0 9 11 ? *", preCheck)
    }
	//Schedule Thanksgiving Day
    if(selectedHoliday == "Thanksgiving Day"){
     	state.pointDay = false
		schedule("0 0 0 ? 11 THU#4 *", preCheck)
    }
 	//Schedule Day After Thanksgiving Day
    if(selectedHoliday == "Day After Thanksgiving"){
     	state.pointDay = false
		schedule("0 0 0 ? 11 FRI#4 *", preCheck)
    }
	//Schedule Christmas Eve
    if(selectedHoliday == "Christmas Eve"){
        //Check if Holiday falls on a Saturday or Sunday
        state.pointDay = true
    	schedule("0 0 0 22 12 ? *", preCheck)
    }
	//Schedule Christmas Day
    if(selectedHoliday == "Christmas Day"){
        //Check if Holiday falls on a Saturday or Sunday
        state.pointDay = true
    	schedule("0 0 0 23 12 ? *", preCheck)
    }    
}
def preCheck(){
	log.debug("Performing preCheck Method")

    if(state.pointDay){
        log.debug("Specific Date Holiday")
        //Captures current weekday 
        def currentDay = new Date().format("EEE",location.timeZone)
        log.debug("currentDay: $currentDay")
        //If current day is Thursday; then holiday falls on a Saturday. Friday is the work holiday
        if(currentDay == "Thu"){
            //Execute actions in 24 hours  (i.e. on Friday)
            runOnce(new Date() + 1,performActionHandler())
        }
        //If current day is Friday; then holiday falls on a Sunday. Monday is the work holiday
        else if(currentDay == "Fri"){
            //Execute actions in 72 hours  (i.e. on Monday)
            runOnce(new Date() + 3,performActionHandler())
        }
        else{
            //Excute actions in 48 hours (i.e. on actual holiday date)
            runOnce(new Date() + 2,performActionHandler())
        }
    }
    else{
        def currentPointDay = new Date().format("dd",location.timeZone)
        //If there are five Mondays in May
        if(selectedHoliday == "Memorial Day" && (currentPointDay == 22 ||currentPointDay == 23)){
            runOnce(new Date() + 7,performActionHandler())
        }
        else{
            performActionHandler()
        }
    }

}

def performActionHandler() {
	log.debug("Perform Actions!")
   
    def currMode = location.mode
    log.debug("Current mode is $currMode")
    //Determines if system is in correct mode to execute actions
    if(modeExecute == null || modeExecute.contains(currMode) == true){
    	log.debug("In right mode!") 
        	//Turns on defined switches
        if(switchesOn != null){
            switchesOn.on()
            log.info("These switches turned on: $switchesOn")
            //Turns off switches after Holiday (24 hours)
            if(invertSwitches){
                runIn(86400, performActionOffHandlerOne)
            }
        }
        //Turns off defined switches
        if(switchesOff != null){
            switchesOff.off()
            log.info("These switches turned off: $switchesOff")
            //Turns off switches after Holiday (24 hours)
            if(invertSwitches){
                runIn(86400, performActionOffHandlerTwo)
            }
        }
        //Sets defined mode
        if(actionMode != null){
            location.setMode(actionMode)
            log.info("Mode set to $actionMode")
        }
        //Executes defined routine
        if(routinesTriggers != null){
            location.helloHome?.execute(routinesTriggers)
            log.info("Executed $routinesTriggers Routine")
        }

        log.debug("Performing Government Holiday Scheduler: $selectedHoliday")
        send("Performing Government Holiday Scheduler: $selectedHoliday")
    }
    else{
    	log.debug("In wrong mode!")   
    }
}
def performActionOffHandlerOne() {
	log.debug("Terminate On Switches Actions!")
	//Turns off switches that were turned on
    switchesOn.off()
    log.info("These switches turned back off: $switchesOn")
}
def performActionOffHandlerTwo() {
	log.debug("Terminate Off Switches Actions!")
    //Turns on swithces that were turned off
    switchesOff.on()
    log.info("These switches turned back on: $switchesOff")
}
//Supporting Methods

// a method that will set the default label of the automation.
// It uses the lights selected and action to create the automation label
def defaultLabel() {
    return("Holiday Scheduler: $selectedHoliday")
}
private send(msg) {
	//Sends notification to specified user
    if (location.contactBookEnabled) {
        //log.debug("Sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage == "Yes") {
            //log.debug("Sending push message")
            sendPush(msg)
        }

        if (phoneNumber) {
            //log.debug("Sending text message")
            sendSms(phoneNumber, msg)
        }
    }

	//log.debug msg
}

private getLabel() {
	app.label ?: "DaddyGrimmlin"
}