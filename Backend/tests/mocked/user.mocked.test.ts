import request from "supertest";
import app from "../../index";
import { client } from "../../services";
import { UserController } from '../../src/controllers/UserController';
import { Request, Response } from 'express';
import axios from 'axios';
import admin from 'firebase-admin';
import fs from "fs";

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
const dummy_token = "eyJhbGciOiJub25lIn0.eyJleHAiOjE4OTM0NTYwMDB9." // Expires in 2030
const google_user_prefix = process.env.GOOGLE_USER_PREFIX || unmocked_data_json.google_user_prefix;
const test_main_user_id = google_user_prefix+"@gmail.com"

/**
 * Test Suite: User APIs - With Mocks
 * - This test suite covers **mocked** backend interactions for the User API.
 * - Mocks are used to control and simulate behavior for:
 *   - Database interactions (MongoDB service)
 *   - Google authentication via `axios`
 *   - Firebase authentication
 */

jest.mock('../../services', () => ({
  client: {
    db: jest.fn().mockReturnValue({
      collection: jest.fn().mockReturnValue({
        findOne: jest.fn(),
        updateOne: jest.fn(),
        insertOne: jest.fn()
      })
    })
  }
}));

jest.mock('axios');
jest.mock('firebase-admin', () => ({
    apps: [],
    initializeApp: jest.fn(() => ({
      firestore: jest.fn(),
    })),
    auth: jest.fn(() => ({
      verifyIdToken: jest.fn(),
    })),
  }));
  

describe('User APIs - With Mocks', () => {
  let userController: UserController;
  let mockRequest: Partial<Request>;
  let mockResponse: Partial<Response>;
  const mockUser = {
    userID: google_user_prefix+'mock-user-123',
    isPaid: false
  };

  beforeEach(() => {
    userController = new UserController();
    mockResponse = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    } as unknown as Response;
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  /**
   * Test Group: getUserProfile (Mocked)
   * - Relies on **mocked database service**
   */
  describe('getUserProfile (Mocked)', () => {
    /**
     * Test Case: Database error simulation
     * 
     * - **Inputs:**
     *   - Request: `GET /userProfile`
     *   - Query Parameter: `userID=valid-user`
     * 
     * - **Mock Behavior:**
     *   - `findOne` is **mocked to throw an error** (`DB Error`).
     * 
     * - **Expected Behavior:**
     *   - The request should fail due to a database error.
     *   - Response status code: **500**
     */
    it('should return 500 on database error', async () => {
      (client.db("cpen321journal").collection("users").findOne as jest.Mock)
        .mockRejectedValue(new Error('DB Error'));
      
      mockRequest = { query: { userID: google_user_prefix+'valid-user' } };
      
      await userController.getUserProfile(mockRequest as Request, mockResponse as Response, jest.fn());
      
      expect(mockResponse.status).toHaveBeenCalledWith(500);
    });
  });

  /**
   * Test Group: createOrUpdateUserProfile (Mocked)
   * - Relies on **mocked Google authentication**
   */
  describe('createOrUpdateUserProfile (Mocked)', () => {
    /**
     * Test Case: Invalid Google token
     * 
     * - **Inputs:**
     *   - Request: `POST /userProfile`
     *   - Body:
     *     ```json
     *     {
     *       "userID": "new-user",
     *       "googleToken": "invalid-token"
     *     }
     *     ```
     * 
     * - **Mock Behavior:**
     *   - `axios.get` is **mocked to reject the request** (`Invalid token` error).
     * 
     * - **Expected Behavior:**
     *   - The request should fail due to invalid authentication.
     *   - Response status code: **403**
     */
    it('should return 403 with invalid Google token', async () => {
      (axios.get as jest.Mock).mockRejectedValue(new Error('Invalid token'));
      
      mockRequest = {
        body: {
          userID: google_user_prefix+'new-user',
          googleToken: 'invalid-token'
        }
      };
      
      await userController.createOrUpdateUserProfile(mockRequest as Request, mockResponse as Response, jest.fn());
      
      expect(mockResponse.status).toHaveBeenCalledWith(403);
    });
  });

  /**
   * Test Group: postFCMToken (Mocked)
   * - Relies on **mocked database service**
   */
  describe('postFCMToken (Mocked)', () => {
    /**
     * Test Case: Database error simulation
     * 
     * - **Inputs:**
     *   - Request: `POST /storeFcmToken`
     *   - Body:
     *     ```json
     *     {
     *       "userID": "valid-user",
     *       "fcmToken": "ioerfrejio",
     *       "timeOffset": "-7:00"
     *     }
     *     ```
     * 
     * - **Mock Behavior:**
     *   - `updateOne` is **mocked to throw an error** (`DB Error`).
     * 
     * - **Expected Behavior:**
     *   - The request should fail due to a database error.
     *   - Response status code: **500**
     */
    it('should return 500 on database error', async () => {
      (client.db("cpen321journal").collection("users").updateOne as jest.Mock)
        .mockRejectedValue(new Error('DB Error'));
      
      mockRequest = { body: { userID: google_user_prefix+'valid-user', fcmToken:"ioerfrejio", timeOffset: "-7:00" } };
      
      await userController.storeFcmToken(mockRequest as Request, mockResponse as Response, jest.fn());
      
      expect(mockResponse.status).toHaveBeenCalledWith(500);
    });
  });

  /**
   * Test Group: changeReminder (Mocked)
   * - Relies on **mocked database service**
   */
  describe('changeReminder (Mocked)', () => {
    /**
     * Test Case: Database error simulation
     * 
     * - **Inputs:**
     *   - Request: `POST /changeReminder`
     *   - Body:
     *     ```json
     *     {
     *       "userID": "test@gmail.com",
     *       "fcmToken": "ioerfrejio",
     *       "timeOffset": "-7:00"
     *     }
     *     ```
     * 
     * - **Mock Behavior:**
     *   - `updateOne` is **mocked to throw an error** (`DB Error`).
     * 
     * - **Expected Behavior:**
     *   - The request should fail due to a database error.
     *   - Response status code: **500**
     */
    it('should return 500 on database error', async () => {
      (client.db("cpen321journal").collection("users").updateOne as jest.Mock)
        .mockRejectedValue(new Error('DB Error'));
      
      mockRequest = { body: { userID: google_user_prefix+'test@gmail.com', fcmToken:"ioerfrejio", timeOffset: "-7:00" } };
      
      await userController.changeReminder(mockRequest as Request, mockResponse as Response, jest.fn());
      
      expect(mockResponse.status).toHaveBeenCalledWith(500);
    });
  });

  /**
   * Test Group: getUserProfile (Mocked)
   * - Relies on **mocked database service**
   */
  describe('getUserIsPaid (Mocked)', () => {
    /**
     * Test Case: Database error simulation
     * 
     * - **Inputs:**
     *   - Request: `GET /user/profile/isPaid`
     *   - Query Parameter: `userID=valid-user`
     * 
     * - **Mock Behavior:**
     *   - `findOne` is **mocked to throw an error** (`DB Error`).
     * 
     * - **Expected Behavior:**
     *   - The request should fail due to a database error.
     *   - Response status code: **500**
     */
    it('should return 500 on database error', async () => {
      (client.db("cpen321journal").collection("users").findOne as jest.Mock)
        .mockRejectedValue(new Error('DB Error'));
      
      mockRequest = { query: { userID: google_user_prefix+'valid-user' } };
      
      await userController.isUserPaid(mockRequest as Request, mockResponse as Response, jest.fn());
      
      expect(mockResponse.status).toHaveBeenCalledWith(500);
    });
  });
});
