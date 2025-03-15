import request from 'supertest';
import { app } from '../index'; // Your Express app instance
import { client } from '../services';
import { ObjectId } from 'mongodb';
import fs from "fs";

describe('User APIs - No Mocks (Integration)', () => {
  const testUserID = 'test@gmail.com';
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
    await client.db("cpen321journal").collection("users").updateOne(
      { userID: "test@gmail.com" }, // Find existing user
      {
          $set: {
              userID: "test@gmail.com",
              isPaid: false,
              googleNumID: google_num_id
          }
      },
      { upsert: true } // Insert if not found
    );
  });
  

  afterAll(async () => {
    // Cleanup test data
    await client.db("cpen321journal").collection("users").deleteMany({ userID: "123445544545" });
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
        .send({userID: "test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken});

        expect(res.statusCode).toEqual(200);
        
    })

    // Test post a new user
    it ("shoudl return 200 for existing user when userID is in the database", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: "123445544545", preferred_name: "test user", googleToken: testGoogleToken});

      expect(res.statusCode).toEqual(200);
    })

    // Test post a user + activity
    it ("shoudl return 400 for wrong activity format", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: "test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken, activities_tracking: "ds"});

      expect(res.statusCode).toEqual(400);
    })

    // Test post a user + activity
    it ("shoudl return 400 for wrong activity format", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: "test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken, activities_tracking: ["ds"]});

      expect(res.statusCode).toEqual(400);
    })

    // Test post a user + activity
    it ("shoudl return 400 for wrong activity format", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: "test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken, activities_tracking: [{ name: 'Exercise', averageValue: 'not a number', unit: 'Hours' }]});

      expect(res.statusCode).toEqual(400);
    })

    // Test post a user + activity
    it ("shoudl return 400 for wrong activity format", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: "test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken, activities_tracking: [{ name: 3434, averageValue: 'not a number', unit: 'Hours' }]});

      expect(res.statusCode).toEqual(400);
    })

    // Test post a user + activity
    it ("shoudl return 400 for wrong activity format", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: "test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken, activities_tracking: [{ name: "exercise", averageValue: 34, unit: 'Hrs' }]});

      expect(res.statusCode).toEqual(400);
    })

  });

  describe('GET /api/profile/isPaid', () => {
    // Test get ispaid an existing user
    it ("should return 200 for existing user when userID is in the database", async () => {
        const res = await request(app)
        .get('/api/profile/isPaid?userID=test@gmail.com')
        
        expect(res.statusCode).toEqual(200);
        
    })

    // Test get ispaid a non-existing user
    it ("should return 200 for existing user when userID is in the database", async () => {
      const res = await request(app)
      .get('/api/profile/isPaid?userID=2345678')
      
      expect(res.statusCode).toEqual(404);
    })
  });

  describe('POST /api/profile/reminder', () => {
    // Test post reminder
    it ("should return 400 for no updated reminder field", async () => {
        const res = await request(app)
        .post('/api/profile/reminder')
        .send({ userID: "2234343" })
        
        expect(res.statusCode).toEqual(400);
        
    })

    // Test post reminder
    it ("should return 200 for existing user when userID is in the database", async () => {
      const res = await request(app)
      .post('/api/profile/reminder')
      .send({ userID: "22343433434343", updated_reminder: {

        Weekday: [1], // Monday in user's timezone
        
        time: '21:00' // 9 PM PDT (UTC-7)
        
        } })
      
      expect(res.statusCode).toEqual(404);
    })

    // Test post reminder
    it ("should return 200 for existing user when userID is in the database", async () => {
      const res = await request(app)
      .post('/api/profile/reminder')
      .send({ userID: "test@gmail.com", updated_reminder: {
        Weekday: [1], // Monday in user's timezone
        time: '21:00' // 9 PM PDT (UTC-7)
        } });
      
      expect(res.statusCode).toEqual(200);
    })
  });

  describe('POST /api/profile/fcmtoken', () => {
    // Test post fcmtoken
    it ("should return 400 for no updated reminder field", async () => {
        const res = await request(app)
        .post('/api/profile/fcmtoken')
        .send({ userID: "2234343" })
        
        expect(res.statusCode).toEqual(400);
    })

    // Test post fcmtoken
    it ("should return 200 for existing user when userID is in the database", async () => {
      const res = await request(app)
      .post('/api/profile/fcmtoken')
      .send({ userID: "22343433434343", fcmToken: "ireofoej"})
      
      expect(res.statusCode).toEqual(400);
    })

    // Test post fcmtoken
    it ("should return 200 for existing user when userID is in the database", async () => {
      const res = await request(app)
      .post('/api/profile/fcmtoken')
      .send({ userID: "test@gmail.com", fcmToken: "ireofoej", timeOffset: "-07:00" });
      
      expect(res.statusCode).toEqual(200);
    })
  });  
  
});