from typing import Any, Text, Dict, List
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
import requests
import logging

class ActionSaveMessage(Action):
    def name(self) -> Text:
        return "action_save_message"

    async def run(
        self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: Dict[Text, Any]
    ) -> List[Dict[Text, Any]]:

        # Retrieve slot values
        date = tracker.get_slot("date")  
        userID = tracker.get_slot("userID")
        google_token = tracker.get_slot("google_token")  
        message = tracker.get_slot("message")  

        logging.info(f"Retrieved slots -> date: {date}, userID: {userID}, google_token: {google_token}, message: {message}")

        # If any field is missing, log and return error
        if not all([date, userID, google_token, message]):
            logging.error("Missing required journal entry fields.")
            dispatcher.utter_message(text="Failed to save journal entry. Missing information.")
            return []

        # Construct API request payload
        payload = {
            "date": date,
            "userID": userID,
            "google_token": google_token,
            "message": message
        }

        # Send request to API
        api_url = "https://cpen321project-journal.duckdns.org/api/journal"
        headers = {"Content-Type": "application/json"}

        try:
            response = requests.post(api_url, headers=headers, json=payload)
            if response.status_code == 200:
                dispatcher.utter_message(text="Journal entry saved successfully!")
            else:
                logging.error(f"API Error: {response.status_code}, {response.text}")
                dispatcher.utter_message(text="Error saving journal entry.")
        except Exception as e:
            logging.error(f"Exception: {str(e)}")
            dispatcher.utter_message(text="Could not connect to journal API.")

        return []
