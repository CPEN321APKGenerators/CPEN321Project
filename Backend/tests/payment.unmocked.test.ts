import request from 'supertest';
import app from '../index';

/**
 * Interface POST /api/payment-sheet
 * Group: Unmocked
 */
describe('Unmocked: POST /api/payment-sheet', () => {
  let server: any;

  beforeAll((done) => {
    server = app.listen(0, () => done()); // Random available port
  });

  afterAll((done) => {
    server.close(done);
  });

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

  test('Missing userID', async () => {
    const res = await request(server)
      .post('/api/payment-sheet')
      .send({});

    expect(res.statusCode).toBe(200);
    expect(res.body.userID).toBeUndefined();
  });
});