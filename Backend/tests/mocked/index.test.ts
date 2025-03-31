import request from 'supertest';
import app from '../../index'; // Adjust path to match your project
import { client } from '../../services'; // Ensure this imports your MongoDB client
import cron from 'node-cron';
import { MongoClient } from 'mongodb';
import admin from 'firebase-admin';
import { scheduleNotifications } from '../../index';

let server: any;

/**
 * **Setup: Start and stop the server before and after tests**
 */
beforeAll(() => {
    server = app.listen(); // Start the server before running tests
});

afterAll(async () => {
    if (server) {
        server.close(); // Close the server after tests
    }
    await client.close(); // Close MongoDB connection
});

/**
 * **Test Group: Basic API Endpoints**
 * - Tests if the root and name endpoints are responding correctly.
 */
describe('Basic API Endpoints', () => {
    /**
     * Test Case: GET /name
     * - **Inputs:** None
     * - **Expected Behavior:**
     *   - Returns a JSON response with `firstName` and `lastName`.
     *   - Response status code: **200**
     */
    it('should return firstName and lastName', async () => {
        const response = await request(app).get('/name');

        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty('firstName', 'FirstName');
        expect(response.body).toHaveProperty('lastName', 'Lastname');
    });

    /**
     * Test Case: GET /
     * - **Inputs:** None
     * - **Expected Behavior:**
     *   - Returns a simple string message.
     *   - Response status code: **200**
     */
    it('should return API is running...', async () => {
        const response = await request(app).get('/');

        expect(response.status).toBe(200);
        expect(response.text).toBe('API is running...');
    });
});

/**
 * **Test Group: Firebase Admin Initialization**
 * - Ensures Firebase Admin SDK is initialized correctly.
 */
describe('Firebase Admin Initialization', () => {
    beforeEach(() => {
        jest.resetModules();
        process.env.NODE_ENV = 'test'; // Prevent server from starting
        jest.clearAllMocks();
    });

    /**
     * Test Case: Firebase should not initialize if an app already exists
     * - **Mock Behavior:** `firebase-admin.apps` returns a non-empty array, meaning Firebase is already initialized.
     * - **Expected Behavior:** `initializeApp()` should **not** be called.
     */
    it('does not initialize Firebase if an app exists', () => {
        jest.doMock('firebase-admin', () => ({
            apps: [{}], // Simulates that Firebase is already initialized
            initializeApp: jest.fn(),
            credential: { cert: jest.fn() },
        }));

        const admin = require('firebase-admin');
        require('../../index'); // Load the app to trigger Firebase initialization check
        expect(admin.initializeApp).not.toHaveBeenCalled();
    });
});

/**
 * **Test Group: Stripe Secret Configuration**
 * - Ensures that the Stripe secret key is properly configured.
 */
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

    /**
     * Test Case: Stripe secret file missing, but environment variable fallback
     * - **Mock Behavior:** `fs.readFileSync` is mocked to throw an error only for the Stripe secret file.
     * - **Expected Behavior:** A **warning** should be logged, and the environment variable should be used instead.
     */
    it('throws error if Stripe secret is missing', () => {
        const fs = require('fs');
        jest.spyOn(fs, 'readFileSync').mockImplementation(() => {
            throw new Error('File not found');
        });
    
        // Clear any existing Stripe secret from the environment
        delete process.env.STRIPE_SECRET;
    
        // Re-require the module to trigger the error
        expect(() => {
            jest.isolateModules(() => {
                require('../../index'); // Force fresh module loading
            });
        }).toThrow();
        
    });
});
