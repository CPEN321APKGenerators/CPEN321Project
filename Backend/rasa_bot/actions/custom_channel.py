import inspect
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
            f"custom_webhook_{type(self).__name__}",
            inspect.getmodule(self).__name__,
        )

        @custom_webhook.route("/", methods=["GET"])
        async def health(request: Request) -> HTTPResponse:
            return response.json({"status": "ok"})

        @custom_webhook.route("/webhook", methods=["POST"])
        async def receive(request: Request) -> HTTPResponse:
            try:
                sender_id = request.json.get("sender")
                text = request.json.get("message")
                metadata = request.json.get("metadata", {})

                if not sender_id or not text:
                    return response.json(
                        {"error": "Missing 'sender' or 'message'"}, status=400
                    )

                collector = CollectingOutputChannel()

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
                }, status=200)

            except Exception as e:
                return response.json({
                    "error": "Internal Server Error",
                    "details": str(e)
                }, status=500)

        return custom_webhook
