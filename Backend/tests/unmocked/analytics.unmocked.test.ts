import request from "supertest";
import app from "../../index"; // Adjust based on your app entry point
import { jest } from "@jest/globals";
import { JournalController } from "../../src/controllers/JournalController";
import { MongoClient, Db, Collection, Document, BulkWriteResult } from "mongodb";
import { client } from "../../services";
import fs from "fs"

describe("Analytics API - Unmocked", () => {
    let unmocked_data_json: any = {}; // Default empty object
    const dataFilePath = `${__dirname}/../unmocked_data.json`;
    try {
        if (fs.existsSync(dataFilePath)) {
            unmocked_data_json = require(dataFilePath);
        } else {
            console.log("Warning: unmocked_data.json not found. Using only environment variables.");
        }
    } catch (error) {
        console.log("Warning: Failed to load unmocked_data.json. Using only environment variables.", error);
    }
    const testGoogleToken = process.env.TEST_GOOGLE_TOKEN || unmocked_data_json.testGoogleToken;
    const google_num_id = process.env.GOOGLE_NUM_ID || unmocked_data_json.googleNumID;
    const googleUserID =  process.env.GOOGLE_USER_ID || unmocked_data_json.userID;

    const mockJournal_1 = {
        date: "2025-01-01",
        userID: googleUserID,
        text : "This is filler for encrypted text.",
        media: [],
        stats: {
            activities: {
                Running: 0.5
            },
            emotions:{
                Joy: 0.5,
                Sadness: 0.5,
                Anger: 0.5,
                Fear: 0.5,
                Gratitude: 0.5,
                Neutral: 0.5,
                Resilience: 0.5,
                SelfAcceptance: 0.5,
                Stress: 0.5,
                SenseOfPurpose: 0.5
            },
            overallScore: 70
        },
        updatedAt: "2025-01-01"
    };

    const mockJournal_2 = {
        date: "2025-01-02",
        userID: googleUserID,
        text : "This is filler for encrypted text.",
        media: [],
        stats: {
            activities: {
                Running: 1
            },
            emotions:{
                Joy: 0.8,
                Sadness: 0.4,
                Anger: 0.5,
                Fear: 0.5,
                Gratitude: 0.5,
                Neutral: 0.5,
                Resilience: 0.5,
                SelfAcceptance: 0.5,
                Stress: 0.3,
                SenseOfPurpose: 0.7
            },
            overallScore: 77
        },
        updatedAt: "2025-01-02"
    };
    const mockJournal_3 = {
        date: "2025-01-03",
        userID: googleUserID,
        text : "This is filler for encrypted text.",
        media: [],
        stats: {
            activities: {
                Running: 0.0
            },
            emotions:{
                Joy: 0.4,
                Sadness: 0.6,
                Anger: 0.6,
                Fear: 0.5,
                Gratitude: 0.5,
                Neutral: 0.5,
                Resilience: 0.5,
                SelfAcceptance: 0.5,
                Stress: 0.3,
                SenseOfPurpose: 0.7
            },
            overallScore: 65
        },
        updatedAt: "2025-01-03"
    };
    const mockJournal_4 = {
        date: "2025-01-04",
        userID: googleUserID,
        text : "This is filler for encrypted text.",
        media: [],
        stats: {
            activities: {
                Running: 2
            },
            emotions:{
                Joy: 0.75,
                Sadness: 0.4,
                Anger: 0.3,
                Fear: 0.5,
                Gratitude: 0.5,
                Neutral: 0.5,
                Resilience: 0.5,
                SelfAcceptance: 0.5,
                Stress: 0.3,
                SenseOfPurpose: 0.7
            },
            overallScore: 80
        },
        updatedAt: "2025-01-04"
    };
    const mockJournal_5 = {
        date: "2025-01-05",
        userID: googleUserID,
        text : "This is filler for encrypted text.",
        media: [],
        stats: {
            activities: {
                Running: 2.5
            },
            emotions:{
                Joy: 0.8,
                Sadness: 0.3,
                Anger: 0.2,
                Fear: 0.5,
                Gratitude: 0.5,
                Neutral: 0.5,
                Resilience: 0.5,
                SelfAcceptance: 0.5,
                Stress: 0.3,
                SenseOfPurpose: 0.7
            },
            overallScore: 83
        },
        updatedAt: "2025-01-05"
    };

    /**
     * Before running tests:
     * - Inserts test user data into the database.
     * - Ensures that test users exist to prevent errors during API calls.
     * - Uses `upsert: true` to avoid duplicates.
     */
    beforeAll(async () => {
    
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: googleUserID }, // Find existing user
            {
                $set: {
                    activities_tracking: [
                        {
                            name: "Running",
                            averageValue: 2,
                            unit: "Hours"
                        }
                    ]
                }
            },
            { upsert: true } // Insert if not found
        );
        await client.db("cpen321journal").collection("journals").updateOne(
            { userID: googleUserID, date: "2025-01-01" }, 
            {
                $set: {
                    media: mockJournal_1.media,
                    stats: mockJournal_1.stats,
                    text: mockJournal_1.text,
                    updatedAt: mockJournal_1.updatedAt

                }
            },
            { upsert: true } // Insert if not found
        );
        await client.db("cpen321journal").collection("journals").updateOne(
            { userID: googleUserID, date: "2025-01-02" }, 
            {
                $set: {
                    media: mockJournal_2.media,
                    stats: mockJournal_2.stats,
                    text: mockJournal_2.text,
                    updatedAt: mockJournal_2.updatedAt

                }
            },
            { upsert: true } // Insert if not found
        );
        await client.db("cpen321journal").collection("journals").updateOne(
            { userID: googleUserID, date: "2025-01-03" }, 
            {
                $set: {
                    media: mockJournal_3.media,
                    stats: mockJournal_3.stats,
                    text: mockJournal_3.text,
                    updatedAt: mockJournal_3.updatedAt

                }
            },
            { upsert: true } // Insert if not found
        );
        await client.db("cpen321journal").collection("journals").updateOne(
            { userID: googleUserID, date: "2025-01-04" }, 
            {
                $set: {
                    media: mockJournal_4.media,
                    stats: mockJournal_4.stats,
                    text: mockJournal_4.text,
                    updatedAt: mockJournal_4.updatedAt

                }
            },
            { upsert: true } // Insert if not found
        );
        await client.db("cpen321journal").collection("journals").updateOne(
            { userID: googleUserID, date: "2025-01-05" }, 
            {
                $set: {
                    media: mockJournal_5.media,
                    stats: mockJournal_5.stats,
                    text: mockJournal_5.text,
                    updatedAt: mockJournal_5.updatedAt

                }
            },
            { upsert: true } // Insert if not found
        );
    
        // Wait a bit to make sure MongoDB processes the insert
        await new Promise(resolve => setTimeout(resolve, 500));
    });
 
    /**
     * Test Case: Retrieve analytics for a week
     * 
     * - **Inputs:**
     *   - Request: `GET /api/analytics`
     *   - Headers: `userID: lkevin2003@gmail.com, date: 2025-01-07`
     * 
     * - **Expected Behavior:**
     *   - Numbers from 2025-01-01 to 2025-01-07 are returned.
     *   - Strings describing trends between activities and emotions should be returned.
     *   - Average of overall score for the week should be returned.
     *   - Response status codes: **200** for `GET`.
     */
    it("should retrieve stats for past week of date and also trends", async () => {
        const expectedResponse = {
            emotionStats: {
                Joy: [0.5, 0.8, 0.4, 0.75, 0.8, 0.8, 0.8],
                Sadness: [0.5, 0.4, 0.6, 0.4, 0.3, 0.3, 0.3],
                Anger: [0.5, 0.5, 0.6, 0.3, 0.2, 0.2, 0.2],
                Fear: [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5],
                Gratitude: [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5],
                Neutral: [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5],
                Resilience: [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5],
                SelfAcceptance: [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5],
                Stress: [0.5, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3],
                SenseOfPurpose: [0.5, 0.7, 0.7, 0.7, 0.7, 0.7, 0.7]
            },
            activityStats: {
                Running: [0.5, 1, 0, 2, 2.5, null, null]
            },
            overallScore: 75,
            summary: [
                {
                    activity: 'Running',
                    emotion: 'Joy',
                    display: 'Joy tends to rise while Running falls after with an average delay of 2 day(s) with 1 notable occurence(s).'
                },
                {
                    activity: 'Running',
                    emotion: 'Sadness',
                    display: 'Sadness tends to fall while Running falls after with an average delay of 2 day(s) with 1 notable occurence(s).'
                },
                {
                    activity: 'Running',
                    emotion: 'Anger',
                    display: 'Anger tends to fall while Running falls after with an average delay of 2 day(s) with 1 notable occurence(s).'
                }
            ]
        };

        const postResponse = await request(app)
            .get("/api/analytics")
            .query({ userID: googleUserID, date: "2025-01-07" });

        expect(postResponse.body).toEqual(expectedResponse);
        expect(postResponse.status).toBe(200);
        
    });

    it("should retrieve empty stats and summary when activities are empty", async () => {
        // Replace activities with an empty array and run the same test
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: googleUserID },
            { $set: { activities_tracking: [] } }
        );

        const emptyExpectedResponse = {
            emotionStats: {
                Joy: [0.5, 0.8, 0.4, 0.75, 0.8, 0.8, 0.8],
                Sadness: [0.5, 0.4, 0.6, 0.4, 0.3, 0.3, 0.3],
                Anger: [0.5, 0.5, 0.6, 0.3, 0.2, 0.2, 0.2],
                Fear: [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5],
                Gratitude: [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5],
                Neutral: [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5],
                Resilience: [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5],
                SelfAcceptance: [0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5],
                Stress: [0.5, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3],
                SenseOfPurpose: [0.5, 0.7, 0.7, 0.7, 0.7, 0.7, 0.7]
            },
            activityStats: {},
            overallScore: 75,
            summary: []
        };

        const emptyPostResponse = await request(app)
            .get("/api/analytics")
            .query({ userID: googleUserID, date: "2025-01-07" });

        expect(emptyPostResponse.status).toBe(200);
        expect(emptyPostResponse.body).toEqual(emptyExpectedResponse);
    });        
    
    /**
     * Clean up after tests:
     * - Removes test user data from the database.
     */
        afterAll(async () => {
            await client.db("cpen321journal").collection("journals").deleteMany({
                userID: googleUserID,
                date: { $in: ["2025-01-01", "2025-01-02", "2025-01-03", "2025-01-04", "2025-01-05"] }
            });
            await client.close();
        });
    

});







