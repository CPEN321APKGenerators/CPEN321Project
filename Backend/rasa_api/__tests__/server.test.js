const axios = require("axios");
const { app, server } = require("../server");
const request = require("supertest");

jest.mock("axios");  

beforeAll(() => {
    process.env.NODE_ENV = 'test';
    jest.spyOn(console, "error").mockImplementation(() => {});  
    jest.spyOn(console, "warn").mockImplementation(() => {});  

    axios.post.mockResolvedValueOnce = jest.fn(); 
});

afterAll((done) => {
    server.close(done); 
});

// Mocked Tests
describe("API Tests for RASA Bot", () => {
/*     test("POST /api/chat - Valid request", async () => {
        const mockResponse = { responses: [{ text: "Please type start to begin journaling." }] };
        axios.post.mockResolvedValueOnce({ data: mockResponse });

        const res = await request(server)
            .post("/api/chat")
            .send({ message: "Hi", sender: "testUser" });

        expect(res.status).toBe(200);
        expect(res.body.responses[0].text).toBe("Please type start to begin journaling.");
    }); */

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

/*     test("POST /api/action - Valid request", async () => {
        const mockResponse = { responses: [{ text: "Action executed" }] };
        axios.post.mockResolvedValueOnce({ data: mockResponse });

        const res = await request(server)
            .post("/api/action")
            .send({ sender: "testUser", tracker: {}, domain: {} });

        expect(res.status).toBe(200);
        expect(res.body.responses[0].text).toBe("Action executed");
    }); */

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


/* // **Unmocked Tests
describe("Unmocked API Tests for RASA Bot", () => {
    test("POST /api/chat - Real request to RASA", async () => {
        await new Promise((resolve) => setTimeout(resolve, 3000)); 
    
        const res = await request(server)
            .post("/api/chat")
            .set("Content-Type", "application/json")
            .send({ message: "Hello", sender: "user123" });
    
        console.log("🔍 Debug Jest API Response:", res.status, JSON.stringify(res.body, null, 2));
    
        expect(res.status).toBe(200);  
        expect(res.body.responses).toBeDefined();
    });

    test("POST /api/action - Real request to RASA Action Server", async () => {
        const res = await request(server)
            .post("/api/action")
            .send({ sender: "realUser", tracker: {}, domain: {} });

        expect(res.status).toBe(200);
    });
}); */