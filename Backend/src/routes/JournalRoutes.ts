import { body, query, header } from "express-validator";
import { JournalController } from "../controllers/JournalController";
import { verifyGoogleToken } from "../middlewares/authentication_functions";

const controller = new JournalController();

export const JournalRoutes = [
    {
        method: "post",
        route: "/api/journal",
        action: controller.postJournal,
        validation: [
            body("date").exists().isISO8601(),
            body("userID").exists().isString(),
            header("authorization").exists().withMessage("Authorization header is required")
        ],
        middlewares: [verifyGoogleToken]
    },
    {
        method: "get",
        route: "/api/journal",
        action: controller.getJournal,
        validation: [
            query("date").exists().isISO8601(),
            query("userID").exists().isString(),
            header("authorization").exists().withMessage("Authorization header is required")
        ],
        middlewares: [verifyGoogleToken]
    },
    {
        method: "put",
        route: "/api/journal",
        action: controller.putJournal,
        validation: [
            body("date").exists().isISO8601(),
            body("userID").exists().isString(),
            body("text").exists().isString(),
            header("authorization").exists().withMessage("Authorization header is required")
        ],
        middlewares: [verifyGoogleToken]
    },
    {
        method: "delete",
        route: "/api/journal",
        action: controller.deleteJournal,
        validation: [
            query("date").exists().isISO8601(),
            query("userID").exists().isString(),
            header("authorization").exists().withMessage("Authorization header is required")
        ],
        middlewares: [verifyGoogleToken]
    },
    {
        method: "get",
        route: "/api/journal/file",
        action: controller.getJournalFile,
        validation: [
            query("userID").exists().isString(),
            query("format").exists(),
            header("authorization").exists().withMessage("Authorization header is required")
        ],
        middlewares: [verifyGoogleToken]
    }
];