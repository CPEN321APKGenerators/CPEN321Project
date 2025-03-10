import request from 'supertest';
import app from '../index'; // Adjust path to match your project
import { client } from '../services'; // Ensure this imports your MongoDB client

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
