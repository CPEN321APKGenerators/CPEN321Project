import cron from 'node-cron';
import admin from 'firebase-admin';
import { MongoClient } from 'mongodb';
import dotenv from 'dotenv';

dotenv.config();

// Initialize Firebase Admin
if (!admin.apps.length) {
  const serviceAccount = require('../config/cpen321project-c324e-firebase-adminsdk');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

// MongoDB Setup
const client = new MongoClient(process.env.MONGODB_URI as string);
const dbName = 'cpen321journal';
const collectionName = 'users';

async function scheduleNotifications() {
  // Run every minute
  cron.schedule('* * * * *', async () => {
    console.log('Checking for scheduled notifications...');

    try {
      await client.connect();
      const db = client.db(dbName);
      const usersCollection = db.collection(collectionName);

      const today = new Date();
      const day = today.getDay(); // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
      const currentTime = today.toTimeString().substring(0, 5);

      const users = await usersCollection.find({}).toArray();

      users.forEach(user => {
        const { reminderSetting, fcmToken, userID } = user;

        if (
          reminderSetting &&
          reminderSetting.Weekday.includes(day) &&
          reminderSetting.time === currentTime
        ) {
          const message = {
            notification: {
              title: 'Journal Reminder',
              body: "It's time to write your journal entry!"
            },
            token: fcmToken,
            data: {
              reminderTime: reminderSetting.time,
              reminderDays: JSON.stringify(reminderSetting.Weekday)
            }
          };

          admin
            .messaging()
            .send(message)
            .then(response =>
              console.log(`Notification sent to ${userID}:`, response)
            )
            .catch(error =>
              console.error(`Error sending notification to ${userID}:`, error)
            );
        }
      });
    } catch (error) {
      console.error('Error checking notifications:', error);
    }
  });
}

export default scheduleNotifications;
