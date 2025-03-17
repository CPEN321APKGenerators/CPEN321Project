import request from "supertest";
import app from "../../index"; // Adjust based on your app entry point
import { jest } from "@jest/globals";
import { JournalController } from "../../src/controllers/JournalController";
import { MongoClient, Db, Collection, Document, BulkWriteResult } from "mongodb";
import { client } from "../../services";

const mockFindOne = jest.fn();

jest.mock("../../services", () => ({
    client: {
        db: jest.fn(() => ({
            collection: jest.fn(() => ({
                findOne: mockFindOne,
            })),
        })),
    },
}));

describe("Analytics API - Mocked", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it("should return an error when database query fails", async () => {
        mockFindOne.mockRejectedValue(new Error("Database error") as never);

        const response = await request(app)
            .get("/api/analytics")
            .query({ userID: "user_123", date: new Date().toISOString() });

        expect(response.status).toBe(500);
        expect(response.body.error).toBe("Internal server error");
    });

    it("should return 404 when user is not found", async () => {
        mockFindOne.mockResolvedValue(null as never);

        const response = await request(app)
            .get("/api/analytics")
            .query({ userID: "user_123", date: new Date().toISOString() });

        expect(response.status).toBe(404);
        expect(response.body.error).toBe("User not found");
    });

    it("should return unauthorized when no user is provided", async () => {
        const response = await request(app)
            .get("/api/journal");

        expect(response.status).toBe(400);
    });

    it("should return an error if date is not valid", async () => {

        const response = await request(app)
            .get("/api/analytics")
            .query({ userID: "user_123", date: "Not Valid Date" });

        expect(response.status).toBe(400);
        expect(response.body.error).toBe("Invalid date format");
    });
});