import express, {NextFunction, Request, Response} from "express";
import { MongoClient } from "mongodb";
import { client } from "./services";
import { JournalRoutes } from "./routes/JournalRoutes";
import { validationResult } from "express-validator";
import morgan from "morgan";

const app = express();

// if this middleware is before get, it means it will run before the get request
app.use(express.json())
app.use(morgan('tiny'))

// const OtherRoutes=[]
// const Routes = [...JournalRoutes, ...OtherRoutes]

JournalRoutes.forEach((route) => {
    (app as any)[route.method](
        route.route,
        route.validation,
        async (req: Request, res: Response, next: NextFunction) => {
            const errors = validationResult(req);
            if (!errors.isEmpty()) {
                /* If there are validation errors, send a response with the error messages */
                return res.status(400).send({ errors: errors.array() });
            }
            try {
                await route.action(
                    req,
                    res,
                    next,
                );
            } catch (err) {
                console.log(err)
                return res.sendStatus(500); // Don't expose internal server workings
            }
        },
    );
});

// Route to get server IP address
app.get('/server-ip', (req, res) => {
    const serverip = req.socket.localAddress; // Server's IP address
    res.json({ serverIP: serverip });
});

// Route to get server local time
app.get('/server-time', (req, res) => {
    const now = new Date();
    const timeZoneOffset = -now.getTimezoneOffset(); // Offset in minutes

    const hoursOffset = Math.floor(timeZoneOffset / 60);
    const minutesOffset = timeZoneOffset % 60;
    const secondsOffset = minutesOffset * 60;

    const offsetString = `GMT${hoursOffset >= 0 ? '+' : ''}${String(hoursOffset).padStart(2, '0')}:${String(secondsOffset).padStart(2, '0')}`;
    const timeString = now.toLocaleTimeString('en-GB', { hour12: false }); // 24-hour format

    const serverStr = `${timeString} ${offsetString}`;
    res.json({ serverTime: serverStr });
});

// Route to get your name
app.get('/name', (req, res) => {
    res.json({ firstName: "FirstName", lastName: "Lastname" });
});
    
client.connect().then( () => {
    console.log("MongoDB Client connected")

    app.listen(process.env.PORT, () => {
        console.log("Listening on port " + process.env.PORT)
    })
}).catch( err => {
    console.log(err)
    client.close()
})