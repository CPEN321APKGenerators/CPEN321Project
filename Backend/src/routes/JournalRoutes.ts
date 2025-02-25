import { body, query } from "express-validator";
import { JournalController } from "../controllers/JournalController";

const controller = new JournalController();

export const JournalRoutes = [
    {
        method: "post",
        route: "/api/journal",
        action: controller.postJournal,
        validation: [
            body("date").exists().isISO8601(),
            body("userID").exists().isString()
        ]
    },
    {
        method: "get",
        route: "/api/journal",
        action: controller.getJournal,
        validation: [
            query("date").exists().isISO8601(),
            query("userID").exists().isString()
        ]
    },
    {
        method: "put",
        route: "/api/journal",
        action: controller.putJournal,
        validation: [
            body("date").exists().isISO8601(),
            body("userID").exists().isString(),
            body("text").exists().isString()
        ]
    },
    {
        method: "delete",
        route: "/api/journal",
        action: controller.deleteJournal,
        validation: [
            query("date").exists().isISO8601(),
            query("userID").exists().isString()
        ]
    }
];