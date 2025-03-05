import requests  
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from typing import List, Dict, Any
import logging

class ActionSaveJournalEntry(Action):
    
    def name(self) -> str:
        return "action_save_message"
    
    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: dict) -> List[Dict[str, Any]]:
        # Retrieve slot values
        date = tracker.get_slot("date")  
        userID = tracker.get_slot("userID")
        google_token = tracker.get_slot("google_token")  
        message = tracker.get_slot("message")  
        
        # Log retrieved data for debugging
        print(f"Retrieved from frontend - date: {date}, userID: {userID}, google_token: {google_token}, message: {message}")
    
        # Check if all necessary slots are provided
        if not message:
            logging.error("No journal entry provided.")
            dispatcher.utter_message(text="No journal entry provided. Please enter a journal message.")
            return []
        
        if not userID:
            logging.error("No user ID provided.")
            dispatcher.utter_message(text="User ID is missing. Please log in again.")
            return []
        
        if not date:
            logging.error("No entry date provided.")
            dispatcher.utter_message(text="Entry date is missing. Please provide the date.")
            return []

        if not google_token:
            logging.error("No google_token provided.")
            dispatcher.utter_message(text="Google Token is missing. Please log in again.")
            return []
        
        # Call API to save the journal entry
        response = self.save_message(date, userID, message, google_token)
        if response and response.status_code == 200:
            dispatcher.utter_message(text="Your journal entry has been saved successfully!")
        else:
            dispatcher.utter_message(text="There was an error saving your journal entry. Please try again.")
        
        return []
    
    def save_message(self, userID: str, message: str, date: str, google_token: str) -> requests.Response:
        BASE_URL = "https://cpen321project-journal.duckdns.org"
        url = f"{BASE_URL}/api/journal"
        
        # Prepare the data for the POST request
        data = {
            "date": date,  
            "userID": userID,   
            "google_token": google_token, 
            "message": message,  
        }

        # Set the headers for the request
        headers = {
            "Authorization": f"Bearer {google_token}",  
            "Content-Type": "application/json"
        }
        
        try:
            # Send POST request
            response = requests.post(url, json=data, headers=headers)
            print(f"Response Status: {response.status_code}, Response Body: {response.text}")
            
            if response.status_code != 200:
                logging.error(f"Failed to save journal entry. Status code: {response.status_code}, Response Body: {response.text}")
            return response
        except requests.exceptions.RequestException as e:
            # Log the exception and return None
            logging.error(f"Request failed: {e}")
            return None

