import request from "supertest";
import app from "../index";
import { client } from "../services";
import { UserController } from '../src/controllers/UserController';
import { Request, Response } from 'express';
import axios from 'axios';
import admin from 'firebase-admin';

jest.mock('../services', () => ({
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
    userID: 'mock-user-123',
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

  describe('getUserProfile (Mocked)', () => {
    // Test Case: Database error simulation
    it('should return 500 on database error', async () => {
      (client.db("cpen321journal").collection("users").findOne as jest.Mock)
        .mockRejectedValue(new Error('DB Error'));
      
      mockRequest = { query: { userID: 'valid-user' } };
      
      await userController.getUserProfile(mockRequest as Request, mockResponse as Response, jest.fn());
      
      expect(mockResponse.status).toHaveBeenCalledWith(500);
    });
  });

  describe('createOrUpdateUserProfile (Mocked)', () => {
    // Test Case: Invalid Google token
    it('should return 403 with invalid Google token', async () => {
      (axios.get as jest.Mock).mockRejectedValue(new Error('Invalid token'));
      
      mockRequest = {
        body: {
          userID: 'new-user',
          googleToken: 'invalid-token'
        }
      };
      
      await userController.createOrUpdateUserProfile(mockRequest as Request, mockResponse as Response, jest.fn());
      
      expect(mockResponse.status).toHaveBeenCalledWith(403);
    });
  });

  
});