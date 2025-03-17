const axios = require('axios');
const express = require('express');
const cors = require('cors');
const fs = require('fs');
const https = require('https');

// Initialize the express app
const app = express();
app.use(express.json());
app.use(cors());


let options = {};
let useHTTPS = false;

try {
    options = {
        key: fs.readFileSync("./ssl/key.pem"),
        cert: fs.readFileSync("./ssl/cert.pem"),
        secureOptions: require("crypto").constants.SSL_OP_NO_TLSv1 | require("crypto").constants.SSL_OP_NO_TLSv1_1,
        minVersion: "TLSv1.2"
    };
} catch (error) {
    console.warn("SSL files not found. Using HTTP instead.");
    useHTTPS = false;
}

// Server URLs for RASA API and Action Server
const RASA_SERVER_URL = process.env.RASA_SERVER_URL || "http://ec2-54-234-28-190.compute-1.amazonaws.com:5005/webhooks/myio/webhook";
const ACTION_SERVER_URL = process.env.ACTION_SERVER_URL || "http://ec2-54-234-28-190.compute-1.amazonaws.com:5055/webhook";

// Route to handle messages from frontend
app.post('/api/chat', async (req, res) => {
    try {
        const { message, sender } = req.body;

        if (!message || !sender) {
            return res.status(400).json({ error: 'Message and sender are required' });
        }

        const response = await axios.post(RASA_SERVER_URL, { message, sender });

        if (response.data && response.data.responses) {
            return res.status(200).json(response.data);  // Send 200 if RASA returns valid data
        } else {
            return res.status(500).json({ error: 'Invalid response from RASA' });
        }
    } catch (error) {
        return res.status(500).json({ error: 'Failed to get response from RASA' });
    }
});

// Route to trigger RASA actions
app.post('/api/action', async (req, res) => {
    try {
        const { sender, tracker, domain } = req.body;

        if (!sender) {
            return res.status(400).json({ error: 'Sender is required' });
        }

        const response = await axios.post(ACTION_SERVER_URL, { sender, tracker, domain });
        return res.status(200).json(response.data);
    } catch (error) {
        return res.status(500).json({ error: 'Failed to get response from RASA Action Server' });
    }
});

// Health check route
app.get('/api/health', (req, res) => {
    res.status(200).json({ status: 'Node.js API is running' });  
});

// Start server (use a fixed port)
const PORT = process.env.PORT || 3001;
let server;

if (!useHTTPS) {
    // HTTP
    server = app.listen(PORT, () => {
        console.log(`Node.js API running in HTTP mode on port ${PORT}`);
    });
} else {
    // HTTPS
    server = https.createServer(options, app).listen(PORT, () => {
        console.log(`Node.js API running with HTTPS on port ${PORT}`);
    });
}

module.exports = { app, server };
