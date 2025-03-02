import express from "express";
import dotenv from "dotenv";
import Database from "./config/database";

dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;
const MONGO_URI = process.env.MONGO_URI || "mongodb://localhost:27017";
const DB_NAME = process.env.DB_NAME || "cpen-321-journal";
const OPEN_AI_API_KEY = process.env.OPEN_AI_API_KEY || "";

const dbInstance = new Database(MONGO_URI, DB_NAME);

// Middleware
app.use(express.json());

// Sample Route
app.get("/", (req, res) => {
  res.send("API is running...");
});

// Start Server
app.listen(PORT, async () => {
    dbInstance.status().then((status: { error: any; url?: string; db?: string }) => console.log("DB Status:", status));
  console.log(`✅ Server running on port ${PORT}`);
});
