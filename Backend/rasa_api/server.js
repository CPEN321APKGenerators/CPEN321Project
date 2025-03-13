require('dotenv').config();
const express = require('express');
const axios = require('axios');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

// RASA server URL 
const RASA_SERVER_URL = process.env.RASA_SERVER_URL || "http://ec2-54-234-28-190.compute-1.amazonaws.com:5005/webhooks/myio/webhook";

app.post('/api/chat', async (req, res) => {
    try {
        const { message, sender } = req.body;

        if (!message || !sender) {
            return res.status(400).json({ error: 'Message and sender are required' });
        }

        const response = await axios.post(RASA_SERVER_URL, { message, sender });

        return res.json(response.data);
    } catch (error) {
        console.error('Error connecting to RASA:', error);
        return res.status(500).json({ error: 'Failed to get response from RASA' });
    }
});

app.get('/api/health', (req, res) => {
    res.json({ status: 'Node.js API is running' });
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
    console.log(`Node.js API running on port ${PORT}`);
});
