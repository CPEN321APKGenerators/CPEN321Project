import request from "supertest";
import app from "../index"; // Adjust the path if needed
import { client } from "../services";
import { stopCronJob } from "../index";

describe("Unmocked: POST /webhook", () => {
    beforeAll(async () => {
        await client.connect();
    });

    afterAll(async () => {
        await client.close();
    });

    test("Valid payment_intent.succeeded event", async () => {
        const fakeUserID = "user_123";
        const eventPayload = {
            type: "payment_intent.succeeded",
            data: {
                object: {
                    amount: 1099,
                    metadata: { userID: fakeUserID },
                },
            },
        };

        const res = await request(app)
            .post("/webhook")
            .set("Content-Type", "application/json")
            .send(eventPayload);

        expect(res.status).toBe(200);

        // Check if the database was updated correctly
        const updatedUser = await client
            .db("cpen321journal")
            .collection("users")
            .findOne({ userID: fakeUserID });

        expect(updatedUser).toBeDefined();
        if (updatedUser != null) expect(updatedUser.isPaid).toBe(true);
    });

    test("Unhandled event type", async () => {
        const res = await request(app)
            .post("/webhook")
            .set("Content-Type", "application/json")
            .send({ type: "some_random_event", data: {} });

        expect(res.status).toBe(200); // Should still return 200 even for unhandled events
    });
});

afterAll(async () => {
    stopCronJob(); // Stop cron jobs to prevent leaks
    await client.close(); // Close the database connection to avoid leaks
});