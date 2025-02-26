import { NextFunction, Request, Response } from "express";
import { client } from "../../services";
import { Parser } from "json2csv";
import fs from "fs";
import path from "path";
import axios from 'axios';
import { PDFDocument, StandardFonts, rgb } from 'pdf-lib';
import { v4 as uuidv4 } from 'uuid';


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
        
        // Ensure strings are used for date and userID
        const entry = await client.db("cpen321journal").collection("journals")
            .findOne({ date: date as string, userID: userID as string });

        res.status(200).json({
            journal: entry ? { 
                text: entry.text, 
                media: entry.media // Return Base64 images
            } : { text: "", media: [] }
        });
    }

    

    async putJournal(req: Request, res: Response, next: NextFunction) {
        const { date, userID, text, media } = req.body;
        
        const result = await client.db("cpen321journal").collection("journals")
            .updateOne(
                { date, userID },
                { $set: { text: text, media: media || [] } }
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

    async postJournalMedia(req: Request, res: Response, next: NextFunction) {
        const { date, userID, media } = req.body;
    
        if (!media || !Array.isArray(media) || media.length === 0) {
            return res.status(400).json({ message: "No media provided" });
        }
    
        // Validate that each media item is in Base64 format
        for (const item of media) {
            if (!isValidBase64(item)) {
                return res.status(400).json({ message: "Invalid media format" });
            }
        }
    
        // Check if an entry already exists
        const existing = await client.db("cpen321journal").collection("journals")
            .findOne({ date, userID });
    
        if (existing) {
            // If entry exists, merge new media with existing media
            const updatedMedia = [...existing.media, ...media];
            
            await client.db("cpen321journal").collection("journals")
                .updateOne(
                    { date, userID },
                    { $set: { media: updatedMedia } }
                );
        } else {
            // If no entry exists, create a new one
            await client.db("cpen321journal").collection("journals")
                .insertOne({
                    date,
                    userID,
                    text: "",
                    media: media,
                    createdAt: new Date()
                });
        }
    
        res.status(201).json({ success: true });
    }
    

    async deleteJournalMedia(req: Request, res: Response, next: NextFunction) {
        const { date, userID, media } = req.query;
    
        if (!media || typeof media !== "string") {
            return res.status(400).json({ message: "Invalid or no media specified for deletion" });
        }
    
        // Retrieve the journal entry
        const entry = await client.db("cpen321journal").collection("journals")
            .findOne({ date, userID });
    
        if (!entry) {
            return res.status(404).json({ message: "Journal entry not found" });
        }
    
        // Filter out the specified media
        const updatedMedia = entry.media.filter((item: string) => item !== media);
    
        // Update the document with the new media array
        const result = await client.db("cpen321journal").collection("journals")
            .updateOne(
                { date, userID },
                { $set: { media: updatedMedia } }
            );
    
        res.status(200).json({ delete_success: result.modifiedCount > 0 });
    }
    
    

    async getJournalMedia(req: Request, res: Response, next: NextFunction) {
        const { date, userID } = req.query;
    
        // Check if the journal entry exists
        const entry = await client.db("cpen321journal").collection("journals")
            .findOne({ date, userID });
    
        if (!entry) {
            return res.status(404).json({ message: "Journal entry not found" });
        }
    
        res.status(200).json({ media: entry.media || [] });
    }    

    async getJournalFile(req: Request, res: Response, next: NextFunction) {
        const { userID, format, googleToken } = req.query;
    
        // 1. Validate Format
        if (!['pdf', 'csv'].includes(format as string)) {
            return res.status(400).json({ message: "Invalid format. Only 'pdf' or 'csv' are accepted." });
        }
    
        // 2. Verify Google Token
        try {
            const response = await axios.get(`https://oauth2.googleapis.com/tokeninfo?id_token=${googleToken}`);
            const googleUserID = response.data.sub;
            console.log(googleUserID)
            
            // if (googleUserID !== userID) {
            //     return res.status(403).json({ message: "Unauthorized access." });
            // }
        } catch (error) {
            return res.status(403).json({ message: "Invalid Google token." });
        }
    
        // 3. Fetch All Journal Entries for the User
        const journals = await client.db("cpen321journal").collection("journals")
            .find({ userID }).toArray();
    
        if (!journals || journals.length === 0) {
            return res.status(404).json({ message: "No journal entries found for this user." });
        }
    
        // 4. Generate File Based on Format
        const filename = `${uuidv4()}.${format}`;
        const filePath = path.join(__dirname, `../../public/${filename}`);
    
        if (format === 'csv') {
            // Generate CSV
            const json2csvParser = new Parser({ fields: ['date', 'text', 'media'] });
            const csv = json2csvParser.parse(journals);
            fs.writeFileSync(filePath, csv);
    
        } else if (format === 'pdf') {
            const pdfDoc = await PDFDocument.create();
            const timesRomanFont = await pdfDoc.embedFont(StandardFonts.TimesRoman);
        
            for (const entry of journals) {
                const page = pdfDoc.addPage();
                const { width, height } = page.getSize();
        
                // Add Date and Text
                page.drawText(`Date: ${entry.date}`, {
                    x: 50,
                    y: height - 50,
                    size: 20,
                    font: timesRomanFont,
                    color: rgb(0, 0, 0)
                });
        
                page.drawText(`Text: ${entry.text}`, {
                    x: 50,
                    y: height - 100,
                    size: 15,
                    font: timesRomanFont,
                    color: rgb(0, 0, 0)
                });
        
                // Embed each image
                let imageY = height - 150;
                for (const [index, mediaItem] of entry.media.entries()) {
                    // Decode Base64
                    const base64Data = mediaItem.split(',')[1];  // Get Base64 data part
                    const imageBuffer = Buffer.from(base64Data, 'base64');
        
                    // Embed the image
                    let embeddedImage;
                    if (mediaItem.startsWith('data:image/png')) {
                        embeddedImage = await pdfDoc.embedPng(imageBuffer);
                    } else if (mediaItem.startsWith('data:image/jpeg') || mediaItem.startsWith('data:image/jpg')) {
                        embeddedImage = await pdfDoc.embedJpg(imageBuffer);
                    }
        
                    if (embeddedImage) {
                        const imageDims = embeddedImage.scale(0.25);
        
                        // Draw the image on the page
                        page.drawImage(embeddedImage, {
                            x: 50,
                            y: imageY - imageDims.height,
                            width: imageDims.width,
                            height: imageDims.height
                        });
        
                        // Adjust Y position for next image
                        imageY -= imageDims.height + 20;
                    } else {
                        // If image is not supported, print text placeholder
                        page.drawText(`Media ${index + 1}: [Unsupported Image Format]`, {
                            x: 50,
                            y: imageY,
                            size: 12,
                            font: timesRomanFont,
                            color: rgb(1, 0, 0)
                        });
                        imageY -= 20;
                    }
                }
            }
        
            const pdfBytes = await pdfDoc.save();
            fs.writeFileSync(filePath, pdfBytes);
        }
        // 5. Return Download URL
        const downloadURL = `${req.protocol}://${req.get('host')}/public/${filename}`;
        res.status(200).json({ filename, downloadURL });
        
    }    

    
}


