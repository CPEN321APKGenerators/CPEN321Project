import request from "supertest";
import app from "../index"; // Adjust based on your app entry point
import { jest } from "@jest/globals";
import { JournalController } from "../src/controllers/JournalController";
import { MongoClient, Db, Collection, Document, BulkWriteResult } from "mongodb";
import { client } from "../services";

const mockJournal = {
    id: "12345",
    date: "2025-03-11",
    userID: "llcce44@gmail.com",
    content: "Today was a good day.",
};

jest.mock("../services", () => ({
    client: {
        db: jest.fn(() => ({
            collection: jest.fn(() => ({
                findOne: jest.fn().mockRejectedValue(new Error("Database error") as never), // Mock error for findOne
                insertOne: jest.fn().mockRejectedValue(new Error("Database insert failed") as never), // Mock error for insertOne
                updateOne: jest.fn().mockRejectedValue(new Error("Database update failed") as never), // Mock error for updateOne
            })),
        })),
    },
}));

// Mocked Tests (Database/API Failures)
describe("Journal API - Mocked", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it("should return an error when database query fails", async () => {
        const response = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer test_token")
            .send(mockJournal);

        expect(response.status).toBe(500);
    });

    it("should return unauthorized when no token is provided", async () => {
        const response = await request(app)
            .post("/api/journal")
            .send(mockJournal);

        expect(response.status).toBe(400);
    });
});
