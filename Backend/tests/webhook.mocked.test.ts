import request from "supertest";
import app from "../index"; // Adjust the path if needed
import { client } from "../services";
import { stopCronJob } from "../index";

// Mocking the database service
jest.mock("../services");

describe("Mocked: POST /webhook", () => {
    let mockDb: any;

    beforeAll(() => {
        // Setting up a mock database object
        mockDb = {
            collection: jest.fn().mockReturnValue({
                findOne: jest.fn().mockResolvedValue({ userID: "user_123", isPaid: false }),
                updateOne: jest.fn(),
            }),
        };

        // Mocking the database client to return the mock database
        (client.db as jest.Mock).mockReturnValue(mockDb);
    });

    afterEach(() => {
        jest.clearAllMocks(); // Clears all mock function calls after each test
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
     *           "metadata": { "userID": "user_123" }
     *         }
     *       }
     *     }
     *     ```
     * 
     * - **Mock Behavior:**
     *   - `updateOne` (database function) is mocked to return `{ acknowledged: true }`
     * 
     * - **Expected Behavior:**
     *   - The user’s `isPaid` status is updated in the database.
     *   - Response status code: **200**
     *   - `updateOne` should be called exactly once.
     */
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

    /**
     * Test Case: Database update failure
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
     *           "metadata": { "userID": "user_123" }
     *         }
     *       }
     *     }
     *     ```
     * 
     * - **Mock Behavior:**
     *   - `updateOne` is **mocked to throw an error** (`Database error`).
     * 
     * - **Expected Behavior:**
     *   - The request should fail due to a database error.
     *   - Response status code: **500**
     *   - `updateOne` should be called exactly once.
     */
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
     * - **Mock Behavior:**
     *   - No database interactions occur for this event type.
     * 
     * - **Expected Behavior:**
     *   - The webhook should **acknowledge the event** but take no further action.
     *   - Response status code: **200**
     */
    test("Unhandled event type", async () => {
        const res = await request(app)
            .post("/webhook")
            .set("Content-Type", "application/json")
            .send({ type: "some_random_event", data: {} });

        expect(res.status).toBe(200);
    });
});

// Cleanup after all tests are run
afterAll(async () => {
    stopCronJob(); // Stop cron jobs to prevent memory leaks
    await client.close(); // Close database connection to avoid leaks
});
