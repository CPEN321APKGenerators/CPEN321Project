const request = require("supertest");
const { app, server } = require("../server");
const axios = require("axios");

jest.mock("axios");

// Suppress logs in tests
beforeAll(() => {
    jest.spyOn(console, "error").mockImplementation(() => {}); 
    jest.spyOn(console, "warn").mockImplementation(() => {});  
});

afterAll((done) => {
    server.close(done); 
});

//**Mocked Tests**
describe("API Tests for RASA Bot", () => {
    test("POST /api/chat - Valid request", async () => {
        const mockResponse = { responses: [{ text: "Hello! How can I help you?" }] };
        axios.post.mockResolvedValueOnce({ data: mockResponse });

        const res = await request(server) 
            .post("/api/chat")
            .send({ message: "Hi", sender: "testUser" });

        expect(res.status).toBe(200);
        expect(res.body.responses[0].text).toBe("Hello! How can I help you?");
    });

    test("POST /api/chat - Missing message", async () => {
        const res = await request(server).post("/api/chat").send({ sender: "testUser" });
        expect(res.status).toBe(400);
        expect(res.body).toEqual({ error: "Message and sender are required" });
    });

    test("POST /api/chat - RASA server error", async () => {
        axios.post.mockRejectedValueOnce(new Error("RASA API Down"));

        const res = await request(server)
            .post("/api/chat")
            .send({ message: "Hi", sender: "testUser" });

        expect(res.status).toBe(500);
        expect(res.body).toEqual({ error: "Failed to get response from RASA" });
    });

    test("POST /api/action - Valid request", async () => {
        const mockResponse = { responses: [{ text: "Action executed" }] };
        axios.post.mockResolvedValueOnce({ data: mockResponse });

        const res = await request(server)
            .post("/api/action")
            .send({ sender: "testUser", tracker: {}, domain: {} });

        expect(res.status).toBe(200);
        expect(res.body.responses[0].text).toBe("Action executed");
    });

    test("POST /api/action - Missing sender", async () => {
        const res = await request(server).post("/api/action").send({ tracker: {}, domain: {} });
        expect(res.status).toBe(400);
        expect(res.body).toEqual({ error: "Sender is required" });
    });

    test("POST /api/action - Action server error", async () => {
        axios.post.mockRejectedValueOnce(new Error("Action server down"));

        const res = await request(server)
            .post("/api/action")
            .send({ sender: "testUser", tracker: {}, domain: {} });

        expect(res.status).toBe(500);
        expect(res.body).toEqual({ error: "Failed to get response from RASA Action Server" });
    });

    test("GET /api/health - Check server status", async () => {
        const res = await request(server).get("/api/health");

        expect(res.status).toBe(200);
        expect(res.body).toEqual({ status: "Node.js API is running" });
    });
});

// **Unmocked Tests**
describe("Unmocked API Tests for RASA Bot", () => {
    test("POST /api/chat - Real request to RASA", async () => {
        const res = await request(server)
            .post("/api/chat")
            .send({ message: "Hi", sender: "realUser" });

        expect(res.status).toBe(200);
        expect(res.body.responses).toBeDefined();
    });

    test("POST /api/action - Real request to RASA Action Server", async () => {
        const res = await request(server)
            .post("/api/action")
            .send({ sender: "realUser", tracker: {}, domain: {} });

        expect(res.status).toBe(200);
    });
});