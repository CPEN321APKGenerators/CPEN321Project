import Stripe from "stripe";
import request from "supertest";
import app, { setStripeInstance } from "../index"; // Import setStripeInstance

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

// Inject the mocked Stripe instance into the app
beforeAll(() => {
    setStripeInstance(mockStripe as unknown as Stripe);
});

describe("Mocked: POST /api/payment-sheet", () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    // Test for a successful payment sheet creation
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

    // Test when Stripe fails to create a customer
    test("Stripe Customer Creation Fails", async () => {
        mockStripe.customers.create.mockRejectedValue(new Error("Customer creation failed"));

        const res = await request(app)
            .post("/api/payment-sheet")
            .send({ userID: "user_123" });

        expect(res.status).toBe(500);
        expect(mockStripe.customers.create).toHaveBeenCalledTimes(1);
    });

    // Test when Ephemeral Key creation fails
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

    // Test when Payment Intent creation fails
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

    // Test for missing userID in request
    test("Missing userID", async () => {
        const res = await request(app).post("/api/payment-sheet").send({});

        expect(res.status).toBe(500);
        expect(res.body).toEqual({});
    });
});
