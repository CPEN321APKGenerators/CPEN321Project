import { body, query } from "express-validator";
import { UserController } from "../controllers/UserController";

const controller = new UserController();

export const UserRoutes = [
    {
        method: "get",
        route: "/api/profile",
        action: controller.getUserProfile,
        validation: [
            query("userID").exists().isString()
        ],
        middlewares: []
    },
    {
        method: "post",
        route: "/api/profile",
        action: controller.createOrUpdateUserProfile,
        validation: [
            body("userID").exists().isString(),
            body("googleToken").exists()
        ],
        middlewares: []
    },
    {
        method: "get",
        route: "/api/profile/isPaid",
        action: controller.isUserPaid,
        validation: [
            query("userID").exists().isString()
        ],
        middlewares: []
    },
    {
        method: "post",
        route: "/api/profile/fcmtoken",
        action: controller.storeFcmToken,
        validation: [
            body("userID").exists().isString(),
            body("fcmToken").exists()
        ],
        middlewares: []
    },
    {
        method: "post",
        route: "/api/profile/reminder",
        action: controller.changeReminder,
        validation: [
            body("userID").exists().isString(),
            body("updated_reminder").exists()
        ],
        middlewares: []
    }
];
