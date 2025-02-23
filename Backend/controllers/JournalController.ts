import { NextFunction, Request, Response } from "express";
import { client } from "../services";
import { GridFSBucket, ObjectId } from "mongodb";
import multer from "multer";
import { GridFsStorage } from "multer-gridfs-storage";


const isValidBase64 = (str: string) => {
    return /^data:image\/(png|jpeg|jpg);base64,[A-Za-z0-9+/=]+$/.test(str);
};


export class JournalController {
    async postJournal(req: Request, res: Response, next: NextFunction) {
        const { date, userID, text, media } = req.body; // media is an array of Base64 strings
        
        // Check for existing entry
        const existing = await client.db("cpen321journal").collection("journals")
            .findOne({ date, userID });
        
        if (existing) {
            return res.status(400).json({ 
                message: "Journal entry already exists for this date" 
            });
        }
    
        // Create new entry with Base64 images
        const result = await client.db("cpen321journal").collection("journals")
            .insertOne({
                date,
                userID,
                text: text || "",
                media: media || [], // Store Base64 images here
                createdAt: new Date()
            });
    
        res.status(201).json({ 
            message: "New journal entry created successfully with images!" 
        });
    }
    

    async getJournal(req: Request, res: Response, next: NextFunction) {
        const { date, userID } = req.query;
        
        const entry = await client.db("cpen321journal").collection("journals")
            .findOne({ date, userID });
    
        res.status(200).json({
            journal: entry ? { 
                text: entry.text, 
                media: entry.media // Return Base64 images
            } : { text: "", media: [] }
        });
    }
    

    async putJournal(req: Request, res: Response, next: NextFunction) {
        const { date, userID, updated_content, updated_media } = req.body;
        
        const result = await client.db("cpen321journal").collection("journals")
            .updateOne(
                { date, userID },
                { $set: { text: updated_content, media: updated_media || [] } }
            );
    
        res.status(200).json({ 
            update_success: result.modifiedCount > 0 
        });
    }
    

    async deleteJournal(req: Request, res: Response, next: NextFunction) {
        const { date, userID } = req.query;
        
        const result = await client.db("cpen321journal").collection("journals")
            .deleteOne({ date, userID });

        res.status(200).json({ 
            delete_success: result.deletedCount > 0 
        });
    }
}

