import request from 'supertest';
import app from '../index'; // Adjust path to match your project
import { client } from '../services'; // Ensure this imports your MongoDB client
import cron from 'node-cron';
import { MongoClient } from 'mongodb';
import admin from 'firebase-admin';
import {scheduleNotifications} from '../index';

let server: any;

beforeAll(() => {
    // Start the server
    server = app.listen();
});

afterAll(async () => {
    if (server) {
        server.close();
    }
    await client.close(); // Close MongoDB connection
});

describe('GET /name', () => {
    it('should return firstName and lastName', async () => {
        const response = await request(app).get('/name');

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('firstName', 'FirstName');
        expect(response.body).toHaveProperty('lastName', 'Lastname');
    });
});

describe('GET /', () => {
    it('should return API is running...', async () => {
        const response = await request(app).get('/');

        expect(response.status).toBe(200);
        expect(response.text).toBe('API is running...');
    });
});

// Firebase Admin Initialization Tests
describe('Firebase Admin Initialization', () => {
    beforeEach(() => {
      jest.resetModules();
      process.env.NODE_ENV = 'test'; // Prevent server from starting
      jest.clearAllMocks();
    });
  
    it('initializes Firebase when no apps exist', () => {
      jest.doMock('firebase-admin', () => ({
        apps: [],
        initializeApp: jest.fn(),
        credential: { cert: jest.fn() },
      }));
  
      const admin = require('firebase-admin');
      require('../index');
      expect(admin.initializeApp).toHaveBeenCalled();
    });
  
    it('does not initialize Firebase if an app exists', () => {
      jest.doMock('firebase-admin', () => ({
        apps: [{}],
        initializeApp: jest.fn(),
        credential: { cert: jest.fn() },
      }));
  
      const admin = require('firebase-admin');
      require('../index');
      expect(admin.initializeApp).not.toHaveBeenCalled();
    });
  });
  
  // Stripe Secret Configuration Tests
  describe('Stripe Secret Configuration', () => {
    beforeEach(() => {
      jest.resetModules();
      process.env.NODE_ENV = 'test'; // Prevent server from starting
      process.env.STRIPE_SECRET = '...';
      jest.clearAllMocks();
    });
  
    afterEach(() => {
      jest.restoreAllMocks();
    });
  
    it('throws error if Stripe secret is missing', () => {
      const fs = require('fs');
      jest.spyOn(fs, 'readFileSync').mockImplementation(() => {
        throw new Error('Missing Stripe Secret Key!');
      });
  
      expect(() => require('../index')).toThrow('Missing Stripe Secret Key!');
    });
  
  
    it('logs warning when Stripe file is missing and uses env fallback', () => {
        const fs = require('fs');
        const path = require('path');
        const originalReadFileSync = fs.readFileSync;
      
        // Calculate the expected path for the Stripe secret file
        const expectedPath = path.join(__dirname, '../src/config/cpen321project-stripe-secret.txt');
      
        // Mock readFileSync to throw only for the Stripe secret file
        jest.spyOn(fs, 'readFileSync').mockImplementation((filePath, ...args) => {
          if (filePath === expectedPath) {
            throw new Error('File not found');
          } else {
            return originalReadFileSync.call(fs, filePath, ...args);
          }
        });
      
        const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
      
        process.env.STRIPE_SECRET = 'env_secret';
        require('../index'); // Now, other files can be read normally
      
        expect(consoleWarnSpy).toHaveBeenCalledWith(
          'Stripe secret file not found, falling back to environment variable.'
        );
      
        // Cleanup
        consoleWarnSpy.mockRestore();
        fs.readFileSync.mockRestore();
      });
  });

