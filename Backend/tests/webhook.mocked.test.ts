import request from "supertest";
import app from "../index"; // Adjust the path if needed
import { client } from "../services";
import { stopCronJob } from "../index";

jest.mock("../services"); // Mock the database service

describe("Mocked: POST /webhook", () => {
    let mockDb: any;

    beforeAll(() => {
        mockDb = {
            collection: jest.fn().mockReturnValue({
                findOne: jest.fn().mockResolvedValue({ userID: "user_123", isPaid: false }),
                updateOne: jest.fn(),
            }),
        };

        (client.db as jest.Mock).mockReturnValue(mockDb);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    test("Valid payment_intent.succeeded event", async () => {
        mockDb.collection().updateOne.mockResolvedValue({ acknowledged: true });

        const res = await request(app)
            .post("/webhook")
            .set("Content-Type", "application/json")
            .send({
                type: "payment_intent.succeeded",
                data: { object: { metadata: { userID: "user_123" } } },
            });

        expect(res.status).toBe(200);
        expect(mockDb.collection().updateOne).toHaveBeenCalledTimes(1);
    });

    test("Database update fails", async () => {
        mockDb.collection().updateOne.mockRejectedValue(new Error("Database error"));

        const res = await request(app)
            .post("/webhook")
            .set("Content-Type", "application/json")
            .send({
                type: "payment_intent.succeeded",
                data: { object: { metadata: { userID: "user_123" } } },
            });

        expect(res.status).toBe(500);  // Now correctly expects a 500 response
        expect(mockDb.collection().updateOne).toHaveBeenCalledTimes(1);
    });

    test("Unhandled event type", async () => {
        const res = await request(app)
            .post("/webhook")
            .set("Content-Type", "application/json")
            .send({ type: "some_random_event", data: {} });

        expect(res.status).toBe(200);
    });
});

afterAll(async () => {
    stopCronJob(); // Stop cron jobs to prevent leaks
    await client.close(); // Close the database connection to avoid leaks
});