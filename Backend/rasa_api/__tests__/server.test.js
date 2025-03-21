// Allow HTTPS with self-signed certs for local HTTPS
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

const request = require("supertest");
const { app, server } = require("../server");
const axios = require("axios");

// Keep a reference to the real axios for unmocked tests
const realAxios = jest.requireActual("axios");

// Mock axios globally
jest.mock("axios");

beforeAll(() => {
    jest.spyOn(console, "error").mockImplementation(() => {});
    jest.spyOn(console, "warn").mockImplementation(() => {});
});

afterAll((done) => {
    server.close(done);
});

//Mocked
describe("Mocked API Tests for RASA Bot", () => {
    test("POST /api/chat - Valid request", async () => {
        axios.post.mockResolvedValueOnce({
            data: {
                messages: [{ text: "Hi!" }, { text: "Please type start to begin journaling." }],
                responses: ["Hi!", "Please type start to begin journaling."],
                metadata: {},
                conversation_id: "testUser"
            }
        });

        const res = await request(server)
            .post("/api/chat")
            .send({ message: "Hi", sender: "testUser" });

        expect(res.status).toBe(200);
        expect(res.body.responses).toContain("Please type start to begin journaling.");
    });

    test("POST /api/chat - Missing message", async () => {
        const res = await request(server)
            .post("/api/chat")
            .send({ sender: "testUser" });

        expect(res.status).toBe(400);
        expect(res.body).toEqual({ error: "Message and sender are required" });
    });

    test("POST /api/chat - RASA server error", async () => {
        axios.post.mockRejectedValueOnce(new Error("RASA API Down"));

        const res = await request(server)
            .post("/api/chat")
            .send({ message: "Hi", sender: "testUser" });

        expect(res.status).toBe(500);
        expect(res.body.error).toBe("Failed to get response from RASA");
    });

    test("POST /api/action - Valid request", async () => {
        axios.post.mockResolvedValueOnce({
            data: {
                messages: [{ text: "Action executed" }],
                responses: ["Action executed"]
            }
        });

        const res = await request(server)
            .post("/api/action")
            .send({ sender: "testUser", tracker: {}, domain: {} });

        expect(res.status).toBe(200);
        expect(res.body.responses).toContain("Action executed");
    });

    test("POST /api/action - Missing sender", async () => {
        const res = await request(server)
            .post("/api/action")
            .send({ tracker: {}, domain: {} });

        expect(res.status).toBe(400);
        expect(res.body).toEqual({ error: "Sender is required" });
    });

    test("POST /api/action - Action server error", async () => {
        axios.post.mockRejectedValueOnce(new Error("Action server down"));

        const res = await request(server)
            .post("/api/action")
            .send({ sender: "testUser", tracker: {}, domain: {} });

        expect(res.status).toBe(500);
        expect(res.body.error).toBe("Failed to get response from RASA Action Server");
    });

    test("GET /api/health - Check server status", async () => {
        const res = await request(server).get("/api/health");

        expect(res.status).toBe(200);
        expect(res.body).toEqual({ status: "Node.js API is running" });
    });
    test("POST /api/chat - Invalid JSON payload", async () => {
        const res = await request(server)
            .post("/api/chat")
            .set("Content-Type", "text/plain")
            .send("invalid-json");
    
        expect(res.status).toBe(400); 
    });
    
    test("POST /api/chat - Empty body", async () => {
        const res = await request(server).post("/api/chat").send({});
        expect(res.status).toBe(400);
        expect(res.body).toEqual({ error: "Message and sender are required" });
    });
    
    test("POST /api/action - Invalid tracker object", async () => {
        axios.post.mockRejectedValueOnce(new Error("Invalid tracker format"));
    
        const res = await request(server)
            .post("/api/action")
            .send({
                sender: "testUser",
                tracker: "not-an-object",
                domain: {}
            });
    
        expect(res.status).toBe(500);
        expect(res.body.error).toBe("Failed to get response from RASA Action Server");
    });
});


//UNMOCKED TESTS (REAL RASA SERVER)
describe("Unmocked API Tests for RASA Bot", () => {
    beforeAll(() => {
        axios.post.mockImplementation(realAxios.post);
    });

    test("POST /api/chat - Real request to RASA", async () => {
        await new Promise((resolve) => setTimeout(resolve, 1000));

        const res = await request(server)
            .post("/api/chat")
            .set("Content-Type", "application/json")
            .send({
                message: "Hello",
                sender: "user123",
                metadata: {
                    userID: "user123",
                    date: "2025-03-21",
                    google_token: "test-token"
                }
            });

        console.log("/api/chat response:", res.status, JSON.stringify(res.body, null, 2));

        expect(res.status).toBe(200);
        expect(Array.isArray(res.body.responses)).toBe(true);
        expect(res.body.responses.length).toBeGreaterThan(0);
    });

    test("POST /api/action - Real request to RASA Action Server", async () => {
        const res = await request(server)
            .post("/api/action")
            .send({
                sender: "realUser",
                tracker: {
                    latest_message: {
                        text: "Today was a hard day.",
                        intent: { name: "journal_provided" },
                        metadata: {
                            userID: "realUser",
                            date: "2025-03-21",
                            google_token: "test-token"
                        }
                    }
                },
                domain: {}
            });

        console.log("/api/action response:", res.status, JSON.stringify(res.body, null, 2));

        expect(res.status).toBe(200);
    });
    test("POST /api/chat - Real request with missing sender", async () => {
        const res = await request(server)
            .post("/api/chat")
            .send({
                message: "Hi"
            });
    
        expect(res.status).toBe(400);
        expect(res.body).toEqual({ error: "Message and sender are required" });
    });
    
    test("POST /api/action - Real request with incomplete tracker", async () => {
        const res = await request(server)
            .post("/api/action")
            .send({
                sender: "realUser"
                // tracker and domain missing
            });
    
        expect(res.status).toBe(200); 
    });
    
});

