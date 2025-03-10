from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
import requests
import logging

class ActionSaveMessage(Action):
    def name(self):
        return "action_save_message"

    def run(self, dispatcher, tracker, domain):
        # Retrieve slot values
        date = tracker.get_slot("date")
        userID = tracker.get_slot("userID")
        google_token = tracker.get_slot("google_token")
        message = tracker.get_slot("message")

        # Log retrieved values
        logging.info(f"Retrieved slots -> date: {date}, userID: {userID}, google_token: {google_token}, message: {message}")

        # Validate required fields
        if not all([date, userID, google_token, message]):
            logging.error("Missing required journal entry fields.")
            dispatcher.utter_message(text="Failed to save journal entry. Missing information.")
            return []

        # Construct request payload
        payload = {
            "date": date,
            "userID": userID,
            "google_token": google_token,  
            "message": message
        }

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {google_token}"
        }

        # Send request to journal API
        response = requests.post("https://cpen321project-journal.duckdns.org/api/journal", json=payload, headers=headers)

        if response.status_code == 200:
            logging.info("Journal entry saved successfully.")
            dispatcher.utter_message(text="Your journal entry has been saved successfully.")
        else:
            logging.error(f"Failed to save journal entry. Status: {response.status_code}, Response: {response.text}")
            dispatcher.utter_message(text="Failed to save journal entry. Please try again later.")

        return []
