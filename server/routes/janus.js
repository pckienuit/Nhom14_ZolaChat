// Janus API Routes for ZolaChat
// Manages video call rooms via Janus SFU

const express = require('express');
const router = express.Router();
const JanusHelper = require('../utils/JanusHelper');

// Initialize Janus Helper
const janus = new JanusHelper({
    httpUrl: process.env.JANUS_HTTP_URL || 'http://localhost:8088/janus',
    wsUrl: process.env.JANUS_WS_URL || 'ws://localhost:8188',
    apiSecret: process.env.JANUS_API_SECRET || 'ZolaChat2025SecretKey',
    adminSecret: process.env.JANUS_ADMIN_SECRET || 'ZolaChat2025AdminKey'
});

/**
 * POST /api/janus/room/create
 * Create a new video room for a call
 */
router.post('/room/create', async (req, res) => {
    try {
        const { callId, options } = req.body;

        if (!callId) {
            return res.status(400).json({
                success: false,
                error: 'callId is required'
            });
        }

        // Check if room already exists
        const existingRoom = await janus.getRoomInfo(callId);
        if (existingRoom.exists) {
            return res.json({
                success: true,
                message: 'Room already exists',
                roomId: callId
            });
        }

        // Create new room
        const result = await janus.createRoom(callId, options);

        res.json({
            success: true,
            ...result
        });
    } catch (error) {
        console.error('[API] Failed to create Janus room:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * POST /api/janus/room/destroy
 * Destroy a video room when call ends
 */
router.post('/room/destroy', async (req, res) => {
    try {
        const { callId, sessionId, handleId } = req.body;

        if (!callId) {
            return res.status(400).json({
                success: false,
                error: 'callId is required'
            });
        }

        await janus.destroyRoom(callId, sessionId, handleId);

        res.json({
            success: true,
            message: 'Room destroyed'
        });
    } catch (error) {
        console.error('[API] Failed to destroy Janus room:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * GET /api/janus/room/:callId
 * Get room information
 */
router.get('/room/:callId', async (req, res) => {
    try {
        const { callId } = req.params;

        const roomInfo = await janus.getRoomInfo(callId);

        res.json({
            success: true,
            ...roomInfo
        });
    } catch (error) {
        console.error('[API] Failed to get room info:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * GET /api/janus/room/:callId/participants
 * List participants in a room
 */
router.get('/room/:callId/participants', async (req, res) => {
    try {
        const { callId } = req.params;
        const { sessionId, handleId } = req.query;

        if (!sessionId || !handleId) {
            return res.status(400).json({
                success: false,
                error: 'sessionId and handleId are required'
            });
        }

        const participants = await janus.listParticipants(callId, sessionId, handleId);

        res.json({
            success: true,
            participants
        });
    } catch (error) {
        console.error('[API] Failed to list participants:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * GET /api/janus/health
 * Health check for Janus connection
 */
router.get('/health', async (req, res) => {
    try {
        // Try to create a test session
        const sessionId = await janus.createSession();
        
        res.json({
            success: true,
            message: 'Janus is reachable',
            sessionId
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: 'Janus is not reachable',
            details: error.message
        });
    }
});

module.exports = router;
