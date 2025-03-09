import requests
import logging
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from typing import List, Dict, Any

class ActionSaveJournalEntry(Action):
    
    def name(self) -> str:
        return "action_save_message"
    
    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: dict) -> List[Dict[str, Any]]:
        # Retrieve slot values
        date = tracker.get_slot("date")  
        userID = tracker.get_slot("userID")
        googleNumID = tracker.get_slot("googleNumID")  
        message = tracker.get_slot("message")  
        
        # Log retrieved data for debugging
        logging.info(f"Retrieved slots -> date: {date}, userID: {userID}, googleNumID: {googleNumID}, message: {message}")

        # Check if all necessary slots are provided
        if not all([date, userID, googleNumID, message]):
            logging.error("Missing required journal entry fields.")
            dispatcher.utter_message(text="Failed to save journal entry. Missing information.")
            return []

        # Call API to save the journal entry
        response = self.save_message(date, userID, message, googleNumID)
        if response and response.status_code == 200:
            dispatcher.utter_message(text="Your journal entry has been saved successfully!")
        else:
            dispatcher.utter_message(text="There was an error saving your journal entry. Please try again.")
        
        return []
    
    def save_message(self, date: str, userID: str, message: str, googleNumID: str) -> requests.Response:
        BASE_URL = "https://cpen321project-journal.duckdns.org"
        url = f"{BASE_URL}/api/journal"
        
        # Prepare the data for the POST request
        data = {
            "date": date,  
            "userID": userID,   
            "googleNumID": googleNumID,  
            "message": message,  
        }

        # Set the headers for the request
        headers = {
            "Authorization": f"Bearer {googleNumID}",  
            "Content-Type": "application/json"
        }
        
        try:
            # Send POST request
            response = requests.post(url, json=data, headers=headers)
            logging.info(f"Response Status: {response.status_code}, Response Body: {response.text}")
            
            if response.status_code != 200:
                logging.error(f"Failed to save journal entry. Status: {response.status_code}, Response: {response.text}")
            return response
        except requests.exceptions.RequestException as e:
            logging.error(f"Request failed: {e}")
            return None
