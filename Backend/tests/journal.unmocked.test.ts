import request from "supertest";
import app from "../index"; // Adjust based on your app entry point
import { jest } from "@jest/globals";
import { JournalController } from "../src/controllers/JournalController";
import { MongoClient, Db, Collection, Document, BulkWriteResult } from "mongodb";
import { client } from "../services";
import fs from "fs";


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
    const dummy_token = "eyJhbGciOiJub25lIn0.eyJleHAiOjE4OTM0NTYwMDB9." // Expires in 2030

    const mockJournal = {
        date: "2025-03-11",
        userID: "llcce44@gmail.com",
        content: "Today was a good day.",
        googleNumID: google_num_id,
    };

    const mockJournal2 = {
        date: "2025-03-11",
        userID: "2334@gmail.com",
        content: "Today was a good day.",
        googleNumID: google_num_id
    };

    beforeAll(async () => {
        // console.log("\nCleaning up database before test...");
        // await client.db("cpen321journal").collection("journals").deleteMany({ userID: "llcce44@gmail.com" });
    
        // console.log("Database cleaned. Inserting user...");
    
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: "llcce44@gmail.com" }, // Find existing user
            {
                $set: {
                    userID: "llcce44@gmail.com",
                    isPaid: false,
                    googleNumID: google_num_id
                }
            },
            { upsert: true } // Insert if not found
        );
    
        // 🔍 Check if the user was inserted
        const testUser = await client.db("cpen321journal").collection("users").findOne({ userID: "llcce44@gmail.com" });
        console.log("Inserted User After Cleanup:", testUser);
    
        // Wait a bit to make sure MongoDB processes the insert
        await new Promise(resolve => setTimeout(resolve, 500));
    });
    

    it("should create and retrieve a journal entry", async () => {
        const postResponse = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send(mockJournal);
    
        expect(postResponse.status).toBe(200);
        expect(postResponse.body).toHaveProperty("message");
    
        // 🔍 Wait for DB to update before retrieving
        await new Promise(resolve => setTimeout(resolve, 500)); 
    
        const getResponse = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({ date: mockJournal.date, userID: mockJournal.userID, googleNumID: google_num_id });
    
        expect(getResponse.status).toBe(200);
        expect(getResponse.body).toHaveProperty("journal");
    });

    it("should respond with googleID and Token not matched", async () => {
        const response = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({ date: mockJournal.date, userID: mockJournal.userID, googleNumID: "123" });

        expect(response.status).toBe(403);
        expect(response.body).toHaveProperty("message", "Unauthorized: googleNumID does not match token");
    });

    it("should respond with Invalid Google token when authenticating", async () => {
        const response = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + "eyJhbGciOiJSUzI1NiIsImtpZCI6IjkxNGZiOWIwODcxODBiYzAzMDMyODQ1MDBjNWY1NDBjNmQ0ZjVlMmYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI1ODUwNDAyMDQyMTAtMmE3ZW9hbjF1YnM3aGJjZWRyY24zb2xyZHJnN2dyMDAuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI1ODUwNDAyMDQyMTAtamxscW8ybjNvZHJmcmY4dGhiZ3ZoaXY2azFwYzVmMmcuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDI3NjgzMjIyNzA1ODAzNzA2OTkiLCJlbWFpbCI6ImxsY2NlNDRAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJDaHJpc3RpbmUgSklBTkciLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jSXVLSEZJZUpSVXZpeGV3Z2Z1UXRBZXRoUXNiMnV0VFY5MWNXbG1vcXdUWExQTGFnPXM5Ni1jIiwiZ2l2ZW5fbmFtZSI6IkNocmlzdGluZSIsImZhbWlseV9uYW1lIjoiSklBTkciLCJpYXQiOjE3NDE5OTEyMjcsImV4cCI6MTc0MTk5NDgyN30.kHuUUU9e3YXIlc2vRarTFfjOoDsvhAb1DoOaJBtX5I6_IH-z14enwUmCJ0Fhme7cDa8LkFQ0BR7-lbQH6503WDaZ33yhVXoMdELKOrrxWC-RrBaivJCbxptt-73glL-b2S_yf4SvECzpiB1PfRE0lNeGcfEL6mq6LyqZBQNHpx3G7x7j8n2AHNNCl3o2zq4jwPsBUW3ZkDrUuEgh4sPMOe3Ern5rjMqEkEQA7Nvc5mqVGaEnOVdaBGgqui2GvnDSHwu14SP4rQNWsbEqpDLIYByvr7YrKGebgq6uG-auoa7E-MSvhw6vV0GhMgzTdRZ8YufaYms2WdPoIj96WYh5Kg") // expired token
            .query({ date: mockJournal.date, userID: mockJournal.userID, googleNumID: google_num_id });

        expect(response.status).toBe(403);
        expect(response.body).toHaveProperty("message", "Invalid Google token when authenticating");
    });

    it("should respond with missing google Token", async () => {
        const response = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " )
            .query({ date: mockJournal.date, userID: mockJournal.userID, googleNumID: google_num_id });

        expect(response.status).toBe(400);
        expect(response.body).toHaveProperty("message", "Missing googleToken");
    });

    it("should respond with missing googleNumID", async () => {
        const response = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + "eyJhbGciOiJSUzI1NiIsImtpZCI6IjkxNGZiOWIwODcxODBiYzAzMDMyODQ1MDBjNWY1NDBjNmQ0ZjVlMmYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI1ODUwNDAyMDQyMTAtMmE3ZW9hbjF1YnM3aGJjZWRyY24zb2xyZHJnN2dyMDAuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI1ODUwNDAyMDQyMTAtamxscW8ybjNvZHJmcmY4dGhiZ3ZoaXY2azFwYzVmMmcuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDI3NjgzMjIyNzA1ODAzNzA2OTkiLCJlbWFpbCI6ImxsY2NlNDRAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJDaHJpc3RpbmUgSklBTkciLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jSXVLSEZJZUpSVXZpeGV3Z2Z1UXRBZXRoUXNiMnV0VFY5MWNXbG1vcXdUWExQTGFnPXM5Ni1jIiwiZ2l2ZW5fbmFtZSI6IkNocmlzdGluZSIsImZhbWlseV9uYW1lIjoiSklBTkciLCJpYXQiOjE3NDE5OTEyMjcsImV4cCI6MTc0MTk5NDgyN30.kHuUUU9e3YXIlc2vRarTFfjOoDsvhAb1DoOaJBtX5I6_IH-z14enwUmCJ0Fhme7cDa8LkFQ0BR7-lbQH6503WDaZ33yhVXoMdELKOrrxWC-RrBaivJCbxptt-73glL-b2S_yf4SvECzpiB1PfRE0lNeGcfEL6mq6LyqZBQNHpx3G7x7j8n2AHNNCl3o2zq4jwPsBUW3ZkDrUuEgh4sPMOe3Ern5rjMqEkEQA7Nvc5mqVGaEnOVdaBGgqui2GvnDSHwu14SP4rQNWsbEqpDLIYByvr7YrKGebgq6uG-auoa7E-MSvhw6vV0GhMgzTdRZ8YufaYms2WdPoIj96WYh5Kg") // expired token
            .query({ date: mockJournal.date, userID: mockJournal.userID});

        expect(response.status).toBe(403);
        expect(response.body).toHaveProperty("message", "Invalid Google token when authenticating");
    });

    it("should respond with user not found", async () => {
        const response = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send({ 
                date: "2025-03-11",
                userID: "234@gmail.com", 
                googleNumID: google_num_id 
            });

        expect(response.status).toBe(404);
    });

    it("should create and retrieve a journal entry", async () => {
        const getResponse = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({ date: mockJournal.date, userID: "2434", googleNumID: google_num_id });
    
        expect(getResponse.status).toBe(404);
    });

    describe('putJournal', () => {
        it('should update an existing journal entry', async () => {
            // Setup initial entry
            await client.db("cpen321journal").collection("journals").insertOne({
                date: "2025-03-12",
                userID: "llcce44@gmail.com",
                text: "",
                media: []
            });
    
            const putResponse = await request(app)
            .put("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send({ date: "2025-03-12", userID: "llcce44@gmail.com", googleNumID: google_num_id});
            
            // expect(putResponse.status).toBe(400);
        });
      });

    describe('deleteJournal', () => {
        it('should delete an existing journal entry', async () => {
            // Setup initial entry
            await client.db("cpen321journal").collection("journals").insertOne({
                date: "2025-03-13",
                userID: "llcce44@gmail.com",
                media: []
            });
        
            const response = await request(app)
            .delete("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({ date: "2025-03-13", userID: "llcce44@gmail.com", googleNumID: google_num_id});
        
            // Check response
            expect(response.status).toBe(200);
        });
    }); 

    describe('getJournalFile', () => {
        it('should get a file', async () => {
            // Setup initial entry
            const postResponse = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send(mockJournal);
    
            expect(postResponse.status).toBe(200);
            expect(postResponse.body).toHaveProperty("message");
        
            const response = await request(app)
            .get("/api/journal/file?userID=llcce44@gmail.com&format=pdf")
            .set("Authorization", "Bearer " + testGoogleToken)
        
            // Check response
            // expect(response.status).toBe(200);
            // expect(response.body).toHaveProperty("ds");
            console.log("getJournalFile: ", response)
        });
    }); 

});







