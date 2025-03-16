import request from "supertest";
import app from "../index"; // Adjust based on your app entry point
import { jest } from "@jest/globals";
import { JournalController } from "../src/controllers/JournalController";
import { MongoClient, Db, Collection, Document, BulkWriteResult } from "mongodb";
import { client } from "../services";
import fs from "fs";
import { Request, Response } from 'express';


// Mocked Tests (Database/API Failures)
describe("Journal 2 API - Mocked", () => {

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

    let journalController: JournalController;
    let mockRequest: Partial<Request>;
    let mockResponse: Partial<Response>;

    beforeAll(async() => {
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: "testllcce44@gmail.com" }, // Find existing user
            {
                $set: {
                    userID: "testllcce44@gmail.com",
                    isPaid: false,
                    googleNumID: google_num_id
                }
            },
            { upsert: true } // Insert if not found
        );
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: "testtest@gmail.com" }, // Find existing user
            {
                $set: {
                    userID: "testtest@gmail.com",
                    isPaid: false,
                    googleNumID: google_num_id
                }
            },
            { upsert: true } // Insert if not found
        );
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: "testnogooglenumid@gmail.com" }, // Find existing user
            {
                $set: {
                    userID: "testtest@gmail.com",
                    isPaid: false
                }
            },
            { upsert: true } // Insert if not found
        );
    })

    beforeEach(() => {
        journalController = new JournalController();
        mockResponse = {
            status: jest.fn().mockReturnThis(),
            json: jest.fn()
        } as unknown as Response;
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('getUserProfile (Mocked)', () => {
        // Test Case: Database error simulation
        it("should return 500 when checking existing entry)", async () => {
            jest.spyOn(client.db().collection("journals"), "findOne").mockResolvedValueOnce(null); // Mock only findOne
    
            const response = await request(app)
                .post("/api/journal")
                .set("Authorization", "Bearer "+testGoogleToken)
                .send({
                    date: "2025-03-11",
                    userID: "testtest@gmail.com",
                    text: "Testing...",
                    googleNumID: google_num_id
                });
    
            expect(response.status).toBe(500);
        });

        
        // it("should return 500 when updating existing entry", async () => {
        //     // Spy on the prototype's updateOne method
        //     const updateOneSpy = jest
        //         .spyOn(client.db("cpen321journal").collection("journals"), 'updateOne')
        //         .mockRejectedValueOnce(new Error("Database update failed"));

        //     const response = await request(app)
        //         .post("/api/journal")
        //         .set("Authorization", "Bearer " + testGoogleToken)
        //         .send({
        //             date: "2025-03-11",
        //             userID: "llcce44@gmail.com",
        //             text: "Testing...",
        //             googleNumID: google_num_id
        //         });
        //     expect(response.status).toBe(500);
        // });
        
    
      });
});
