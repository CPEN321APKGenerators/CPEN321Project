import inspect
import json
from sanic import Blueprint, response
from sanic.request import Request
from sanic.response import HTTPResponse
from typing import Text, Callable, Awaitable

from rasa.core.channels.channel import (
    InputChannel,
    CollectingOutputChannel,
    UserMessage,
)


class MyIO(InputChannel):
    def name(self) -> Text:
        """Name of the custom channel."""
        return "myio"

    def blueprint(
        self, on_new_message: Callable[[UserMessage], Awaitable[None]]
    ) -> Blueprint:
        """Sanic blueprint for the custom webhook."""

        custom_webhook = Blueprint(
            "custom_webhook_{}".format(type(self).__name__),
            inspect.getmodule(self).__name__,
        )

        @custom_webhook.route("/", methods=["GET"])
        async def health(request: Request) -> HTTPResponse:
            return response.json({"status": "ok"})

        @custom_webhook.route("/webhook", methods=["POST"])
        async def receive(request: Request) -> HTTPResponse:
            sender_id = request.json.get("sender")  # Extract sender ID
            text = request.json.get("message")  # Extract message
            metadata = request.json.get("metadata", {})  # Extract metadata

            collector = CollectingOutputChannel()

            # Send message to Rasa with metadata
            await on_new_message(
                UserMessage(
                    text=text,
                    output_channel=collector,
                    sender_id=sender_id,
                    metadata=metadata,
                )
            )

            return response.json({
                "messages": collector.messages,
                "metadata": metadata,
                "conversation_id": sender_id
            })

        return custom_webhook
