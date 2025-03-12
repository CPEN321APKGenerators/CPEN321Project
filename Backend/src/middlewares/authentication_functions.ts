import axios from 'axios';
import { Request, Response, NextFunction } from 'express';

// Middleware to verify Google Token and googleNumID
export async function verifyGoogleToken(req: Request, res: Response, next: NextFunction) {
    const googleToken = req.headers.authorization?.split(' ')[1];
    const googleNumID = req.body.googleNumID || req.query.googleNumID;

    if (!googleToken || !googleNumID) {
        return res.status(400).json({ message: "Missing googleToken or googleNumID" });
    }

    try {
        const response = await axios.get(`https://oauth2.googleapis.com/tokeninfo?id_token=${googleToken}`);
        const verifiedGoogleNumID = response.data.sub;
        
        if (verifiedGoogleNumID !== googleNumID) {
            return res.status(403).json({ message: "Unauthorized: googleNumID does not match token" });
        }

        next();
    } catch (error) {
        return res.status(403).json({ message: "Invalid Google token when authenticating" });
    }
}
