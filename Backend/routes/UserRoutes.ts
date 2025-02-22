import { body, query } from "express-validator";
import { UserController } from "../controllers/UserController";

const controller = new UserController();

export const UserRoutes = [
    {
        method: "post",
        route: "/api/profile",
        action: controller.createOrGetUserProfile,
        validation: [
            body("userID").exists().isString()
        ]
    },
    {
        method: "get",
        route: "/api/profile/isPaid",
        action: controller.isUserPaid,
        validation: [
            query("userID").exists().isString()
        ]
    },
    {
        method: "post",
        route: "/api/profile/fcmtoken",
        action: controller.storeFcmToken,
        validation: [
            body("userID").exists().isString(),
            body("fcmToken").exists()
        ]
    },
    {
        method: "post",
        route: "/api/profile/reminder",
        action: controller.changeReminder,
        validation: [
            body("userID").exists().isString(),
            body("updated_reminder").exists()
        ]
    }
];
