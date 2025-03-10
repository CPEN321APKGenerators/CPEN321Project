from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
import openai
import logging
import json

class ActionSaveMessage(Action):
    def name(self):
        return "action_save_message"

    def run(self, dispatcher, tracker, domain):
        # Retrieve slot values
        date = tracker.get_slot("date")
        userID = tracker.get_slot("userID")
        google_token = tracker.get_slot("google_token")
        message = tracker.get_slot("message")

        logging.info(f"Retrieved slots -> date: {date}, userID: {userID}, google_token: {google_token}, message: {message}")

        # Ensure all required fields are captured
        if not all([date, userID, google_token, message]):
            logging.error("Missing required journal entry fields.")
            dispatcher.utter_message(text="Failed to save journal entry. Missing information.")
            return []

        # Call GPT to analyze or enhance journal entry
        gpt_response = openai.ChatCompletion.create(
            model="gpt-4-0613",
            messages=[
                {"role": "system", "content": "You are a journaling assistant that extracts and processes journal entries."},
                {"role": "user", "content": message}
            ]
        )

        gpt_analysis = gpt_response["choices"][0]["message"]["content"]
        logging.info(f"GPT Response: {gpt_analysis}")

        # Send request to backend API
        payload = {
            "date": date,
            "userID": userID,
            "google_token": google_token,  
            "message": gpt_analysis  
        }

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {google_token}"
        }

        response = requests.post("https://cpen321project-journal.duckdns.org/api/journal", json=payload, headers=headers)

        if response.status_code == 200:
            logging.info("Journal entry saved successfully.")
            dispatcher.utter_message(text="Your journal entry has been processed and saved successfully.")
        else:
            logging.error(f"Failed to save journal entry. Status: {response.status_code}, Response: {response.text}")
            dispatcher.utter_message(text="Failed to save journal entry. Please try again later.")

        return []
