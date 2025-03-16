import request from 'supertest';
import app from '../../index';

/**
 * Test Suite: Unmocked: POST /api/payment-sheet
 * - This test suite covers **real backend interactions** for the `/api/payment-sheet` endpoint.
 * - It does **not** use mocks, meaning actual payment processing logic is tested.
 */

describe('Unmocked: POST /api/payment-sheet', () => {
  let server: any;

  /**
   * Before running the tests:
   * - Start the app on a random available port to prevent conflicts.
   */
  beforeAll((done) => {
    server = app.listen(0, () => done()); // Random available port
  });

  /**
   * After all tests are complete:
   * - Ensure the server is properly closed to prevent resource leaks.
   */
  afterAll((done) => {
    server.close(done);
  });

  /**
   * Test Case: Valid `userID` provided
   * 
   * - **Inputs:**
   *   - Request Type: `POST`
   *   - URL: `/api/payment-sheet`
   *   - Body:
   *     ```json
   *     {
   *       "userID": "testUser123"
   *     }
   *     ```
   * 
   * - **Expected Behavior:**
   *   - The API should process the payment request and return a **valid payment intent response**.
   *   - Response status code: **200**
   *   - Response body should contain:
   *     ```json
   *     {
   *       "paymentIntent": "someString",
   *       "ephemeralKey": "someString",
   *       "customer": "someString",
   *       "publishableKey": "someString",
   *       "userID": "testUser123"
   *     }
   *     ```
   *   - Each returned value should be a **string**.
   */
  test('Valid userID provided', async () => {
    const res = await request(server)
      .post('/api/payment-sheet')
      .send({ userID: 'testUser123' });

    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual({
      paymentIntent: expect.any(String),
      ephemeralKey: expect.any(String),
      customer: expect.any(String),
      publishableKey: expect.any(String),
      userID: 'testUser123',
    });
  });

  /**
   * Test Case: Missing `userID`
   * 
   * - **Inputs:**
   *   - Request Type: `POST`
   *   - URL: `/api/payment-sheet`
   *   - Body:
   *     ```json
   *     {}
   *     ```
   * 
   * - **Expected Behavior:**
   *   - The API should still return **200**, but `userID` should be **undefined** in the response.
   *   - Response status code: **200**
   *   - Response body should **not** contain `userID`.
   */
  test('Missing userID', async () => {
    const res = await request(server)
      .post('/api/payment-sheet')
      .send({});

    expect(res.statusCode).toBe(200);
    expect(res.body.userID).toBeUndefined();
  });
});
