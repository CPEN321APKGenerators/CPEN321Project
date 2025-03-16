import Stripe from "stripe";
import request from "supertest";
import app, { setStripeInstance } from "../index"; // Import setStripeInstance

/**
 * Test Suite: Mocked: POST /api/payment-sheet
 * - This test suite **mocks Stripe API interactions** for the `/api/payment-sheet` endpoint.
 * - Instead of using real Stripe API calls, we use **mock functions** to simulate responses and failures.
 */

// Manually mock the Stripe module
const mockStripe = {
    customers: {
        create: jest.fn(),
    },
    ephemeralKeys: {
        create: jest.fn(),
    },
    paymentIntents: {
        create: jest.fn(),
    },
};

// Inject the mocked Stripe instance into the app before tests
beforeAll(() => {
    setStripeInstance(mockStripe as unknown as Stripe);
});

describe("Mocked: POST /api/payment-sheet", () => {
    /**
     * Clears all mock data after each test to avoid contamination.
     */
    afterEach(() => {
        jest.clearAllMocks();
    });

    /**
     * Test Case: Valid Payment Request - Stripe Success
     * 
     * - **Inputs:**
     *   - Request Type: `POST`
     *   - URL: `/api/payment-sheet`
     *   - Body:
     *     ```json
     *     {
     *       "userID": "user_123"
     *     }
     *     ```
     * 
     * - **Mock Behavior:**
     *   - `mockStripe.customers.create` returns `{ id: "cus_test123" }`
     *   - `mockStripe.ephemeralKeys.create` returns `{ secret: "ephemeral_key_test" }`
     *   - `mockStripe.paymentIntents.create` returns `{ client_secret: "pi_test_secret" }`
     * 
     * - **Expected Behavior:**
     *   - The API should return a **valid payment response**.
     *   - Response status code: **200**
     *   - Response body should contain:
     *     ```json
     *     {
     *       "paymentIntent": "pi_test_secret",
     *       "ephemeralKey": "ephemeral_key_test",
     *       "customer": "cus_test123"
     *     }
     *     ```
     *   - Each Stripe function should be called **exactly once**.
     */
    test("Valid Payment Request - Stripe Success", async () => {
        // Mock Stripe responses
        mockStripe.customers.create.mockResolvedValue({ id: "cus_test123" });
        mockStripe.ephemeralKeys.create.mockResolvedValue({ secret: "ephemeral_key_test" });
        mockStripe.paymentIntents.create.mockResolvedValue({ client_secret: "pi_test_secret" });

        const res = await request(app)
            .post("/api/payment-sheet")
            .send({ userID: "user_123" });

        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty("paymentIntent", "pi_test_secret");
        expect(res.body).toHaveProperty("ephemeralKey", "ephemeral_key_test");
        expect(res.body).toHaveProperty("customer", "cus_test123");

        expect(mockStripe.customers.create).toHaveBeenCalledTimes(1);
        expect(mockStripe.ephemeralKeys.create).toHaveBeenCalledTimes(1);
        expect(mockStripe.paymentIntents.create).toHaveBeenCalledTimes(1);
    });

    /**
     * Test Case: Stripe Customer Creation Fails
     * 
     * - **Inputs:**
     *   - Request Type: `POST`
     *   - URL: `/api/payment-sheet`
     *   - Body:
     *     ```json
     *     {
     *       "userID": "user_123"
     *     }
     *     ```
     * 
     * - **Mock Behavior:**
     *   - `mockStripe.customers.create` **throws an error** (`Customer creation failed`).
     * 
     * - **Expected Behavior:**
     *   - The request should fail due to a Stripe error.
     *   - Response status code: **500**
     *   - `mockStripe.customers.create` should be called exactly **once**.
     */
    test("Stripe Customer Creation Fails", async () => {
        mockStripe.customers.create.mockRejectedValue(new Error("Customer creation failed"));

        const res = await request(app)
            .post("/api/payment-sheet")
            .send({ userID: "user_123" });

        expect(res.status).toBe(500);
        expect(mockStripe.customers.create).toHaveBeenCalledTimes(1);
    });

    /**
     * Test Case: Stripe Ephemeral Key Creation Fails
     * 
     * - **Inputs:** (same as above)
     * - **Mock Behavior:**
     *   - `mockStripe.customers.create` returns `{ id: "cus_test123" }`
     *   - `mockStripe.ephemeralKeys.create` **throws an error** (`Ephemeral key failed`).
     * 
     * - **Expected Behavior:**
     *   - The request should fail due to a Stripe error.
     *   - Response status code: **500**
     */
    test("Stripe Ephemeral Key Creation Fails", async () => {
        mockStripe.customers.create.mockResolvedValue({ id: "cus_test123" });
        mockStripe.ephemeralKeys.create.mockRejectedValue(new Error("Ephemeral key failed"));

        const res = await request(app)
            .post("/api/payment-sheet")
            .send({ userID: "user_123" });

        expect(res.status).toBe(500);
        expect(mockStripe.customers.create).toHaveBeenCalledTimes(1);
        expect(mockStripe.ephemeralKeys.create).toHaveBeenCalledTimes(1);
    });

    /**
     * Test Case: Stripe Payment Intent Creation Fails
     * 
     * - **Inputs:** (same as above)
     * - **Mock Behavior:**
     *   - `mockStripe.customers.create` returns `{ id: "cus_test123" }`
     *   - `mockStripe.ephemeralKeys.create` returns `{ secret: "ephemeral_key_test" }`
     *   - `mockStripe.paymentIntents.create` **throws an error** (`Payment intent failed`).
     * 
     * - **Expected Behavior:**
     *   - The request should fail due to a Stripe error.
     *   - Response status code: **500**
     */
    test("Stripe Payment Intent Creation Fails", async () => {
        mockStripe.customers.create.mockResolvedValue({ id: "cus_test123" });
        mockStripe.ephemeralKeys.create.mockResolvedValue({ secret: "ephemeral_key_test" });
        mockStripe.paymentIntents.create.mockRejectedValue(new Error("Payment intent failed"));

        const res = await request(app)
            .post("/api/payment-sheet")
            .send({ userID: "user_123" });

        expect(res.status).toBe(500);
        expect(mockStripe.customers.create).toHaveBeenCalledTimes(1);
        expect(mockStripe.ephemeralKeys.create).toHaveBeenCalledTimes(1);
        expect(mockStripe.paymentIntents.create).toHaveBeenCalledTimes(1);
    });

    /**
     * Test Case: Missing `userID`
     * 
     * - **Inputs:**
     *   - Request Type: `POST`
     *   - URL: `/api/payment-sheet`
     *   - Body:
     *     ```json
     *     {}
     *     ```
     * 
     * - **Expected Behavior:**
     *   - The request should return **500**, and response body should be **empty**.
     */
    test("Missing userID", async () => {
        const res = await request(app).post("/api/payment-sheet").send({});

        expect(res.status).toBe(500);
        expect(res.body).toEqual({});
    });
});
