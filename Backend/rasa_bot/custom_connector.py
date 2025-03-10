import json
import inspect
from sanic import Blueprint, response
from sanic.request import Request
from sanic.response import HTTPResponse
from typing import Text, Dict, Any, Optional, Callable, Awaitable

from rasa.core.channels.channel import (
    InputChannel,
    CollectingOutputChannel,
    UserMessage,
)

class CustomRestInput(InputChannel):
    """A custom REST input channel that extracts metadata."""

    def name(self) -> Text:
        return "custom_rest"

    def blueprint(
        self, on_new_message: Callable[[UserMessage], Awaitable[None]]
    ) -> Blueprint:

        custom_webhook = Blueprint(
            "custom_webhook_{}".format(type(self).__name__),
            inspect.getmodule(self).__name__,
        )

        @custom_webhook.route("/", methods=["GET"])
        async def health(request: Request) -> HTTPResponse:
            return response.json({"status": "ok"})

        @custom_webhook.route("/webhook", methods=["POST"])
        async def receive(request: Request) -> HTTPResponse:
            """Receives user messages and extracts metadata."""

            sender_id = request.json.get("sender", "default_sender")
            text = request.json.get("message", "")
            metadata = request.json.get("metadata", {})  # Extract metadata

            collector = CollectingOutputChannel()

            await on_new_message(
                UserMessage(
                    text,
                    collector,
                    sender_id,
                    input_channel=self.name(),
                    metadata=metadata,  # Pass metadata to UserMessage
                )
            )

            return response.json(collector.messages)

        return custom_webhook
