import request from "supertest";
import app from "../../index"; // Adjust based on your app entry point
import { jest } from "@jest/globals";
import { JournalController } from "../../src/controllers/JournalController";
import { MongoClient, Db, Collection, Document, BulkWriteResult } from "mongodb";
import { client } from "../../services";
import fs from "fs";

/**
 * Test Suite: Journal API - Mocked
 * - This test suite covers **mocked** backend interactions for the Journal API.
 * - Mocks are used to control and simulate behavior for:
 *   - Database interactions (MongoDB service)
 *   - Authentication failures (missing token)
 */

jest.mock("../../services", () => ({
    client: {
        db: jest.fn(() => ({
            collection: jest.fn(() => ({
                findOne: jest.fn().mockRejectedValue(new Error("Database error") as never), // Mock error for findOne
                insertOne: jest.fn().mockRejectedValue(new Error("Database insert failed") as never), // Mock error for insertOne
                updateOne: jest.fn().mockRejectedValue(new Error("Database update failed") as never), // Mock error for updateOne
            })),
        })),
    },
}));

// Mocked Tests (Database/API Failures)
describe("Journal API - Mocked", () => {

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
    const testGoogleToken = process.env.TEST_GOOGLE_TOKEN || unmocked_data_json.testGoogleToken
    const google_num_id = process.env.GOOGLE_NUM_ID || unmocked_data_json.googleNumID
    console.log(testGoogleToken);

    const mockJournal = {
        date: "2025-03-11",
        userID: "llcce44@gmail.com",
        text: "Today was a good day.",
        googleNumID: google_num_id
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    /**
     * Test Case: Database failure when querying journal entry
     * 
     * - **Inputs:**
     *   - Request: `POST /api/journal`
     *   - Headers:
     *     ```json
     *     {
     *       "Authorization": "Bearer <valid_token>"
     *     }
     *     ```
     *   - Body:
     *     ```json
     *     {
     *       "date": "2025-03-11",
     *       "userID": "llcce44@gmail.com",
     *       "text": "Today was a good day.",
     *       "googleNumID": "<mock_google_num_id>"
     *     }
     *     ```
     * 
     * - **Mock Behavior:**
     *   - `findOne`, `insertOne`, and `updateOne` are **mocked to throw errors** simulating database failures.
     * 
     * - **Expected Behavior:**
     *   - The API should fail due to database query issues.
     *   - Response status code: **500**
     */
    it("should return an error when database query fails", async () => {
        const response = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer "+testGoogleToken)
            .send(mockJournal);

        expect(response.status).toBe(500);
    });

    /**
     * Test Case: Unauthorized request (missing token)
     * 
     * - **Inputs:**
     *   - Request: `POST /api/journal`
     *   - Body:
     *     ```json
     *     {
     *       "date": "2025-03-11",
     *       "userID": "llcce44@gmail.com",
     *       "text": "Today was a good day.",
     *       "googleNumID": "<mock_google_num_id>"
     *     }
     *     ```
     * 
     * - **Mock Behavior:**
     *   - No token is provided in the request.
     * 
     * - **Expected Behavior:**
     *   - The API should return an authentication failure.
     *   - Response status code: **400**
     */
    it("should return unauthorized when no token is provided", async () => {
        const response = await request(app)
            .post("/api/journal")
            .send(mockJournal);

        expect(response.status).toBe(400);
    });

    /**
     * **Test Case: Database failure when fetching a journal entry**
     * - **Inputs:**
     *   - Request: `GET /api/journal`
     *   - Query Params: `{ date, userID, googleNumID }`
     * - **Mock Behavior:**
     *   - `findOne` is mocked to throw an error.
     * - **Expected Behavior:**
     *   - API should return **500 Internal Server Error**.
     */
    it("should return 500 when getting existing entry", async () => {
        // Mocking the database call to return null (simulating failure)
        jest.spyOn(client.db("cpen321journal").collection("journals"), "findOne").mockImplementationOnce(() => {
            throw new Error("Database error");
        });

        // API Request
        const response = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({
                date: "2025-03-11",
                userID: "testtest@gmail.com",
                googleNumID: google_num_id
            });

        // Assertions
        expect(response.status).toBe(500);
    });

    /**
     * **Test Case: Database failure when updating an entry**
     * - **Inputs:**
     *   - Request: `PUT /api/journal`
     *   - Body: `{ date, userID, text, googleNumID }`
     * - **Mock Behavior:**
     *   - `updateOne` is mocked to throw an error.
     * - **Expected Behavior:**
     *   - API should return **500 Internal Server Error**.
     */
    it("should return 500 when updating existing entry", async () => {
        // Mocking the database call to return null (simulating failure)
        jest.spyOn(client.db("cpen321journal").collection("journals"), "findOne").mockImplementationOnce(() => {
            throw new Error("Database error");
        });

        // API Request
        const response = await request(app)
            .put("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send({
                date: "2025-03-11",
                userID: "testtest@gmail.com",
                text: "helo",
                googleNumID: google_num_id
            });

        // Assertions
        expect(response.status).toBe(500);
    });

    /**
     * **Test Case: Database failure when deleting an entry**
     * - **Inputs:**
     *   - Request: `DELETE /api/journal`
     *   - Query Params: `{ date, userID, googleNumID }`
     * - **Mock Behavior:**
     *   - `findOne` is mocked to throw an error.
     * - **Expected Behavior:**
     *   - API should return **500 Internal Server Error**.
     */
    it("should return 500 when deleting entry", async () => {
        // Mocking the database call to return null (simulating failure)
        jest.spyOn(client.db("cpen321journal").collection("journals"), "findOne").mockImplementationOnce(() => {
            throw new Error("Database error");
        });

        // API Request
        const response = await request(app)
            .delete("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({
                date: "2025-03-11",
                userID: "testtest@gmail.com",
                googleNumID: google_num_id
            });

        // Assertions
        expect(response.status).toBe(500);
    });

    /**
     * **Test Case: Database failure when retrieving a file**
     * - **Inputs:**
     *   - Request: `GET /api/journal/file`
     *   - Query Params: `{ userID, format, googleNumID }`
     * - **Mock Behavior:**
     *   - `findOne` is mocked to throw an error.
     * - **Expected Behavior:**
     *   - API should return **500 Internal Server Error**.
     */
    it("should return 500 when getting file", async () => {
        // Mocking the database call to return null (simulating failure)
        jest.spyOn(client.db("cpen321journal").collection("journals"), "findOne").mockImplementationOnce(() => {
            throw new Error("Database error");
        });

        // API Request
        const response = await request(app)
            .get("/api/journal/file")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({
                userID: "testtest@gmail.com",
                format: "csv",
                googleNumID: google_num_id
            });

        // Assertions
        expect(response.status).toBe(500);
    });
});
