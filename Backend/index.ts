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
const stripeSecret = (() => {
    try {
      return fs.readFileSync(path.join(__dirname, './src/config/cpen321project-stripe-secret.txt'), 'utf8').trim();
    } catch (error) {
      console.warn("Stripe secret file not found, falling back to environment variable.");
      return process.env.STRIPE_SECRET || "";
    }
})();

if (!stripeSecret) {
    throw new Error("Missing Stripe Secret Key!");
}

console.log(stripeSecret);
// const OtherRoutes=[]
const Routes = [...AnalysisRoutes, ...JournalRoutes, ...UserRoutes];
// const Routes = [...JournalRoutes];

Routes.forEach((route) => {
    // Middleware to check for validation errors
    const checkValidationErrors = (req: Request, res: Response, next: NextFunction) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({ errors: errors.array() });
        }
        next();
    };

    // Middleware to handle the controller action and errors
    const handleController = async (req: Request, res: Response, next: NextFunction) => {
        try {
            await route.action(req, res, next);
        } catch (err) {
            console.error(err);
            res.sendStatus(500);
        }
    };

    // Apply the route with all necessary middleware
    (app as any)[route.method](
        route.route,
        [...route.validation], // Express-validator checks
        checkValidationErrors, // Check for validation errors
        ...route.middlewares, // Custom middlewares (e.g., verifyGoogleToken)
        handleController // Controller action
    );
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
        publishableKey: process.env.PUBLISHABLE_STRIPE_KEY,
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

app.post('/webhook', express.raw({ type: 'application/json' }), async (request, response) => {
    let event = request.body;
    console.log("webhook event: ", event);

    try {
        switch (event.type) {
            case 'payment_intent.succeeded':
                const paymentIntent = event.data.object;
                console.log(`PaymentIntent for ${paymentIntent.amount} was successful!`);
                await handlePaymentIntentSucceeded(paymentIntent);  // Await function
                break;
            default:
                console.log(`Unhandled event type ${event.type}.`);
        }

        response.sendStatus(200);
    } catch (error) {
        if (error instanceof Error)
            console.error("Webhook error:", error.message);
        response.sendStatus(500);  // Ensure a 500 response on failure
    }
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
        try {
            const result = await client.db("cpen321journal").collection("users").updateOne(
                { userID },
                { $set: updatedFields }
            );
        
            if (!result.acknowledged) {
                throw new Error("Failed to update database");
            }
        
            console.log("Updated user!");
        } catch (error) {
            if (error instanceof Error) {
                console.error("Database update failed:", error.message);
            }
            throw error;
        }        
    }
}

let cronJob: any;  // Add a reference to the cron job

export async function scheduleNotifications() {
    cronJob = cron.schedule('* * * * *', async () => {
        console.log('Checking for scheduled notifications...');
        try {
            await client.connect();
            const db = client.db('cpen321journal');
            const usersCollection = db.collection('users');

            const now = DateTime.utc();
            const utcDay = now.weekday;
            const utcTime = now.toFormat('HH:mm');

            console.log("Current UTC Day:", utcDay);
            console.log("Current UTC Time:", utcTime);

            const users = await usersCollection.find({}).toArray();

            users.forEach(user => {
                const { reminderSetting, fcmToken, userID } = user;
                console.log("Stored Reminder:", reminderSetting);

                if (
                    reminderSetting &&
                    reminderSetting.Weekday.includes(utcDay) && 
                    reminderSetting.time === utcTime
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

// Stop the cron job when tests are done
export function stopCronJob() {
    if (cronJob) cronJob.stop();
}

// Initialize Cron Jobs
if (process.env.NODE_ENV !== "test") {
    scheduleNotifications();
}


export default app;