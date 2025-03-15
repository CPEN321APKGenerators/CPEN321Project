import request from "supertest";
import app from "../index"; // Adjust based on your app entry point
import { jest } from "@jest/globals";
import { JournalController } from "../src/controllers/JournalController";
import { MongoClient, Db, Collection, Document, BulkWriteResult } from "mongodb";
import { client } from "../services";
import fs from "fs";

jest.mock("../services", () => ({
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
    try {
        if (fs.existsSync("./tests/unmocked_data.json")) {
            unmocked_data_json = require("./unmocked_data.json");
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

    it("should return an error when database query fails", async () => {
        const response = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer "+testGoogleToken)
            .send(mockJournal);

        expect(response.status).toBe(500);
    });

    it("should return unauthorized when no token is provided", async () => {
        const response = await request(app)
            .post("/api/journal")
            .send(mockJournal);

        expect(response.status).toBe(400);
    });
});
