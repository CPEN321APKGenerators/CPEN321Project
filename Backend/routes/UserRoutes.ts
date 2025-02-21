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
    // },
    // {
    //     method: "post",
    //     route: "/api/profile/reminder",
    //     action: controller.changeReminder,
    //     validation: [
    //         body("userID").exists().isString()
    //         // body("status").optional().isIn(["active", "inactive"]),
    //         // body("reminders.enabled").optional().isBoolean(),
    //         // body("reminders.time").optional().matches(/^\d{2}:\d{2}$/),
    //         // body("reminders.frequency").optional().isIn(["daily", "weekly"])
    //     ]
    }
];
