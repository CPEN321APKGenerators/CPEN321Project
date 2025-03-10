import inspect
import json
from sanic import Blueprint, response
from sanic.request import Request
from sanic.response import HTTPResponse
from typing import Text, Dict, Any, Callable, Awaitable

import rasa.utils.endpoints
from rasa.core.channels.channel import (
    InputChannel,
    CollectingOutputChannel,
    UserMessage,
)

class MyIO(InputChannel):
    """Custom REST Input channel to receive metadata."""

    def name(self) -> Text:
        """Name of the custom channel."""
        return "myio"

    def get_metadata(self, request: Request) -> Dict[Text, Any]:
        """Extract metadata from request JSON."""
        return request.json.get("metadata", {})

    def blueprint(
        self, on_new_message: Callable[[UserMessage], Awaitable[None]]
    ) -> Blueprint:
        """Define the blueprint for the custom webhook."""

        custom_webhook = Blueprint(
            "custom_webhook_{}".format(type(self).__name__),
            inspect.getmodule(self).__name__,
        )

        @custom_webhook.route("/", methods=["GET"])
        async def health(request: Request) -> HTTPResponse:
            return response.json({"status": "ok"})

        @custom_webhook.route("/webhook", methods=["POST"])
        async def receive(request: Request) -> HTTPResponse:
            """Handle user messages and extract metadata."""

            sender_id = request.json.get("sender", "default_sender")
            text = request.json.get("message", "")  # Corrected key
            metadata = self.get_metadata(request)  # Extract metadata

            collector = CollectingOutputChannel()

            await on_new_message(
                UserMessage(
                    text,
                    collector,
                    sender_id,
                    input_channel=self.name(),
                    metadata=metadata,
                    headers=request.headers,
                )
            )

            response_dict = {
                "messages": collector.messages,
                "metadata": metadata,
                "conversation_id": sender_id,
                "tracker_state": collector.tracker_state,
            }
            return response.json(response_dict)

        return custom_webhook
