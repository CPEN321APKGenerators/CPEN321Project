import { MongoClient } from "mongodb";

const dbUri = process.env.DB_URI;
if (!dbUri) {
    throw new Error("DB_URI environment variable is not set");
}

export const client = new MongoClient(dbUri);