from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk.events import FollowupAction

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

        # Check for missing metadata or very short messages
        if not all([date, userID, google_token, message]):
            dispatcher.utter_message(text="Apologies, something went wrong while saving your entry. Let's try again.")
            return [FollowupAction("utter_journaling_prompt")]

        if len(message.strip()) < 10:
            dispatcher.utter_message(text="That felt a bit short 🤏 — want to expand on it a little?")
            return [FollowupAction("utter_journaling_prompt")]

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
                dispatcher.utter_message(text="Boom! 💣 Your thoughts are locked in. Keep it going!")
                dispatcher.utter_message(response="utter_post_journal_options")
                return []
            else:
                logging.error(f"Save failed: {response.status_code} - {response.text}")
                dispatcher.utter_message(text="Hmm, that didn’t save right. Let’s try again:")
                return [FollowupAction("utter_journaling_prompt")]

        except Exception as e:
            logging.exception("Exception during journal save")
            dispatcher.utter_message(text="There was a problem saving your journal. Let's try again:")
            return [FollowupAction("utter_journaling_prompt")]
