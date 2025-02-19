import { NextFunction, Request, Response } from "express";
import { client } from "../services";
import { ObjectId } from "mongodb";

export class JournalController {
    async postJournal(req: Request, res: Response, next: NextFunction) {
        const { date, userID } = req.body;
        
        // Check for existing entry
        const existing = await client.db("cpen321journal").collection("journals")
            .findOne({ date, userID });
        
        if (existing) {
            return res.status(400).json({ 
                message: "Journal entry already exists for this date" 
            });
        }

        // Create new entry
        const result = await client.db("cpen321journal").collection("journals")
            .insertOne({
                date,
                userID,
                text: "",
                media: [],
                createdAt: new Date()
            });

        res.status(201).json({ 
            // TODO: replace this message to chatbot msg
            message: "New journal entry created successfully! Start reflecting!" 
        });
    }

    async getJournal(req: Request, res: Response, next: NextFunction) {
        const { date, userID } = req.query;
        
        const entry = await client.db("cpen321journal").collection("journals")
            .findOne({ date, userID });

        res.status(200).json({
            journal: entry ? { 
                text: entry.text, 
                media: entry.media 
            } : { text: "", media: [] }
        });
    }

    async putJournal(req: Request, res: Response, next: NextFunction) {
        const { date, userID, updated_content } = req.body;
        
        const result = await client.db("cpen321journal").collection("journals")
            .updateOne(
                { date, userID },
                { $set: { text: updated_content } }
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