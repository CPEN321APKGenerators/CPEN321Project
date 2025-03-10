import express, { NextFunction, Request, Response } from "express";
import dotenv from "dotenv";
dotenv.config();

import { MongoClient } from "mongodb";
import { client } from "./services";
import { JournalRoutes } from "./src/routes/JournalRoutes";
import { AnalysisRoutes } from "./src/routes/AnalysisRoutes";
import { validationResult } from "express-validator";
import morgan from "morgan";
import { UserRoutes } from "./src/routes/UserRoutes";
import cron from 'node-cron';
import admin from 'firebase-admin';
import fs from 'fs';
import path from 'path';

const { DateTime } = require('luxon');


const app = express();

// if this middleware is before get, it means it will run before the get request
app.use(express.json())
app.use(morgan('tiny'))

// Initialize Firebase Admin
if (!admin.apps.length) {
    const serviceAccount = require('./src/config/cpen321project-c324e-firebase-adminsdk.json');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

// Read the secret as a string
const stripeSecret = fs.readFileSync(path.join(__dirname, './src/config/cpen321project-stripe-secret.txt'), 'utf8').trim();
console.log(stripeSecret)
// const OtherRoutes=[]
const Routes = [...AnalysisRoutes, ...JournalRoutes, ...UserRoutes];
// const Routes = [...JournalRoutes];

Routes.forEach((route) => {
    (app as any)[route.method](
        route.route,
        route.validation,
        async (req: Request, res: Response, next: NextFunction) => {
            const errors = validationResult(req);
            if (!errors.isEmpty()) {
                /* If there are validation errors, send a response with the error messages */
                return res.status(400).send({ errors: errors.array() });
            }
            try {
                await route.action(
                    req,
                    res,
                    next,
                );
            } catch (err) {
                console.log(err)
                return res.sendStatus(500); // Don't expose internal server workings
            }
        },
    );
});

// Route to get server IP address
app.get('/server-ip', (req, res) => {
    const serverip = req.socket.localAddress; // Server's IP address
    res.json({ serverIP: serverip });
});

// Route to get server local time
app.get('/server-time', (req, res) => {
    const now = new Date();
    const timeZoneOffset = -now.getTimezoneOffset(); // Offset in minutes

    const hoursOffset = Math.floor(timeZoneOffset / 60);
    const minutesOffset = timeZoneOffset % 60;
    const secondsOffset = minutesOffset * 60;

    const offsetString = `GMT${hoursOffset >= 0 ? '+' : ''}${String(hoursOffset).padStart(2, '0')}:${String(secondsOffset).padStart(2, '0')}`;
    const timeString = now.toLocaleTimeString('en-GB', { hour12: false }); // 24-hour format

    const serverStr = `${timeString} ${offsetString}`;
    res.json({ serverTime: serverStr });
});

// Route to get your name
app.get('/name', (req, res) => {
    res.json({ firstName: "FirstName", lastName: "Lastname" });
});

// Add a route for the root URL
app.get('/', (req, res) => {
    res.send("API is running...");
});

app.use('/public', express.static(path.join(__dirname, 'public')));


// Do NOT start the server if in test environment
if (process.env.NODE_ENV !== 'test') {
    client.connect().then(() => {
      console.log("MongoDB Client connected");
      
      // Main server
      app.listen(process.env.PORT, () => {
        console.log("Listening on port " + process.env.PORT);
      });
  
      // Stripe webhook server
      app.listen(4242, () => console.log('Webhook Running on port 4242'));
    }).catch(err => {
      console.log(err);
      client.close();
    });
  }

export { app };

import Stripe from 'stripe';

export const setStripeInstance = (mockedStripe: Stripe) => {
    stripe = mockedStripe;
};

// Allow injecting a mocked Stripe instance
export const createStripeInstance = (secret: string) => new Stripe(secret, {
apiVersion: '2025-02-24.acacia',
});

let stripe = createStripeInstance(stripeSecret);

  
// This example sets up an endpoint using the Express framework.
// Watch this video to get started: https://youtu.be/rPR2aJ6XnAc.

// In your /api/payment-sheet route handler
// In your /api/payment-sheet route handler
app.post('/api/payment-sheet', async (req, res) => {
    try {
      const { userID } = req.body;
      console.log("user id from payment: ", userID);
  
      // Customer creation
      const customer = await stripe.customers.create();
      if (!customer?.id) throw new Error('Failed to create customer');
  
      // Ephemeral key
      const ephemeralKey = await stripe.ephemeralKeys.create(
        { customer: customer.id },
        { apiVersion: '2025-02-24.acacia' }
      );
  
      // Payment intent
      const paymentIntent = await stripe.paymentIntents.create({
        amount: 1099,
        currency: 'cad',
        customer: customer.id,
        automatic_payment_methods: { enabled: true },
        metadata: { userID }
      });
  
      res.json({
        paymentIntent: paymentIntent.client_secret,
        ephemeralKey: ephemeralKey.secret,
        customer: customer.id,
        publishableKey: 'pk_test_51QwDbGG6TJZ7pu2RAQVhbPsY2hJ7YGawx4M14Ld89ijypNVLWlne8aEivnlObsBwTqq1IfZT7NyVkQU3Ftzj08qF00KP7rf6ZM',
        userID
      });
      
    } catch (err) {
      console.error('Payment sheet error:', err);
      res.sendStatus(500);
    }
});

// Replace this endpoint secret with your endpoint's unique secret 
// If you are testing with the CLI, find the secret by running 'stripe listen'
// If you are using an endpoint defined with the API or dashboard, look in your webhook settings
// at https://dashboard.stripe.com/webhooks
// const endpointSecret = 'whsec_...';


// app.listen(4242, () => console.log('Webhook Running on port 4242'));

app.post('/webhook', express.raw({type: 'application/json'}), (request, response) => {
  let event = request.body;
  console.log("webhook event: ", event)

  // Handle the event
  switch (event.type) {
    case 'payment_intent.succeeded':
        const paymentIntent = event.data.object;
        console.log(`PaymentIntent for ${paymentIntent.amount} was successful! from webhook!`);
        console.log("payment intent userID: ", paymentIntent.metadata);
        console.log("payment intent userID: ", paymentIntent.metadata.userID);
        // Then define and call a method to handle the successful payment intent.
        handlePaymentIntentSucceeded(paymentIntent);
        break;
    case 'payment_method.attached':
      const paymentMethod = event.data.object;
      // Then define and call a method to handle the successful attachment of a PaymentMethod.
      // handlePaymentMethodAttached(paymentMethod);
      break;
    default:
      // Unexpected event type
      console.log(`Unhandled event type ${event.type}.`);
  }

  // Return a 200 response to acknowledge receipt of the event
  response.send();
});



async function handlePaymentIntentSucceeded(paymentIntent: any) {
    const userID = paymentIntent.metadata.userID;
    const existingUser = await client.db("cpen321journal").collection("users").findOne({ userID });
    if (existingUser) {
        // User exists, update the provided fields only
        const updatedFields: any = {
            updatedAt: new Date()
        };

        updatedFields.isPaid = true;
        await client.db("cpen321journal").collection("users").updateOne(
            { userID },
            { $set: updatedFields }
        );
        console.log("updated user!")
    }
}

async function scheduleNotifications() {
    // Run every minute
    cron.schedule('* * * * *', async () => {
    // Run at the start of every hour
    // cron.schedule('0 * * * *', async () => {
        console.log('Checking for scheduled notifications...');

        try {
            await client.connect();
            const db = client.db('cpen321journal');
            const usersCollection = db.collection('users');

            const now = DateTime.utc();
            const utcDay = now.weekday; // 1 = Monday, 7 = Sunday
            const utcTime = now.toFormat('HH:mm');

            console.log("Current UTC Day:", utcDay);
            console.log("Current UTC Time:", utcTime);

            const users = await usersCollection.find({}).toArray();

            users.forEach(user => {
                const { reminderSetting, fcmToken, userID } = user;
                console.log("Stored Reminder:", reminderSetting);

                const weekdays = reminderSetting?.Weekday || [];
                const reminderTime = reminderSetting?.time || null;

                if (
                    Array.isArray(weekdays) &&
                    reminderSetting &&
                    reminderSetting.Weekday.includes(utcDay) && // Check UTC weekday
                    reminderSetting.time === utcTime // Check UTC time
                ) {
                    console.log("Reminder matched for user:", userID);

                    const message = {
                        data: {
                            title: 'Journal Reminder',
                            body: "It's time to write your journal entry!",
                            reminderTime: reminderSetting.time,
                            reminderDays: JSON.stringify(reminderSetting.Weekday)
                        },
                        token: fcmToken
                    };

                    admin.messaging().send(message)
                        .then(response => console.log(`Notification sent to ${userID}:`, response))
                        .catch(error => console.error(`Error sending notification to ${userID}:`, error));
                }
            });
        } catch (error) {
            console.error('Error checking notifications:', error);
        }
    });

}

// Initialize Cron Jobs
scheduleNotifications();


export default app;