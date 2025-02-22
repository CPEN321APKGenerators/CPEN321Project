import { NextFunction, Request, Response } from "express";
import { client } from "../services";
import { ObjectId } from "mongodb";
import admin from "firebase-admin";

// Initialize Firebase Admin SDK (Ensure serviceAccountKey.json is properly configured)
if (!admin.apps.length) {
    const serviceAccount = require("../config/cpen321project-c324e-firebase-adminsdk")
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
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
            const result = await client.db("cpen321journal").collection("users").updateOne(
                { userID },
                { $set: { reminderSetting: updated_reminder, updatedAt: new Date() } },
                { upsert: true }
            );

            if (result.acknowledged) {
                // Send Push Notification using FCM
                // await this.sendReminderNotification(userID, updated_reminder);
            
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
        const { userID, fcmToken } = req.body;

        if (!userID || !fcmToken) {
            return res.status(400).json({ error: "userID and fcmToken are required" });
        }

        try {
            const result = await client.db("cpen321journal").collection("users").updateOne(
                { userID },
                { $set: { fcmToken, updatedAt: new Date() } },
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