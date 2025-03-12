import { NextFunction, Request, Response } from "express";
import { client } from "../../services";
import { ObjectId } from "mongodb";
import admin from "firebase-admin";
import { DateTime } from "luxon";
import axios from 'axios';

// Initialize Firebase Admin SDK (Ensure serviceAccountKey.json is properly configured)
if (!admin.apps.length) {
    try {
        let serviceAccount;

        // Check if the environment variable is set (used for GitHub Actions & testing)
        if (process.env.FIREBASE_CONFIG) {
            console.log("Using Firebase config from environment variable");
            serviceAccount = JSON.parse(process.env.FIREBASE_CONFIG);
        } else {
            console.log("Using Firebase config from JSON file");
            serviceAccount = require("../config/cpen321project-c324e-firebase-adminsdk.json");
        }

        // Initialize Firebase
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
        });

        console.log("Firebase Admin Initialized");

    } catch (error) {
        console.error("Failed to initialize Firebase:", error);
    }
}


const getServerOffset = () => {
    const offset = new Date().getTimezoneOffset(); // in minutes
    const absOffset = Math.abs(offset);
    const hours = String(Math.floor(absOffset / 60)).padStart(2, '0');
    const minutes = String(absOffset % 60).padStart(2, '0');
    const sign = offset > 0 ? '-' : '+';
    return `${sign}${hours}:${minutes}`;
};

const convertToUtc = (userTime: any, userOffset: any, userWeekdays: any) => {
    const utcWeekdays = userWeekdays.map((day: any) => {
        // Get the current date and time in the user's timezone
        let userDateTime = DateTime.now()
            .setZone(`UTC${userOffset}`)
            .set({ weekday: day, hour: parseInt(userTime.split(':')[0]), minute: parseInt(userTime.split(':')[1]), second: 0, millisecond: 0 });
        
        // Adjust for past time today
        if (userDateTime < DateTime.now().setZone(`UTC${userOffset}`)) {
            userDateTime = userDateTime.plus({ days: 7 });
        }
        
        // Convert to UTC
        const utcDateTime = userDateTime.toUTC();
        const utcTime = utcDateTime.toFormat('HH:mm');
        let utcDay = utcDateTime.weekday;

        // Check if the day changed after conversion
        if (utcDateTime < userDateTime) {
            utcDay = (utcDay % 7) + 1;
        }

        console.log(`User Weekday: ${day}, UTC Weekday: ${utcDay}, User Time: ${userTime}, UTC Time: ${utcTime}`);
        return utcDay;
    });

    // Convert the time for storage (all weekdays share the same time)
    const date = DateTime.now().setZone(`UTC${userOffset}`);
    const userDateTime = date.set({ hour: parseInt(userTime.split(':')[0]), minute: parseInt(userTime.split(':')[1]) });
    const utcDateTime = userDateTime.toUTC();
    const utcTime = utcDateTime.toFormat('HH:mm');

    return { utcTime, utcWeekdays };
};


export class UserController {
    async getUserProfile(req: Request, res: Response, next: NextFunction) {
        const { userID } = req.query;
    
        if (!userID) {
            return res.status(400).json({ error: "userID is required" });
        }
    
        try {
            // Check if the user exists
            const user = await client.db("cpen321journal").collection("users").findOne({ userID });
    
            if (!user) {
                return res.status(404).json({ error: "User not found" });
            }
    
            // Return the user profile
            const profile = {
                isPaid: user.isPaid || false,
                reminderSetting: user.reminderSetting || {},
                preferred_name: user.preferred_name || "",
                activities_tracking: user.activities_tracking || [],
                userReminderTime: user.userReminderTime || [],
                createdAt: user.createdAt || "",
                fcmToken: user.fcmToken || "",
                timeOffset: user.timeOffset || "",
                googleNumID: user.googleNumID || ""
            };
    
            return res.status(200).json(profile);
    
        } catch (err) {
            console.error("Error getting user profile:", err);
            res.status(500).json({ error: "Internal server error" });
        }
    }
    

    async createOrUpdateUserProfile(req: Request, res: Response, next: NextFunction) {
        const { 
            userID, 
            isPaid, 
            preferred_name, 
            activities_tracking,
            googleToken
        } = req.body;
    
        var verifiedGoogleNumID;
    
        // Check Required Fields
        if (!userID) {
            return res.status(400).json({ error: "userID is required" });
        }
    
        // if (!googleToken) {
        //     return res.status(400).json({ message: "Missing googleToken" });
        // }
    
        // Verify Google Token
        try {
            const response = await axios.get(`https://oauth2.googleapis.com/tokeninfo?id_token=${googleToken}`);
            verifiedGoogleNumID = response.data.sub;
            console.log("Verified Google NumID: ", verifiedGoogleNumID);
        } catch (error) {
            return res.status(403).json({ message: "Invalid Google token" });
        }
    
        // Input Validation
        if (preferred_name && typeof preferred_name !== 'string') {
            return res.status(400).json({ error: "preferred_name must be a string" });
        }
    
        // Validate activities_tracking
        if (activities_tracking) {
            if (!Array.isArray(activities_tracking)) {
                return res.status(400).json({ error: "activities_tracking must be an array" });
            }
    
            // Check each activity object in the array
            for (const activity of activities_tracking) {
                if (
                    typeof activity !== 'object' ||
                    typeof activity.name !== 'string' ||
                    typeof activity.averageValue !== 'number' ||
                    !['Hours', 'Minutes', 'Times'].includes(activity.unit)
                ) {
                    return res.status(400).json({ 
                        error: "Each activity must be an object with 'name' (string), 'averageValue' (number), and 'unit' (Hours, Minutes, or Times)" 
                    });
                }
            }
        }
    
        try {
            // Check if the user already exists
            const existingUser = await client.db("cpen321journal").collection("users").findOne({ userID });
    
            if (existingUser) {
                // User exists, update the provided fields only
                const updatedFields: any = {
                    updatedAt: new Date()
                };

                if (verifiedGoogleNumID) updatedFields.googleNumID = verifiedGoogleNumID;
    
                if (isPaid !== undefined) updatedFields.isPaid = isPaid;
                if (preferred_name !== undefined) updatedFields.preferred_name = preferred_name;
                if (activities_tracking !== undefined) updatedFields.activities_tracking = activities_tracking;
    
                await client.db("cpen321journal").collection("users").updateOne(
                    { userID },
                    { $set: updatedFields }
                );
    
                // Return the updated profile
                return res.status(200).json({
                    message: "User profile updated successfully",
                    updatedFields
                });
    
            } else {
                // User does not exist, create a new profile
                const newUser = {
                    userID,
                    isPaid: isPaid || false,
                    reminderSetting: {"Weekday":[], "time":"9:00"},
                    preferred_name: preferred_name || "",
                    activities_tracking: activities_tracking || [],
                    createdAt: new Date(),
                    updatedAt: new Date(),
                    googleNumID: verifiedGoogleNumID   // Set googleNumID when creating a new user
                };
    
                const result = await client.db("cpen321journal").collection("users").insertOne(newUser);
    
                if (result.acknowledged) {
                    res.status(200).json(newUser);  // Return the newly created profile
                } else {
                    res.status(500).json({ error: "Failed to create user profile" });
                }
            }
        } catch (err) {
            console.error("Error creating or updating user profile:", err);
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

    async upgradeUser(req: Request, res: Response, next: NextFunction) {
        const {userID} = req.body;
    
        if (!userID) {
            return res.status(400).json({ error: "userID is required" });
        }
        try {
            // Check if the user already exists
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
    
                // Return the updated profile
                return res.status(200).json({
                    message: "User profile updated successfully",
                    updatedFields
                });
    
            } else {
                console.log(console.log("user id not found: ", userID));
                res.status(404).json({ error: "User does not exist."});
            }
        } catch (err) {
            console.error("Error upgrading user:", err);
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

            // Store the user's original time and weekdays
            const userReminderTime = {
                Weekday: userWeekdays,
                time: userTime
            };

            // Convert reminder time and weekdays to UTC
            const { utcTime, utcWeekdays } = convertToUtc(userTime, userOffset, userWeekdays);

            // Store converted UTC time and weekdays
            updated_reminder.time = utcTime;
            updated_reminder.Weekday = utcWeekdays;

            console.log("User Reminder Time:", userReminderTime);
            console.log("Reminder stored in UTC:", updated_reminder);

            // Update reminder settings in the database
            const result = await client.db("cpen321journal").collection("users").updateOne(
                { userID },
                {
                    $set: {
                        reminderSetting: updated_reminder,      // Store converted UTC time and weekdays
                        userReminderTime: userReminderTime,   // Store original user input
                        updatedAt: new Date()
                    }
                },
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

