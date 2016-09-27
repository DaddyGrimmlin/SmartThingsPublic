/**
 *  Holiday Scheduler
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
    name: "Holiday Scheduler",
    namespace: "DaddyGrimmlin/parent",
    singleInstance: true,
    author: "Chris Grimm",
    description: "Parent Container for Holiday Scheduler Child SmartApps",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png")


preferences {
    //Input for the child app.
    page(name: "mainPage", title: "Government Holiday Scheduler", install: true, uninstall: true,submitOnChange: true) {
        section {
            app(name: "govHolidayScheduler", appName: "Government Holiday Scheduler", namespace: "DaddyGrimmlin/schedulers", title: "Create New Goverment Holiday Scheduler", multiple: true)
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
	initialize()
}

def initialize() {
    log.debug("there are ${childApps.size()} child smartapps")
    childApps.each {child ->
        log.debug("child app: ${child.label}")
    }
}

// TODO: implement event handlers