const request = require("supertest");
const app = require("../server");
const axios = require("axios");


jest.mock("axios");
beforeEach(() => {
  jest.clearAllMocks();
});

describe("API Tests for RASA Bot", () => {
  //Test /api/chat 
  test("POST /api/chat - Valid request", async () => {
    const mockResponse = { responses: [{ text: "Hello! How can I help you?" }] };
    axios.post.mockResolvedValueOnce({ data: mockResponse });
    const res = await request(app)
      .post("/api/chat")
      .send({ message: "Hi", sender: "testUser" });
    expect(res.status).toBe(200);
    expect(res.body.responses[0].text).toBe("Please type start to begin journaling.");
  });

  // ❌ Test /api/chat - Missing parameters
  test("POST /api/chat - Missing message", async () => {
    const res = await request(app)
      .post("/api/chat")
      .send({ sender: "testUser" }); // Missing "message"

    expect(res.status).toBe(400);
    expect(res.body).toEqual({ error: "Message and sender are required" });
  });

  // ❌ Test /api/chat - RASA server error
  test("POST /api/chat - RASA server error", async () => {
    axios.post.mockRejectedValueOnce(new Error("RASA API Down"));

    const res = await request(app)
      .post("/api/chat")
      .send({ message: "Hi", sender: "testUser" });

    expect(res.status).toBe(500);
    expect(res.body).toEqual({ error: "Failed to get response from RASA" });
  });

  // ✅ Test /api/action - Successful case
  test("POST /api/action - Valid request", async () => {
    const mockActionResponse = { responses: [{ text: "Action executed" }] };

    axios.post.mockResolvedValueOnce({ data: mockActionResponse });

    const res = await request(app)
      .post("/api/action")
      .send({ sender: "testUser", tracker: {}, domain: {} });

    expect(res.status).toBe(200);
    expect(res.body.responses[0].text).toBe("Action executed");
  });

  // ❌ Test /api/action - Missing sender
  test("POST /api/action - Missing sender", async () => {
    const res = await request(app)
      .post("/api/action")
      .send({ tracker: {}, domain: {} });

    expect(res.status).toBe(400);
    expect(res.body).toEqual({ error: "Sender is required" });
  });

  // ❌ Test /api/action - Action server failure
  test("POST /api/action - Action server error", async () => {
    axios.post.mockRejectedValueOnce(new Error("Action server down"));

    const res = await request(app)
      .post("/api/action")
      .send({ sender: "testUser", tracker: {}, domain: {} });

    expect(res.status).toBe(500);
    expect(res.body).toEqual({ error: "Failed to get response from RASA Action Server" });
  });

  // ✅ Test /api/health - Health check route
  test("GET /api/health - Check server status", async () => {
    const res = await request(app).get("/api/health");

    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: "Node.js API is running" });
  });
});
