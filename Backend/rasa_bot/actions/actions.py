import requests  
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from typing import List, Dict, Any
import logging

class ActionSaveJournalEntry(Action):
    
    def name(self) -> str:
        return "action_save_journal_entry"
    
    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: dict) -> List[Dict[str, Any]]:
        journal_entry = tracker.get_slot("journal_entry")
        entry_date = tracker.get_slot("date")  # Getting date from frontend
        user_id = tracker.get_slot("userID")
        user_google_token = tracker.get_slot("google_token")
        print(f"Retrieved from frontend - user_id: {user_id}, entry_date: {entry_date}, journal_entry: {journal_entry}, user_google_token: {user_google_token}")
        
        # Check to see if we get required data from front end 
        if not journal_entry:
            logging.error("No journal entry provided.")
            dispatcher.utter_message(text="No journal entry provided.")
            return []
        
        if not user_id:
            logging.error("No user ID provided.")
            dispatcher.utter_message(text="User ID is missing.")
            return []
        
        if not entry_date:
            logging.error("No entry date provided.")
            dispatcher.utter_message(text="Entry date is missing.")
            return []
        
        if not user_google_token:
            logging.error("No user Google token provided.")
            dispatcher.utter_message(text="User Google token is missing.")
            return []
        
        response = self.save_journal_entry(user_id, journal_entry, entry_date, user_google_token)
        if response and response.status_code == 200:
            dispatcher.utter_message(text="Your journal entry has been saved successfully!")
        else:
            dispatcher.utter_message(text="There was an error saving your journal entry.")
        
        return []
    
    def save_journal_entry(self, user_id: str, journal_entry: str, entry_date: str, user_google_token: str) -> requests.Response:
        BASE_URL = "https://cpen321project-journal.duckdns.org"
        url = f"{BASE_URL}/api/journal"
        
        # Prepare the data to send 
        data = {
            "date": entry_date,  # The date comes from the slot
            "userID": user_id,   
            "text": journal_entry,  
            "media": []  # Empty media 
        }

        # Prepare the headers
        headers = {
            "Authorization": f"Bearer {user_google_token}",
            "Content-Type": "application/json"
        }
        
        # Send POST request 
        try:
            response = requests.post(url, json=data, headers=headers)
            print(f"Response Status: {response.status_code}, Response Body: {response.text}")
            
            if response.status_code != 200:
                logging.error(f"Failed to save journal entry. Status code: {response.status_code}, Response Body: {response.text}")
            return response
        except requests.exceptions.RequestException as e:
            logging.error(f"Request failed: {e}")
            return None
