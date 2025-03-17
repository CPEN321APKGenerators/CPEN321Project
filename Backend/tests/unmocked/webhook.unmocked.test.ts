import request from "supertest";
import app from "../../index"; // Adjust the path if needed
import { client } from "../../services";
import { stopCronJob } from "../../index";

/**
 * Test Suite: Unmocked: POST /webhook
 * - This test suite covers **real database interactions** (i.e., no mocking is used).
 * - The tests verify the API behavior using an **actual connection to the database**.
 * - It checks whether the database is updated as expected upon receiving webhook events.
 */

describe("Unmocked: POST /webhook", () => {
    /**
     * Establish a real database connection before running tests.
     */
    beforeAll(async () => {
        await client.connect();
    });

    /**
     * Close the database connection after all tests have run.
     */
    afterAll(async () => {
        await client.close();
    });

    /**
     * Test Case: Valid `payment_intent.succeeded` event
     * 
     * - **Inputs:**
     *   - Request Type: `POST`
     *   - URL: `/webhook`
     *   - Headers: `Content-Type: application/json`
     *   - Body:
     *     ```json
     *     {
     *       "type": "payment_intent.succeeded",
     *       "data": {
     *         "object": {
     *           "amount": 1099,
     *           "metadata": { "userID": "user_123" }
     *         }
     *       }
     *     }
     *     ```
     * 
     * - **Expected Behavior:**
     *   - The API should process the payment event.
     *   - The corresponding user record should be updated in the database (`isPaid: true`).
     *   - Response status code: **200**
     * 
     * - **Post-Test Verification:**
     *   - The database is queried to confirm that the user’s `isPaid` status was updated.
     */
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

    /**
     * Test Case: Unhandled event type
     * 
     * - **Inputs:**
     *   - Request Type: `POST`
     *   - URL: `/webhook`
     *   - Headers: `Content-Type: application/json`
     *   - Body:
     *     ```json
     *     {
     *       "type": "some_random_event",
     *       "data": {}
     *     }
     *     ```
     * 
     * - **Expected Behavior:**
     *   - The webhook should **acknowledge** the event without taking further action.
     *   - Response status code: **200**
     */
    test("Unhandled event type", async () => {
        const res = await request(app)
            .post("/webhook")
            .set("Content-Type", "application/json")
            .send({ type: "some_random_event", data: {} });

        expect(res.status).toBe(200); // Should still return 200 even for unhandled events
    });
});

/**
 * Cleanup: Ensure cron jobs and database connections are closed after tests.
 */
afterAll(async () => {
    stopCronJob(); // Stop cron jobs to prevent memory leaks
    await client.close(); // Close the database connection to avoid resource leaks
});
