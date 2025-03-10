from rasa.core.channels.rest import RestInput
from sanic.request import Request
from sanic import Blueprint
import json

class CustomRestInput(RestInput):
    @staticmethod
    def _extract_metadata(req: Request):
        """Extract metadata from the request JSON."""
        return req.json.get("metadata", {})

    @staticmethod
    async def receive(request: Request):
        sender_id = request.json.get("sender", None)
        text = request.json.get("message", "")
        metadata = CustomRestInput._extract_metadata(request)

        try:
            from rasa.shared.core.events import UserUttered
            from rasa.core.channels.channel import UserMessage, InputChannel
            from rasa.shared.core.trackers import DialogueStateTracker

            input_channel = request.json.get("input_channel") or "custom_rest"
            user_msg = UserMessage(text, None, sender_id, input_channel=input_channel, metadata=metadata)

            return user_msg
        except Exception as e:
            return {"error": str(e)}

    def blueprint(self, on_new_message):
        custom_webhook = Blueprint("custom_webhook", __name__)

        @custom_webhook.route("/webhook", methods=["POST"])
        async def webhook(request: Request):
            sender_id = request.json.get("sender", None)
            text = request.json.get("message", "")
            metadata = self._extract_metadata(request)

            try:
                await on_new_message(
                    UserMessage(text, None, sender_id, metadata=metadata)
                )
                return response.json({"status": "ok"})
            except Exception as e:
                return response.json({"error": str(e)}, status=400)

        return custom_webhook
