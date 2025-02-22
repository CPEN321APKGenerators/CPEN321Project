import { NextFunction, Request, Response } from "express";
import { client } from "../services";
import { ObjectId } from "mongodb";
import admin from "firebase-admin";
import { DateTime } from "luxon";

// Initialize Firebase Admin SDK (Ensure serviceAccountKey.json is properly configured)
if (!admin.apps.length) {
    const serviceAccount = require("../config/cpen321project-c324e-firebase-adminsdk.json")
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}

const getServerOffset = () => {
    const offset = new Date().getTimezoneOffset(); // in minutes
    const absOffset = Math.abs(offset);
    const hours = String(Math.floor(absOffset / 60)).padStart(2, '0');
    const minutes = String(absOffset % 60).padStart(2, '0');
    const sign = offset > 0 ? '-' : '+';
    return `${sign}${hours}:${minutes}`;
};

// Convert User Time and Weekday to UTC
const convertToUtc = (userTime: any, userOffset: any, userWeekdays: any) => {
    // Convert user time to UTC time
    const userDateTime = DateTime.fromFormat(userTime, 'HH:mm', { zone: `UTC${userOffset}` });
    const utcDateTime = userDateTime.toUTC();
    const utcTime = utcDateTime.toFormat('HH:mm');

    // Convert each user weekday to the corresponding UTC weekday
    const utcWeekdays = userWeekdays.map((day:any) => {
        // Create a DateTime object with the user's weekday and time
        const date = DateTime.now().set({ weekday: day, hour: userDateTime.hour, minute: userDateTime.minute }).setZone(`UTC${userOffset}`);
        
        // Convert to UTC and get the weekday
        const utcDay = date.toUTC().weekday;
        
        console.log(`User Weekday: ${day}, UTC Weekday: ${utcDay}`);
        return utcDay;
    });
    return { utcTime, utcWeekdays };
}

export class UserController {
    // Create or Get User Profile
    async createOrGetUserProfile(req: Request, res: Response, next: NextFunction) {
        const { userID, isPaid = false, reminderSetting = {} } = req.body;
        
        if (!userID) {
            return res.status(400).json({ error: "userID is required" });
        }

        try {
            // Check if the user already exists
            const existingUser = await client.db("cpen321journal").collection("users").findOne({ userID });

            if (existingUser) {
                // User exists, return existing profile
                const profile = {
                    isPaid: existingUser.isPaid || false,
                    reminderSetting: existingUser.reminderSetting || {}
                };
                return res.status(200).json(profile);
            } else {
                // User does not exist, create a new profile
                const newUser = {
                    userID,
                    isPaid,
                    reminderSetting,
                    createdAt: new Date(),
                    updatedAt: new Date()
                };

                const result = await client.db("cpen321journal").collection("users").insertOne(newUser);

                if (result.acknowledged) {
                    res.status(201).json(newUser);  // Return the newly created profile
                } else {
                    res.status(500).json({ error: "Failed to create user profile" });
                }
            }
        } catch (err) {
            console.error("Error creating or getting user profile:", err);
            res.status(500).json({ error: "Internal server error" });
        }
    }

    // Check if User is Paid
    async isUserPaid(req: Request, res: Response, next: NextFunction) {
        const { userID } = req.query;

        if (!userID) {
            return res.status(400).json({ error: "userID is required" });
        }

        try {
            // Find user by userID
            const user = await client.db("cpen321journal").collection("users").findOne({ userID });

            if (!user) {
                // User not found
                return res.status(404).json({ error: "User not found" });
            }

            // Check isPaid status
            const isPaid = user.isPaid || false;

            res.status(200).json({ isPaid });
        } catch (err) {
            console.error("Error checking if user is paid:", err);
            res.status(500).json({ error: "Internal server error" });
        }
    }

    // Update Reminder Settings
    async changeReminder(req: Request, res: Response, next: NextFunction) {
        const { updated_reminder, userID } = req.body;

        if (!updated_reminder || !userID) {
            return res.status(400).json({ error: "updated_reminder and userID are required" });
        }

        try {
            // Fetch user timeOffset
            const user = await client.db("cpen321journal").collection("users").findOne({ userID });

            if (!user) {
                return res.status(404).json({ error: "User not found" });
            }

            const userOffset = user.timeOffset;
            const userTime = updated_reminder.time;
            const userWeekdays = updated_reminder.Weekday;

            // Convert reminder time and weekdays to UTC
            const { utcTime, utcWeekdays } = convertToUtc(userTime, userOffset, userWeekdays);

            // Store converted UTC time and weekdays
            updated_reminder.time = utcTime;
            updated_reminder.Weekday = utcWeekdays;

            console.log("Reminder stored in UTC:", updated_reminder);

            // Update reminder settings
            const result = await client.db("cpen321journal").collection("users").updateOne(
                { userID },
                { $set: { reminderSetting: updated_reminder, updatedAt: new Date() } },
                { upsert: true }
            );

            if (result.acknowledged) {
                res.status(200).json({ update_success: true });
            } else {
                res.status(500).json({ update_success: false });
            }
        } catch (err) {
            console.error("Error updating reminder:", err);
            res.status(500).json({ update_success: false });
        }
    }




    // Send Reminder Notification via Firebase Cloud Messaging (FCM)
    // async sendReminderNotification(userID: string, reminderSetting: any) {
    //     try {
    //         const user = await client.db("cpen321journal").collection("users").findOne({ userID });

    //         if (!user || !user.fcmToken) {
    //             console.log("FCM Token not found for user:", userID);
    //             return;
    //         }

    //         const message = {
    //             notification: {
    //                 title: "Journal Reminder",
    //                 body: "It's time to write your journal entry!"
    //             },
    //             token: user.fcmToken,
    //             data: {
    //                 reminderTime: reminderSetting.time,
    //                 reminderDays: JSON.stringify(reminderSetting.Weekday)
    //             }
    //         };

    //         const response = await admin.messaging().send(message);
    //         console.log("Successfully sent notification:", response);
    //     } catch (err) {
    //         console.error("Error sending notification:", err);
    //     }
    // }


    // Store FCM Token
    async storeFcmToken(req: Request, res: Response, next: NextFunction) {
        const { userID, fcmToken, timeOffset } = req.body;

        if (!userID || !fcmToken || !timeOffset) {
            return res.status(400).json({ error: "userID, fcmToken, timeOffset are required" });
        }

        try {
            const result = await client.db("cpen321journal").collection("users").updateOne(
                { userID },
                { $set: { fcmToken, timeOffset, updatedAt: new Date() } },
                { upsert: true }
            );

            if (result.acknowledged) {
                res.status(200).json({ success: true });
            } else {
                res.status(500).json({ success: false });
            }
        } catch (err) {
            console.error("Error storing FCM Token:", err);
            res.status(500).json({ success: false });
        }
    }


}

