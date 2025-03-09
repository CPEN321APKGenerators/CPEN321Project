import requests
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from typing import Dict, Any, List
import logging

class ActionSaveJournalEntry(Action):
    
    def name(self) -> str:
        return "action_save_message"
    
    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[str, Any]) -> List[Dict[str, Any]]:
        # Retrieve slot values
        date = tracker.get_slot("date")  
        userID = tracker.get_slot("userID")
        google_token = tracker.get_slot("google_token")  
        message = tracker.get_slot("message")  

        # Extract GoogleNumID from google_token
        googleNumID = self.extract_google_num_id(google_token)

        # Log retrieved data for debugging
        logging.info(f"Received journal entry: date={date}, userID={userID}, google_token={google_token}, message={message}, googleNumID={googleNumID}")

        # Validate required fields
        if not all([date, userID, google_token, message, googleNumID]):
            logging.error("Missing required journal entry fields.")
            dispatcher.utter_message(text="Failed to save journal entry. Missing information.")
            return []

        # Send API request
        response = self.save_message(date, userID, googleNumID, message, google_token)
        if response and response.status_code == 200:
            dispatcher.utter_message(text="Congrats on being consistent, your entry is saved and processed.")
        else:
            dispatcher.utter_message(text="There was an error saving your journal entry. Please try again.")

        return []
    
    def save_message(self, date: str, userID: str, googleNumID: str, message: str, google_token: str) -> requests.Response:
        API_URL = "https://cpen321project-journal.duckdns.org/api/journal"
        
        # Prepare the payload
        data = {
            "date": date,
            "userID": userID,
            "googleNumID": googleNumID,
            "message": message
        }

        # Set the headers
        headers = {
            "Authorization": f"Bearer {google_token}",
            "Content-Type": "application/json"
        }
        
        try:
            # Send the request
            response = requests.post(API_URL, json=data, headers=headers)
            logging.info(f"API Response: {response.status_code} - {response.text}")
            return response
        except requests.exceptions.RequestException as e:
            logging.error(f"API request failed: {e}")
            return None

    def extract_google_num_id(self, google_token: str) -> str:
        """ Extracts the GoogleNumID from the ID token """
        try:
            import jwt
            decoded_token = jwt.decode(google_token, options={"verify_signature": False})
            return decoded_token.get("sub", "")
        except Exception as e:
            logging.error(f"Failed to decode Google token: {e}")
            return ""
