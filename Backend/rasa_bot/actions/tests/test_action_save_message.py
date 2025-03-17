import pytest
import requests
import requests_mock
import logging
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk.interfaces import Tracker

from actions import ActionSaveMessage 

@pytest.fixture
def dispatcher():
    return CollectingDispatcher()

@pytest.fixture
def tracker():
    return Tracker(
        sender_id="user1",
        slots={},
        latest_message={
            "text": "Test journal entry",
            "metadata": {
                "date": "2025-03-17",
                "userID": "12345",
                "google_token": "test_google_token"
            }
        },
        events=[],
        paused=False,
        followup_action=None,
        active_loop=None,
        latest_action_name=None
    )

@pytest.fixture
def action():
    return ActionSaveMessage()

def test_action_save_message_success(dispatcher, tracker, action):
    with requests_mock.Mocker() as m:
        m.post(
            "https://cpen321project-journal.duckdns.org/api/journal",
            json={},
            status_code=200
        )

        events = action.run(dispatcher, tracker, {})
        messages = dispatcher.messages
        assert len(messages) == 1
        assert messages[0]["text"] == "Your journal entry has been saved successfully."

def test_action_save_message_missing_fields(dispatcher, action):
    tracker = Tracker(
        sender_id="user1",
        slots={},
        latest_message={
            "text": "Test journal entry",
            "metadata": {
                "date": None,
                "userID": "12345",
                "google_token": None
            }
        },
        events=[],
        paused=False,
        followup_action=None,
        active_loop=None,
        latest_action_name=None
    )

    events = action.run(dispatcher, tracker, {})
    messages = dispatcher.messages
    assert len(messages) == 1
    assert messages[0]["text"] == "Failed to save journal entry. Missing information."

def test_action_save_message_api_failure(dispatcher, tracker, action):
    with requests_mock.Mocker() as m:
        m.post(
            "https://cpen321project-journal.duckdns.org/api/journal",
            json={"error": "Server error"},
            status_code=500
        )

        events = action.run(dispatcher, tracker, {})
        messages = dispatcher.messages
        assert len(messages) == 1
        assert messages[0]["text"] == "Failed to save journal entry. Please try again later."
