import request from 'supertest';
import { app } from '../index'; // Your Express app instance
import { client } from '../services';
import { ObjectId } from 'mongodb';
import fs from "fs";

describe('User APIs - No Mocks (Integration)', () => {
  const testUserID = 'llcce44@gmail.com';
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

  beforeAll(async () => {
    // Initialize test user
    await client.db("cpen321journal").collection("users").insertOne({
      userID: testUserID,
      isPaid: false,
      googleNumID: google_num_id
    });
  });

  afterAll(async () => {
    // Cleanup test data
    // await client.db("cpen321journal").collection("users").deleteMany({ userID: testUserID });
  });

  describe('GET /api/profile', () => {
    // Test Case: Valid userID
    it('should return 200 with user profile when valid userID is provided', async () => {
      const res = await request(app)
        .get('/api/profile')
        .query({ userID: testUserID });
      
      expect(res.statusCode).toEqual(200);
      expect(res.body).toHaveProperty('preferred_name');
    });

    // Test Case: Missing userID
    it('should return 400 when userID is missing', async () => {
      const res = await request(app)
        .get('/api/profile');
      
      expect(res.statusCode).toEqual(400);
    });
  });

  describe('POST /api/profile', () => {
    // Test post an existing user
    it ("shoudl return 200 for existing user when userID is in the database", async () => {
        const res = await request(app)
        .post('/api/profile')
        .send({userID: "llcce44@gmail.com", preferred_name: "test user", googleToken: testGoogleToken});

        expect(res.statusCode).toEqual(200);
        
    })

  });
  
});