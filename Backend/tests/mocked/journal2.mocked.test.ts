import request from "supertest";
import app from "../../index"; // Adjust based on your app entry point
import { jest } from "@jest/globals";
import { JournalController } from "../../src/controllers/JournalController";
import { MongoClient, Db, Collection, Document, BulkWriteResult } from "mongodb";
import { client } from "../../services";
import fs from "fs";
import { Request, Response } from "express";

// Mocked Tests (Database/API Failures)
describe("Journal 2 API - Mocked", () => {
    
    /**
     * Loads unmocked test data from JSON if available.
     * Falls back to environment variables if the file is missing.
     */
    let unmocked_data_json: any = {}; // Default empty object
    // Attempt to load external test data if available
    try {
        const dataFilePath = `${__dirname}/../unmocked_data.json`; // Adjusted path

        if (fs.existsSync(dataFilePath)) {
            unmocked_data_json = require(dataFilePath);
        } else {
            console.log("Warning: unmocked_data.json not found. Using only environment variables.");
        }
    } catch (error) {
        console.log("Warning: Failed to load unmocked_data.json. Using only environment variables.", error);
    }

    // Fetch test credentials (Token & GoogleNumID) from environment or JSON
    const testGoogleToken = process.env.TEST_GOOGLE_TOKEN || unmocked_data_json.testGoogleToken;
    const google_num_id = process.env.GOOGLE_NUM_ID || unmocked_data_json.googleNumID;
    const google_user_prefix = process.env.GOOGLE_USER_PREFIX || unmocked_data_json.google_user_prefix;
    const test_main_user_id = google_user_prefix+"@gmail.com"

    console.log(testGoogleToken);

    // Mocked journal entry for test cases
    const mockJournal = {
        date: "2025-03-11",
        userID: test_main_user_id,
        text: "Today was a good day.",
        googleNumID: google_num_id
    };

    let journalController: JournalController;
    let mockRequest: Partial<Request>;
    let mockResponse: Partial<Response>;

    /**
     * **Before All Tests:**
     * - Ensures test user entries exist in the database.
     * - Inserts/updates three test users with different conditions.
     */
    beforeAll(async () => {
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: google_user_prefix+"test"+test_main_user_id+"@gmail.com" }, // Test user 1
            {
                $set: {
                    userID: google_user_prefix+"test"+test_main_user_id+"@gmail.com",
                    isPaid: false,
                    googleNumID: google_num_id
                }
            },
            { upsert: true } // Insert if not found
        );

        await client.db("cpen321journal").collection("users").updateOne(
            { userID: google_user_prefix+"testtest@gmail.com" }, // Test user 2
            {
                $set: {
                    userID: google_user_prefix+"testtest@gmail.com",
                    isPaid: false,
                    googleNumID: google_num_id
                }
            },
            { upsert: true }
        );

        await client.db("cpen321journal").collection("users").updateOne(
            { userID: google_user_prefix+"testnogooglenumid@gmail.com" }, // Test user 3 (without googleNumID)
            {
                $set: {
                    userID: google_user_prefix+"testnogooglenumid@gmail.com",
                    isPaid: false
                }
            },
            { upsert: true }
        );
    });

    /**
     * **Before Each Test:**
     * - Creates a new instance of `JournalController`.
     * - Mocks the response object using Jest.
     */
    beforeEach(() => {
        journalController = new JournalController();
        mockResponse = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn()
        } as unknown as Response;
    });

    /**
     * **After Each Test:**
     * - Clears all mocks to ensure clean test isolation.
     */
    afterEach(() => {
        jest.clearAllMocks();
    });

    /**
     * **Test Group: post /api/journal (Mocked)**
     * - Tests API error handling using database mocking.
     */
    describe("post /api/journal (Mocked)", () => {
        
        /**
         * **Test Case: Database error simulation**
         * - **Inputs**:
         *   - **Authorization Header**: Bearer {testGoogleToken}
         *   - **Payload**:
         *     ```json
         *     {
         *       "date": "2025-03-11",
         *       "userID": "testtest@gmail.com",
         *       "text": "Testing...",
         *       "googleNumID": google_num_id
         *     }
         *     ```
         * - **Expected Status Code**: `500 Internal Server Error`
         * - **Expected Behavior**:
         *   - Simulates a database failure.
         *   - The system should handle the error gracefully.
         *   - The response should contain an appropriate error message.
         */
        it("should return 500 when checking existing entry", async () => {
            // Mocking the database call to return null (simulating failure)
            jest.spyOn(client.db("cpen321journal").collection("journals"), "findOne").mockResolvedValueOnce(null);

            // API Request
            const response = await request(app)
                .post("/api/journal")
                .set("Authorization", "Bearer " + testGoogleToken)
                .send({
                    date: "2025-03-11",
                    userID: google_user_prefix+"testtest@gmail.com",
                    text: "Testing...",
                    googleNumID: google_num_id
                });

            // Assertions
            expect(response.status).toBe(500);
        });


        

    });
});
