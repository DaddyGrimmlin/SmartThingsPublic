/**
 *  Dynamic Event Scheduler
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
    name: "Dynamic Event Scheduler",
    namespace: "DaddyGrimmlin",
    author: "Chris Grimm",
    description: "Dynamically Schedule Events with Basic/Advance Schedules and presence ",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)

 preferences {
    page(name: "page1", title: "Define Event Parameters", nextPage: "page2") {
  		section(title: "Event Name") {
            label(title: "Assign a name", required: true)
		}
		section("For Whom?") {
			input("presenceHome", "capability.presenceSensor", title: "Select Person", required: false, multiple: true)
            //log.debug("presenceHome: $presenceHome")
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

    page(name: "page2", title: "Define Schedule", nextPage: "page3", required: true)

    page(name: "page3", title: "Define Actions", nextPage: "page4", required: true) 

    page(name: "page4", title: "Define Exceptions", install: true, uninstall: true) 
}

def page2() {
    dynamicPage(name: "page2") {
        //log.debug("Entered Scheduler")
        section(title: "Basic Schedule"){
            input("basicDaysOfWeek", "enum", title: "Which Days?", options: [
                "Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"
            ], required: true, multiple: true)  	
            input("basicFrequency", "enum", title: "What Frequency?", options: [
                "Weekly","Biweekly","Every 3 Weeks","Monthly"
            ], required: true, multiple: false)                     
            input("basicTimeOne", "time", title: "Select Start Time", required: true)
            input("basicStart", "enum", title: "How many weeks to offset first run?", options: [
                0,1,2,3,4
            ], required: true, multiple: false)  
            paragraph("Note: The offset must be less than the set Frequency.")
        }    
        section(title: "Advance Options"){
            //input("basicTimeTwo", "time", title: "Select 2nd try start time", required: false)
            input("basicDelay", "number", title: "Delay execution? By How many minutes?", required: false, hideable: true, hidden: true)
            input("basicModeExecute", "mode",title:"Execute only in specifc mode(s)?", multiple: true, required: false, hideable: true, hidden: true) 
            input("basicDurationOne", "number", title: "Do you want to turn ON switches that were turned OFF? After how many minutes?", required: false, hideable: true, hidden: true)
            input("basicDurationTwo", "number", title: "Do you want to turn OFF switches that were turned ON? After how many minutes?", required: false, hideable: true, hidden: true)  
        }   
    }
}

def page3() {
    dynamicPage(name: "page3") {
        //log.debug("Entered Actions Configuration")    
        section(title: "Lights/Virtual Switches") { 
			input("switchesOn", "capability.switch",title: "Turn on which device(s)?", multiple: true, required: false, hideable: true, hidden: true)
			input("switchesOff", "capability.switch",title: "Turn off which device(s)?", multiple: true, required: false, hideable: true, hidden: true)

 		}  
        section(title: "Modes") {           
			input("actionMode", "mode",title: "Switch to which mode?", multiple: false, required: false, hideable: true, hidden: true)
             
 		}
        section(title: "Routine") {           
			// get the available actions
            def actions = location.helloHome?.getPhrases()*.label
            if (actions) {
           	// sort them alphabetically
            actions.sort()
            log.trace actions
            // use the actions as the options for an enum input
            input("routinesTriggers", "enum", title: "Execute which routines?", options: actions, required: false, hideable: true, hidden: true)
            
            }	   	
 		}   
    } 
}

def page4() {
    dynamicPage(name: "page4") {
        //log.debug("routinesTriggers: $routinesTriggers")
        section(title: "Lights/Virtual Switches") {           
			input("switchesOnException", "capability.switch",title:"Do not allow when which device(s) are on?", multiple: true, required: false, hideable: true, hidden: true)
            input("switchesOffException", "capability.switch",title:"Do not allow when which device(s) are off", multiple: true, required: false, hideable: true, hidden: true)
			paragraph("Note: If any of these switches are on/off then exception will be triggered")
 		}
        section(title: "Modes") {           
			input("actionModeException", "mode",title:"Do not allow when which mode(s) are set?", multiple: true, required: false)
 			paragraph("Note: Any mode(s) selected here will override previously selected mode(s) to execute in.")
        }
    }
}

//SmartApp Installation Methods

def installed() {
	log.info("Installed with settings: ${settings}")
	initialize()
}

def updated() {
	log.info("Updated with settings: ${settings}")
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	//log.debug("Starting to initialize SmartApp")
    //Initializes/defines Global Variables
    state.startFlag = false
    state.occurranceCount = basicStart.toInteger()
    state.triggerDay = null
    //state.secondTry = false
    subscribe(presenceHome,"presence",presenceHandler)
    subscribe(switchesOnException, "switch", switchesOnHandler)
    subscribe(switchesOffException, "switch", switchesOffHandler)
    basicSchedule()
}



//SmartApp Main Pogram 

def presenceHandler(evt) {
	//log.debug("evt: ${evt.value}")
    //Sets Global Variable to TRUE if any of the selected people are home
    state.presenceTrigger = presenceHome.latestValue("presence").contains("present")
	//log.debug("presenceTrigger: ${state.presenceTrigger}")
}
def switchesOnHandler(evt) {
	//log.debug("evt: ${evt.value}")
    //Sets Glabal Variable to TRUE if any of the selected switches are on
	state.theOnSwitches = switchesOnException.latestValue("switch").contains("on")
	//log.debug("theOnSwitches: ${state.theOnSwitches}")
}
def switchesOffHandler(evt) {
	//log.debug("evt: ${evt.value}")
    //Sets Glabal Variable to TRUE if any of the selected switches are off
	state.theOffSwitches = switchesOffException.latestValue("switch").contains("off")
	//log.debug("theOffSwitches: ${state.theOffSwitches}")
}
def basicSchedule() {
	//log.debug("Basic Schedule Start")
    //Converts inputs into a Cron String or Array
    def cronSchedule = cronIt(basicTimeOne, null, null, basicDaysOfWeek, null, true)
    //log.debug("cronSchedule = $cronSchedule")
	//Uses Cron String to schedule execution of actions
    schedule(cronSchedule, appAction)
    //log.debug("appAction Scheduled...")   
}

def appAction() {
	//log.debug("App Action!")
    
    //log.debug("startFlag = ${state.startFlag}")
    //Sets trigger day on first exectuion of appAction
    if(state.startFlag == false) {
    	state.startFlag = true
        //log.debug("startFlag = ${state.startFlag}")
        //Sets three letter abbreviation for day of week that first execution occurred 
        state.triggerDay = new Date().format("EEE",location.timeZone)
        //log.debug("triggerDay = ${state.triggerDay}")
    }
    //log.debug("occuranceCount: ${state.occurranceCount}")
    //Determines if appAction should execute actions per correct Frequency and per first run offset, if applicable
    def executeActionFrequency = false
	//if(state.secondTry == false){
        if(basicFrequency == "Weekly" && (state.occurranceCount == 0 || state.occurranceCount == 1)){
            //log.debug("Weekly Trigger!")
            executeActionFrequency = true
            //Resets Frequency run
            state.occurranceCount = 0
        }
        else if(basicFrequency == "Biweekly" && (state.occurranceCount == 0 || state.occurranceCount == 2)){
            //log.debug("Biweekly Trigger!")
            executeActionFrequency = true
            //Resets Frequency run
            state.occurranceCount = 0
        }
        else if(basicFrequency == "Every 3 Weeks" && (state.occurranceCount == 0 || state.occurranceCount == 3)){
            //log.debug("Every 3 Weeks Trigger!")
            executeActionFrequency = true
            //Resets Frequency run
            state.occurranceCount = 0
        }
        else if(basicFrequency == "Monthly" && (state.occurranceCount == 0 || state.occurranceCount == 4)){
            //log.debug("Monthly Trigger!")
            executeActionFrequency = true
            //Resets Frequency run
            state.occurranceCount = 0
        }
        else{
            //log.debug("Error in selection code")
        }
		
        //Increases occurance count by 1, if set trigger day is the current day
        def currentDay = new Date().format("EEE",location.timeZone)
        //log.debug("currentDay = $currentDay")
        if(state.triggerDay == currentDay) {
            state.occurranceCount = state.occurranceCount + 1
            //log.debug("occuranceCount = ${state.occurranceCount}")
        }
    //}

    def executeActionMode = false
	def currMode = location.mode
    //log.debug("Current mode is $currMode")
    //Determines if system is in correct mode to execute actions
    if(basicModeExecute == null){
    	//log.debug("In right mode!")
        executeActionMode = true    
    }
    else if(basicModeExecute.contains(currMode)){
    	//log.debug("In right mode!")
        executeActionMode = true  
    }
    else{
    	//log.debug("In wrong mode!")   
    }
    
    //Determines if the system is in one of the defined exception modes
    if(actionModeException != null && actionModeException.contains(currMode)){
     	//log.debug("Can't execute in this mode!")
        executeActionMode = false     
    }
    
    //Determines if any of the defined exception switches are on or off
    def executeActionDevices = true
    //log.debug("theOnSwitches = ${state.theOnSwitches} theOffSwitches = ${state.theOffSwitches}")
	if(state.theOnSwitches || state.theOffSwitches){
    	executeActionDevices = false
    }

    //log.debug("presenceTrigger = ${state.presenceTrigger}")
    //log.debug("executeActionFrequency = $executeActionFrequency")
    //log.debug("executeActionMode = $executeActionMode")
    //log.debug("executeActionDevices = $executeActionDevices")
	
    //Determines if Frequency, Modes, and Exceptions are in the correct state
    def executeAction = false
	if(executeActionFrequency && executeActionMode && executeActionDevices){
    	executeAction = true
    }
    
    //log.debug("executeAction = $executeAction")
    //Determines if presence and states are correct, then executes defined actions
    if ((state.presenceTrigger || state.presenceTrigger == null) && executeAction) {
    	//state.secondTry = false
    	if(basicDelay == null){
        	performActionOnHandler()
        }
        else{
        	//If delay is defined, then execute on delay
    		runIn(basicDelay*60, performActionOnHandler)
        }
    }
    else{
    	log.info("Action Failed: Presence Trigger = ${state.presenceTrigger} | Frequency Trigger = $executeActionFrequency | Mode Trigger = $executeActionMode | Device Trigger = $executeActionDevices")
		/*if(state.secondTry == false){
            if(basicTimeTwo != null){
                log.info("2nd try scheduled for ${timeToday(basicTimeTwo,TimeZone.getTimeZone('GMT'))}")
                state.secondTry = true
                runOnce(timeToday(basicTimeTwo,TimeZone.getTimeZone('GMT')),appAction)
            }
        }
        else{
        	state.secondTry = false
        }*/
   	}
    //log.debug("Performing Dynamic Event Scheduler: $label")
    send("Performing Dynamic Event Scheduler: $label")
}

def performActionOnHandler() {
	//log.debug("Perform Actions!")
	//Turns on defined switches
	if(switchesOn != null){
		switchesOn.on()
    	//log.info("These switches turned on: $switchesOn")
        //Turns off switches if time is defined
        if(basicDurationOne != null){
    		runIn(basicDurationOne*60, performActionOffHandlerOne)
    	}
    }
    //Turns off defined switches
    if(switchesOff != null){
    	switchesOff.off()
    	//log.info("These switches turned off: $switchesOff")
        //Turns on switches if time is defined
        if(basicDurationTwo != null){
    		runIn(basicDurationTwo*60, performActionOffHandlerTwo)
    	}
    }
    //Sets defined mode
    if(actionMode != null){
    	location.setMode(actionMode)
    	//log.info("Mode set to $actionMode")
    }
    //Executes defined routine
    if(routinesTriggers != null){
    	location.helloHome?.execute(routinesTriggers)
    	//log.info("Executed $routinesTriggers Routine")
    }
}
def performActionOffHandlerOne() {
	//log.debug("Terminate On Switches Actions!")
	//Turns off switches that were turned on
    switchesOn.off()
    //log.info("These switches turned back off: $switchesOn")
}
def performActionOffHandlerTwo() {
	//log.debug("Terminate Off Switches Actions!")
    //Turns on swithces that were turned off
    switchesOff.on()
    //log.info("These switches turned back on: $switchesOff")
}



//SmartApp Methods

def cronIt(rawStartTime, dayOfMonth, rawMonth, rawDayOfWeek, yearValue, stringOption) {
	//rawStartTime:  Java Format Date String. If NULL, starts at 0:0:0
    //dayOfMonth:  Day of month 1-31. If NULL, no specific value is set
    //rawMonth:  One month or multiple months of year, three letter abbrevations.  If NULL, sets for every month
    //rawDayOfWeek:  One day or multiple days of week, full day string. If NULL, no specific value is set
    //yearValue: Sets for specific year. NULL value is valid
    //stringOption:  If TRUE, returns String. If FALSE, returns Array
    
	//log.debug("Started to Cron It")
    //Sets time of day
    def cronValue = []
    if(rawStartTime){
    	//Parses out Java String Format into array [HOUR,MINUTE,SECONDS]
    	def timeOfDay = timeParse(rawStartTime)
    	//log.debug("timeOfDay: $timeOfDay")
        cronValue[0] = timeOfDay[2]
        cronValue[1] = timeOfDay[1]
        cronValue[2] = timeOfDay[0]
    }
    else{
    	cronValue[0] = "0"
        cronValue[1] = "0"
        cronValue[2] = "0"
    }
    //Sets day of month
    if(dayOfMonth){
    	cronValue[3] = dayOfMonth
    }
    else{
    	cronValue[3] = "?"
    } 
    //Set specific month
    if(rawMonth){
    	//Converts String list into a String
    	def selectMonths = listParse(rawMonth,3,",")
        //log.debug("selectMonths = $selectMonths")
        cronValue[4] = selectMonths
    }
    else{
    	cronValue[4] = "*"
    }
    //Sets day of week
    if(rawDayOfWeek){
    	//Converts String list into a String
		def dayOfWeek = listParse(rawDayOfWeek,3,",")
    	//log.debug("dayOfWeek: $dayOfWeek")
        cronValue[5] = dayOfWeek
    }
    else{
    	cronValue[5] = "?"
    }
    //Sets specific year
    if(rawYear){
    	cronValue[6] = yearValue
    }
    //log.debug("cronValue = $cronValue")
    //Converts String List into a String
    def cronResult = listParse(cronValue,null," ")
    //log.debug("cronResult = $cronResult")
    if(stringOption){
    	return(cronResult)
    }
    else{
    	return(cronValue)
    }
}

def listParse(inputList,resultLength,listSeparator){
	//inputList: Array or List of values
    //resultLength: Truncates each string value to specific length
    //listSeparator: Designates a character to seperate the string values with
    
	//log.debug("Started to Parse List")
    //If single string entry instead of an array of strings, converts to array/list
   	def selectList = listCheckConvert(inputList)
	def i = 0
    def listString = ""
    //Converts array to single string
	while(selectList[i] != null){
    	//log.debug("Parsing Loop Iteration: $i || selectList: ${selectList[i]}")
    	if(resultLength){
        	//Builds string and formats each entry to be upper case and truncates it as designated
    		listString = listString + selectList[i++].toUpperCase().substring(0,resultLength)
       	}
        else{
        	//If formating is not required
        	listString = listString + selectList[i++]
        }
        if(selectList[i] != null){
        	//Adds string designated separator 
        	listString = listString + listSeparator
        }
        //log.debug("listString = $listString")
   	}
    //log.debug("listString (Final) = $listString")
    return(listString)
}

def listCheckConvert(inputList) {
	//inputList: String or List of Strings

	//log.debug("Started to Check and/or Convert List")
    //If single string then length will be 1 and will need to be converted to a array/list
    def length = inputList[0].length()
    //log.debug("$length")
    
    if(length > 1) {
    	return(inputList)
    }
    else {
    	def outputList = []
        outputList << inputList
        //log.debug("$outputList[0]")
    	return(outputList)
    }
}

def timeParse(inputTime) {
	//Takes Java Format String and returns an array/list of [Hour,Minute,Seconds]
	//log.debug("Started to Parse Time: $inputTime")
	def outputTime = []
	outputTime[0] = timeToday(inputTime, TimeZone.getTimeZone('GMT')).format("HH")
    outputTime[1] = timeToday(inputTime, TimeZone.getTimeZone('GMT')).format("mm")
    outputTime[2] = timeToday(inputTime, TimeZone.getTimeZone('GMT')).format("ss")
    //log.debug("outputTime: $outputTime") 
    return(outputTime)
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