from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from typing import List, Dict, Any
import requests
import logging

class ActionSaveJournalEntry(Action):
    
    def name(self) -> str:
        return "action_save_journal_entry"
    
    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: dict) -> List[Dict[str, Any]]:
        journal_entry = tracker.get_slot("journal_entry")
        entry_date = tracker.get_slot("date")  # Getting date from frontend
        user_id = tracker.get_slot("userID")
        user_google_token = tracker.get_slot("google_token")

        print(user_id, entry_date, journal_entry)
        
        if journal_entry and user_id and entry_date:
            response = self.save_journal_entry(user_id, journal_entry, entry_date, user_google_token)
            if response and response.status_code == 200:
                dispatcher.utter_message(text="Your journal entry has been saved successfully!")
            else:
                dispatcher.utter_message(text="There was an error saving your journal entry.")
        else:
            dispatcher.utter_message(text="No journal entry provided.")
        
        return []
    
    def save_journal_entry(self, user_id: str, journal_entry: str, entry_date: str, user_google_token: str) -> requests.Response:
        #Using BASE URL
        BASE_URL = "https://cpen321project-journal.duckdns.org"
        url = f"{BASE_URL}/api/journal"
        
        
        # Prepare the data to send
        data = {
            "date": entry_date,  # The date comes from the slot
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
