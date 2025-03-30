import request from 'supertest';
import { app } from '../../index'; // Your Express app instance
import { client } from '../../services';
import { ObjectId } from 'mongodb';
import fs from "fs";

/**
 * Test Suite: User APIs - No Mocks (Integration)
 * - This test suite covers **real backend interactions** for the User API.
 * - It interacts with the actual database (no mocks used).
 * - Tests focus on verifying the expected API behavior under real conditions.
 */

describe('User APIs - No Mocks (Integration)', () => {
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

  const testGoogleToken = process.env.TEST_GOOGLE_TOKEN || unmocked_data_json.testGoogleToken;
  const google_num_id = process.env.GOOGLE_NUM_ID || unmocked_data_json.googleNumID;
  const google_user_prefix = process.env.GOOGLE_USER_PREFIX || unmocked_data_json.google_user_prefix;
  const testUserID = google_user_prefix+"123@gmail.com"

  console.log(testGoogleToken);

  /**
   * Set up the test database state before running tests.
   */
  beforeAll(async () => {
    await client.db("cpen321journal").collection("users").updateOne(
      { userID: google_user_prefix+"test@gmail.com" }, // Find existing user
      {
          $set: {
              userID: google_user_prefix+"test@gmail.com",
              isPaid: false,
              googleNumID: google_num_id
          }
      },
      { upsert: true } // Insert if not found
    );
    await client.db("cpen321journal").collection("users").updateOne(
      { userID: google_user_prefix+"123@gmail.com" }, // Find existing user
      {
          $set: {
              userID: google_user_prefix+"123@gmail.com",
              isPaid: false,
              googleNumID: google_num_id
          }
      },
      { upsert: true } // Insert if not found
    );
    await client.db("cpen321journal").collection("users").updateOne(
      { userID: google_user_prefix+"@gmail.com" }, // Find existing user
      {
          $set: {
              userID: google_user_prefix+"@gmail.com",
              isPaid: false,
              googleNumID: google_num_id
          }
      },
      { upsert: true } // Insert if not found
    );
  });

  /**
   * Cleanup: Remove test data after all tests have run.
   */
  afterAll(async () => {
    await client.db("cpen321journal").collection("users").deleteMany({ userID: google_user_prefix+"123445544545" });
  });

  /**
   * Test Group: GET /api/profile (Unmocked)
   * - Tests retrieving user profiles from the real database.
   */
  describe('GET /api/profile', () => {
    /**
     * Test Case: Valid userID
     * - **Inputs:** GET request with a valid userID.
     * - **Expected Status:** 200
     * - **Expected Behavior:** Returns user profile with `preferred_name`.
     */
    it('should return 200 with user profile when valid userID is provided', async () => {
      const res = await request(app)
        .get('/api/profile')
        .query({userID:google_user_prefix+"test@gmail.com"});
      
      expect(res.statusCode).toEqual(200);
      expect(res.body).toHaveProperty('preferred_name');
    });

    /**
     * Test Case: Missing userID
     * - **Inputs:** GET request without userID.
     * - **Expected Status:** 400
     * - **Expected Behavior:** API should return a bad request response.
     */
    it('should return 400 when userID is missing', async () => {
      const res = await request(app)
        .get('/api/profile');
      
      expect(res.statusCode).toEqual(400);
    });
  });

  /**
   * Test Group: POST /api/profile (Unmocked)
   * - Tests creating/updating user profiles in the real database.
   */
  describe('POST /api/profile', () => {
    /**
     * Test Case: Valid existing user update
     * - **Inputs:** POST request with an existing userID.
     * - **Expected Status:** 200
     */
    it ("should return 200 for existing user when userID is in the database", async () => {
        const res = await request(app)
        .post('/api/profile')
        .send({userID: google_user_prefix+"test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken});

        expect(res.statusCode).toEqual(200);
    });

    /**
     * Test Case: Valid new user creation
     * - **Inputs:** POST request with a new userID.
     * - **Expected Status:** 200
     */
    it ("should return 200 for new user when userID is not in the database", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: google_user_prefix+"123445544545", preferred_name: "test user", googleToken: testGoogleToken});

      expect(res.statusCode).toEqual(200);
    });

    /**
     * Test Cases: Invalid activity tracking format
     * - **Inputs:** POST request with incorrect activity format.
     * - **Expected Status:** 400
     */
    it ("should return 400 for wrong activity format", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: google_user_prefix+"test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken, activities_tracking: "ds"});

      expect(res.statusCode).toEqual(400);
    });

    it ("shoudl return 400 for wrong activity format", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: google_user_prefix+"test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken, activities_tracking: ["ds"]});

      expect(res.statusCode).toEqual(400);
    })

    it ("shoudl return 400 for wrong activity format", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: google_user_prefix+"test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken, activities_tracking: [{ name: 'Exercise', averageValue: 'not a number', unit: 'Hours' }]});

      expect(res.statusCode).toEqual(400);
    })

    it ("shoudl return 400 for wrong activity format", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: google_user_prefix+"test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken, activities_tracking: [{ name: 3434, averageValue: 'not a number', unit: 'Hours' }]});

      expect(res.statusCode).toEqual(400);
    })

    it ("shoudl return 400 for wrong activity format", async () => {
      const res = await request(app)
      .post('/api/profile')
      .send({userID: google_user_prefix+"test@gmail.com", preferred_name: "test user", googleToken: testGoogleToken, activities_tracking: [{ name: "exercise", averageValue: 34, unit: 'Hrs' }]});

      expect(res.statusCode).toEqual(400);
    })

  });

  /**
   * Test Group: GET /api/profile/isPaid (No Mocks)
   * - Verifies user payment status retrieval.
   */
  describe('GET /api/profile/isPaid', () => {
    /**
     * Test Case: Existing user
     * - **Inputs:** Valid `userID`
     * - **Expected Behavior:** Should return **200** OK.
     */
    it ("should return 200 for existing user when userID is in the database", async () => {
        const res = await request(app)
        .get('/api/profile/isPaid')
        .query({userID:google_user_prefix+"test@gmail.com"})
        
        expect(res.statusCode).toEqual(200);
        
    })

    /**
     * Test Case: Non-existing user
     * - **Inputs:** Invalid `userID`
     * - **Expected Behavior:** Should return **404** Not Found.
     */
    it ("should return 404 for non-existing user", async () => {
      const res = await request(app)
      .get('/api/profile/isPaid?userID=2345678')
      
      expect(res.statusCode).toEqual(404);
    })
  });

  /**
   * Test Group: POST /api/profile/reminder (No Mocks)
   * - Tests user reminder update.
   */
  describe('POST /api/profile/reminder', () => {
    it ("should return 400 for no updated reminder field", async () => {
        const res = await request(app)
        .post('/api/profile/reminder')
        .send({ userID: "2234343" })
        
        expect(res.statusCode).toEqual(400);
        
    })

    it ("should return 404 for non-existing user", async () => {
      const res = await request(app)
      .post('/api/profile/reminder')
      .send({ userID: "22343433434343", updated_reminder: {

        Weekday: [1], // Monday in user's timezone
        
        time: '21:00' // 9 PM PDT (UTC-7)
        
        } })
      
      expect(res.statusCode).toEqual(404);
    })

    it ("should return 200 for existing user when userID is in the database", async () => {
      const res = await request(app)
      .post('/api/profile/reminder')
      .send({ userID: google_user_prefix+"test@gmail.com", updated_reminder: {
        Weekday: [1], // Monday in user's timezone
        time: '21:00' // 9 PM PDT (UTC-7)
        } });
      
      expect(res.statusCode).toEqual(200);
    })
  });

  /**
   * Test Group: POST /api/profile/fcmtoken (No Mocks)
   * - Tests FCM token storage.
   */
  describe('POST /api/profile/fcmtoken', () => {
    it ("should return 400 for missing fcmToken field", async () => {
        const res = await request(app)
        .post('/api/profile/fcmtoken')
        .send({ userID: "2234343" })
        
        expect(res.statusCode).toEqual(400);
    })

    it ("should return 400 for invalid userID", async () => {
      const res = await request(app)
      .post('/api/profile/fcmtoken')
      .send({ userID: "22343433434343", fcmToken: "ireofoej"})
      
      expect(res.statusCode).toEqual(400);
    })

    it ("should return 200 for valid user update", async () => {
      const res = await request(app)
      .post('/api/profile/fcmtoken')
      .send({ userID: google_user_prefix+"test@gmail.com", fcmToken: "ireofoej", timeOffset: "-07:00" });
      
      expect(res.statusCode).toEqual(200);
    })
  });  
  
});