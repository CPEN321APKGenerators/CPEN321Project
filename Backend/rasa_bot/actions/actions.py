from datetime import datetime
import logging
import requests
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from typing import List, Dict, Any

class ActionSaveJournalEntry(Action):
    
    def name(self) -> str:
        return "action_save_journal_entry"
    
    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: dict) -> List[Dict[str, Any]]:
        # Get the journal entry, date, and user ID from slots
        journal_entry = tracker.get_slot("journal_entry")
        entry_date = datetime.now().strftime('%Y-%m-%d')  
        user_id = tracker.get_slot("userID")
        user_google_token = tracker.get_slot("google_token")  

        print(user_id, entry_date, journal_entry)
        if journal_entry and user_id:
            response = self.save_journal_entry(user_id, journal_entry, entry_date, user_google_token)
            if response and response.status_code == 200:
                dispatcher.utter_message(text="Your journal entry has been saved successfully!")
            else:
                dispatcher.utter_message(text="There was an error saving your journal entry.")
        else:
            dispatcher.utter_message(text="No journal entry provided.")
        
        return []
    
    def save_journal_entry(self, user_id: str, journal_entry: str, entry_date: str, user_google_token: str) -> requests.Response:
        url = "http://ec2-35-183-201-213.ca-central-1.compute.amazonaws.com:5000/api/journal"
        
        data = {
            "date": entry_date,  # Ensure date is in ISO8601 format
            "userID": user_id,  
            "text": journal_entry,
        }

        # Prepare the request body with headers
        headers = {
            "Authorization": f"Bearer {user_google_token}",  # Include Authorization token
            "Content-Type": "application/json"
        }
        
        try:
            # Send POST request to backend API
            response = requests.post(url, json=data, headers=headers)
            if response.status_code != 200:
                logging.error(f"Failed to save journal entry. Status code: {response.status_code}")
            return response
        except requests.exceptions.RequestException as e:
            logging.error(f"Request failed: {e}")
            return None
