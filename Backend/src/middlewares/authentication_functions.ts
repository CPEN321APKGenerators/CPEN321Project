import axios from 'axios';
import { Request, Response, NextFunction } from 'express';
import { client } from "../../services";

async function getGoogleNumID(userID: string): Promise<string | null> {
    console.log("\n Checking MongoDB for userID:", `"${userID}"`, "Type:", typeof userID);

    const user = await client.db("cpen321journal").collection("users").findOne({});
    console.log(" MongoDB user data:", user, "Type of stored userID:", typeof user?.userID);

    const query = { userID: String(userID).trim() }; // 🔹 Ensure it’s always a string
    console.log("Query being used:", query);

    const matchedUser = await client.db("cpen321journal").collection("users").findOne(query);
    console.log("User Retrieved in getGoogleNumID:", matchedUser);

    return matchedUser ? matchedUser.googleNumID : null;
}

// Middleware to verify Google Token and googleNumID
export async function verifyGoogleToken(req: Request, res: Response, next: NextFunction) {
    const googleToken = req.headers.authorization?.split(' ')[1];
    var googleNumID = req.body.googleNumID || req.query.googleNumID;
    const userID = req.body.userID || req.query.userID;

    if (!googleToken) {
        return res.status(400).json({ message: "Missing googleToken" });
    }

    if (!googleNumID) {
        googleNumID = await getGoogleNumID(userID); 
    }

    try {
        const response = await axios.get(`https://oauth2.googleapis.com/tokeninfo?id_token=${googleToken}`);
        const verifiedGoogleNumID = response.data.sub;
        
        if (verifiedGoogleNumID !== googleNumID) {
            return res.status(403).json({ message: "Unauthorized: googleNumID does not match token" + verifiedGoogleNumID + ";" + googleNumID });
        }

        next();
    } catch (error) {
        return res.status(403).json({ message: "Invalid Google token when authenticating" });
    }
}
