from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
import requests
import logging


class ActionSaveMessage(Action):
    def name(self):
        return "action_save_message"

    def run(self, dispatcher: CollectingDispatcher, tracker: Tracker, domain: dict):
        metadata = tracker.latest_message.get("metadata", {})
        date = metadata.get("date")
        userID = metadata.get("userID")
        google_token = metadata.get("google_token")
        message = tracker.latest_message.get("text")

        logging.info(f"[SaveMessage] Retrieved -> date: {date}, userID: {userID}, token: {bool(google_token)}, message: {bool(message)}")

        if not all([date, userID, google_token, message]):
            dispatcher.utter_message(text="Apologies, something went wrong while saving your entry. Let's try again.")
            dispatcher.utter_message(response="utter_journaling_prompt")
            return []

        payload = {
            "date": date,
            "userID": userID,
            "google_token": google_token,
            "text": message
        }

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {google_token}"
        }

        try:
            response = requests.post(
                "https://cpen321project-journal.duckdns.org/api/journal",
                json=payload,
                headers=headers,
                timeout=8
            )

            if response.status_code == 200:
                logging.info("Journal entry saved successfully.")
            else:
                logging.error(f"Failed with status {response.status_code}: {response.text}")
                dispatcher.utter_message(text="Hmm, that didn’t save right. Let’s try that again:")
                dispatcher.utter_message(response="utter_journaling_prompt")
        except Exception as e:
            logging.exception("Error during journal save")
            dispatcher.utter_message(text="There was a problem saving your journal. Let's try again:")
            dispatcher.utter_message(response="utter_journaling_prompt")

        return []
