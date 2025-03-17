import pytest
from sanic import Sanic
from sanic.testing import SanicTestClient
from rasa.core.channels.channel import UserMessage
from customchannel import MyIO  
from unittest.mock import AsyncMock

@pytest.fixture
def test_app():
    """Create a test Sanic app instance."""
    app = Sanic("test_app")
    my_channel = MyIO()
    app.blueprint(my_channel.blueprint(AsyncMock()))  
    return app.test_client

@pytest.mark.asyncio
async def test_webhook_valid_request(test_app: SanicTestClient):
    """Test a valid message sent to the custom webhook."""

    # Mock request payload
    request_data = {
        "sender": "testUser",
        "message": "Hi",
        "metadata": {
            "date": "2025-03-17",
            "userID": "testUser@gmail.com",
            "google_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6IkRFTU9fQ0xJRU5UX0tFWSIsInR5cCI6IkpXVCJ9."
            "eyJpc3MiOiJodHRwczovL2F1dGguZ29vZ2xlLmNvbS9jbGllbnQiLCJhdWQiOiIxMjM0NTY3ODkwMTIzNCIsImV4cCI6MTc4NTMwMDAwMCwiaWF0IjoxNzE2NDAwMDAwLCJzdWIiOiJnb29nbGUtb3RoZXItYXBwIn0."
            "bVbTgRqTw7_LHL8vKYek5OqKQv4Y28bmvD8gGlAsnLY"
        }
    }

    response = await test_app.post("/webhook", json=request_data)
    assert response.status == 200
    response_json = response.json
    assert "messages" in response_json
    assert "metadata" in response_json
    assert "conversation_id" in response_json

    assert response_json["metadata"]["date"] == "2025-03-17"
    assert response_json["metadata"]["userID"] == "12345"
    assert response_json["metadata"]["google_token"] == "test_google_token"

@pytest.mark.asyncio
async def test_webhook_missing_fields(test_app: SanicTestClient):
    """Test a request with missing message fields."""
    
    request_data = {
        "sender": "testUser",
        "metadata": {
            "date": "2025-03-17",
            "userID": "12345"
        }  # Missing "message" and "google_token"
    }

    response = await test_app.post("/webhook", json=request_data)

    # ✅ Check response status (should fail)
    assert response.status == 400
