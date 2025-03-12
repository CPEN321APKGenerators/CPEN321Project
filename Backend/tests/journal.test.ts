import request from "supertest";
import app from "../index"; // Adjust based on your app entry point
import { jest } from "@jest/globals";
import { JournalController } from "../src/controllers/JournalController";
import { MongoClient, Db, Collection, Document, BulkWriteResult } from "mongodb";
import { client } from "../services";
import fs from "fs";

const mockJournal = {
    id: "12345",
    date: "2025-03-11",
    userID: "llcce44@gmail.com",
    content: "Today was a good day.",
};

// Unmocked Tests (Real API Calls)
describe("Journal API - Unmocked", () => {
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
    

    beforeAll(async () => {
        // Initialize test user
        await client.db("cpen321journal").collection("users").insertOne({
          userID: "llcce44@gmail.com",
          isPaid: false,
          googleNumID: google_num_id
        });
    });

    it("should create a journal entry", async () => {
        const response = await request(app)
            .post("/api/journal")
            .set("Authorization", testGoogleToken)
            .send(mockJournal);

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty("message", "New journal entry created successfully with encrypted text and images!");
    });

    it("should retrieve a journal entry", async () => {
        const response = await request(app)
            .get("/api/journal")
            .set("Authorization", testGoogleToken)
            .query({ date: mockJournal.date, userID: mockJournal.userID });

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty("journal");
    });
});



