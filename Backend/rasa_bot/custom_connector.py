from typing import Text, Dict, Any
from rasa.core.channels.rest import RestInput
from rasa.core.channels.channel import UserMessage, InputChannel
from sanic import Blueprint, response
from sanic.request import Request
import json

class CustomRestInput(RestInput):
    @staticmethod
    def _extract_metadata(req: Request) -> Dict[Text, Any]:
        return req.json.get("metadata", {})  

    def blueprint(self, on_new_message):
        custom_webhook = Blueprint("custom_webhook", __name__)

        @custom_webhook.route("/webhook", methods=["POST"])
        async def receive(request: Request):
            sender_id = request.json.get("sender", "default")
            text = request.json.get("message", "")
            metadata = self._extract_metadata(request)

            try:
                await on_new_message(
                    UserMessage(text, None, sender_id, input_channel=self.name(), metadata=metadata)
                )
            except Exception as e:
                print(f"Error: {e}")

            return response.json({"status": "success"})

        return custom_webhook
